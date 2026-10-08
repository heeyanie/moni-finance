package app;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;

/** Database connection plus an updated automatic schema migration. */
public final class DatabaseConnection {

    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/moni_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Manila";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "";

    private static boolean schemaReady = false;

    private DatabaseConnection() {}

    public static synchronized Connection getConnection() throws SQLException {
        String url = System.getenv("MONI_DB_URL") != null ? System.getenv("MONI_DB_URL") : DEFAULT_URL;
        String user = System.getenv("MONI_DB_USER") != null ? System.getenv("MONI_DB_USER") : DEFAULT_USER;
        String pass = System.getenv("MONI_DB_PASS") != null ? System.getenv("MONI_DB_PASS") : DEFAULT_PASSWORD;

        Connection c = DriverManager.getConnection(url, user, pass);
        if (!schemaReady) {
            ensureSchema(c);
            schemaReady = true;
        }
        return c;
    }

    private static void ensureSchema(Connection c) throws SQLException {
        try (Statement s = c.createStatement()) {
            s.executeUpdate("CREATE DATABASE IF NOT EXISTS moni_db");
        } catch (SQLException ignored) {}

        try (Statement s = c.createStatement()) {
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

            s.executeUpdate("CREATE TABLE IF NOT EXISTS budgets ("
                    + "budget_id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "user_id INT NOT NULL, "
                    + "category VARCHAR(100) NOT NULL, "
                    + "budget_limit DECIMAL(12,2) NOT NULL DEFAULT 0.00, "
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

        addColumnIfMissing(c, "user_settings", "allowance_amount", "DECIMAL(12,2) NOT NULL DEFAULT 0.00");
        addColumnIfMissing(c, "user_settings", "allowance_frequency", "VARCHAR(30) NOT NULL DEFAULT 'Daily'");
        addColumnIfMissing(c, "user_settings", "daily_allowance", "DECIMAL(12,2) NOT NULL DEFAULT 0.00");
    }

    private static void addColumnIfMissing(Connection c, String table, String column, String definition)
            throws SQLException {
        DatabaseMetaData meta = c.getMetaData();
        try (ResultSet rs = meta.getColumns(c.getCatalog(), null, table, column)) {
            if (rs.next()) return;
        }

        String sql = "ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition;
        try (Statement s = c.createStatement()) {
            s.executeUpdate(sql);
        }
    }
}