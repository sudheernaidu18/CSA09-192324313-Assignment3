# Question 4: Thread Creation and the Thread Life Cycle

**Student Name**: Korasika Sudheer  
**Register Number**: 192324313  
**Course**: Programming in Java (CSA09) – CO3  
**Marks**: 7 Marks | **Bloom's Level**: K4 – Analyse  

---

## 1. Problem Context & Overview
An examination cell operates four optical scanner stations simultaneously. Each station processes batches of physical answer scripts concurrently. The simulation investigates thread creation mechanisms (`Thread` vs `Runnable`), thread state transitions, non-deterministic OS scheduling, thread priorities, and daemon thread termination.

---

## 2. Student A – Thread Design & Architecture

### 2.1 Extending `Thread` vs. Implementing `Runnable`

| Dimension | `class Station extends Thread` | `class Station implements Runnable` | Preferred Approach |
|---|---|---|---|
| **Inheritance** | Consumes Java's single inheritance slot; cannot extend another class | Allows the class to extend another domain base class | **`Runnable`** |
| **Separation of Concerns** | Tightly couples task logic with thread lifecycle management | Separates the task (`Runnable`) from the execution worker (`Thread`) | **`Runnable`** |
| **Thread Pooling** | Cannot be submitted directly to modern `ExecutorService` pools | Directly reusable across thread pools and `CompletableFuture` | **`Runnable`** |
| **Resource Overhead** | Each task instance carries full Thread object overhead | Clean, lightweight functional object | **`Runnable`** |

### 2.2 Thread Life-Cycle State Diagram

```
                ┌────────────────┐
                │      NEW       │  new Thread()
                └───────┬────────┘
                        │ .start()
                        ▼
                ┌────────────────┐
  OS Scheduler  │    RUNNABLE    │ ◄───────────────────┐
 ─────────────► │ (Ready/Running)│                     │
                └───────┬────────┘                     │
      ┌─────────────────┼──────────────────┐           │
      │ wait()          │ sleep(t)/join(t) │ Lock      │ notify()/timeout/
      ▼                 ▼                  ▼ acquired  │ lock free
┌───────────┐    ┌───────────────┐   ┌────────────┐    │
│  WAITING  │    │ TIMED_WAITING │   │  BLOCKED   │ ───┘
└───────────┘    └───────────────┘   └────────────┘
      │                 │                  │
      └─────────────────┴──────────────────┘
                        │ run() returns
                        ▼
                ┌────────────────┐
                │   TERMINATED   │
                └────────────────┘
```

### 2.3 Proof: `run()` vs. `start()`
- Invoking `testThread.run()` prints:
  `Inside worker: Thread Name = main`  
  *Proof*: Executing `run()` is a standard synchronous method invocation in the calling thread (`main`). No new OS thread or call stack is spawned.
- Invoking `testThread.start()` prints:
  `Inside worker: Thread Name = WorkerThread-X`  
  *Proof*: `start()` invokes the native JVM method `JVM_StartThread`, requesting the OS kernel to allocate a new thread stack, transitions to `RUNNABLE`, and schedules `run()`.

### 2.4 Why `join()` is Essential
Calling `station.join()` causes the `main` thread to transition into `WAITING` state until the target station thread terminates. Without `join()`, `main` proceeds immediately, printing a premature total of 0 scanned scripts before background hardware has completed.

---

## 3. Student B – Behavioural Experiments & Empirical Findings

### 3.1 Ten Independent Runs: Finish Order Non-Determinism

| Run Index | Finish Order Sequence (Station IDs) | Fastest Station | Slowest Station |
|---|---|---|---|
| Run #01 | `[4, 2, 1, 3]` | Station 4 | Station 3 |
| Run #02 | `[4, 1, 2, 3]` | Station 4 | Station 3 |
| Run #03 | `[2, 1, 3, 4]` | Station 2 | Station 4 |
| Run #04 | `[4, 3, 1, 2]` | Station 4 | Station 2 |
| Run #05 | `[2, 4, 3, 1]` | Station 2 | Station 1 |
| Run #06 | `[3, 1, 4, 2]` | Station 3 | Station 2 |
| Run #07 | `[2, 4, 1, 3]` | Station 2 | Station 3 |
| Run #08 | `[2, 4, 1, 3]` | Station 2 | Station 3 |
| Run #09 | `[1, 4, 2, 3]` | Station 1 | Station 3 |
| Run #10 | `[4, 3, 2, 1]` | Station 4 | Station 1 |

*Observation*: Even with identical script workloads, different stations finish first across runs due to operating system preemptive time-slicing and CPU core cache affinities.

### 3.2 Thread Priorities Experiment
Assigning `Thread.MAX_PRIORITY` (10) to Station 4 and `Thread.MIN_PRIORITY` (1) to Station 1 provides a statistical hint to the OS thread scheduler, but **does not guarantee deterministic execution order**. Modern operating systems (e.g., macOS Darwin / Linux CFS) prevent starvation by dynamically boosting thread priorities.

### 3.3 Daemon Thread Observation
When Station 4 was configured with `setDaemon(true)`:
The daemon thread printed 1 heartbeat (`Scanned batch heartbeat #1`), but when `main()` completed, the JVM exited immediately without waiting for the remaining 9 batches. Daemons are aborted abruptly without executing `finally` blocks.

---

## 4. Joint Conclusion
**Why can the same multithreaded program produce different output orders on different runs, and why must a correct program not depend on that order?**
- **Reason**: Thread execution is governed by the OS preemptive scheduler, which interleaves execution slices (quanta) based on CPU interrupts, cache misses, and hardware bus arbitration.
- **Rule of Correctness**: A multithreaded program must be written such that **correctness is an invariant** independent of scheduling order. Algorithms must rely on synchronization barriers (`CountDownLatch`, `join()`, atomic primitives), never on execution sequence assumptions.

---

## 5. Alternative Concept and Complexity Analysis
- **Alternative**: Using `ExecutorService executor = Executors.newFixedThreadPool(4)` instead of manual Thread creation.
- **Completion Time**: For $n$ scripts distributed over $p$ thread pool workers, completion time is $O(n/p)$ with zero thread recreation latency.
- **Memory Consumption**:
  - *Manual Thread per Batch*: Creating $n$ threads requires $n \times 1\text{ MB}$ stack space on 64-bit JVMs ($10,000\text{ threads} = 10\text{ GB}$, crashing JVM with `OutOfMemoryError: unable to create new native thread`).
  - *Fixed Thread Pool*: Limits memory strictly to $p \times 1\text{ MB} = 4\text{ MB}$ regardless of workload scale.
