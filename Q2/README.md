# Question 2: User-Defined Exceptions and Propagation in an Online Examination Portal

**Student Name**: Korasika Sudheer  
**Register Number**: 192324313  
**Course**: Programming in Java (CSA09) – CO3  
**Marks**: 7 Marks | **Bloom's Level**: K4 – Analyse  

---

## 1. Problem Context & Architectural Layers
The Online Examination Portal enforces academic integrity and resource quotas. Submissions are processed across three distinct software layers:
1. **Controller Layer**: Handles HTTP requests and presents user-friendly error codes and messages to students.
2. **Service Layer**: Evaluates business constraints (student enrollment validation and submission window deadline).
3. **Storage Layer**: Writes uploaded PDF answer scripts to persistent storage, enforcing filesystem quotas.

---

## 2. Student A – Exception Hierarchy and Propagation Design

### 2.1 Class Diagram & Exception Hierarchy

```
                      java.lang.Exception
                               ▲
                               │
                       PortalBaseException
                               ▲
         ┌─────────────────────┼─────────────────────┐
         │                     │                     │
ExamWindowClosedException  StudentNotRegisteredException  SubmissionStorageException
  (Checked Business)         (Checked Security)            (Checked Domain I/O)
                                                            (Wraps IOException)
```

### 2.2 Rationale for Checked vs. Unchecked
- **Checked Exceptions (`Exception`)**: All three exceptions represent anticipated domain failures that the calling layer is expected to handle and recover from (e.g., presenting a clear rejection message to the student or offering a retry). Making them checked forces the compiler to ensure that the Controller layer handles every failure path.
- **Unchecked Exceptions (`RuntimeException`)**: Reserved for programming bugs (e.g., `NullPointerException`, illegal states), not for business validation rules.

### 2.3 Exception Propagation Path

```
 [StorageSubsystem]
       │  throws IOException ("Disk quota exceeded")
       ▼  caught inside Storage.save()
  Wraps into SubmissionStorageException(msg, cause=IOException)
       │  throws SubmissionStorageException
       ▼
 [ExaminationService]
       │  declared in method signature: throws SubmissionStorageException
       ▼  propagates unhindered through service boundary
 [ExaminationController]
       │  catches SubmissionStorageException
       ▼
  Translates into HTTP 507 INSUFFICIENT STORAGE with user-facing explanation
```

---

## 3. Student B – Failure Scenarios and Chaining Evidence

### 3.1 Observed Behavior Test Table

| Scenario | Input Condition | Layer Where Detected | Exception Thrown | Root Cause Chained? | Controller Presentation |
|---|---|---|---|---|---|
| TC-01 | Valid student, on time, < 500KB | All layers pass | None | N/A | HTTP 200 OK (ACK Generated) |
| TC-02 | Unregistered ID `999999999` | Service Layer | `StudentNotRegisteredException` | None | HTTP 403 Forbidden |
| TC-03 | Submission at 15:15 (> 15:00) | Service Layer | `ExamWindowClosedException` | None | HTTP 410 Gone |
| TC-04 | 950KB File (> 500KB quota) | Storage Layer | `SubmissionStorageException` | Yes (`IOException`) | HTTP 507 with root cause logged |

### 3.2 Evidence of Exception Chaining
When `SubmissionStorageException` wraps `IOException`, `getCause()` retains the complete low-level driver stack trace:
```java
e.getMessage() -> "Could not store answer02.pdf"
e.getCause().getMessage() -> "Disk quota exceeded"
```
**Information Lost if re-thrown without cause**:
If the constructor `new SubmissionStorageException(msg)` were called without `e`, the underlying OS failure details (disk full vs permission denied vs network timeout) are discarded. In production debugging, developers cannot diagnose why the write failed.

---

## 4. Complete the Code Verification
In `Storage.java`:
```java
// TODO 1: Wrap the IOException in a SubmissionStorageException, keeping the IOException as its cause
throw new SubmissionStorageException("Could not store " + fileName, e);
```
**Actual Program Output**:
```
Saved answer01.pdf
Error: Could not store answer02.pdf
Cause: Disk quota exceeded
```
*Note on Question in PDF*: If `e` were omitted from the constructor call (`super(message)`), `e.getCause()` would return `null`, causing `e.getCause().getMessage()` to throw a `NullPointerException`.

---

## 5. Joint Conclusion
**Why is it better for the storage layer to throw a meaningful `SubmissionStorageException` than letting an `IOException` travel unchanged to the user interface?**
1. **Abstraction and Encapsulation**: Leaking raw `IOException` reveals implementation details of the server (POSIX paths, storage device types), which is a security vulnerability (CWE-209).
2. **Domain-Specific Recovery**: The user interface does not know what to do with a generic `IOException`. A `SubmissionStorageException` enables the UI to present a targeted prompt (e.g., *"File exceeds upload limit; compress your PDF"*).

---

## 6. Alternative Concept and Complexity Analysis
- **Alternative**: Extending `RuntimeException` (Unchecked).
  - *Code Impact*: Eliminates `throws` clauses in method signatures; callers can ignore exceptions.
  - *Risk*: Callers can easily forget to handle domain errors, crashing threads at runtime.
- **Complexity**:
  - *Time*: Creating an exception takes $O(d)$ time where $d$ is call stack depth.
  - *Space*: Chaining retains references to both exception objects and both `StackTraceElement[]` arrays on the heap, increasing memory footprint by ~2 KB per chained exception.
