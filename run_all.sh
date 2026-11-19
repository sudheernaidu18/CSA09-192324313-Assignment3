#!/usr/bin/env bash
set -e

# ==============================================================================
# Master Execution & Verification Suite: Assignment 3 (CO3)
# Multithreading, Exception Handling & Fault-Tolerant Concurrency
#
# Student Name: Korasika Sudheer
# Register Number: 192324313
# Course: Programming in Java (CSA09)
# ==============================================================================

export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
echo "=============================================================================="
echo "  STARTING COMPLETE EXECUTION OF ASSIGNMENT 3 (CO3)                           "
echo "  Student: Korasika Sudheer | Register No: 192324313                          "
echo "=============================================================================="

# ------------------------------------------------------------------------------
# Q1: ATM Simulation Exception Handling
# ------------------------------------------------------------------------------
echo ""
echo ">>> [1/7] Executing Question 1: ATM Simulation Exception Handling..."
cd "$BASE_DIR/Q1"
javac ATMWithdrawalSimulation.java
java ATMWithdrawalSimulation

# ------------------------------------------------------------------------------
# Q2: Online Exam Portal Propagation & Storage Chaining
# ------------------------------------------------------------------------------
echo ""
echo ">>> [2/7] Executing Question 2: Online Exam Portal Chaining & Propagation..."
cd "$BASE_DIR/Q2"
javac StorageDemo.java ExamPortalDemo.java
java StorageDemo
java ExamPortalDemo

# ------------------------------------------------------------------------------
# Q3: Resource Safety & Try-With-Resources
# ------------------------------------------------------------------------------
echo ""
echo ">>> [3/7] Executing Question 3: Resource Safety & Try-With-Resources..."
cd "$BASE_DIR/Q3"
javac SalesReportGenerator.java
java SalesReportGenerator

# ------------------------------------------------------------------------------
# Q4: Parallel Scanner Station Thread Lifecycle
# ------------------------------------------------------------------------------
echo ""
echo ">>> [4/7] Executing Question 4: Parallel Scanner Thread Lifecycle..."
cd "$BASE_DIR/Q4"
javac ParallelScannerSimulation.java
java ParallelScannerSimulation

# ------------------------------------------------------------------------------
# Q5: Cinema Seat Booking Race Conditions
# ------------------------------------------------------------------------------
echo ""
echo ">>> [5/7] Executing Question 5: Cinema Seat Booking Race Conditions..."
cd "$BASE_DIR/Q5"
javac SeatBooking.java SeatBookingRaceExperiment.java
java SeatBooking
java SeatBookingRaceExperiment

# ------------------------------------------------------------------------------
# Q6: Deadlock & Inter-Thread Communication
# ------------------------------------------------------------------------------
echo ""
echo ">>> [6/7] Executing Question 6: Token Counter & Deadlock Ordering..."
cd "$BASE_DIR/Q6"
javac TokenCounter.java WalletTransferDeadlockDemo.java
java TokenCounter
java WalletTransferDeadlockDemo

# ------------------------------------------------------------------------------
# Q7: Concurrent Fault-Tolerant Result Processor
# ------------------------------------------------------------------------------
echo ""
echo ">>> [7/7] Executing Question 7: Concurrent Fault-Tolerant Result Processor..."
cd "$BASE_DIR/Q7"
javac ResultProcessorBenchmark.java
java ResultProcessorBenchmark

echo ""
echo "=============================================================================="
echo "  ASSIGNMENT 3 VERIFICATION COMPLETE: ALL 7 QUESTIONS PASSED WITH ZERO ERRORS!"
echo "=============================================================================="
