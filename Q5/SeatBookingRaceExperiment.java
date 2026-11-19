import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Assignment 3 - CO3: Question 5
 * Cinema Seat Booking Race Condition Experiment & Benchmark
 * 
 * Student Name: Korasika Sudheer
 * Register Number: 192324313
 */

public class SeatBookingRaceExperiment {

    // 1. Unsynchronized Flawed Implementation
    static class UnsynchronizedBooking {
        final boolean[] booked;
        int bookingsMade = 0;
        UnsynchronizedBooking(int seats) { booked = new boolean[seats]; }

        public boolean book(int seat) {
            if (!booked[seat]) {
                // Yield/small delay to widen the race window
                try { Thread.sleep(1); } catch (InterruptedException ignored) {}
                booked[seat] = true;
                bookingsMade++;
                return true;
            }
            return false;
        }
    }

    // 2. Synchronized Method Implementation
    static class SynchronizedMethodBooking {
        final boolean[] booked;
        int bookingsMade = 0;
        SynchronizedMethodBooking(int seats) { booked = new boolean[seats]; }

        public synchronized boolean book(int seat) {
            if (!booked[seat]) {
                booked[seat] = true;
                bookingsMade++;
                return true;
            }
            return false;
        }
    }

    // 3. Fine-Grained Synchronized Block (Per-Seat Lock)
    static class FineGrainedBlockBooking {
        final boolean[] booked;
        final Object[] locks;
        int bookingsMade = 0;
        final Object countLock = new Object();

        FineGrainedBlockBooking(int seats) {
            booked = new boolean[seats];
            locks = new Object[seats];
            for (int i = 0; i < seats; i++) locks[i] = new Object();
        }

        public boolean book(int seat) {
            synchronized (locks[seat]) {
                if (!booked[seat]) {
                    booked[seat] = true;
                    synchronized (countLock) { bookingsMade++; }
                    return true;
                }
            }
            return false;
        }
    }

    // 4. Lock-Free AtomicBoolean Implementation
    static class AtomicBooking {
        final AtomicBoolean[] booked;
        final AtomicInteger bookingsMade = new AtomicInteger(0);

        AtomicBooking(int seats) {
            booked = new AtomicBoolean[seats];
            for (int i = 0; i < seats; i++) booked[i] = new AtomicBoolean(false);
        }

        public boolean book(int seat) {
            if (booked[seat].compareAndSet(false, true)) {
                bookingsMade.incrementAndGet();
                return true;
            }
            return false;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("===========================================================");
        System.out.println("  CINEMA RACE CONDITION FORENSICS & BENCHMARK SUITE     ");
        System.out.println("  Student: Korasika Sudheer | Register No: 192324313        ");
        System.out.println("===========================================================");

        // Test 1: 10 Runs of Unsynchronized Version
        System.out.println("\n--- Experiment A: 10 Runs of Unsynchronized Booking (Target = 10 seats) ---");
        int doubleBookingCount = 0;
        for (int run = 1; run <= 10; run++) {
            UnsynchronizedBooking ub = new UnsynchronizedBooking(10);
            Thread[] threads = new Thread[100];
            for (int i = 0; i < 100; i++) {
                final int seat = i % 10;
                threads[i] = new Thread(() -> ub.book(seat));
                threads[i].start();
            }
            for (Thread t : threads) t.join();

            boolean anomaly = ub.bookingsMade > 10;
            if (anomaly) doubleBookingCount++;
            System.out.println("Run #" + String.format("%02d", run) + " -> Seats Sold: " + ub.bookingsMade +
                    (anomaly ? " [CRITICAL RACE ANOMALY: Double Booking Detected!]" : " [Passed]"));
        }
        System.out.println("Total Anomaly Runs with Double Bookings: " + doubleBookingCount + " / 10");

        // Test 2: Synchronized Method Verification (10 Runs)
        System.out.println("\n--- Experiment B: 10 Runs of Synchronized Method Booking ---");
        boolean allPassed = true;
        for (int run = 1; run <= 10; run++) {
            SynchronizedMethodBooking smb = new SynchronizedMethodBooking(10);
            Thread[] threads = new Thread[100];
            for (int i = 0; i < 100; i++) {
                final int seat = i % 10;
                threads[i] = new Thread(() -> smb.book(seat));
                threads[i].start();
            }
            for (Thread t : threads) t.join();
            if (smb.bookingsMade != 10) allPassed = false;
        }
        System.out.println("Synchronized Method 10-Run Result: Exactly 10 seats sold on every run = " + allPassed);

        // Test 3: High-Contention Benchmark (10,000 Bookings)
        System.out.println("\n--- Experiment C: High-Contention Benchmark (10,000 Booking Requests) ---");
        final int BENCHMARK_OPS = 10000;
        final int BENCHMARK_SEATS = 100;

        // 1. Synchronized Method
        SynchronizedMethodBooking bmSync = new SynchronizedMethodBooking(BENCHMARK_SEATS);
        long start = System.nanoTime();
        Thread[] thSync = new Thread[BENCHMARK_OPS];
        for (int i = 0; i < BENCHMARK_OPS; i++) {
            final int seat = i % BENCHMARK_SEATS;
            thSync[i] = new Thread(() -> bmSync.book(seat));
            thSync[i].start();
        }
        for (Thread t : thSync) t.join();
        long syncTimeMs = (System.nanoTime() - start) / 1_000_000;

        // 2. Fine-Grained Per-Seat Lock
        FineGrainedBlockBooking bmBlock = new FineGrainedBlockBooking(BENCHMARK_SEATS);
        start = System.nanoTime();
        Thread[] thBlock = new Thread[BENCHMARK_OPS];
        for (int i = 0; i < BENCHMARK_OPS; i++) {
            final int seat = i % BENCHMARK_SEATS;
            thBlock[i] = new Thread(() -> bmBlock.book(seat));
            thBlock[i].start();
        }
        for (Thread t : thBlock) t.join();
        long blockTimeMs = (System.nanoTime() - start) / 1_000_000;

        // 3. AtomicBoolean
        AtomicBooking bmAtomic = new AtomicBooking(BENCHMARK_SEATS);
        start = System.nanoTime();
        Thread[] thAtomic = new Thread[BENCHMARK_OPS];
        for (int i = 0; i < BENCHMARK_OPS; i++) {
            final int seat = i % BENCHMARK_SEATS;
            thAtomic[i] = new Thread(() -> bmAtomic.book(seat));
            thAtomic[i].start();
        }
        for (Thread t : thAtomic) t.join();
        long atomicTimeMs = (System.nanoTime() - start) / 1_000_000;

        System.out.println("Benchmark Results for 10,000 Threads:");
        System.out.println("  1. Synchronized Method (Coarse Lock):      " + syncTimeMs + " ms | Seats Sold: " + bmSync.bookingsMade);
        System.out.println("  2. Fine-Grained Synchronized Block:        " + blockTimeMs + " ms | Seats Sold: " + bmBlock.bookingsMade);
        System.out.println("  3. Lock-Free AtomicBoolean.compareAndSet:  " + atomicTimeMs + " ms | Seats Sold: " + bmAtomic.bookingsMade.get());
    }
}
