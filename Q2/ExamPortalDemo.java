import java.io.IOException;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Assignment 3 - CO3: Question 2
 * Full 3-Layer Online Examination Portal Architecture
 * 
 * Student Name: Korasika Sudheer
 * Register Number: 192324313
 */

// Custom Exception Hierarchy
class PortalBaseException extends Exception {
    public PortalBaseException(String message) { super(message); }
    public PortalBaseException(String message, Throwable cause) { super(message, cause); }
}

// Checked: Business temporal constraint
class ExamWindowClosedException extends PortalBaseException {
    public ExamWindowClosedException(String message) { super(message); }
}

// Checked: Authorization/Registration domain constraint
class StudentNotRegisteredException extends PortalBaseException {
    public StudentNotRegisteredException(String message) { super(message); }
}

// Layer 3: Storage Layer
class StorageSubsystem {
    public void persistAnswerScript(String studentId, String fileName, long sizeKb) throws SubmissionStorageException {
        try {
            if (sizeKb > 500) {
                // Low-level I/O disk quota failure
                throw new IOException("POSIX write failure: storage volume reached limit 500KB (requested " + sizeKb + "KB).");
            }
            System.out.println("[STORAGE] Script stored successfully on distributed disk: " + fileName);
        } catch (IOException ioe) {
            // Chaining original low-level exception into domain-level checked exception
            throw new SubmissionStorageException("Could not store answer script " + fileName + " for student " + studentId, ioe);
        }
    }
}

// Layer 2: Service Layer
class ExaminationService {
    private final StorageSubsystem storage = new StorageSubsystem();
    private final Set<String> registeredStudents = new HashSet<>();
    private final LocalTime examDeadline = LocalTime.of(15, 0); // 3:00 PM

    public ExaminationService() {
        registeredStudents.add("192324313");
        registeredStudents.add("192324314");
    }

    public void processSubmission(String studentId, String fileName, long sizeKb, LocalTime submissionTime)
            throws ExamWindowClosedException, StudentNotRegisteredException, SubmissionStorageException {

        // Rule 1: Registration Check
        if (!registeredStudents.contains(studentId)) {
            throw new StudentNotRegisteredException("Student ID '" + studentId + "' is not enrolled in CSA09 Java Course.");
        }

        // Rule 2: Temporal Deadline Check
        if (submissionTime.isAfter(examDeadline)) {
            throw new ExamWindowClosedException("Submission rejected: Exam window closed at " + examDeadline +
                    " (attempted at " + submissionTime + ").");
        }

        // Rule 3: Storage Call (Propagates SubmissionStorageException up to Controller)
        storage.persistAnswerScript(studentId, fileName, sizeKb);
    }
}

// Layer 1: Controller Layer (Presentation Boundary)
class ExaminationController {
    private final ExaminationService service = new ExaminationService();

    public void handleStudentSubmission(String studentId, String fileName, long sizeKb, LocalTime time) {
        System.out.println("\n--- [CONTROLLER] Incoming Submission Request: Student=" + studentId + " ---");
        try {
            service.processSubmission(studentId, fileName, sizeKb, time);
            System.out.println("[CONTROLLER RESPONSE: HTTP 200 OK] Submission Confirmed! Acknowledgment ID: ACK-" + studentId);
        } catch (StudentNotRegisteredException snre) {
            System.err.println("[CONTROLLER ERROR: HTTP 403 FORBIDDEN] " + snre.getMessage());
        } catch (ExamWindowClosedException ewce) {
            System.err.println("[CONTROLLER ERROR: HTTP 410 GONE] " + ewce.getMessage());
        } catch (SubmissionStorageException sse) {
            System.err.println("[CONTROLLER ERROR: HTTP 507 INSUFFICIENT STORAGE] " + sse.getMessage());
            System.err.println("  ↳ Underlying Root Cause: " + sse.getCause().getClass().getName() + " -> " + sse.getCause().getMessage());
        }
    }
}

public class ExamPortalDemo {
    public static void main(String[] args) {
        System.out.println("===========================================================");
        System.out.println("  ONLINE EXAM PORTAL - 3-LAYER PROPAGATION & CHAINING    ");
        System.out.println("  Student: Korasika Sudheer | Register No: 192324313        ");
        System.out.println("===========================================================");

        ExaminationController controller = new ExaminationController();

        // Scenario 1: Normal Valid Submission
        controller.handleStudentSubmission("192324313", "Sudheer_Java_CO3.pdf", 250, LocalTime.of(14, 30));

        // Scenario 2: Student Not Registered
        controller.handleStudentSubmission("999999999", "Unknown_Java.pdf", 180, LocalTime.of(14, 45));

        // Scenario 3: Exam Window Closed (Late Submission)
        controller.handleStudentSubmission("192324313", "Late_Sudheer.pdf", 300, LocalTime.of(15, 15));

        // Scenario 4: Storage File Exceeded (Chained Exception)
        controller.handleStudentSubmission("192324314", "Oversized_Project.pdf", 950, LocalTime.of(14, 50));
    }
}
