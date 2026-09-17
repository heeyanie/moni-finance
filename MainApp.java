package app;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Expense {
    private double amount;
    private String category;
    private String description;
    private String date;

    public Expense(double amount, String category, String description, String date) {
        this.amount = amount;
        this.category = category;
        this.description = description;
        this.date = date;
    }

    public double getAmount() { return amount; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public String getDate() { return date; }
}

class TransactionRecord {
    private String date;
    private String flowType;
    private String categorySource;
    private String description;
    private double amount;

    public TransactionRecord(String date, String flowType, String categorySource, String description, double amount) {
        this.date = date;
        this.flowType = flowType;
        this.categorySource = categorySource;
        this.description = description;
        this.amount = amount;
    }

    public String getDate() { return date; }
    public String getFlowType() { return flowType; }
    public String getCategorySource() { return categorySource; }
    public String getDescription() { return description; }
    public double getAmount() { return amount; }
}

class StudentAccount {
    private double currentBalance;

    public StudentAccount(double startingBalance) {
        this.currentBalance = startingBalance;
    }

    public double getCurrentBalance() { 
        return currentBalance; 
    }

    public void updateBalance(double amount) {
        this.currentBalance -= amount;
    }

    public void deposit(double amount) {
        this.currentBalance += amount;
    }

    public boolean withdraw(double amount) {
        if (amount > 0 && this.currentBalance >= amount) {
            this.currentBalance -= amount;
            return true;
        }
        return false;
    }
}

class Savings {
    private double totalSavings;

    public Savings(double initialSavings) {
        this.totalSavings = initialSavings;
    }

    public double getTotalSavings() {
        return totalSavings;
    }

    public void addSavings(double amount) {
        this.totalSavings += amount;
    }

    public boolean withdrawSavings(double amount) {
        if (amount > 0 && this.totalSavings >= amount) {
            this.totalSavings -= amount;
            return true;
        }
        return false;
    }
}

class Budget {
    private Map<String, Double> categoryLimits = new HashMap<>();
    private Map<String, Double> categorySpent = new HashMap<>();

    public Budget() {
        categoryLimits.put("Food", 500.0);
        categoryLimits.put("Transportation", 200.0);
        categoryLimits.put("School-related", 300.0);
        categoryLimits.put("Miscellaneous", 250.0);
        categoryLimits.put("Other", 150.0);

        for (String cat : categoryLimits.keySet()) {
            categorySpent.put(cat, 0.0);
        }
    }

    public boolean checkBudgetExceeded(String category, double amount) {
        double currentSpent = categorySpent.getOrDefault(category, 0.0);
        double limit = categoryLimits.getOrDefault(category, 0.0);
        return (currentSpent + amount) > limit;
    }

    public void addSpending(String category, double amount) {
        categorySpent.put(category, categorySpent.getOrDefault(category, 0.0) + amount);
    }

    public double getLimit(String category) {
        return categoryLimits.getOrDefault(category, 0.0);
    }

    public double getSpent(String category) {
        return categorySpent.getOrDefault(category, 0.0);
    }
}

class ExpenseManager {
    private StudentAccount account;
    private Savings savings;
    private Budget budget;
    private Map<String, Double> dailyFundsAddedTracker = new HashMap<>();
    private List<TransactionRecord> transactionLogs = new ArrayList<>();

    public static final double DAILY_ADD_LIMIT = 400.00;
    public static final double MAX_OVERDRAFT_LIMIT = -200.00;
    public static final ZoneId PH_ZONE = ZoneId.of("Asia/Manila");

    public ExpenseManager(StudentAccount account, Savings savings, Budget budget) {
        this.account = account;
        this.savings = savings;
        this.budget = budget;
    }

    public static LocalDate getPhilippineToday() {
        return LocalDate.now(PH_ZONE);
    }

    public boolean validateAmount(String amountStr) {
        try {
            double amt = Double.parseDouble(amountStr);
            return amt > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public boolean validateExpense(String amountStr, String category) {
        return validateAmount(amountStr) && category != null && !category.trim().isEmpty();
    }

    public Expense createExpense(double amount, String category, String description, String date) {
        return new Expense(amount, category, description, date);
    }

    public void updateBalance(double amount) {
        account.updateBalance(amount);
    }

    public double getDailyFundsAddedToday(String dateStr) {
        return dailyFundsAddedTracker.getOrDefault(dateStr, 0.0);
    }

    public double getRemainingDailyAllowanceCap(String dateStr) {
        return Math.max(0.0, DAILY_ADD_LIMIT - getDailyFundsAddedToday(dateStr));
    }

    public boolean canAddDailyFunds(double amount, String dateStr) {
        double currentAdded = getDailyFundsAddedToday(dateStr);
        return (currentAdded + amount) <= DAILY_ADD_LIMIT;
    }

    public void depositToWallet(double amount, String dateStr) {
        account.deposit(amount);
        double currentAdded = getDailyFundsAddedToday(dateStr);
        dailyFundsAddedTracker.put(dateStr, currentAdded + amount);
    }

    public void depositToSavings(double amount) {
        savings.addSavings(amount);
    }

    public boolean transferWalletToSavings(double amount) {
        if (amount > 0 && account.getCurrentBalance() >= amount) {
            account.withdraw(amount);
            savings.addSavings(amount);
            return true;
        }
        return false;
    }

    public double coverNegativeBalanceFromSavings() {
        double deficit = Math.abs(account.getCurrentBalance());
        double availableSavings = savings.getTotalSavings();
        double transferAmount = Math.min(deficit, availableSavings);

        if (transferAmount > 0) {
            savings.withdrawSavings(transferAmount);
            account.deposit(transferAmount);
        }
        return transferAmount;
    }

    public boolean isBudgetExceeded(String category, double amount) {
        return budget.checkBudgetExceeded(category, amount);
    }

    public void commitSpending(String category, double amount) {
        budget.addSpending(category, amount);
    }

    public double getRemainingBalance() {
        return account.getCurrentBalance();
    }

    public double getSavingsBalance() {
        return savings.getTotalSavings();
    }

    public Budget getBudget() {
        return budget;
    }

    public void logTransaction(TransactionRecord record) {
        transactionLogs.add(record);
    }

    public List<TransactionRecord> getTransactionLogs() {
        return transactionLogs;
    }
}

public class MainApp extends JFrame {
    private static final long serialVersionUID = 1L;
    private User currentUser;
    private StudentAccount studentAccount = new StudentAccount(2000.00);
    private Savings savings = new Savings(0.00);
    private Budget budget = new Budget();
    private ExpenseManager manager = new ExpenseManager(studentAccount, savings, budget);

    private JLabel balanceLabel;
    private JLabel savingsLabel;
    private JLabel budgetLimitReminderLabel;
    private JLabel dailyAddCapLabel;

    private JTextField amountField;
    private JComboBox<String> categoryCombo;
    private JTextField descField;
    private JTextField expenseDateField;

    private JTextField depositField;
    private JTextField depositSourceField;
    private JTextField depositDateField;

    private JTextField savingsDepositField;
    private JTextField savingsNoteField;
    private JTextField transferAmountField;

    private JComboBox<String> filterFlowCombo;
    private JComboBox<String> filterCategoryCombo;
    private JComboBox<String> filterDateRangeCombo;
    private JTextField filterSpecificDateField;

    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> rowSorter;

    public MainApp(User user) {
        this.currentUser = user;

        setTitle("Student Budget & Expense Management System - Logged in as: " + currentUser.getFullName());
        setSize(1200, 850);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        JPanel topPanel = new JPanel(new GridLayout(2, 2, 10, 5));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        topPanel.setBackground(new Color(245, 247, 250));

        JPanel userBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        userBar.setOpaque(false);
        JLabel titleLabel = new JLabel("Welcome, " + currentUser.getFullName() + " (" + currentUser.getStudentNumber() + ")");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        userBar.add(titleLabel);

        JButton logoutBtn = new JButton("Log Out");
        logoutBtn.setFont(new Font("Arial", Font.PLAIN, 11));
        logoutBtn.setFocusPainted(false);
        userBar.add(logoutBtn);
        topPanel.add(userBar);

        savingsLabel = new JLabel();
        savingsLabel.setFont(new Font("Arial", Font.BOLD, 16));
        savingsLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        topPanel.add(savingsLabel);

        balanceLabel = new JLabel();
        balanceLabel.setFont(new Font("Arial", Font.BOLD, 16));
        topPanel.add(balanceLabel);

        dailyAddCapLabel = new JLabel();
        dailyAddCapLabel.setFont(new Font("Arial", Font.BOLD, 13));
        dailyAddCapLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        topPanel.add(dailyAddCapLabel);

        add(topPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(8, 8));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        filterPanel.setBorder(BorderFactory.createTitledBorder("Logbook Filters (Category, Flow, Daily/Weekly Log)"));

        filterPanel.add(new JLabel("Flow:"));
        filterFlowCombo = new JComboBox<>(new String[]{"All Flows", "Money In", "Money Out"});
        filterPanel.add(filterFlowCombo);

        filterPanel.add(new JLabel("Category / Source:"));
        filterCategoryCombo = new JComboBox<>(new String[]{"All Categories", "Food", "Transportation", "School-related", "Miscellaneous", "Other", "Allowance", "Savings"});
        filterPanel.add(filterCategoryCombo);

        filterPanel.add(new JLabel("Time Scope:"));
        filterDateRangeCombo = new JComboBox<>(new String[]{"All Time", "Specific Day", "Past 7 Days (Weekly Log)"});
        filterPanel.add(filterDateRangeCombo);

        filterPanel.add(new JLabel("Date (YYYY-MM-DD):"));
        filterSpecificDateField = new JTextField(ExpenseManager.getPhilippineToday().toString(), 9);
        filterSpecificDateField.setEnabled(false);
        filterPanel.add(filterSpecificDateField);

        JButton applyFilterBtn = new JButton("Apply Filters");
        applyFilterBtn.setFont(new Font("Arial", Font.BOLD, 12));
        filterPanel.add(applyFilterBtn);

        JButton resetFilterBtn = new JButton("Reset");
        resetFilterBtn.setFont(new Font("Arial", Font.BOLD, 12));
        filterPanel.add(resetFilterBtn);

        centerPanel.add(filterPanel, BorderLayout.NORTH);

        String[] columns = {"Date", "Flow Type", "Category / Source", "Description", "Amount (PHP)"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable transactionTable = new JTable(tableModel);
        rowSorter = new TableRowSorter<>(tableModel);
        transactionTable.setRowSorter(rowSorter);

        JScrollPane scrollPane = new JScrollPane(transactionTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Transaction History & Audit Log"));
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        JPanel bottomContainer = new JPanel(new GridLayout(1, 4, 8, 8));
        bottomContainer.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

        JPanel expensePanel = new JPanel(new GridLayout(6, 2, 5, 5));
        expensePanel.setBorder(BorderFactory.createTitledBorder("Record Expense"));

        expensePanel.add(new JLabel("Amount (PHP):"));
        amountField = new JTextField();
        expensePanel.add(amountField);

        expensePanel.add(new JLabel("Category:"));
        categoryCombo = new JComboBox<>(new String[]{"Food", "Transportation", "School-related", "Miscellaneous", "Other"});
        expensePanel.add(categoryCombo);

        expensePanel.add(new JLabel("Budget Limit:"));
        budgetLimitReminderLabel = new JLabel();
        budgetLimitReminderLabel.setFont(new Font("Arial", Font.BOLD, 11));
        expensePanel.add(budgetLimitReminderLabel);

        expensePanel.add(new JLabel("Description:"));
        descField = new JTextField();
        expensePanel.add(descField);

        expensePanel.add(new JLabel("Date:"));
        expenseDateField = new JTextField(ExpenseManager.getPhilippineToday().toString());
        expensePanel.add(expenseDateField);

        JButton recordBtn = new JButton("Record Expense");
        recordBtn.setFont(new Font("Arial", Font.BOLD, 12));
        expensePanel.add(new JLabel());
        expensePanel.add(recordBtn);

        bottomContainer.add(expensePanel);

        JPanel fundPanel = new JPanel(new GridLayout(5, 2, 5, 5));
        fundPanel.setBorder(BorderFactory.createTitledBorder("Add Funds (Daily Cap: 400)"));

        fundPanel.add(new JLabel("Amount (PHP):"));
        depositField = new JTextField();
        fundPanel.add(depositField);

        fundPanel.add(new JLabel("Source:"));
        depositSourceField = new JTextField("Allowance");
        fundPanel.add(depositSourceField);

        fundPanel.add(new JLabel("Date (PHT - Fixed):"));
        depositDateField = new JTextField(ExpenseManager.getPhilippineToday().toString());
        depositDateField.setEditable(false);
        depositDateField.setFocusable(false);
        depositDateField.setBackground(new Color(238, 238, 238));
        fundPanel.add(depositDateField);

        fundPanel.add(new JLabel("Destination:"));
        JLabel fundDestLabel = new JLabel("Spendable Wallet");
        fundDestLabel.setFont(new Font("Arial", Font.BOLD, 11));
        fundPanel.add(fundDestLabel);

        JButton addFundsBtn = new JButton("Add Funds");
        addFundsBtn.setFont(new Font("Arial", Font.BOLD, 12));
        fundPanel.add(new JLabel());
        fundPanel.add(addFundsBtn);

        bottomContainer.add(fundPanel);

        JPanel savingsPanel = new JPanel(new GridLayout(5, 2, 5, 5));
        savingsPanel.setBorder(BorderFactory.createTitledBorder("Add Savings"));

        savingsPanel.add(new JLabel("Savings Amount:"));
        savingsDepositField = new JTextField();
        savingsPanel.add(savingsDepositField);

        savingsPanel.add(new JLabel("Destination:"));
        JLabel savingsDestLabel = new JLabel("Savings");
        savingsDestLabel.setFont(new Font("Arial", Font.BOLD, 11));
        savingsDestLabel.setForeground(new Color(230, 81, 0));
        savingsPanel.add(savingsDestLabel);

        savingsPanel.add(new JLabel("Note:"));
        savingsNoteField = new JTextField("Emergency Fund");
        savingsPanel.add(savingsNoteField);

        savingsPanel.add(new JLabel());
        savingsPanel.add(new JLabel());

        JButton addSavingsBtn = new JButton("Add Savings");
        addSavingsBtn.setFont(new Font("Arial", Font.BOLD, 12));
        savingsPanel.add(new JLabel());
        savingsPanel.add(addSavingsBtn);

        bottomContainer.add(savingsPanel);

        JPanel transferPanel = new JPanel(new GridLayout(5, 2, 5, 5));
        transferPanel.setBorder(BorderFactory.createTitledBorder("Wallet -> Savings"));

        transferPanel.add(new JLabel("Amount:"));
        transferAmountField = new JTextField();
        transferPanel.add(transferAmountField);

        transferPanel.add(new JLabel("From:"));
        JLabel fromLabel = new JLabel("Spendable Wallet");
        fromLabel.setFont(new Font("Arial", Font.BOLD, 11));
        transferPanel.add(fromLabel);

        transferPanel.add(new JLabel("To:"));
        JLabel toLabel = new JLabel("Savings");
        toLabel.setFont(new Font("Arial", Font.BOLD, 11));
        toLabel.setForeground(new Color(230, 81, 0));
        transferPanel.add(toLabel);

        transferPanel.add(new JLabel());
        transferPanel.add(new JLabel());

        JButton transferBtn = new JButton("Transfer Funds");
        transferBtn.setFont(new Font("Arial", Font.BOLD, 12));
        transferPanel.add(new JLabel());
        transferPanel.add(transferBtn);

        bottomContainer.add(transferPanel);

        add(bottomContainer, BorderLayout.SOUTH);

        updateCategoryLimitReminder();
        updateDisplays();

        logoutBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int confirm = JOptionPane.showConfirmDialog(
                    MainApp.this,
                    "Are you sure you want to log out, " + currentUser.getFullName() + "?",
                    "Confirm Logout",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
                );

                if (confirm == JOptionPane.YES_OPTION) {
                    dispose();
                    AuthDialog authDialog = new AuthDialog(null);
                    authDialog.setVisible(true);

                    User reUser = authDialog.getAuthenticatedUser();
                    if (reUser != null) {
                        new MainApp(reUser).setVisible(true);
                    } else {
                        System.exit(0);
                    }
                }
            }
        });

        ActionListener filterChangeListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selected = (String) filterDateRangeCombo.getSelectedItem();
                filterSpecificDateField.setEnabled("Specific Day".equals(selected));
                applyTableFilters();
            }
        };

        filterFlowCombo.addActionListener(filterChangeListener);
        filterCategoryCombo.addActionListener(filterChangeListener);
        filterDateRangeCombo.addActionListener(filterChangeListener);

        applyFilterBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                applyTableFilters();
            }
        });

        resetFilterBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                filterFlowCombo.setSelectedIndex(0);
                filterCategoryCombo.setSelectedIndex(0);
                filterDateRangeCombo.setSelectedIndex(0);
                filterSpecificDateField.setText(ExpenseManager.getPhilippineToday().toString());
                rowSorter.setRowFilter(null);
            }
        });

        categoryCombo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateCategoryLimitReminder();
            }
        });

        recordBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String amountText = amountField.getText().trim();
                String category = (String) categoryCombo.getSelectedItem();
                String desc = descField.getText().trim();
                String date = expenseDateField.getText().trim();

                if (!manager.validateExpense(amountText, category)) {
                    JOptionPane.showMessageDialog(MainApp.this, 
                        "Invalid amount. Please enter a valid positive number.", 
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                double amount = Double.parseDouble(amountText);
                Expense expense = manager.createExpense(amount, category, desc, date);

                TransactionRecord record = new TransactionRecord(
                    expense.getDate(), 
                    "Money Out", 
                    expense.getCategory(), 
                    expense.getDescription(), 
                    -expense.getAmount()
                );
                manager.logTransaction(record);

                tableModel.addRow(new Object[]{
                    record.getDate(),
                    record.getFlowType(),
                    record.getCategorySource(),
                    record.getDescription(),
                    String.format("%.2f", record.getAmount())
                });

                manager.updateBalance(amount);

                boolean budgetExceeded = manager.isBudgetExceeded(category, amount);
                manager.commitSpending(category, amount);

                if (budgetExceeded) {
                    JOptionPane.showMessageDialog(MainApp.this,
                        "Budget Exceeded for " + category + "!\n" +
                        "Limit: PHP " + String.format("%.2f", budget.getLimit(category)) + "\n" +
                        "Total Spent: PHP " + String.format("%.2f", budget.getSpent(category)),
                        "Budget Alert", JOptionPane.WARNING_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(MainApp.this,
                        "Expense Recorded Successfully!\nRemaining Balance: PHP " + 
                        String.format("%.2f", manager.getRemainingBalance()),
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                }

                if (manager.getRemainingBalance() < ExpenseManager.MAX_OVERDRAFT_LIMIT) {
                    JOptionPane.showMessageDialog(MainApp.this,
                        "CRITICAL OVERDRAFT WARNING!\n" +
                        "Your wallet balance has dropped below the -PHP 200.00 limit.\n" +
                        "Current Balance: PHP " + String.format("%.2f", manager.getRemainingBalance()),
                        "Maximum Overdraft Exceeded", JOptionPane.ERROR_MESSAGE);
                }

                if (manager.getRemainingBalance() < 0) {
                    double deficit = Math.abs(manager.getRemainingBalance());
                    double currentSavings = manager.getSavingsBalance();

                    if (currentSavings > 0) {
                        double transferAmount = Math.min(deficit, currentSavings);
                        int choice = JOptionPane.showConfirmDialog(
                            MainApp.this,
                            "Warning: Wallet balance is now negative (PHP " + 
                            String.format("%.2f", manager.getRemainingBalance()) + ").\n" +
                            "Would you like to transfer PHP " + String.format("%.2f", transferAmount) + 
                            " from your Savings to cover this?",
                            "Borrow From Savings",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE
                        );

                        if (choice == JOptionPane.YES_OPTION) {
                            double transferred = manager.coverNegativeBalanceFromSavings();
                            TransactionRecord transferLog = new TransactionRecord(
                                ExpenseManager.getPhilippineToday().toString(),
                                "Money In",
                                "Savings",
                                "Covered Negative Deficit",
                                transferred
                            );
                            manager.logTransaction(transferLog);

                            tableModel.addRow(new Object[]{
                                transferLog.getDate(),
                                transferLog.getFlowType(),
                                transferLog.getCategorySource(),
                                transferLog.getDescription(),
                                String.format("+%.2f", transferLog.getAmount())
                            });

                            JOptionPane.showMessageDialog(MainApp.this,
                                "Transferred PHP " + String.format("%.2f", transferred) + 
                                " from Savings to Wallet.\nNew Wallet Balance: PHP " + 
                                String.format("%.2f", manager.getRemainingBalance()) + 
                                "\nRemaining Savings: PHP " + String.format("%.2f", manager.getSavingsBalance()),
                                "Transfer Successful",
                                JOptionPane.INFORMATION_MESSAGE);
                        }
                    } else {
                        JOptionPane.showMessageDialog(MainApp.this,
                            "Warning: Balance is now negative (PHP " + 
                            String.format("%.2f", manager.getRemainingBalance()) + ").\n" +
                            "Savings balance is PHP 0.00, so funds cannot be borrowed.",
                            "Negative Balance Warning", JOptionPane.WARNING_MESSAGE);
                    }
                }

                updateDisplays();
                updateCategoryLimitReminder();
                applyTableFilters();
                amountField.setText("");
                descField.setText("");
            }
        });

        addFundsBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String fundText = depositField.getText().trim();
                String source = depositSourceField.getText().trim();
                String dateText = ExpenseManager.getPhilippineToday().toString();
                depositDateField.setText(dateText);

                if (!manager.validateAmount(fundText)) {
                    JOptionPane.showMessageDialog(MainApp.this, 
                        "Invalid amount. Please enter a valid positive number.", 
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                double depositAmt = Double.parseDouble(fundText);
                double alreadyAdded = manager.getDailyFundsAddedToday(dateText);
                double remainingCap = manager.getRemainingDailyAllowanceCap(dateText);

                if (!manager.canAddDailyFunds(depositAmt, dateText)) {
                    JOptionPane.showMessageDialog(MainApp.this, 
                        "Daily Limit Exceeded for " + dateText + " (PHT)!\n" +
                        "Daily Limit: PHP 400.00\n" +
                        "Already Added Today: PHP " + String.format("%.2f", alreadyAdded) + "\n" +
                        "Remaining Allowed Today: PHP " + String.format("%.2f", remainingCap) + "\n" +
                        "You must wait until tomorrow (Philippine Standard Time) to add more funds.", 
                        "Daily Limit Reached", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                manager.depositToWallet(depositAmt, dateText);

                TransactionRecord record = new TransactionRecord(
                    dateText,
                    "Money In",
                    source.isEmpty() ? "Allowance" : source,
                    "Added Funds to Wallet",
                    depositAmt
                );
                manager.logTransaction(record);

                tableModel.addRow(new Object[]{
                    record.getDate(),
                    record.getFlowType(),
                    record.getCategorySource(),
                    record.getDescription(),
                    String.format("+%.2f", record.getAmount())
                });

                JOptionPane.showMessageDialog(MainApp.this,
                    "Funds added successfully!\nAdded: PHP " + String.format("%.2f", depositAmt) +
                    "\nRemaining allowance capacity for " + dateText + ": PHP " + 
                    String.format("%.2f", manager.getRemainingDailyAllowanceCap(dateText)) +
                    "\nNew Wallet Balance: PHP " + String.format("%.2f", manager.getRemainingBalance()),
                    "Deposit Success", JOptionPane.INFORMATION_MESSAGE);

                updateDisplays();
                applyTableFilters();
                depositField.setText("");
            }
        });

        addSavingsBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String savingsText = savingsDepositField.getText().trim();
                String note = savingsNoteField.getText().trim();

                if (!manager.validateAmount(savingsText)) {
                    JOptionPane.showMessageDialog(MainApp.this, 
                        "Invalid amount. Please enter a valid positive number.", 
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                double savingsAmt = Double.parseDouble(savingsText);
                manager.depositToSavings(savingsAmt);

                TransactionRecord record = new TransactionRecord(
                    ExpenseManager.getPhilippineToday().toString(),
                    "Money In",
                    "Savings",
                    note.isEmpty() ? "Added to Savings" : note,
                    savingsAmt
                );
                manager.logTransaction(record);

                tableModel.addRow(new Object[]{
                    record.getDate(),
                    record.getFlowType(),
                    record.getCategorySource(),
                    record.getDescription(),
                    String.format("+%.2f", record.getAmount())
                });

                JOptionPane.showMessageDialog(MainApp.this,
                    "Savings recorded successfully!\nTotal Savings: PHP " + 
                    String.format("%.2f", manager.getSavingsBalance()),
                    "Savings Success", JOptionPane.INFORMATION_MESSAGE);

                updateDisplays();
                applyTableFilters();
                savingsDepositField.setText("");
            }
        });

        transferBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String transferText = transferAmountField.getText().trim();

                if (!manager.validateAmount(transferText)) {
                    JOptionPane.showMessageDialog(MainApp.this,
                        "Invalid amount. Please enter a valid positive number.",
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                double transferAmt = Double.parseDouble(transferText);

                if (transferAmt > manager.getRemainingBalance()) {
                    JOptionPane.showMessageDialog(MainApp.this,
                        "Insufficient wallet balance!\nAvailable Wallet Balance: PHP " +
                        String.format("%.2f", manager.getRemainingBalance()),
                        "Transfer Failed", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                int confirm = JOptionPane.showConfirmDialog(
                    MainApp.this,
                    "Confirm transfer of PHP " + String.format("%.2f", transferAmt) + 
                    " from Wallet to Savings?",
                    "Confirm Transfer",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
                );

                if (confirm == JOptionPane.YES_OPTION) {
                    boolean success = manager.transferWalletToSavings(transferAmt);
                    if (success) {
                        TransactionRecord record = new TransactionRecord(
                            ExpenseManager.getPhilippineToday().toString(),
                            "Money Out",
                            "Savings",
                            "Moved from Wallet to Savings",
                            -transferAmt
                        );
                        manager.logTransaction(record);

                        tableModel.addRow(new Object[]{
                            record.getDate(),
                            record.getFlowType(),
                            record.getCategorySource(),
                            record.getDescription(),
                            String.format("%.2f", record.getAmount())
                        });

                        JOptionPane.showMessageDialog(MainApp.this,
                            "Transfer successful!\nTransferred: PHP " + String.format("%.2f", transferAmt) +
                            "\nNew Wallet Balance: PHP " + String.format("%.2f", manager.getRemainingBalance()) +
                            "\nNew Savings Balance: PHP " + String.format("%.2f", manager.getSavingsBalance()),
                            "Transfer Success", JOptionPane.INFORMATION_MESSAGE);

                        updateDisplays();
                        applyTableFilters();
                        transferAmountField.setText("");
                    }
                }
            }
        });
    }

    private void applyTableFilters() {
        final String selectedFlow = (String) filterFlowCombo.getSelectedItem();
        final String selectedCategory = (String) filterCategoryCombo.getSelectedItem();
        final String selectedTimeScope = (String) filterDateRangeCombo.getSelectedItem();
        final String specificDay = filterSpecificDateField.getText().trim();
        final LocalDate today = ExpenseManager.getPhilippineToday();

        RowFilter<DefaultTableModel, Object> customFilter = new RowFilter<DefaultTableModel, Object>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Object> entry) {
                String rowDateStr = entry.getStringValue(0).trim();
                String rowFlow = entry.getStringValue(1).trim();
                String rowCategorySource = entry.getStringValue(2).trim();

                if (selectedFlow != null && !"All Flows".equalsIgnoreCase(selectedFlow)) {
                    if (!rowFlow.equalsIgnoreCase(selectedFlow)) {
                        return false;
                    }
                }

                if (selectedCategory != null && !"All Categories".equalsIgnoreCase(selectedCategory)) {
                    if (!rowCategorySource.equalsIgnoreCase(selectedCategory)) {
                        return false;
                    }
                }

                if ("Specific Day".equalsIgnoreCase(selectedTimeScope)) {
                    if (!rowDateStr.equals(specificDay)) {
                        return false;
                    }
                } else if ("Past 7 Days (Weekly Log)".equalsIgnoreCase(selectedTimeScope)) {
                    try {
                        LocalDate rowDate = LocalDate.parse(rowDateStr);
                        long daysBetween = ChronoUnit.DAYS.between(rowDate, today);
                        if (daysBetween < 0 || daysBetween > 7) {
                            return false;
                        }
                    } catch (Exception ex) {
                        return false;
                    }
                }

                return true;
            }
        };

        rowSorter.setRowFilter(customFilter);
    }

    private void updateCategoryLimitReminder() {
        String selectedCategory = (String) categoryCombo.getSelectedItem();
        double limit = manager.getBudget().getLimit(selectedCategory);
        double spent = manager.getBudget().getSpent(selectedCategory);
        budgetLimitReminderLabel.setText(String.format("PHP %.2f (Spent: %.2f)", limit, spent));
        if (spent >= limit) {
            budgetLimitReminderLabel.setForeground(Color.RED);
        } else {
            budgetLimitReminderLabel.setForeground(new Color(34, 139, 34));
        }
    }

    private void updateDisplays() {
        double current = manager.getRemainingBalance();
        balanceLabel.setText(String.format("Wallet Balance: PHP %.2f", current));
        if (current < 0) {
            balanceLabel.setForeground(Color.RED);
        } else {
            balanceLabel.setForeground(new Color(34, 139, 34));
        }

        savingsLabel.setText(String.format("Savings: PHP %.2f", manager.getSavingsBalance()));
        savingsLabel.setForeground(new Color(230, 81, 0));

        String todayStr = ExpenseManager.getPhilippineToday().toString();
        double remainingCap = manager.getRemainingDailyAllowanceCap(todayStr);
        dailyAddCapLabel.setText(String.format("Today's Allowance Cap Remaining: PHP %.2f / 400.00", remainingCap));
        if (remainingCap <= 0) {
            dailyAddCapLabel.setForeground(Color.RED);
        } else {
            dailyAddCapLabel.setForeground(new Color(46, 125, 50));
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AuthDialog authDialog = new AuthDialog(null);
            authDialog.setVisible(true);

            User user = authDialog.getAuthenticatedUser();
            if (user != null) {
                new MainApp(user).setVisible(true);
            } else {
                System.exit(0);
            }
        });
    }
}