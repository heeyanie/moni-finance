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
 * Handles Moni account registration and login.
 *
 * Passwords are stored as SHA-256 hashes instead of plain text.
 */
public final class AuthManager {

    private static final String FIND_EXISTING =
            "SELECT user_id FROM users WHERE student_number = ? OR email = ?";

    private static final String INSERT_USER =
            "INSERT INTO users(student_number, full_name, email, password) "
            + "VALUES (?, ?, ?, ?)";

    private static final String INSERT_ACCOUNT =
            "INSERT INTO accounts(user_id, wallet_balance, savings_balance) "
            + "VALUES (?, 0.00, 0.00)";

    private static final String INSERT_SETTINGS =
            "INSERT INTO user_settings(user_id) VALUES (?)";

    private static final String FIND_FOR_LOGIN =
            "SELECT user_id, student_number, full_name, email, password "
            + "FROM users WHERE student_number = ?";

    private AuthManager() {
        // Prevent creating an instance of this utility class.
    }

    /**
     * Registers a new Moni user.
     *
     * Creates:
     * 1. users row
     * 2. accounts row
     * 3. user_settings row
     *
     * All three operations are done inside one transaction.
     */
    public static boolean register(
            String studentNumber,
            String fullName,
            String email,
            String password) {

        // Basic validation
        if (isBlank(studentNumber)
                || isBlank(fullName)
                || isBlank(email)
                || isBlank(password)) {
            return false;
        }

        studentNumber = studentNumber.trim();
        fullName = fullName.trim();
        email = email.trim().toLowerCase();

        try (Connection connection = DatabaseConnection.getConnection()) {

            // Check if student number or email already exists.
            if (alreadyExists(connection, studentNumber, email)) {
                return false;
            }

            connection.setAutoCommit(false);

            try {
                // 1. Create user
                int userId = insertUser(
                        connection,
                        studentNumber,
                        fullName,
                        email,
                        hashPassword(password)
                );

                // 2. Create account with zero balances
                insertForUser(
                        connection,
                        INSERT_ACCOUNT,
                        userId
                );

                // 3. Create default settings
                insertForUser(
                        connection,
                        INSERT_SETTINGS,
                        userId
                );

                connection.commit();
                return true;

            } catch (SQLException e) {
                connection.rollback();
                throw e;

            } finally {
                connection.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.err.println("Registration failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Logs a user in using their student number and password.
     *
     * @return User object when successful, otherwise null.
     */
    public static User login(
            String studentNumber,
            String password) {

        if (isBlank(studentNumber) || isBlank(password)) {
            return null;
        }

        studentNumber = studentNumber.trim();

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(FIND_FOR_LOGIN)
        ) {

            statement.setString(1, studentNumber);

            try (ResultSet result = statement.executeQuery()) {

                // User does not exist.
                if (!result.next()) {
                    return null;
                }

                String storedHash = result.getString("password");
                String enteredHash = hashPassword(password);

                // Password does not match.
                if (!storedHash.equals(enteredHash)) {
                    return null;
                }

                // Login successful.
                return new User(
                        result.getInt("user_id"),
                        result.getString("student_number"),
                        result.getString("full_name"),
                        result.getString("email")
                );
            }

        } catch (SQLException e) {
            System.err.println("Login failed: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Inserts a new user and returns the generated user_id.
     */
    private static int insertUser(
            Connection connection,
            String studentNumber,
            String fullName,
            String email,
            String passwordHash) throws SQLException {

        try (
                PreparedStatement statement =
                        connection.prepareStatement(
                                INSERT_USER,
                                Statement.RETURN_GENERATED_KEYS
                        )
        ) {

            statement.setString(1, studentNumber);
            statement.setString(2, fullName);
            statement.setString(3, email);
            statement.setString(4, passwordHash);

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating user failed.");
            }

            try (ResultSet keys = statement.getGeneratedKeys()) {

                if (keys.next()) {
                    return keys.getInt(1);
                }

                throw new SQLException(
                        "Creating user failed: no user ID was generated."
                );
            }
        }
    }

    /**
     * Inserts a user-related row such as accounts or user_settings.
     */
    private static void insertForUser(
            Connection connection,
            String sql,
            int userId) throws SQLException {

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, userId);

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException(
                        "Failed to create user-related record."
                );
            }
        }
    }

    /**
     * Checks whether the student number or email is already registered.
     */
    private static boolean alreadyExists(
            Connection connection,
            String studentNumber,
            String email) throws SQLException {

        try (PreparedStatement statement =
                     connection.prepareStatement(FIND_EXISTING)) {

            statement.setString(1, studentNumber);
            statement.setString(2, email);

            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    /**
     * Checks whether a string is null or empty.
     */
    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Creates a SHA-256 password hash.
     *
     * The result is a 64-character lowercase hexadecimal string.
     */
    private static String hashPassword(String password) {

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(
                            password.getBytes(StandardCharsets.UTF_8)
                    );

            StringBuilder hex = new StringBuilder();

            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }

            return hex.toString();

        } catch (NoSuchAlgorithmException e) {

            // SHA-256 is required by every standard Java installation.
            throw new IllegalStateException(
                    "SHA-256 is not available.",
                    e
            );
        }
    }
}