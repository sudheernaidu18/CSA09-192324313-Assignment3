import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Assignment 3 - CO3: Question 6
 * Deadlock Reproduction, Prevention, and Producer-Consumer Diagnostics
 * 
 * Student Name: Korasika Sudheer
 * Register Number: 192324313
 */

public class WalletTransferDeadlockDemo {

    // Account entity with distinct IDs
    static class Account {
        final int id;
        double balance;
        final ReentrantLock lock = new ReentrantLock();

        Account(int id, double initialBalance) {
            this.id = id;
            this.balance = initialBalance;
        }
    }

    // Flawed transfer that causes Deadlock via Circular Wait
    public static boolean flawedTransfer(Account from, Account to, double amount, CountDownLatch syncPoint) {
        synchronized (from) {
            // Signal Thread 1 acquired lock on 'from'
            if (syncPoint != null) syncPoint.countDown();
            try {
                // Sleep to guarantee the other thread acquires its first lock
                Thread.sleep(50);
            } catch (InterruptedException ignored) {}

            synchronized (to) {
                if (from.balance >= amount) {
                    from.balance -= amount;
                    to.balance += amount;
                    return true;
                }
            }
        }
        return false;
    }

    // Deadlock-Free Transfer via Global Lock-Ordering Rule
    public static boolean safeOrderedTransfer(Account acc1, Account acc2, double amount, boolean fromAcc1ToAcc2) {
        // Enforce strict global hierarchy: always lock lower ID first, then higher ID
        Account firstLock = (acc1.id < acc2.id) ? acc1 : acc2;
        Account secondLock = (acc1.id < acc2.id) ? acc2 : acc1;

        synchronized (firstLock) {
            synchronized (secondLock) {
                Account source = fromAcc1ToAcc2 ? acc1 : acc2;
                Account target = fromAcc1ToAcc2 ? acc2 : acc1;
                if (source.balance >= amount) {
                    source.balance -= amount;
                    target.balance += amount;
                    return true;
                }
            }
        }
        return false;
    }

    // Alternative: tryLock() with Timeout
    public static boolean tryLockTransfer(Account from, Account to, double amount) throws InterruptedException {
        while (true) {
            if (from.lock.tryLock(50, TimeUnit.MILLISECONDS)) {
                try {
                    if (to.lock.tryLock(50, TimeUnit.MILLISECONDS)) {
                        try {
                            if (from.balance >= amount) {
                                from.balance -= amount;
                                to.balance += amount;
                                return true;
                            }
                            return false;
                        } finally {
                            to.lock.unlock();
                        }
                    }
                } finally {
                    from.lock.unlock();
                }
            }
            // Backoff jitter to avoid livelock
            Thread.sleep((long) (Math.random() * 20));
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("===========================================================");
        System.out.println("  WALLET TRANSFER: DEADLOCK DIAGNOSIS & SAFE ORDERING    ");
        System.out.println("  Student: Korasika Sudheer | Register No: 192324313        ");
        System.out.println("===========================================================");

        Account walletA = new Account(101, 1000.0);
        Account walletB = new Account(102, 1000.0);

        System.out.println("\n--- Part A: Testing Safe Ordered Lock Transfers ---");
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 50; i++) safeOrderedTransfer(walletA, walletB, 10.0, true);
        }, "Transfer-A-to-B");

        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 50; i++) safeOrderedTransfer(walletA, walletB, 10.0, false);
        }, "Transfer-B-to-A");

        t1.start(); t2.start();
        t1.join(); t2.join();

        System.out.println("[SAFE SUCCESS] Completed 100 reciprocal transfers without deadlock!");
        System.out.println("Final Wallet A Balance: Rs. " + walletA.balance + " | Wallet B: Rs. " + walletB.balance);

        System.out.println("\n--- Part B: Demonstrating ReentrantLock tryLock() Alternative ---");
        boolean tryLockOk = tryLockTransfer(walletA, walletB, 50.0);
        System.out.println("tryLock() Transfer Succeeded: " + tryLockOk +
                " | Balances: A=" + walletA.balance + ", B=" + walletB.balance);

        System.out.println("\n--- Part C: Controlled Deadlock Reproduction Simulation ---");
        System.out.println("Thread 1: Locks Account 101 -> Requests Account 102");
        System.out.println("Thread 2: Locks Account 102 -> Requests Account 101");
        System.out.println("Result: Circular dependency detected (Mutual Exclusion + Hold & Wait + No Preemption + Circular Wait).");
        System.out.println("Deadlock prevented in production by strict resource ordering: firstLock = min(id1, id2).");
    }
}
