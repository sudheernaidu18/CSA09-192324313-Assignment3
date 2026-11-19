# Question 3: Resource Safety, Debugging, and Robust Code

**Student Name**: Korasika Sudheer  
**Register Number**: 192324313  
**Course**: Programming in Java (CSA09) – CO3  
**Marks**: 7 Marks | **Bloom's Level**: K4 – Analyse  

---

## 1. Problem Context & Defect Diagnosis
The retail sales reporting application aggregates daily revenue across hundreds of branch store files into a central summary file. Under production workloads, the application terminates abruptly with `java.io.IOException: Too many open files` (EMFILE/ENFILE error), leaving reports truncated and corrupted.

---

## 2. Student A – Debugging & Experimental Analysis

### 2.1 Locating the Resource Leak Point
Using step-through debugging on `generateReportManual`:
1. The method opens a `BufferedWriter` for the summary file.
2. Inside the file iteration loop, `new BufferedReader(new FileReader(storeFile))` allocates an OS file descriptor.
3. When `store_03_corrupt.txt` encounters an invalid numeric token (`NumberFormatException`), execution jumps immediately out of the loop directly to the `catch` block.
4. The line `reader.close()` is bypassed. Because `reader` was local to the loop body, the open descriptor remains dangling in the operating system's file table.
5. In a loop over $f$ stores, every subsequent exception abandons another open file descriptor until the OS process limit (typically 1024 or 256) is reached.

### 2.2 Experimental Measurements Table

| Test Version | Total Input Files | Injected Failure Location | Files Closed | Open Handles Leaked | OS Exhaustion Risk |
|---|---|---|---|---|---|
| **Manual `try-catch-finally`** | 5 | Store 3 (`store_03_corrupt.txt`) | 4 | **1 leaked** | HIGH (Leaks accumulate across runs) |
| **Safe `try-with-resources`** | 5 | Store 3 (`store_03_corrupt.txt`) | 5 | **0 leaked** | NONE (Deterministic closing) |

### 2.3 Logging Implementation
Targeted logging records the exact filename causing parsing errors:
```java
System.err.println("[LOG ERROR] Detected corrupted data in: " + storeFile.getName());
```

---

## 3. Student B – Design and Correctness

### 3.1 Resource Lifecycle & Closing Guarantees
- **Resources Opened**:
  1. 1 output write handle (`BufferedWriter` -> `summaryFile`).
  2. $f$ input read handles (`BufferedReader` -> `storeFile_i`).
- **Where Closed**:
  - In `try-with-resources`, every class implementing `java.lang.AutoCloseable` is automatically closed at the closing brace of the `try` block, before any `catch` or `finally` executes.
- **Closing Order**: Resources declared in the try-specification are closed in **reverse order** of declaration (LIFO order), ensuring dependent decorator streams close before their underlying physical channels.
- **Suppressed Exceptions**: If `reader.close()` throws an `IOException` while an exception is already in flight inside the try block, try-with-resources attaches the secondary exception to the primary exception via `Throwable.addSuppressed(Throwable)`. They can be retrieved via `e.getSuppressed()` without masking the original root cause.

---

## 4. Joint Engineering Decision
**Under what conditions is `try-with-resources` not sufficient on its own to make the report generator robust?**
1. **Corrupt Partial Writes**: If the application crashes halfway through writing the summary file, `try-with-resources` closes the stream, but leaves a half-written, corrupt summary file on disk. To make it robust, a **write-to-temporary-file and atomic rename (`Files.move(..., ATOMIC_MOVE)`)** pattern is required.
2. **Resource Allocation Failure Prior to Block**: If an exception occurs during the initialization expression itself, or outside the try parenthesis, the resource is not tracked.
3. **Non-AutoCloseable APIs**: Legacy C/JNI libraries or custom lock primitives that do not implement `AutoCloseable`.

---

## 5. Alternative Concept and Complexity Analysis
- **Alternative**: Explicit `close()` in `finally` vs. No closing vs. `try-with-resources`.
- **Time Complexity**:
  - All approaches process $f$ files in $O(f)$ time.
- **Space & Handle Complexity**:
  - *No Closing*: $O(f)$ open descriptors permanently held until JVM exit.
  - *Buggy Manual*: Holds $O(k)$ leaked descriptors where $k$ is number of failed files.
  - *try-with-resources*: Strictly $O(1)$ maximum open handles (at most 1 active reader + 1 active writer open concurrently).
