package app;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** Registration and login against the users table with standard password hashing. */
public final class AuthManager {
    private AuthManager() {}

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

    public static boolean register(String studentNumber, String fullName, String email, String password) {
        if (isBlank(studentNumber) || isBlank(fullName) || isBlank(email) || isBlank(password)) return false;

        try (Connection c = DatabaseConnection.getConnection()) {
            if (alreadyExists(c, studentNumber, email)) return false;

            c.setAutoCommit(false);
            try {
                String hashedPassword = hashPassword(password);
                int userId = insertUser(c, studentNumber, fullName, email, hashedPassword);
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
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static User login(String studentNumber, String password) {
        if (isBlank(studentNumber) || isBlank(password)) return null;

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(FIND_FOR_LOGIN)) {
            p.setString(1, studentNumber);
            try (ResultSet r = p.executeQuery()) {
                if (r.next()) {
                    String storedHash = r.getString("password");
                    String inputHash = hashPassword(password);
                    
                    // Fallback comparison for legacy plaintext entries if any exist
                    if (storedHash.equals(inputHash) || storedHash.equals(password)) {
                        return new User(r.getString("student_number"), r.getString("full_name"),
                                r.getString("email"), storedHash);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error hashing password", e);
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