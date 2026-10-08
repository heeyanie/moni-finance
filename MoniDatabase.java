package app;

import java.sql.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

public class MoniDatabase {
    private MoniDatabase() {}

    public static class AccountData {
        public final double walletBalance, savingsBalance;
        public AccountData(double wallet, double savings) {
            walletBalance = wallet;
            savingsBalance = savings;
        }
    }

    public static int getUserId(User user) throws Exception {
        try (Connection c = DatabaseConnection.getConnection()) {
            return getUserId(c, user);
        }
    }

    private static int getUserId(Connection c, User user) throws Exception {
        try (PreparedStatement p = c.prepareStatement("SELECT user_id FROM users WHERE student_number=?")) {
            p.setString(1, user.getStudentNumber());
            try (ResultSet r = p.executeQuery()) {
                if (r.next()) return r.getInt(1);
            }
        }
        throw new Exception("User account was not found.");
    }

    private static void ensureUserRows(Connection c, int userId) throws SQLException {
        insertIfMissing(c, userId, "accounts",
                "INSERT INTO accounts(user_id, wallet_balance, savings_balance) VALUES(?, 0.00, 0.00)");
        insertIfMissing(c, userId, "user_settings",
                "INSERT INTO user_settings(user_id) VALUES(?)");
    }

    private static void insertIfMissing(Connection c, int userId, String table, String insertSql)
            throws SQLException {
        try (PreparedStatement p = c.prepareStatement("SELECT 1 FROM " + table + " WHERE user_id=?")) {
            p.setInt(1, userId);
            try (ResultSet r = p.executeQuery()) {
                if (r.next()) return;
            }
        }
        try (PreparedStatement p = c.prepareStatement(insertSql)) {
            p.setInt(1, userId);
            p.executeUpdate();
        }
    }

    public static AccountData loadAccount(User user) throws Exception {
        try (Connection c = DatabaseConnection.getConnection()) {
            int id = getUserId(c, user);
            ensureUserRows(c, id);
            try (PreparedStatement p = c.prepareStatement(
                    "SELECT wallet_balance,savings_balance FROM accounts WHERE user_id=?")) {
                p.setInt(1, id);
                try (ResultSet r = p.executeQuery()) {
                    if (r.next()) return new AccountData(r.getDouble(1), r.getDouble(2));
                }
            }
        }
        throw new Exception("No account exists for this user.");
    }

    public static UserSettings loadSettings(User user) throws Exception {
        UserSettings s = new UserSettings();

        String sql = "SELECT allowance_amount,allowance_frequency,daily_allowance,"
                   + "daily_spending_limit,weekly_spending_limit,show_wallet,show_savings,"
                   + "show_daily,show_weekly,show_transactions,show_budget,show_quick_actions,"
                   + "setup_completed FROM user_settings WHERE user_id=?";

        try (Connection c = DatabaseConnection.getConnection()) {
            int id = getUserId(c, user);
            ensureUserRows(c, id);

            try (PreparedStatement p = c.prepareStatement(sql)) {
                p.setInt(1, id);
                try (ResultSet r = p.executeQuery()) {
                    if (r.next()) {
                        s.setAllowanceAmount(r.getDouble("allowance_amount"));
                        s.setAllowanceFrequency(r.getString("allowance_frequency"));
                        s.setDailyAllowance(r.getDouble("daily_allowance"));
                        s.setDailyLimit(r.getDouble("daily_spending_limit"));
                        s.setWeeklyLimit(r.getDouble("weekly_spending_limit"));
                        s.setShowWallet(r.getBoolean("show_wallet"));
                        s.setShowSavings(r.getBoolean("show_savings"));
                        s.setShowDaily(r.getBoolean("show_daily"));
                        s.setShowWeekly(r.getBoolean("show_weekly"));
                        s.setShowTransactions(r.getBoolean("show_transactions"));
                        s.setShowBudget(r.getBoolean("show_budget"));
                        s.setShowQuickActions(r.getBoolean("show_quick_actions"));
                        s.setSetupCompleted(r.getBoolean("setup_completed"));
                    }
                }
            }

            String b = "SELECT category,budget_limit FROM budgets WHERE user_id=? ORDER BY budget_id";
            try (PreparedStatement p = c.prepareStatement(b)) {
                p.setInt(1, id);
                try (ResultSet r = p.executeQuery()) {
                    while (r.next()) s.setCategoryLimit(r.getString(1), r.getDouble(2));
                }
            }
        }
        return s;
    }

    public static boolean isSetupCompleted(User user) throws Exception {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(
                     "SELECT setup_completed FROM user_settings WHERE user_id=?")) {
            p.setInt(1, getUserId(c, user));
            try (ResultSet r = p.executeQuery()) {
                return r.next() && r.getBoolean(1);
            }
        }
    }

    public static void saveSettings(User user, UserSettings s) throws Exception {
        String sql = "UPDATE user_settings SET allowance_amount=?,allowance_frequency=?,daily_allowance=?,"
                   + "daily_spending_limit=?,weekly_spending_limit=?,show_wallet=?,show_savings=?,show_daily=?,"
                   + "show_weekly=?,show_transactions=?,show_budget=?,show_quick_actions=?,setup_completed=? "
                   + "WHERE user_id=?";

        try (Connection c = DatabaseConnection.getConnection()) {
            int id = getUserId(c, user);
            c.setAutoCommit(false);
            try {
                ensureUserRows(c, id);

                try (PreparedStatement p = c.prepareStatement(sql)) {
                    p.setDouble(1, s.getAllowanceAmount());
                    p.setString(2, s.getAllowanceFrequency());
                    p.setDouble(3, s.getDailyAllowance());
                    p.setDouble(4, s.getDailyLimit());
                    p.setDouble(5, s.getWeeklyLimit());
                    p.setBoolean(6, s.isShowWallet());
                    p.setBoolean(7, s.isShowSavings());
                    p.setBoolean(8, s.isShowDaily());
                    p.setBoolean(9, s.isShowWeekly());
                    p.setBoolean(10, s.isShowTransactions());
                    p.setBoolean(11, s.isShowBudget());
                    p.setBoolean(12, s.isShowQuickActions());
                    p.setBoolean(13, s.isSetupCompleted());
                    p.setInt(14, id);
                    p.executeUpdate();
                }

                try (PreparedStatement d = c.prepareStatement("DELETE FROM budgets WHERE user_id=?")) {
                    d.setInt(1, id);
                    d.executeUpdate();
                }

                String ins = "INSERT INTO budgets(user_id,category,budget_limit) VALUES(?,?,?)";
                try (PreparedStatement p = c.prepareStatement(ins)) {
                    for (Map.Entry<String,Double> e : s.getCategoryLimits().entrySet()) {
                        p.setInt(1, id);
                        p.setString(2, e.getKey());
                        p.setDouble(3, e.getValue());
                        p.addBatch();
                    }
                    p.executeBatch();
                }
                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    public static void updateBalances(User user, double wallet, double savings) throws Exception {
        String sql = "UPDATE accounts SET wallet_balance=?,savings_balance=? WHERE user_id=?";
        try (Connection c = DatabaseConnection.getConnection()) {
            int id = getUserId(c, user);
            ensureUserRows(c, id);
            try (PreparedStatement p = c.prepareStatement(sql)) {
                p.setDouble(1, wallet);
                p.setDouble(2, savings);
                p.setInt(3, id);
                p.executeUpdate();
            }
        }
    }

    public static void saveTransactionAndBalances(User user, TransactionRecord record,
                                                   double wallet, double savings) throws Exception {
        try (Connection c = DatabaseConnection.getConnection()) {
            int id = getUserId(c, user);
            c.setAutoCommit(false);
            try {
                ensureUserRows(c, id);
                try (PreparedStatement p = c.prepareStatement(
                        "UPDATE accounts SET wallet_balance=?, savings_balance=? WHERE user_id=?")) {
                    p.setDouble(1, wallet);
                    p.setDouble(2, savings);
                    p.setInt(3, id);
                    p.executeUpdate();
                }

                try (PreparedStatement p = c.prepareStatement(
                        "INSERT INTO transactions(user_id,transaction_date,flow_type,category_source,description,amount) "
                      + "VALUES(?,?,?,?,?,?)")) {
                    p.setInt(1, id);
                    p.setDate(2, java.sql.Date.valueOf(LocalDate.parse(record.getDate())));
                    p.setString(3, record.getFlowType());
                    p.setString(4, record.getCategorySource());
                    p.setString(5, record.getDescription());
                    p.setDouble(6, record.getAmount());
                    p.executeUpdate();
                }
                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    public static List<TransactionRecord> getTransactions(User user, LocalDate start, LocalDate end) throws Exception {
        List<TransactionRecord> list = new ArrayList<>();
        String sql = "SELECT transaction_date,flow_type,category_source,description,amount "
                   + "FROM transactions WHERE user_id=? AND transaction_date BETWEEN ? AND ? "
                   + "ORDER BY transaction_date DESC,transaction_id DESC";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, getUserId(user));
            p.setDate(2, java.sql.Date.valueOf(start));
            p.setDate(3, java.sql.Date.valueOf(end));
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    list.add(new TransactionRecord(
                            r.getDate(1).toLocalDate().toString(),
                            r.getString(2), r.getString(3), r.getString(4), r.getDouble(5)));
                }
            }
        }
        return list;
    }

    public static List<TransactionRecord> getRecentTransactions(User user, int limit) throws Exception {
        List<TransactionRecord> list = new ArrayList<>();
        int safe = Math.max(1, Math.min(limit, 300));
        String sql = "SELECT transaction_date,flow_type,category_source,description,amount "
                   + "FROM transactions WHERE user_id=? ORDER BY transaction_date DESC,transaction_id DESC LIMIT " + safe;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, getUserId(user));
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    list.add(new TransactionRecord(
                            r.getDate(1).toLocalDate().toString(),
                            r.getString(2), r.getString(3), r.getString(4), r.getDouble(5)));
                }
            }
        }
        return list;
    }

    public static double getAllowanceReceived(User user, LocalDate start, LocalDate end) throws Exception {
        String sql = "SELECT COALESCE(SUM(amount),0) FROM transactions "
                   + "WHERE user_id=? AND transaction_date BETWEEN ? AND ? "
                   + "AND flow_type='Money In' AND category_source='Allowance'";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, getUserId(user));
            p.setDate(2, java.sql.Date.valueOf(start));
            p.setDate(3, java.sql.Date.valueOf(end));
            try (ResultSet r = p.executeQuery()) {
                return r.next() ? r.getDouble(1) : 0;
            }
        }
    }

    /** Returns spending (Money Out) for a date range, excluding transfers into Savings. */
    public static double getSpent(User user, LocalDate start, LocalDate end) throws Exception {
        String sql = "SELECT COALESCE(SUM(ABS(amount)),0) FROM transactions "
                   + "WHERE user_id=? AND transaction_date BETWEEN ? AND ? "
                   + "AND flow_type='Money Out' "
                   + "AND (category_source IS NULL OR category_source <> 'Savings')";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, getUserId(user));
            p.setDate(2, java.sql.Date.valueOf(start));
            p.setDate(3, java.sql.Date.valueOf(end));
            try (ResultSet r = p.executeQuery()) {
                return r.next() ? r.getDouble(1) : 0;
            }
        }
    }

    public static LocalDate weekStart(LocalDate date) {
        return date.with(DayOfWeek.MONDAY);
    }

    public static LocalDate weekEnd(LocalDate date) {
        return weekStart(date).plusDays(6);
    }

    public static LocalDate monthStart(LocalDate date) {
        return date.withDayOfMonth(1);
    }

    public static LocalDate monthEnd(LocalDate date) {
        return date.withDayOfMonth(date.lengthOfMonth());
    }

    public static LocalDate periodStart(LocalDate date, String frequency) {
        if ("Weekly".equalsIgnoreCase(frequency)) return weekStart(date);
        if ("Monthly".equalsIgnoreCase(frequency)) return monthStart(date);
        return date;
    }

    public static LocalDate periodEnd(LocalDate date, String frequency) {
        if ("Weekly".equalsIgnoreCase(frequency)) return weekEnd(date);
        if ("Monthly".equalsIgnoreCase(frequency)) return monthStart(date).plusDays(date.lengthOfMonth() - 1);
        return date;
    }
}