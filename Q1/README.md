# Question 1: Handling Failure in a Bank ATM Simulation

**Student Name**: Korasika Sudheer  
**Register Number**: 192324313  
**Course**: Programming in Java (CSA09) – CO3  
**Marks**: 6 Marks | **Bloom's Level**: K4 – Analyse  

---

## 1. Problem Context
An ATM system processes cash withdrawals by reading account credentials, withdrawal amounts, checking account balances, and interfacing with card-reader hardware. Runtime failures can stem from invalid format inputs (typing letters), negative amounts, insufficient funds, or missing hardware devices.

---

## 2. Student A – Exception Hierarchy & Architecture Design

### 2.1 Relevant Throwable Hierarchy Diagram

```
                 java.lang.Throwable
                         ▲
         ┌───────────────┴───────────────┐
         │                               │
  java.lang.Error             java.lang.Exception
  (VirtualMachineError,                  ▲
   OutOfMemoryError)          ┌──────────┴──────────┐
                              │                     │
                     java.io.IOException      java.lang.RuntimeException
                              ▲                 (Unchecked Exceptions)
                              │                     ▲
                   java.io.FileNotFoundException    ├── java.lang.NumberFormatException
                              ▲                     ├── java.util.InputMismatchException
                              │                     └── java.lang.IllegalArgumentException
                  InsufficientFundsException
                      (Checked Domain Rule)
```

### 2.2 Failure Points Classification Table

| Failure Point | Exception Type | Checked / Unchecked | Where Caught & Handled | User Experience / Impact |
|---|---|---|---|---|
| Card reader file missing / hardware unplugged | `FileNotFoundException` | **Checked** | Explicit `catch (FileNotFoundException)` | User sees *"Hardware offline, card ejected safely"*; no cash deducted. |
| User enters letters for account number | `InputMismatchException` / `NumberFormatException` | **Unchecked** | Multi-catch `catch (InputMismatchException \| ...)` | Prompts user *"Invalid account format. Enter digits only"*. |
| Negative withdrawal amount (e.g., -500) | `IllegalArgumentException` | **Unchecked** | Multi-catch block | Rejects request; prevents balance credit loophole. |
| Withdrawal amount exceeds balance | `InsufficientFundsException` | **Checked** (User-defined) | Dedicated domain catch block | Explains exact deficit shortfall to the user. |
| Unexpected JVM/hardware crash | `Exception` / `Error` | Fallback | `catch (Exception e)` / System handler | Clean teardown without leaving transaction hung. |

### 2.3 Justification for Catch Block Ordering
In Java, catch blocks must be organized from **most specific (subclasses)** to **most generic (superclasses)**:
1. `InsufficientFundsException` and `FileNotFoundException` are handled first to execute tailored recovery (e.g., reporting deficit or hardware reset).
2. `InputMismatchException | IllegalArgumentException` uses Java multi-catch for non-fatal client validation errors.
3. `Exception` is placed last as a safety net. If `Exception` were placed first, the Java compiler would emit an unreachable catch block error.

### 2.4 What the `finally` Block Guarantees
The `finally` block guarantees that critical hardware and session resources are closed regardless of whether the transaction succeeded, caught an exception, or threw an unhandled error. In the ATM, this guarantees:
- The ATM card is ejected and not swallowed in the reader.
- In-memory cryptographic session keys are scrubbed.
- The ATM returns to an idle, receptive state for the next customer.

---

## 3. Student B – Testing, Tracing & Defect Demonstration

### 3.1 Five Designed Test Scenarios

| Test Case | Account Input | Amount Input | Simulated Hardware | Catch Block Triggered | Finally Executed? | Status |
|---|---|---|---|---|---|---|
| TC-01 (Normal) | `192324313` | `3500.00` | Normal (`false`) | None (Success path) | Yes (`[FINALLY] Session Terminated`) | PASSED |
| TC-02 (Letters) | `ACC_ABC_INVALID`| `2000.00` | Normal (`false`) | `InputMismatchException` | Yes (`[FINALLY] Session Terminated`) | PASSED |
| TC-03 (Negative) | `192324313` | `-500.00` | Normal (`false`) | `IllegalArgumentException` | Yes (`[FINALLY] Session Terminated`) | PASSED |
| TC-04 (Overdraft)| `192324313` | `50000.00` | Normal (`false`) | `InsufficientFundsException` | Yes (`[FINALLY] Session Terminated`) | PASSED |
| TC-05 (HW Fault) | `192324313` | `1000.00` | Missing (`true`) | `FileNotFoundException` | Yes (`[FINALLY] Transaction aborted`) | PASSED |

### 3.2 Uncaught Stack Trace Analysis
If `InsufficientFundsException` is left uncaught, the JVM outputs:
```
Exception in thread "main" InsufficientFundsException: Transaction Declined: Insufficient balance in account. Shortfall deficit: Rs. 38500.0
    at ATMWithdrawalSimulation.executeTransaction(ATMWithdrawalSimulation.java:65)
    at ATMWithdrawalSimulation.main(ATMWithdrawalSimulation.java:108)
```
- **Line 1**: Shows the thread name (`main`), exception class name, and detailed error message with shortfall.
- **Line 2**: Points to the exact method and line (`executeTransaction:65`) where `throw new InsufficientFundsException` was triggered.
- **Line 3**: Identifies the caller in the call stack (`main:108`) that initiated the execution.

### 3.3 Broader Catch Placed Before Narrower Catch
Placing `catch (Exception e)` before `catch (InsufficientFundsException ife)` fails compilation:
```text
ATMWithdrawalSimulation.java:75: error: exception InsufficientFundsException has already been caught
        } catch (InsufficientFundsException ife) {
          ^
1 error
```
The Java compiler enforces the unreachable code rule (JLS §14.20).

---

## 4. Joint Design Question & Defense
**Should the ATM program catch every exception with a single `catch (Exception e)`?**
- **Verdict**: Strictly unacceptable for production banking software.
- **What is lost**:
  1. *Granular Recovery*: Hardware failures require device resets, while invalid amounts require prompting the user to re-enter. Catching `Exception` blurs user typos with critical hardware faults.
  2. *Security & Auditing*: Compliance logs cannot distinguish between potential fraud/overdraft attacks and simple malformed input.
- **Acceptable Condition**: Only as an ultimate fallback at the outermost boundary of the application loop (after all specific catches) to log unhandled runtime faults and prevent the JVM from crashing.

---

## 5. Alternative Concept and Complexity Analysis
- **Alternative**: Replacing exceptions with pre-checks (`Scanner.hasNextInt()`, range checks, and returning status codes like `-1`, `-2`).
- **Readability & Maintenance**: Error codes require manual cascading if-else checks across every method call, polluting core business logic with defensive branches.
- **Time Complexity**:
  - *Error Codes*: $O(1)$ constant integer comparison.
  - *Exceptions*: Normal path has zero overhead ($O(1)$). Exception throw incurs $O(d)$ time to capture the thread stack trace where $d$ is call stack depth (~15–30 frames).
- **Space Complexity**:
  - *Error Codes*: $O(1)$ primitive return value.
  - *Exceptions*: $O(d)$ memory allocation on the heap for `StackTraceElement[]` objects.
