import java.io.File;
import java.io.FileNotFoundException;
import java.util.InputMismatchException;
import java.util.Scanner;

/**
 * Assignment 3 - CO3: Question 1
 * Handling Failure in a Bank ATM Simulation
 * 
 * Student Name: Korasika Sudheer
 * Register Number: 192324313
 * Course: Programming in Java (CSA09)
 */

// Custom Checked Exception for business domain rule violation
class InsufficientFundsException extends Exception {
    private final double deficit;
    public InsufficientFundsException(String message, double deficit) {
        super(message);
        this.deficit = deficit;
    }
    public double getDeficit() { return deficit; }
}

public class ATMWithdrawalSimulation {
    private static double accountBalance = 15000.00;
    private static final String CARD_READER_FILE = "card_reader_config.sys";

    /**
     * Executes an ATM withdrawal transaction demonstrating comprehensive exception handling.
     * 
     * @param accNumStr Input string for account number
     * @param amountStr Input string for withdrawal amount
     * @param simulateMissingHardware if true, simulates card reader hardware failure
     */
    public static void executeTransaction(String accNumStr, String amountStr, boolean simulateMissingHardware) {
        System.out.println("\n-----------------------------------------------------------");
        System.out.println("Processing Withdrawal: Acc=" + accNumStr + ", Amount=" + amountStr);
        boolean sessionOpen = false;

        try {
            // Check 1: Hardware Card Reader file presence (Checked Exception)
            if (simulateMissingHardware) {
                File cardReader = new File("non_existent_card_sensor.sys");
                if (!cardReader.exists()) {
                    throw new FileNotFoundException("Card reader sensor file missing or hardware disconnected.");
                }
            }
            sessionOpen = true;
            System.out.println("[AUDIT] Card authenticated. ATM transaction session established.");

            // Check 2: Parse Account Number (Unchecked NumberFormatException / InputMismatch)
            long accountNumber;
            try {
                accountNumber = Long.parseLong(accNumStr);
            } catch (NumberFormatException nfe) {
                throw new InputMismatchException("Invalid account number format: Alphabets or symbols detected.");
            }

            // Check 3: Parse and Validate Withdrawal Amount (Unchecked IllegalArgumentException)
            double amount;
            try {
                amount = Double.parseDouble(amountStr);
            } catch (NumberFormatException nfe) {
                throw new InputMismatchException("Invalid withdrawal amount: Must be numeric.");
            }

            if (amount <= 0) {
                throw new IllegalArgumentException("Withdrawal amount must be strictly positive (> 0). Provided: " + amount);
            }

            // Check 4: Business Rule - Balance verification (Custom Checked Exception)
            if (amount > accountBalance) {
                double shortfall = amount - accountBalance;
                throw new InsufficientFundsException("Transaction Declined: Insufficient balance in account.", shortfall);
            }

            // Normal Execution
            accountBalance -= amount;
            System.out.println("[SUCCESS] Cash Dispensed: Rs. " + amount);
            System.out.println("[INFO] Remaining Account Balance: Rs. " + accountBalance);

        } catch (InsufficientFundsException ife) {
            System.err.println("[CATCH - Domain Rule Checked Exception] " + ife.getMessage()
                    + " Shortfall deficit: Rs. " + ife.getDeficit());
        } catch (FileNotFoundException fnfe) {
            System.err.println("[CATCH - Hardware Checked Exception] " + fnfe.getMessage());
        } catch (InputMismatchException | IllegalArgumentException me) {
            // Multi-catch for invalid user input format/domain constraints
            System.err.println("[CATCH - Multi-Catch Unchecked Exception] Input Validation Failed: " + me.getMessage());
        } catch (Exception e) {
            // General fallback
            System.err.println("[CATCH - General Fallback] Unexpected error: " + e.getMessage());
        } finally {
            // Finally guarantees cleanup: card ejection and audit log closure
            if (sessionOpen) {
                System.out.println("[FINALLY] Session Terminated. Card ejected safely. Hardware bus reset.");
            } else {
                System.out.println("[FINALLY] Transaction aborted. ATM returned to standby mode.");
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("===========================================================");
        System.out.println("  BANK ATM SIMULATION - EXCEPTION HANDLING VERIFICATION  ");
        System.out.println("  Student: Korasika Sudheer | Register No: 192324313        ");
        System.out.println("===========================================================");

        // Test Input 1: Valid Transaction
        executeTransaction("192324313", "3500.00", false);

        // Test Input 2: Letters instead of numbers in account
        executeTransaction("ACC_ABC_INVALID", "2000.00", false);

        // Test Input 3: Negative withdrawal amount
        executeTransaction("192324313", "-500.00", false);

        // Test Input 4: Request exceeding balance
        executeTransaction("192324313", "50000.00", false);

        // Test Input 5: Card reader hardware missing
        executeTransaction("192324313", "1000.00", true);

        System.out.println("\n[SUMMARY] All 5 test paths executed with guaranteed finally cleanup.");
    }
}
