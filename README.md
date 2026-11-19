# Programming in Java: Assignment 3 – CO3
## Multithreading, Exception Handling & Concurrent Systems

**Student Name**: Korasika Sudheer  
**Register Number**: 192324313  
**Subject**: Programming in Java (Course Code: CSA09)  
**Outcome**: CO3 (Exception Handling & Multithreading, Sessions 27–38)  
**Total Marks**: 50 Marks  
**Java Version**: Java 25.0.1 LTS (`openjdk 25.0.1`)  
**Repository**: [https://github.com/sudheernaidu18/CSA09-192324313-Assignment3](https://github.com/sudheernaidu18/CSA09-192324313-Assignment3)

---

## 1. Project Directory Structure

```
CSA09-192324313-Assignment3/
├── Q1/                                # Handling Failure in Bank ATM Simulation (6 Marks)
│   ├── ATMWithdrawalSimulation.java   # Multi-catch, hardware checks, balance validation
│   ├── README.md                      # Throwable hierarchy, failure table, catch order justification
│   └── output/
│       └── screenshot.png             # Verified terminal execution screenshot
├── Q2/                                # User-Defined Exceptions & Propagation (7 Marks)
│   ├── StorageDemo.java               # Completed TODO 1 (SubmissionStorageException with cause)
│   ├── ExamPortalDemo.java            # Full 3-layer architecture (Controller -> Service -> Storage)
│   ├── README.md                      # Propagation diagrams, getCause() chaining, loss analysis
│   └── output/
│       └── screenshot.png             # StorageDemo & ExamPortalDemo execution screenshot
├── Q3/                                # Resource Safety, Debugging & Robust Code (7 Marks)
│   ├── SalesReportGenerator.java      # Manual try-catch leak vs try-with-resources safe version
│   ├── README.md                      # Resource lifecycle, suppressed exceptions, atomic writes
│   └── output/
│       └── screenshot.png             # File handle leak count and safe recovery screenshot
├── Q4/                                # Thread Creation & Thread Life Cycle (7 Marks)
│   ├── ParallelScannerSimulation.java # Thread vs Runnable, isAlive(), join(), priority, daemon
│   ├── README.md                      # Lifecycle state diagram, 10-run scheduler non-determinism
│   └── output/
│       └── screenshot.png             # Execution output screenshot
├── Q5/                                # Race Conditions in Cinema Seat Booking (6 Marks)
│   ├── SeatBooking.java               # Completed TODO 1 (atomic check-then-book)
│   ├── SeatBookingRaceExperiment.java # 10 runs unsynchronized race vs synchronized benchmark
│   ├── README.md                      # Interleaving diagram, 10k benchmark, 20-screen cinema design
│   └── output/
│       └── screenshot.png             # Race condition reproduction & benchmark screenshot
├── Q6/                                # Deadlock & Inter-Thread Communication (7 Marks)
│   ├── TokenCounter.java              # Completed TODO 1 & 2 (Mess Token Counter bounded buffer)
│   ├── WalletTransferDeadlockDemo.java# 4 Coffman conditions, circular wait & lock ordering rule
│   ├── README.md                      # Loop wait vs if wait, notifyAll vs notify, sleep vs wait
│   └── output/
│       └── screenshot.png             # Token sum 210 and deadlock prevention screenshot
├── Q7/                                # Fault-Tolerant Concurrent Result Processor (10 Marks)
│   ├── ResultProcessorBenchmark.java  # 10,000 files benchmark (Pool 1, 2, 4, 8), fault-tolerance
│   ├── README.md                      # 4-stage engineering defense, Amdahl's law, LongAdder
│   └── output/
│       └── screenshot.png             # 10,000 files benchmark table & ground-truth verification
├── run_all.sh                         # Master test & benchmark execution script
└── README.md                          # Root assignment documentation & submission report
```

---

## 2. Compilation and Execution Instructions

### Prerequisites
- JDK 17, 21, or 25 installed
- Bash/Zsh shell

### Execute All Tasks at Once
```bash
git clone https://github.com/sudheernaidu18/CSA09-192324313-Assignment3.git
cd CSA09-192324313-Assignment3
chmod +x run_all.sh
./run_all.sh
```

### Compile & Run Individual Tasks
```bash
# Question 1: Bank ATM Simulation
cd Q1 && javac ATMWithdrawalSimulation.java && java ATMWithdrawalSimulation

# Question 2: Online Exam Portal Storage
cd ../Q2 && javac StorageDemo.java ExamPortalDemo.java && java StorageDemo && java ExamPortalDemo

# Question 3: Daily Sales Report Resource Safety
cd ../Q3 && javac SalesReportGenerator.java && java SalesReportGenerator

# Question 4: Parallel Answer-Script Scanning
cd ../Q4 && javac ParallelScannerSimulation.java && java ParallelScannerSimulation

# Question 5: Cinema Seat Booking Race Condition
cd ../Q5 && javac SeatBooking.java SeatBookingRaceExperiment.java && java SeatBooking && java SeatBookingRaceExperiment

# Question 6: Token Counter & Deadlock Prevention
cd ../Q6 && javac TokenCounter.java WalletTransferDeadlockDemo.java && java TokenCounter && java WalletTransferDeadlockDemo

# Question 7: 10,000 Scripts Result Processor
cd ../Q7 && javac ResultProcessorBenchmark.java && java ResultProcessorBenchmark
```

---

## 3. Mark Distribution & Assessment Breakdown

| Question | Assessed Area | Bloom's Level | Marks | Status |
|---|---|---|---|---|
| **Q1** | Exception hierarchy, try, catch (multi-catch) and finally | K4 – Analyse | 6 | **Verified & Documented** |
| **Q2** | User-defined exceptions, 3-layer propagation and chaining | K4 – Analyse | 7 | **Verified & Documented** |
| **Q3** | Resource safety, try-with-resources and debugging | K4 – Analyse | 7 | **Verified & Documented** |
| **Q4** | Thread creation (Thread vs Runnable), lifecycle, scheduling | K4 – Analyse | 7 | **Verified & Documented** |
| **Q5** | Race conditions, critical sections and synchronization | K4 – Analyse | 6 | **Verified & Documented** |
| **Q6** | Deadlock prevention (lock ordering) & Bounded Buffer wait/notify | K4 – Analyse | 7 | **Verified & Documented** |
| **Q7** | Concurrent fault-tolerant processor from first principles | K5 – Evaluate | 10 | **Verified & Documented** |
| **TOTAL**| | | **50 / 50** | **100% Complete** |

---

## 4. Individual Contribution Record (Pair-Work Compliance)

| Question | Student A (Korasika Sudheer) – Primary Responsibility | Student B (Korasika Sudheer) – Primary Responsibility | Joint Contribution |
|---|---|---|---|
| **Q1** | Throwable hierarchy diagram, failure table, catch ordering justification, finally guarantees | 5 test inputs, catch/finally logs, uncaught stack trace explanation, broader catch before narrower | Defensive error code vs exception analysis |
| **Q2** | Exception hierarchy class diagram, checked vs unchecked rationale, 3-layer propagation path | Failure scenarios, `getCause()` chaining demonstration, info loss evidence, test table | Joint conclusion on `SubmissionStorageException` |
| **Q3** | Step-through debugging, failing input creation, open file descriptor leak counting, error logging | Identification of opened resources, manual leak explanation, try-with-resources reverse closing, suppressed exceptions | When try-with-resources is not sufficient on its own |
| **Q4** | Thread vs Runnable comparison, thread lifecycle state diagram, `run()` vs `start()` proof, `join()` mechanism | 10-run finish order recording, thread priority bias experiment, daemon thread observation, scheduler analysis | Scheduler non-determinism & correctness invariant |
| **Q5** | Reproducing double booking race condition over 10 runs, step-by-step thread interleaving diagram, critical section identification | 10,000 booking benchmark (sync method vs fine-grained block vs AtomicBoolean), static sync analysis | 20-screen cinema synchronization architecture |
| **Q6** | Wait in loop rationale, `notifyAll()` vs `notify()` hazard, buffer bounds invariant ($0 \le \text{count} \le 5$), `sleep()` vs `wait()` | Deadlock pair construction, 4 Coffman conditions identification, global lock-ordering prevention rule | Deadlock vs producer-consumer commonalities |
| **Q7** | Stage 2 Design: `ExecutorService`, `Callable`, `Future`, `LongAdder`, class diagram, pseudocode | Stage 3 Implementation: Injected corrupt/missing files, benchmark for 1, 2, 4, 8 thread pools, ground truth verification | Stage 1 Problem Interpretation & Stage 4 Design Defense |
