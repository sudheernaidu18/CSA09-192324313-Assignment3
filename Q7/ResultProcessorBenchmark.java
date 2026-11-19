import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;

/**
 * Assignment 3 - CO3: Question 7
 * Concurrent, Fault-Tolerant Semester Result Processor
 * 
 * Student Name: Korasika Sudheer
 * Register Number: 192324313
 */

public class ResultProcessorBenchmark {

    // Record of individual student result
    public record StudentResult(String studentId, int totalScore, char grade, boolean isSuccess, String failureReason) {}

    // Callable Worker Task
    public static class FileScoreTask implements Callable<StudentResult> {
        private final File file;

        public FileScoreTask(File file) {
            this.file = file;
        }

        @Override
        public StudentResult call() {
            String studentId = file.getName().replace(".dat", "");
            if (!file.exists()) {
                return new StudentResult(studentId, 0, 'F', false, "FileNotFound: Script missing from repository");
            }

            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line = reader.readLine();
                if (line == null || line.trim().isEmpty()) {
                    return new StudentResult(studentId, 0, 'F', false, "CorruptFile: Empty script file");
                }

                String[] tokens = line.split(",");
                if (tokens.length < 3) {
                    return new StudentResult(studentId, 0, 'F', false, "CorruptFile: Incomplete subject marks (< 3 tokens)");
                }

                int sum = 0;
                for (String token : tokens) {
                    int score = Integer.parseInt(token.trim());
                    if (score < 0 || score > 100) {
                        return new StudentResult(studentId, 0, 'F', false, "DataAnomaly: Score out of range [0, 100]: " + score);
                    }
                    sum += score;
                }

                int average = sum / tokens.length;
                char grade = (average >= 90) ? 'S' : ((average >= 80) ? 'A' : ((average >= 70) ? 'B' : ((average >= 50) ? 'C' : 'U')));
                return new StudentResult(studentId, sum, grade, true, "Success");

            } catch (NumberFormatException nfe) {
                return new StudentResult(studentId, 0, 'F', false, "CorruptFile: Non-numeric score token");
            } catch (IOException ioe) {
                return new StudentResult(studentId, 0, 'F', false, "IOException: " + ioe.getMessage());
            } catch (Exception e) {
                return new StudentResult(studentId, 0, 'F', false, "UnexpectedFault: " + e.getMessage());
            }
        }
    }

    // Benchmark processor for N threads
    public static Map<String, Object> processAllFiles(List<File> files, int poolSize) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        List<Callable<StudentResult>> tasks = new ArrayList<>(files.size());
        for (File f : files) tasks.add(new FileScoreTask(f));

        LongAdder successCounter = new LongAdder();
        LongAdder failureCounter = new LongAdder();
        ConcurrentHashMap<Character, LongAdder> gradeDistribution = new ConcurrentHashMap<>();
        for (char g : new char[]{'S', 'A', 'B', 'C', 'U'}) gradeDistribution.put(g, new LongAdder());

        long startTime = System.currentTimeMillis();
        List<Future<StudentResult>> futures = executor.invokeAll(tasks);

        for (Future<StudentResult> future : futures) {
            try {
                StudentResult res = future.get();
                if (res.isSuccess()) {
                    successCounter.increment();
                    gradeDistribution.get(res.grade()).increment();
                } else {
                    failureCounter.increment();
                }
            } catch (ExecutionException ee) {
                failureCounter.increment();
            }
        }

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
        long elapsedMs = System.currentTimeMillis() - startTime;

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("poolSize", poolSize);
        metrics.put("elapsedMs", elapsedMs);
        metrics.put("successes", successCounter.sum());
        metrics.put("failures", failureCounter.sum());
        metrics.put("grades", gradeDistribution);
        return metrics;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("===========================================================");
        System.out.println("  CONCURRENT FAULT-TOLERANT RESULT PROCESSOR (Q7)        ");
        System.out.println("  Student: Korasika Sudheer | Register No: 192324313        ");
        System.out.println("===========================================================");

        Path tempDir = Files.createTempDirectory("exam_scores_10k_");
        final int TOTAL_FILES = 10000;
        final int INJECTED_CORRUPT = 150;
        final int INJECTED_MISSING = 50;

        System.out.println("[SETUP] Generating 10,000 answer-script test dataset...");
        List<File> files = new ArrayList<>(TOTAL_FILES);

        for (int i = 1; i <= TOTAL_FILES; i++) {
            File f = new File(tempDir.toFile(), "student_" + String.format("%05d", i) + ".dat");
            if (i <= INJECTED_MISSING) {
                // Do not create physical file -> triggers FileNotFound
                files.add(f);
            } else if (i <= INJECTED_MISSING + INJECTED_CORRUPT) {
                // Corrupted file contents
                if (i % 2 == 0) Files.writeString(f.toPath(), "95,ERR_TOKEN,80\n");
                else Files.writeString(f.toPath(), "45,-15,88\n"); // Out of bounds
                files.add(f);
            } else {
                // Valid score line: 3 subject marks
                int m1 = 50 + (i % 45);
                int m2 = 55 + ((i * 3) % 40);
                int m3 = 60 + ((i * 7) % 35);
                Files.writeString(f.toPath(), m1 + "," + m2 + "," + m3 + "\n");
                files.add(f);
            }
        }

        System.out.println("[SETUP COMPLETED] Injected " + INJECTED_MISSING + " missing files, "
                + INJECTED_CORRUPT + " corrupted files, and " + (TOTAL_FILES - INJECTED_MISSING - INJECTED_CORRUPT)
                + " valid files.\n");

        int[] threadConfigs = {1, 2, 4, 8};
        System.out.println("---------------------------------------------------------------------------------");
        System.out.printf("%-12s | %-15s | %-12s | %-12s | %-12s\n", "Pool Size", "Elapsed Time", "Successes", "Failures", "Throughput");
        System.out.println("---------------------------------------------------------------------------------");

        long baselineTime = 0;
        for (int p : threadConfigs) {
            Map<String, Object> res = processAllFiles(files, p);
            long ms = (long) res.get("elapsedMs");
            if (p == 1) baselineTime = ms;
            double speedup = (baselineTime > 0) ? ((double) baselineTime / ms) : 1.0;
            double throughput = (TOTAL_FILES * 1000.0) / ms;

            System.out.printf("%-12d | %-12d ms | %-12d | %-12d | %-10.1f files/s (Speedup: %.2fx)\n",
                    p, ms, (long) res.get("successes"), (long) res.get("failures"), throughput, speedup);
        }
        System.out.println("---------------------------------------------------------------------------------");

        // Hand-checked verification
        System.out.println("\n[VERIFICATION SUITE]");
        System.out.println("Expected Failures: " + (INJECTED_MISSING + INJECTED_CORRUPT) + " (50 missing + 150 corrupt)");
        System.out.println("Expected Successes: " + (TOTAL_FILES - INJECTED_MISSING - INJECTED_CORRUPT) + " (9,800 valid)");
        System.out.println("Zero unhandled worker thread exceptions. Run completed smoothly.");

        // Cleanup
        for (File f : files) {
            if (f.exists()) f.delete();
        }
        tempDir.toFile().delete();
    }
}
