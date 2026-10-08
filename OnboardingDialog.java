package app;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class OnboardingDialog extends JDialog {

    private static final double SPENDING_RATE = 0.80;
    private static final double SAVINGS_RATE = 0.10;
    private static final double BUFFER_RATE = 0.10;

    private static final String[] CATEGORIES = {
            "Food", "Transportation", "School", "Entertainment", "Shopping", "Bills", "Health", "Other"
    };
    private static final String[] STEPS = {"Allowance", "Spending plan", "Categories", "Dashboard"};
    private static final int LAST_PAGE = STEPS.length - 1;

    private final JTextField allowanceField = Theme.field();
    private final JComboBox<String> frequencyBox = new JComboBox<>(new String[]{"Daily", "Weekly", "Monthly"});
    private final JLabel perDayPreview = Theme.text(" ", Theme.BODY_BOLD, Theme.ACCENT);

    private final JTextField dailyLimitField = Theme.field();
    private final JTextField weeklyLimitField = Theme.field();
    private final JLabel dailyAllowanceValue = new JLabel();
    private final JLabel spendingValue = new JLabel();
    private final JLabel savingsValue = new JLabel();
    private final JLabel bufferValue = new JLabel();
    private final JLabel weeklyValue = new JLabel();

    private final Map<String, JCheckBox> categoryChecks = new LinkedHashMap<>();
    private final Map<String, JTextField> categoryFields = new LinkedHashMap<>();
    private final Set<String> editedCategories = new HashSet<>();
    private final JLabel categoryTotal = Theme.text(" ", Theme.BODY_BOLD, Theme.TEXT);

    private final JCheckBox wallet = new JCheckBox("Wallet balance", true);
    private final JCheckBox savings = new JCheckBox("Savings", true);
    private final JCheckBox daily = new JCheckBox("Today's spending plan", true);
    private final JCheckBox weekly = new JCheckBox("Weekly spending", true);
    private final JCheckBox transactions = new JCheckBox("Transactions", true);
    private final JCheckBox budget = new JCheckBox("Category budgets", true);
    private final JCheckBox quickActions = new JCheckBox("Quick actions", true);

    private final boolean customize;
    private final CardLayout cards = new CardLayout();
    private final JPanel pages = new JPanel(cards);
    private final Theme.Bar[] stepBars = new Theme.Bar[STEPS.length];
    private final JLabel[] stepLabels = new JLabel[STEPS.length];
    private final JLabel errorLabel = Theme.text(" ", Theme.BODY_BOLD, Theme.RED);
    private Theme.FlatButton backButton;
    private Theme.FlatButton nextButton;
    private int page;

    private boolean limitsEdited;
    private int quietChanges;

    private UserSettings result;

    public OnboardingDialog(Window parent, User user) {
        this(parent, user, null);
    }

    public OnboardingDialog(Window parent, User user, UserSettings initial) {
        super(parent, initial == null ? "Set up Moni" : "Edit your plan", ModalityType.APPLICATION_MODAL);
        customize = initial != null;

        setSize(840, 740);
        setMinimumSize(new Dimension(720, 600));
        setLocationRelativeTo(parent);
        setContentPane(buildContent());

        if (initial != null) loadInitial(initial);
        updateRecommendation();
        showPage(0);

        getRootPane().setDefaultButton(nextButton);
        Theme.onEscape(this, this::dispose);
    }

    public UserSettings getResult() {
        return result;
    }

    private JPanel buildContent() {
        JPanel root = new JPanel(new BorderLayout(0, 18));
        root.setBackground(Theme.BG);
        root.setBorder(new EmptyBorder(24, 28, 20, 28));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        Theme.stack(header, 0, Theme.text(customize ? "Edit your plan" : "Set up your plan",
                Theme.H1, Theme.TEXT));
        Theme.stack(header, 6, Theme.text(customize
                        ? "Update your allowance, spending limits, categories and dashboard."
                        : "Four quick steps. Tell Moni about your allowance and it will suggest "
                                + "how much you can spend each day.",
                Theme.BODY, Theme.MUTED));
        Theme.stack(header, 20, buildStepIndicator());

        pages.setOpaque(false);
        pages.add(Theme.scroll(createAllowancePage()), "0");
        pages.add(Theme.scroll(createPlanPage()), "1");
        pages.add(Theme.scroll(createCategoryPage()), "2");
        pages.add(Theme.scroll(createDashboardPage()), "3");

        JPanel card = Theme.card(new BorderLayout());
        card.setBorder(new EmptyBorder(24, 28, 24, 28));
        card.add(pages, BorderLayout.CENTER);

        backButton = new Theme.FlatButton("Back", Theme.ButtonKind.SECONDARY);
        nextButton = new Theme.FlatButton("Continue", Theme.ButtonKind.PRIMARY);
        backButton.addActionListener(e -> showPage(page - 1));
        nextButton.addActionListener(e -> next());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(backButton);
        buttons.add(nextButton);

        JPanel footer = new JPanel(new BorderLayout(12, 0));
        footer.setOpaque(false);
        if (customize) {
            Theme.FlatButton cancel = new Theme.FlatButton("Cancel", Theme.ButtonKind.LINK);
            cancel.addActionListener(e -> dispose());
            footer.add(cancel, BorderLayout.WEST);
        }
        footer.add(errorLabel, BorderLayout.CENTER);
        footer.add(buttons, BorderLayout.EAST);

        root.add(header, BorderLayout.NORTH);
        root.add(card, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);
        return root;
    }

    private JPanel buildStepIndicator() {
        JPanel steps = new JPanel(new GridLayout(1, STEPS.length, 10, 0));
        steps.setOpaque(false);
        for (int i = 0; i < STEPS.length; i++) {
            stepBars[i] = new Theme.Bar(4);
            stepLabels[i] = Theme.text((i + 1) + "  " + STEPS[i], Theme.SMALL, Theme.MUTED);
            JPanel step = new JPanel(new BorderLayout(0, 8));
            step.setOpaque(false);
            step.add(stepBars[i], BorderLayout.NORTH);
            step.add(stepLabels[i], BorderLayout.CENTER);
            steps.add(step);
        }
        return steps;
    }

    private void showPage(int newPage) {
        page = Math.max(0, Math.min(LAST_PAGE, newPage));
        cards.show(pages, String.valueOf(page));

        for (int i = 0; i < STEPS.length; i++) {
            stepBars[i].set(i <= page ? 1 : 0, Theme.ACCENT);
            stepLabels[i].setForeground(i <= page ? Theme.TEXT : Theme.MUTED);
            stepLabels[i].setFont(i == page ? Theme.font(Font.BOLD, 12) : Theme.SMALL);
        }
        backButton.setVisible(page > 0);
        nextButton.setText(page < LAST_PAGE ? "Continue" : customize ? "Save changes" : "Finish setup");
        errorLabel.setText(" ");
        if (page == 2) updateCategoryTotal();
    }

    private void next() {
        if (!validatePage()) return;
        if (page < LAST_PAGE) {
            showPage(page + 1);
        } else {
            result = collectSettings();
            dispose();
        }
    }

    private Theme.Column createAllowancePage() {
        Theme.Column col = new Theme.Column();
        col.addRow(sectionTitle("How much allowance do you get?"), 0);
        col.addRow(help("Start with the amount you normally receive. Moni turns it into a daily amount "
                + "and uses that to suggest a spending plan."), 6);

        col.addRow(fieldLabel("Allowance amount (\u20B1)"), 24);
        col.addRow(allowanceField, 6);

        col.addRow(fieldLabel("How often do you receive it?"), 16);
        Theme.styleCombo(frequencyBox);
        col.addRow(frequencyBox, 6);

        col.addRow(perDayPreview, 14);

        JPanel tip = new Theme.RoundedPanel(new BorderLayout(), 14, Theme.ACCENT_SOFT, null);
        tip.setBorder(new EmptyBorder(12, 14, 12, 14));
        tip.add(new Theme.WrapText("Example: if you get \u20B13,500 every week, Moni works out "
                + "\u20B13,500 \u00F7 7 = \u20B1500 a day.", Theme.BODY, Theme.TEXT), BorderLayout.CENTER);
        col.addRow(tip, 24);

        allowanceField.getDocument().addDocumentListener(Theme.onChange(this::updateRecommendation));
        frequencyBox.addActionListener(e -> updateRecommendation());
        return col;
    }

    private Theme.Column createPlanPage() {
        Theme.Column col = new Theme.Column();
        col.addRow(sectionTitle("Your suggested spending plan"), 0);
        col.addRow(help(String.format("Moni suggests spending %d%% of your daily allowance, saving %d%%, "
                        + "and keeping %d%% spare for surprises. These are only starting values.",
                percent(SPENDING_RATE), percent(SAVINGS_RATE), percent(BUFFER_RATE))), 6);

        JPanel summary = new Theme.RoundedPanel(new GridBagLayout(), 14, Theme.ACCENT_SOFT, null);
        summary.setBorder(new EmptyBorder(14, 16, 14, 16));
        summaryRow(summary, 0, "Daily allowance", dailyAllowanceValue, true);
        summaryRow(summary, 1, String.format("Spending (%d%%)", percent(SPENDING_RATE)), spendingValue, false);
        summaryRow(summary, 2, String.format("Savings (%d%%)", percent(SAVINGS_RATE)), savingsValue, false);
        summaryRow(summary, 3, String.format("Spare for surprises (%d%%)", percent(BUFFER_RATE)), bufferValue, false);
        summaryRow(summary, 4, "Suggested weekly limit", weeklyValue, true);
        col.addRow(summary, 18);

        col.addRow(Theme.text("Your limits", Theme.font(Font.BOLD, 16), Theme.TEXT), 20);
        col.addRow(help("These start with Moni's suggestion. Change them if your real needs are different."), 4);

        JPanel limits = new JPanel(new GridBagLayout());
        limits.setOpaque(false);
        labelledField(limits, 0, "Daily spending limit (\u20B1)", dailyLimitField);
        labelledField(limits, 1, "Weekly spending limit (\u20B1)", weeklyLimitField);
        col.addRow(limits, 10);

        Theme.FlatButton reset = new Theme.FlatButton("Use Moni's suggestion", Theme.ButtonKind.LINK);
        reset.addActionListener(e -> applySuggestedLimits());
        JPanel resetRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        resetRow.setOpaque(false);
        resetRow.add(reset);
        col.addRow(resetRow, 6);

        Runnable limitsChanged = () -> {
            if (quietChanges == 0) limitsEdited = true;
            refreshCategorySuggestions();
        };
        dailyLimitField.getDocument().addDocumentListener(Theme.onChange(limitsChanged));
        weeklyLimitField.getDocument().addDocumentListener(Theme.onChange(limitsChanged));
        return col;
    }

    private static void summaryRow(JPanel panel, int row, String name, JLabel value, boolean strong) {
        GridBagConstraints l = new GridBagConstraints();
        l.gridx = 0;
        l.gridy = row;
        l.weightx = 1;
        l.anchor = GridBagConstraints.WEST;
        l.insets = new Insets(row == 0 ? 0 : 6, 0, 0, 12);
        panel.add(Theme.text(name, strong ? Theme.BODY_BOLD : Theme.BODY, Theme.TEXT), l);

        GridBagConstraints v = (GridBagConstraints) l.clone();
        v.gridx = 1;
        v.weightx = 0;
        v.anchor = GridBagConstraints.EAST;
        v.insets = new Insets(row == 0 ? 0 : 6, 0, 0, 0);
        value.setFont(strong ? Theme.font(Font.BOLD, 16) : Theme.BODY_BOLD);
        value.setForeground(strong ? Theme.ACCENT : Theme.TEXT);
        panel.add(value, v);
    }

    private static void labelledField(JPanel panel, int row, String label, JTextField field) {
        GridBagConstraints l = new GridBagConstraints();
        l.gridx = 0;
        l.gridy = row;
        l.anchor = GridBagConstraints.WEST;
        l.insets = new Insets(6, 0, 6, 16);
        panel.add(Theme.text(label, Theme.BODY_BOLD, Theme.TEXT), l);

        GridBagConstraints f = new GridBagConstraints();
        f.gridx = 1;
        f.gridy = row;
        f.weightx = 1;
        f.fill = GridBagConstraints.HORIZONTAL;
        f.insets = new Insets(6, 0, 6, 0);
        panel.add(field, f);
    }

    private Theme.Column createCategoryPage() {
        Theme.Column col = new Theme.Column();
        col.addRow(sectionTitle("Pick your spending categories"), 0);
        col.addRow(help("Tick the categories you actually use. Moni suggests a weekly budget (\u20B1 per week) "
                + "for each one based on your weekly limit, and you can change any amount."), 6);

        JPanel grid = new JPanel(new GridLayout(0, 2, 24, 10));
        grid.setOpaque(false);
        for (String category : CATEGORIES) {
            JCheckBox check = new JCheckBox(category);
            Theme.styleCheck(check);
            check.setPreferredSize(new Dimension(140, check.getPreferredSize().height));
            JTextField amount = Theme.field();
            amount.setColumns(6);
            Theme.setFieldEnabled(amount, false);

            check.addActionListener(e -> {
                Theme.setFieldEnabled(amount, check.isSelected());
                boolean empty = amount.getText().trim().isEmpty();
                if (check.isSelected() && (empty || !editedCategories.contains(category))) {
                    setQuietly(amount, Theme.plain(suggestedCategory(category)));
                }
                if (check.isSelected()) amount.requestFocusInWindow();
                updateCategoryTotal();
            });
            amount.getDocument().addDocumentListener(Theme.onChange(() -> {
                if (quietChanges == 0) editedCategories.add(category);
                updateCategoryTotal();
            }));

            categoryChecks.put(category, check);
            categoryFields.put(category, amount);

            JPanel cell = new JPanel(new BorderLayout(8, 0));
            cell.setOpaque(false);
            cell.add(check, BorderLayout.WEST);
            cell.add(amount, BorderLayout.CENTER);
            grid.add(cell);
        }
        col.addRow(grid, 16);
        col.addRow(categoryTotal, 14);
        col.addRow(help("Suggested shares of your weekly limit: Food 40%, Transportation 20%, School 20%, "
                + "Entertainment 10%, Other 10%, and 5% each for Shopping, Bills and Health."), 6);
        return col;
    }

    private double suggestedCategory(String category) {
        double weeklyLimit = Theme.parseAmount(weeklyLimitField, 0);
        double rate;
        switch (category) {
            case "Food": rate = 0.40; break;
            case "Transportation":
            case "School": rate = 0.20; break;
            case "Entertainment":
            case "Other": rate = 0.10; break;
            case "Shopping":
            case "Bills":
            case "Health": rate = 0.05; break;
            default: rate = 0;
        }
        return Math.round(weeklyLimit * rate);
    }

    private void updateCategoryTotal() {
        double total = 0;
        for (String category : CATEGORIES) {
            if (categoryChecks.get(category).isSelected()) {
                total += Theme.parseAmount(categoryFields.get(category), 0);
            }
        }
        double weeklyLimit = Theme.parseAmount(weeklyLimitField, 0);
        boolean over = total > weeklyLimit + 0.005;
        categoryTotal.setText("Total: " + Theme.money(total) + " of your " + Theme.money(weeklyLimit)
                + " weekly limit" + (over ? "  (" + Theme.money(total - weeklyLimit) + " over)" : ""));
        categoryTotal.setForeground(over ? Theme.RED : Theme.TEXT);
    }

    private Theme.Column createDashboardPage() {
        Theme.Column col = new Theme.Column();
        col.addRow(sectionTitle("Choose what your dashboard shows"), 0);
        col.addRow(help("You can change this any time with the Edit plan button."), 6);

        JPanel grid = new JPanel(new GridLayout(0, 2, 24, 16));
        grid.setOpaque(false);
        grid.add(option(daily, "What's left to spend today, based on your limits."));
        grid.add(option(wallet, "Money you have available to spend."));
        grid.add(option(savings, "How much you've set aside."));
        grid.add(option(weekly, "Progress against your weekly limit."));
        grid.add(option(budget, "Weekly progress for each spending category."));
        grid.add(option(transactions, "Searchable history of your money in and out."));
        grid.add(option(quickActions, "Buttons for adding expenses, funds and savings."));
        col.addRow(grid, 18);
        return col;
    }

    private static JPanel option(JCheckBox check, String description) {
        Theme.styleCheck(check);
        JLabel desc = Theme.text(description, Theme.SMALL, Theme.MUTED);
        desc.setBorder(new EmptyBorder(2, 26, 0, 0));
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.add(check, BorderLayout.NORTH);
        panel.add(desc, BorderLayout.CENTER);
        return panel;
    }

    private void updateRecommendation() {
        double allowance = Theme.parseAmount(allowanceField, 0);
        if (allowance <= 0) {
            for (JLabel label : new JLabel[]{dailyAllowanceValue, spendingValue, savingsValue, bufferValue, weeklyValue}) {
                label.setText(Theme.money(0));
            }
            perDayPreview.setText(" ");
            return;
        }

        double perDay = UserSettings.perDay(allowance, selectedFrequency());
        double dailySpending = Math.round(perDay * SPENDING_RATE);
        double dailySavings = Math.round(perDay * SAVINGS_RATE);
        double dailyBuffer = Math.round(perDay * BUFFER_RATE);
        double weeklySpending = Math.round(dailySpending * 7);

        dailyAllowanceValue.setText(Theme.money(perDay) + " / day");
        spendingValue.setText(Theme.money(dailySpending));
        savingsValue.setText(Theme.money(dailySavings));
        bufferValue.setText(Theme.money(dailyBuffer));
        weeklyValue.setText(Theme.money(weeklySpending) + " / week");
        perDayPreview.setText("That's about " + Theme.money(perDay) + " a day.");

        if (!limitsEdited) applySuggestedLimits();
    }

    private void applySuggestedLimits() {
        double allowance = Theme.parseAmount(allowanceField, 0);
        if (allowance <= 0) return;

        double perDay = UserSettings.perDay(allowance, selectedFrequency());
        double spending = Math.round(perDay * SPENDING_RATE);
        setQuietly(dailyLimitField, Theme.plain(spending));
        setQuietly(weeklyLimitField, Theme.plain(spending * 7));
        limitsEdited = false;
        refreshCategorySuggestions();
    }

    private void refreshCategorySuggestions() {
        for (String category : CATEGORIES) {
            if (categoryChecks.get(category).isSelected() && !editedCategories.contains(category)) {
                setQuietly(categoryFields.get(category), Theme.plain(suggestedCategory(category)));
            }
        }
        updateCategoryTotal();
    }

    private String selectedFrequency() {
        return String.valueOf(frequencyBox.getSelectedItem());
    }

    private void setQuietly(JTextField field, String text) {
        quietChanges++;
        try {
            field.setText(text);
        } finally {
            quietChanges--;
        }
    }

    private boolean validatePage() {
        switch (page) {
            case 0:
                if (Theme.parseAmount(allowanceField, -1) <= 0) {
                    return fail("Enter an allowance amount greater than zero.", allowanceField);
                }
                return true;

            case 1: {
                double dailyLimit = Theme.parseAmount(dailyLimitField, -1);
                double weeklyLimit = Theme.parseAmount(weeklyLimitField, -1);
                if (dailyLimit <= 0) return fail("Enter a daily limit greater than zero.", dailyLimitField);
                if (weeklyLimit <= 0) return fail("Enter a weekly limit greater than zero.", weeklyLimitField);
                if (weeklyLimit < dailyLimit) {
                    return fail("Your weekly limit can't be smaller than your daily limit.", weeklyLimitField);
                }
                return true;
            }

            case 2: {
                boolean any = false;
                for (String category : CATEGORIES) {
                    if (!categoryChecks.get(category).isSelected()) continue;
                    any = true;
                    JTextField field = categoryFields.get(category);
                    if (Theme.parseAmount(field, -1) <= 0) {
                        return fail("Enter a weekly budget for " + category + ".", field);
                    }
                }
                return any || fail("Pick at least one category.", null);
            }

            default: {
                boolean any = wallet.isSelected() || savings.isSelected() || daily.isSelected()
                        || weekly.isSelected() || transactions.isSelected() || budget.isSelected()
                        || quickActions.isSelected();
                return any || fail("Pick at least one thing to show on your dashboard.", null);
            }
        }
    }

    private boolean fail(String message, JComponent focus) {
        errorLabel.setText(message);
        if (focus != null) focus.requestFocusInWindow();
        return false;
    }

    private UserSettings collectSettings() {
        UserSettings s = new UserSettings();
        s.setAllowanceAmount(Theme.parseAmount(allowanceField, 0));
        s.setAllowanceFrequency(selectedFrequency());
        s.setDailyLimit(Theme.parseAmount(dailyLimitField, 0));
        s.setWeeklyLimit(Theme.parseAmount(weeklyLimitField, 0));

        s.setShowWallet(wallet.isSelected());
        s.setShowSavings(savings.isSelected());
        s.setShowDaily(daily.isSelected());
        s.setShowWeekly(weekly.isSelected());
        s.setShowTransactions(transactions.isSelected());
        s.setShowBudget(budget.isSelected());
        s.setShowQuickActions(quickActions.isSelected());
        s.setSetupCompleted(true);

        for (String category : CATEGORIES) {
            if (categoryChecks.get(category).isSelected()) {
                s.setCategoryLimit(category, Theme.parseAmount(categoryFields.get(category), 0));
            }
        }
        return s;
    }

    private void loadInitial(UserSettings s) {
        quietChanges++;
        try {
            allowanceField.setText(Theme.plain(s.getAllowanceAmount()));
            frequencyBox.setSelectedItem(s.getAllowanceFrequency());
            dailyLimitField.setText(Theme.plain(s.getDailyLimit()));
            weeklyLimitField.setText(Theme.plain(s.getWeeklyLimit()));

            wallet.setSelected(s.isShowWallet());
            savings.setSelected(s.isShowSavings());
            daily.setSelected(s.isShowDaily());
            weekly.setSelected(s.isShowWeekly());
            transactions.setSelected(s.isShowTransactions());
            budget.setSelected(s.isShowBudget());
            quickActions.setSelected(s.isShowQuickActions());

            for (Map.Entry<String, Double> entry : s.getCategoryLimits().entrySet()) {
                JCheckBox check = categoryChecks.get(entry.getKey());
                JTextField field = categoryFields.get(entry.getKey());
                if (check == null || field == null) continue;
                check.setSelected(true);
                Theme.setFieldEnabled(field, true);
                field.setText(Theme.plain(entry.getValue()));
                editedCategories.add(entry.getKey());
            }
        } finally {
            quietChanges--;
        }
        limitsEdited = true;
    }

    private static JLabel sectionTitle(String text) {
        return Theme.text(text, Theme.font(Font.BOLD, 20), Theme.TEXT);
    }

    private static JLabel fieldLabel(String text) {
        return Theme.text(text, Theme.BODY_BOLD, Theme.TEXT);
    }

    private static Theme.WrapText help(String text) {
        return new Theme.WrapText(text, Theme.BODY, Theme.MUTED);
    }

    private static int percent(double rate) {
        return (int) Math.round(rate * 100);
    }
}