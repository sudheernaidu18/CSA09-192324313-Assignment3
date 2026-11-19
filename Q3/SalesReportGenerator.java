import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Assignment 3 - CO3: Question 3
 * Resource Safety, Debugging, and Robust Code
 * 
 * Student Name: Korasika Sudheer
 * Register Number: 192324313
 */

public class SalesReportGenerator {

    /**
     * Flawed Implementation: Manual try-catch-finally
     * Demonstrates leak when one file fails or throws during close.
     */
    public static int generateReportManual(List<File> storeFiles, File summaryFile) {
        int openFileHandles = 0;
        BufferedWriter writer = null;
        List<BufferedReader> openedReaders = new ArrayList<>();

        System.out.println("\n[MANUAL VERSION] Processing " + storeFiles.size() + " files...");
        try {
            writer = new BufferedWriter(new FileWriter(summaryFile));
            openFileHandles++; // 1 for writer

            double totalSales = 0.0;
            for (File storeFile : storeFiles) {
                BufferedReader reader = new BufferedReader(new FileReader(storeFile));
                openedReaders.add(reader);
                openFileHandles++;

                String line = reader.readLine();
                if (line == null) throw new IOException("Empty file detected: " + storeFile.getName());

                // Simulated error injection on corrupt store file
                if (storeFile.getName().contains("corrupt")) {
                    System.err.println("[LOG ERROR] Parsing failure in store file: " + storeFile.getName());
                    throw new NumberFormatException("Corrupted sales record token: 'ERR_DATA_NULL'");
                }

                double sales = Double.parseDouble(line.trim());
                totalSales += sales;
                writer.write("Store [" + storeFile.getName() + "]: Rs. " + sales);
                writer.newLine();

                // If exception occurs before manual close, reader is leaked!
                reader.close();
                openedReaders.remove(reader);
                openFileHandles--;
            }

            writer.write("TOTAL SALES: Rs. " + totalSales);
            writer.newLine();
            System.out.println("[MANUAL SUCCESS] Report generated successfully.");

        } catch (Exception e) {
            System.err.println("[MANUAL EXCEPTION CAUGHT] " + e.getClass().getSimpleName() + ": " + e.getMessage());
        } finally {
            // Flawed manual finally block: if writer.close() fails, remaining readers are leaked
            try {
                if (writer != null) {
                    writer.close();
                    openFileHandles--;
                }
            } catch (IOException ioe) {
                System.err.println("Error closing writer: " + ioe.getMessage());
            }
            // Notice: openedReaders were NOT closed because exception happened before their close() call!
        }

        System.out.println("[AUDIT MANUAL] Open File Handles Remaining Unclosed: " + openFileHandles);
        // Clean up remaining open readers for JVM safety
        for (BufferedReader r : openedReaders) {
            try { r.close(); } catch (Exception ignored) {}
        }
        return openFileHandles;
    }

    /**
     * Robust Implementation: Modern try-with-resources
     * Guarantees all open file handles are deterministically closed in reverse order,
     * capturing any suppressed exceptions.
     */
    public static int generateReportSafe(List<File> storeFiles, File summaryFile) {
        int openFileHandles = 0;
        System.out.println("\n[SAFE VERSION - try-with-resources] Processing " + storeFiles.size() + " files...");

        try (BufferedWriter writer = Files.newBufferedWriter(summaryFile.toPath())) {
            double totalSales = 0.0;

            for (File storeFile : storeFiles) {
                // Each store file reader scoped strictly in try-with-resources
                try (BufferedReader reader = Files.newBufferedReader(storeFile.toPath())) {
                    String line = reader.readLine();
                    if (line == null) throw new IOException("Empty file: " + storeFile.getName());

                    if (storeFile.getName().contains("corrupt")) {
                        System.err.println("[LOG ERROR] Detected corrupted data in: " + storeFile.getName());
                        throw new NumberFormatException("Corrupted sales token: 'ERR_DATA_NULL'");
                    }

                    double sales = Double.parseDouble(line.trim());
                    totalSales += sales;
                    writer.write("Store [" + storeFile.getName() + "]: Rs. " + sales + "\n");
                } catch (NumberFormatException nfe) {
                    System.err.println("[SAFE RECOVERY] Logged corrupt file '" + storeFile.getName() +
                            "'. Resource closed automatically. Continuing execution.");
                }
            }

            writer.write("TOTAL SALES: Rs. " + totalSales + "\n");
            System.out.println("[SAFE SUCCESS] Report completed with zero resource leaks.");

        } catch (IOException e) {
            System.err.println("[SAFE I/O EXCEPTION] " + e.getMessage());
            for (Throwable suppressed : e.getSuppressed()) {
                System.err.println("  ↳ Suppressed Exception during close: " + suppressed.getMessage());
            }
        }

        // try-with-resources guarantees zero open handles remaining
        System.out.println("[AUDIT SAFE] Open File Handles Remaining: " + openFileHandles);
        return openFileHandles;
    }

    public static void main(String[] args) throws IOException {
        System.out.println("===========================================================");
        System.out.println("  RESOURCE SAFETY & TRY-WITH-RESOURCES INVESTIGATION     ");
        System.out.println("  Student: Korasika Sudheer | Register No: 192324313        ");
        System.out.println("===========================================================");

        Path tempDir = Files.createTempDirectory("sales_investigation_");
        List<File> testFiles = new ArrayList<>();

        // Create 5 sample store files, file 3 is deliberately corrupted
        for (int i = 1; i <= 5; i++) {
            File f = new File(tempDir.toFile(), (i == 3 ? "store_03_corrupt.txt" : "store_0" + i + ".txt"));
            Files.writeString(f.toPath(), (i == 3 ? "ERR_DATA_NULL\n" : String.valueOf(1000.0 * i) + "\n"));
            testFiles.add(f);
        }

        File manualSummary = new File(tempDir.toFile(), "summary_manual.txt");
        File safeSummary = new File(tempDir.toFile(), "summary_safe.txt");

        // Test 1: Run Manual Version
        int leakedManual = generateReportManual(testFiles, manualSummary);

        // Test 2: Run Safe Version
        int leakedSafe = generateReportSafe(testFiles, safeSummary);

        System.out.println("\n-----------------------------------------------------------");
        System.out.println("COMPARISON SUMMARY:");
        System.out.println("Manual version file descriptor leaks on failure: " + leakedManual);
        System.out.println("Safe try-with-resources file descriptor leaks:   " + leakedSafe);
        System.out.println("-----------------------------------------------------------");

        // Clean up temp files
        for (File f : testFiles) f.delete();
        manualSummary.delete();
        safeSummary.delete();
        tempDir.toFile().delete();
    }
}
