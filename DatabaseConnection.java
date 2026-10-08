package app;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Opens connections to the MySQL database. The first time a connection is opened it also
 * creates any missing tables, so the app works on an empty moni_db database.
 *
 * The connection settings can be changed with the MONI_DB_URL, MONI_DB_USER and MONI_DB_PASS
 * environment variables; otherwise the XAMPP defaults below are used.
 */
public final class DatabaseConnection {

    // serverTimezone keeps DATE columns from shifting by a day when converted to Java dates.
    private static final String URL = envOrDefault("MONI_DB_URL",
            "jdbc:mysql://localhost:3306/moni_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Manila");
    private static final String USER = envOrDefault("MONI_DB_USER", "root");
    private static final String PASSWORD = envOrDefault("MONI_DB_PASS", "");

    private static boolean schemaReady = false;

    private DatabaseConnection() {}

    /** Returns a new connection. The caller must close it (use try-with-resources). */
    public static synchronized Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
        if (!schemaReady) {
            try {
                createTables(connection);
                addMissingColumns(connection);
                hashPlainPasswords(connection);
                schemaReady = true;
            } catch (SQLException e) {
                connection.close();
                throw e;
            }
        }
        return connection;
    }

    private static String envOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return value != null ? value : defaultValue;
    }

    private static void createTables(Connection connection) throws SQLException {
        try (Statement s = connection.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS users ("
                    + "user_id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "student_number VARCHAR(50) NOT NULL UNIQUE, "
                    + "full_name VARCHAR(150) NOT NULL, "
                    + "email VARCHAR(150) NOT NULL UNIQUE, "
                    + "password VARCHAR(255) NOT NULL)");

            s.executeUpdate("CREATE TABLE IF NOT EXISTS accounts ("
                    + "account_id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "user_id INT NOT NULL UNIQUE, "
                    + "wallet_balance DECIMAL(12,2) NOT NULL DEFAULT 0.00, "
                    + "savings_balance DECIMAL(12,2) NOT NULL DEFAULT 0.00, "
                    + "FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE)");

            s.executeUpdate("CREATE TABLE IF NOT EXISTS user_settings ("
                    + "settings_id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "user_id INT NOT NULL UNIQUE, "
                    + "allowance_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00, "
                    + "allowance_frequency VARCHAR(30) NOT NULL DEFAULT 'Daily', "
                    + "daily_allowance DECIMAL(12,2) NOT NULL DEFAULT 0.00, "
                    + "daily_spending_limit DECIMAL(12,2) NOT NULL DEFAULT 0.00, "
                    + "weekly_spending_limit DECIMAL(12,2) NOT NULL DEFAULT 0.00, "
                    + "show_wallet BOOLEAN NOT NULL DEFAULT TRUE, "
                    + "show_savings BOOLEAN NOT NULL DEFAULT TRUE, "
                    + "show_daily BOOLEAN NOT NULL DEFAULT TRUE, "
                    + "show_weekly BOOLEAN NOT NULL DEFAULT TRUE, "
                    + "show_transactions BOOLEAN NOT NULL DEFAULT TRUE, "
                    + "show_budget BOOLEAN NOT NULL DEFAULT TRUE, "
                    + "show_quick_actions BOOLEAN NOT NULL DEFAULT TRUE, "
                    + "setup_completed BOOLEAN NOT NULL DEFAULT FALSE, "
                    + "FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE)");

            // One budget per category per user, same as in moni_db.sql.
            s.executeUpdate("CREATE TABLE IF NOT EXISTS budgets ("
                    + "budget_id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "user_id INT NOT NULL, "
                    + "category VARCHAR(100) NOT NULL, "
                    + "budget_limit DECIMAL(12,2) NOT NULL DEFAULT 0.00, "
                    + "UNIQUE (user_id, category), "
                    + "FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE)");

            s.executeUpdate("CREATE TABLE IF NOT EXISTS transactions ("
                    + "transaction_id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "user_id INT NOT NULL, "
                    + "transaction_date DATE NOT NULL, "
                    + "flow_type VARCHAR(50) NOT NULL, "
                    + "category_source VARCHAR(100), "
                    + "description VARCHAR(255), "
                    + "amount DECIMAL(12,2) NOT NULL DEFAULT 0.00, "
                    + "FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE)");
        }
    }

    /**
     * The allowance columns were added after the first version of user_settings, so older
     * databases that already have the table don't get them from CREATE TABLE IF NOT EXISTS.
     */
    private static void addMissingColumns(Connection connection) throws SQLException {
        addColumnIfMissing(connection, "user_settings", "allowance_amount", "DECIMAL(12,2) NOT NULL DEFAULT 0.00");
        addColumnIfMissing(connection, "user_settings", "allowance_frequency", "VARCHAR(30) NOT NULL DEFAULT 'Daily'");
        addColumnIfMissing(connection, "user_settings", "daily_allowance", "DECIMAL(12,2) NOT NULL DEFAULT 0.00");
    }

    /**
     * Early test accounts (like the ones in moni_db.sql) were saved with the password as typed.
     * Login only accepts hashes now, so convert any password that isn't already a 64-character
     * SHA-256 hex string. SHA2() gives the same result as AuthManager.hashPassword().
     */
    private static void hashPlainPasswords(Connection connection) throws SQLException {
        try (Statement s = connection.createStatement()) {
            s.executeUpdate("UPDATE users SET password = SHA2(password, 256) "
                    + "WHERE password NOT REGEXP '^[0-9a-f]{64}$'");
        }
    }

    private static void addColumnIfMissing(Connection connection, String table, String column, String definition)
            throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();
        try (ResultSet columns = meta.getColumns(connection.getCatalog(), null, table, column)) {
            if (columns.next()) return;
        }
        try (Statement s = connection.createStatement()) {
            s.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }
}