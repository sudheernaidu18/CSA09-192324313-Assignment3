# Question 6: Deadlock and Inter-Thread Communication

**Student Name**: Korasika Sudheer  
**Register Number**: 192324313  
**Course**: Programming in Java (CSA09) – CO3  
**Marks**: 7 Marks | **Bloom's Level**: K4 – Analyse  

---

## 1. Problem Context & Overview
- **Part A (Deadlock)**: Hostel wallet accounts allow peer-to-peer balance transfers. Each transfer locks both the sender and receiver accounts. When two students initiate cross-transfers concurrently, a circular wait deadlock locks both threads permanently.
- **Part B (Inter-Thread Communication)**: A mess food token counter acts as a bounded buffer of capacity 5. A cook produces meal tokens, and students consume them using `wait()`, `notify()`, and `notifyAll()`.

---

## 2. Student A – Producer–Consumer Design & Verification

### 2.1 Why `wait()` Must Be Called Inside a `while` Loop
1. **Spurious Wakeups**: The POSIX/JVM threading specification permits threads to wake up from `wait()` without an explicit `notify()` signal. A `while` loop re-evaluates the invariant condition (`count == buffer.length` or `count == 0`) before proceeding.
2. **State Mutation by Competing Consumers**: In an architecture with multiple consumer threads, when `notifyAll()` is called, several waiting consumers wake up. The first consumer to re-acquire the monitor consumes the token (`count--`). If written with `if`, a second consumer would bypass the emptiness check and attempt to read from an empty buffer, resulting in buffer underflow or invalid tokens.

### 2.2 `notify()` vs. `notifyAll()` Hazard
- If `notify()` is used when multiple producers and consumers are waiting, the JVM arbitrarily selects **one** thread from the wait set. If a consumer wakes another consumer instead of a producer, the newly awakened consumer finds `count == 0`, goes back to sleep, and **all threads become permanently blocked in a lost-signal deadlock**.
- Calling `notifyAll()` wakes all threads, allowing whichever thread matches the state condition (producer when empty, consumer when full) to advance.

### 2.3 Buffer Bounds Invariant
Over all execution steps, the circular buffer maintains:
$$0 \le \text{count} \le 5$$
- Position advancing: `in = (in + 1) % buffer.length`
- Position retreating: `out = (out + 1) % buffer.length`

### 2.4 Difference Between `sleep()` and `wait()`

| Attribute | `Thread.sleep(millis)` | `Object.wait()` |
|---|---|---|
| **Class Origin** | `java.lang.Thread` static method | `java.lang.Object` final instance method |
| **Monitor Lock State** | **Holds all locks**; does NOT release the monitor | **Releases the monitor** atomically upon entering wait state |
| **Wakeup Mechanism** | Time expiration or `interrupt()` | Explicit `notify()`, `notifyAll()`, or timeout |
| **Context** | Can be called anywhere | Must be called inside a `synchronized` block |

---

## 3. Student B – Deadlock Analysis and Prevention

### 3.1 Four Coffman Conditions for Deadlock

```
   [Account 101] ◄────── Held by ────── [Thread 1]
         ▲                                   │
      Waiting                             Waiting
       for                                  for
         │                                   ▼
     [Thread 2] ────── Held by ──────► [Account 102]
```

1. **Mutual Exclusion**: Each account monitor can only be held by one thread at a time.
2. **Hold and Wait**: Thread 1 holds Account 101 while waiting to acquire Account 102.
3. **No Preemption**: A thread cannot forcibly take away a lock held by another thread.
4. **Circular Wait**: Thread 1 waits for Account 102 (held by Thread 2), while Thread 2 waits for Account 101 (held by Thread 1).

### 3.2 Deadlock Prevention: Global Lock-Ordering Rule
Deadlock is broken by mathematically eliminating the **Circular Wait** condition:
```java
Account firstLock  = (acc1.id < acc2.id) ? acc1 : acc2;
Account secondLock = (acc1.id < acc2.id) ? acc2 : acc1;

synchronized (firstLock) {
    synchronized (secondLock) {
        // Execute atomic funds transfer
    }
}
```
**Why this works**: All threads in the JVM acquire locks in the identical ascending order of account IDs ($101 \rightarrow 102$). Thread 2 can never acquire lock 102 while waiting for 101, making circular dependency topologically impossible.

---

## 4. Complete the Code Verification
In `TokenCounter.java`:
```java
// TODO 1: Wait while the counter is full, add the token at position in, then wake waiting threads
while (count == buffer.length) {
    wait();
}
buffer[in] = token;
in = (in + 1) % buffer.length;
count++;
notifyAll();

// TODO 2: Wait while the counter is empty, remove the token at position out, wake waiting threads and return
while (count == 0) {
    wait();
}
int token = buffer[out];
out = (out + 1) % buffer.length;
count--;
notifyAll();
return token;
```
**Expected & Actual Output**:
```
Sum of tokens collected: 210 (expected 210)
```

---

## 5. Joint Conclusion
**What do deadlock and the producer–consumer problem have in common, and how does each design ensure threads make progress?**
- **Commonality**: Both involve shared resources governed by mutual exclusion where threads must coordinate access under constrained states.
- **Progress Guarantee**:
  - *Deadlock Prevention*: Enforces a global lock acquisition hierarchy ($A < B$) or uses timed back-off (`tryLock(timeout)`).
  - *Producer-Consumer*: Uses guarded suspension with condition signaling (`wait()`/`notifyAll()`), ensuring waiting threads are notified immediately when the resource availability state flips.

---

## 6. Alternative Concept and Complexity Analysis
- **Alternative 1**: Using `java.util.concurrent.ArrayBlockingQueue<Integer>(5)` eliminates manual lock/notify plumbing; internally encapsulates lock conditions (`notEmpty`, `notFull`) with zero possibility of missed signals.
- **Alternative 2**: `ReentrantLock.tryLock(timeout)` for wallet transfers:
  - If the second lock is not available within 50 ms, the first lock is released and the thread backs off, preventing permanent deadlock.
- **Complexity**:
  - Time per operation: $O(1)$ amortized for enqueue/dequeue.
  - Space: $O(K)$ where $K$ is buffer capacity ($K=5$).
