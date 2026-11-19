/**
 * Assignment 3 - CO3: Question 5 (Complete the Code)
 * Race Conditions in a Cinema Seat-Booking System
 * 
 * Student Name: Korasika Sudheer
 * Register Number: 192324313
 */

public class SeatBooking {
    private final boolean[] booked;
    private int bookingsMade = 0;

    SeatBooking(int seats) {
        booked = new boolean[seats];
    }

    // TODO 1: Make the check-then-book operation atomic so that a seat can never be sold twice
    public synchronized boolean book(int seat) {
        if (!booked[seat]) {
            // Check-then-act critical section made atomic via synchronized
            booked[seat] = true;
            bookingsMade++;
            return true;
        }
        return false;
    }

    public int getBookingsMade() {
        return bookingsMade;
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("===========================================================");
        System.out.println("  CINEMA SEAT BOOKING - SYNCHRONIZATION VERIFICATION    ");
        System.out.println("  Student: Korasika Sudheer | Register No: 192324313        ");
        System.out.println("===========================================================");

        SeatBooking screen = new SeatBooking(10);
        Thread[] users = new Thread[100];
        for (int i = 0; i < users.length; i++) {
            final int seat = i % 10; // 10 users compete for every seat
            users[i] = new Thread(() -> screen.book(seat));
            users[i].start();
        }
        for (Thread t : users) t.join();
        System.out.println("Seats sold: " + screen.bookingsMade);
    }
}
