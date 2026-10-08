package app;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Creating accounts and signing in. Passwords are never stored as typed: only their
 * SHA-256 hash is saved in users.password, and login compares hashes.
 */
public final class AuthManager {

    private static final String FIND_EXISTING =
            "SELECT user_id FROM users WHERE student_number=? OR email=?";
    private static final String INSERT_USER =
            "INSERT INTO users(student_number, full_name, email, password) VALUES(?,?,?,?)";
    private static final String INSERT_ACCOUNT =
            "INSERT INTO accounts(user_id, wallet_balance, savings_balance) VALUES(?, 0.00, 0.00)";
    private static final String INSERT_SETTINGS =
            "INSERT INTO user_settings(user_id) VALUES(?)";
    private static final String FIND_FOR_LOGIN =
            "SELECT student_number, full_name, email, password FROM users WHERE student_number=?";

    private AuthManager() {}

    /**
     * Creates the user together with their accounts and user_settings rows.
     *
     * @return false if a field is empty, the student number or email is already taken,
     *         or the database can't be reached
     */
    public static boolean register(String studentNumber, String fullName, String email, String password) {
        if (isBlank(studentNumber) || isBlank(fullName) || isBlank(email) || isBlank(password)) return false;

        try (Connection c = DatabaseConnection.getConnection()) {
            if (alreadyExists(c, studentNumber, email)) return false;

            // All three rows are saved together, so a failure can't leave a user without an account.
            c.setAutoCommit(false);
            try {
                int userId = insertUser(c, studentNumber, fullName, email, hashPassword(password));
                insertForUser(c, INSERT_ACCOUNT, userId);
                insertForUser(c, INSERT_SETTINGS, userId);
                c.commit();
                return true;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("Registration failed: " + e.getMessage());
            return false;
        }
    }

    /** Returns the signed-in user, or null if the student number or password is wrong. */
    public static User login(String studentNumber, String password) {
        if (isBlank(studentNumber) || isBlank(password)) return null;

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(FIND_FOR_LOGIN)) {
            p.setString(1, studentNumber);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) return null;

                String storedHash = r.getString("password");
                if (!storedHash.equals(hashPassword(password))) return null;

                return new User(r.getString("student_number"), r.getString("full_name"),
                        r.getString("email"), storedHash);
            }
        } catch (SQLException e) {
            System.err.println("Login failed: " + e.getMessage());
            return null;
        }
    }

    /** SHA-256 of the password as 64 lowercase hex characters (same as MySQL's SHA2(text, 256)). */
    private static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // Every Java installation is required to support SHA-256.
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static boolean alreadyExists(Connection c, String studentNumber, String email) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(FIND_EXISTING)) {
            p.setString(1, studentNumber);
            p.setString(2, email);
            try (ResultSet r = p.executeQuery()) {
                return r.next();
            }
        }
    }

    private static int insertUser(Connection c, String studentNumber, String fullName,
                                  String email, String hashedPassword) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(INSERT_USER, Statement.RETURN_GENERATED_KEYS)) {
            p.setString(1, studentNumber);
            p.setString(2, fullName);
            p.setString(3, email);
            p.setString(4, hashedPassword);
            p.executeUpdate();
            try (ResultSet keys = p.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("No user id was generated.");
                return keys.getInt(1);
            }
        }
    }

    /** Runs an INSERT whose only parameter is the user id. */
    private static void insertForUser(Connection c, String sql, int userId) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, userId);
            p.executeUpdate();
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
