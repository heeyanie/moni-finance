package app;

/**
 * The signed-in Moni user. The password is not kept here: it is only needed while signing in.
 */
public class User {
    private final int id;
    private final String studentNumber;
    private final String fullName;
    private final String email;

    public User(int id, String studentNumber, String fullName, String email) {
        this.id = id;
        this.studentNumber = studentNumber;
        this.fullName = fullName;
        this.email = email;
    }

    /** The user_id column, used to look up this user's rows in the other tables. */
    public int getId() {
        return id;
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
}
