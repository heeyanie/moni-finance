package app;

/**
 * Represents a Moni user account.
 */
public class User {
    private final String studentNumber;
    private final String fullName;
    private final String email;
    private final String password;

    public User(String studentNumber, String fullName, String email, String password) {
        this.studentNumber = studentNumber;
        this.fullName = fullName;
        this.email = email;
        this.password = password;
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }
}