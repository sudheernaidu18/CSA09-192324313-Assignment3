import java.util.*;

/**
 * Assignment 3 - CO3: Question 4
 * Thread Creation and the Thread Life Cycle: Parallel Answer-Script Scanning
 * 
 * Student Name: Korasika Sudheer
 * Register Number: 192324313
 */

// Approach 1: Extending java.lang.Thread
class ScannerStationThread extends Thread {
    private final int stationId;
    private final int scriptBatchCount;
    private int scannedCount = 0;

    public ScannerStationThread(int stationId, int scriptBatchCount) {
        super("ScannerStation-Thread-" + stationId);
        this.stationId = stationId;
        this.scriptBatchCount = scriptBatchCount;
    }

    @Override
    public void run() {
        for (int i = 0; i < scriptBatchCount; i++) {
            scannedCount++;
            try {
                // Simulate physical optical scanner feed latency
                Thread.sleep(15);
            } catch (InterruptedException e) {
                System.err.println(getName() + " interrupted!");
                return;
            }
        }
        System.out.println("[" + getName() + "] Finished scanning " + scannedCount + " scripts. Thread State: " + getState());
    }

    public int getScannedCount() { return scannedCount; }
    public int getStationId() { return stationId; }
}

// Approach 2: Implementing java.lang.Runnable
class ScannerStationTask implements Runnable {
    private final int stationId;
    private final int scriptBatchCount;
    private int scannedCount = 0;

    public ScannerStationTask(int stationId, int scriptBatchCount) {
        this.stationId = stationId;
        this.scriptBatchCount = scriptBatchCount;
    }

    @Override
    public void run() {
        for (int i = 0; i < scriptBatchCount; i++) {
            scannedCount++;
            try {
                Thread.sleep(15);
            } catch (InterruptedException e) {
                return;
            }
        }
        System.out.println("[ScannerTask-" + stationId + " via " + Thread.currentThread().getName() +
                "] Finished scanning " + scannedCount + " scripts.");
    }

    public int getScannedCount() { return scannedCount; }
    public int getStationId() { return stationId; }
}

public class ParallelScannerSimulation {

    public static List<Integer> runBatchSimulation(boolean setPriorities) throws InterruptedException {
        final int NUM_STATIONS = 4;
        final int BATCH_SIZE = 50;
        List<Integer> finishOrder = Collections.synchronizedList(new ArrayList<>());

        Thread[] stations = new Thread[NUM_STATIONS];
        for (int i = 0; i < NUM_STATIONS; i++) {
            final int id = i + 1;
            stations[i] = new Thread(() -> {
                for (int s = 0; s < BATCH_SIZE; s++) {
                    try { Thread.sleep(5); } catch (InterruptedException ignored) {}
                }
                finishOrder.add(id);
            }, "Station-" + id);

            if (setPriorities) {
                // Station 1: MIN, Station 4: MAX
                int priority = (i == 3) ? Thread.MAX_PRIORITY : ((i == 0) ? Thread.MIN_PRIORITY : Thread.NORM_PRIORITY);
                stations[i].setPriority(priority);
            }
        }

        // Verify NEW state
        // System.out.println("Before start: " + stations[0].getName() + " state = " + stations[0].getState());

        for (Thread t : stations) t.start();

        // Verify isAlive() and TIMED_WAITING
        Thread.sleep(10);
        // System.out.println("During run: " + stations[0].getName() + " isAlive=" + stations[0].isAlive());

        // Wait for all to finish with join()
        for (Thread t : stations) t.join();

        return finishOrder;
    }

    public static void testDaemonBehavior() throws InterruptedException {
        System.out.println("\n--- [EXPERIMENT] Daemon Thread Verification ---");
        Thread daemonScanner = new Thread(() -> {
            try {
                System.out.println("[DAEMON STATION] Background audit scanner started...");
                for (int i = 1; i <= 10; i++) {
                    Thread.sleep(20);
                    System.out.println("[DAEMON STATION] Scanned batch heartbeat #" + i);
                }
            } catch (InterruptedException ignored) {}
        });

        daemonScanner.setDaemon(true);
        daemonScanner.start();

        // Main thread does a tiny amount of work and terminates without joining daemon
        Thread.sleep(35);
        System.out.println("[MAIN] Main thread exiting. Watch JVM terminate daemon without finishing 10 batches.");
    }

    public static void testRunVsStart() {
        System.out.println("\n--- [PROOF] Calling run() vs start() ---");
        Thread testThread = new Thread(() -> {
            System.out.println("Inside worker: Thread Name = " + Thread.currentThread().getName() +
                    " (Is Daemon=" + Thread.currentThread().isDaemon() + ")");
        }, "WorkerThread-X");

        System.out.println("1. Invoking run() directly:");
        testThread.run(); // Synchronous call on main thread

        System.out.println("2. Invoking start():");
        testThread.start(); // Spawns new OS thread
        try { testThread.join(); } catch (InterruptedException ignored) {}
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("===========================================================");
        System.out.println("  PARALLEL SCANNER: THREAD LIFECYCLE & SCHEDULING        ");
        System.out.println("  Student: Korasika Sudheer | Register No: 192324313        ");
        System.out.println("===========================================================");

        // Part 1: Thread vs Runnable Execution
        System.out.println("\n--- Part A: Executing via Extending Thread ---");
        ScannerStationThread t1 = new ScannerStationThread(1, 10);
        ScannerStationThread t2 = new ScannerStationThread(2, 10);
        t1.start(); t2.start();
        t1.join(); t2.join();

        System.out.println("\n--- Part B: Executing via Implementing Runnable ---");
        ScannerStationTask task3 = new ScannerStationTask(3, 10);
        ScannerStationTask task4 = new ScannerStationTask(4, 10);
        Thread rt3 = new Thread(task3, "Scanner-3");
        Thread rt4 = new Thread(task4, "Scanner-4");
        rt3.start(); rt4.start();
        rt3.join(); rt4.join();

        // Part 2: 10 Runs Order Recording
        System.out.println("\n--- Part C: 10 Runs Finish Order Non-Determinism Table ---");
        for (int run = 1; run <= 10; run++) {
            List<Integer> order = runBatchSimulation(false);
            System.out.println("Run #" + String.format("%02d", run) + " Finish Sequence: " + order);
        }

        // Part 3: Priorities
        System.out.println("\n--- Part D: Priority Bias Experiment (Station 4=MAX, Station 1=MIN) ---");
        for (int run = 1; run <= 3; run++) {
            List<Integer> order = runBatchSimulation(true);
            System.out.println("Priority Run #" + run + " Sequence: " + order);
        }

        // Part 4: run() vs start()
        testRunVsStart();

        // Part 5: Daemon thread
        testDaemonBehavior();
    }
}
