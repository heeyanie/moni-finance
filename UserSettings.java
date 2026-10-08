package app;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** The user's budget plan and dashboard choices, stored in the user_settings and budgets tables. */
public class UserSettings {
    private double allowanceAmount;
    private String allowanceFrequency = "Daily";
    private double dailyLimit;
    private double weeklyLimit;

    private boolean showWallet = true;
    private boolean showSavings = true;
    private boolean showDaily = true;
    private boolean showWeekly = true;
    private boolean showTransactions = true;
    private boolean showBudget = true;
    private boolean showQuickActions = true;
    private boolean setupCompleted;

    private final Map<String, Double> categoryLimits = new LinkedHashMap<>();

    public double getAllowanceAmount() {
        return allowanceAmount;
    }

    public void setAllowanceAmount(double value) {
        this.allowanceAmount = Math.max(0, value);
    }

    public String getAllowanceFrequency() {
        return allowanceFrequency;
    }

    public void setAllowanceFrequency(String value) {
        if (value == null || value.trim().isEmpty()) {
            this.allowanceFrequency = "Daily";
        } else {
            this.allowanceFrequency = value.trim();
        }
    }

    /** The allowance worked out per day, rounded to whole pesos. */
    public double getDailyAllowance() {
        return perDay(allowanceAmount, allowanceFrequency);
    }

    public double getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(double value) {
        this.dailyLimit = Math.max(0, value);
    }

    public double getWeeklyLimit() {
        return weeklyLimit;
    }

    public void setWeeklyLimit(double value) {
        this.weeklyLimit = Math.max(0, value);
    }

    public boolean isShowWallet() { return showWallet; }
    public void setShowWallet(boolean value) { this.showWallet = value; }

    public boolean isShowSavings() { return showSavings; }
    public void setShowSavings(boolean value) { this.showSavings = value; }

    public boolean isShowDaily() { return showDaily; }
    public void setShowDaily(boolean value) { this.showDaily = value; }

    public boolean isShowWeekly() { return showWeekly; }
    public void setShowWeekly(boolean value) { this.showWeekly = value; }

    public boolean isShowTransactions() { return showTransactions; }
    public void setShowTransactions(boolean value) { this.showTransactions = value; }

    public boolean isShowBudget() { return showBudget; }
    public void setShowBudget(boolean value) { this.showBudget = value; }

    public boolean isShowQuickActions() { return showQuickActions; }
    public void setShowQuickActions(boolean value) { this.showQuickActions = value; }

    public boolean isSetupCompleted() { return setupCompleted; }
    public void setSetupCompleted(boolean value) { this.setupCompleted = value; }

    public Map<String, Double> getCategoryLimits() {
        return Collections.unmodifiableMap(categoryLimits);
    }

    public void setCategoryLimit(String category, double limit) {
        if (category != null && !category.trim().isEmpty()) {
            categoryLimits.put(category.trim(), Math.max(0, limit));
        }
    }

    public void removeCategoryLimit(String category) {
        if (category != null) {
            categoryLimits.remove(category.trim());
        }
    }

    public double getCategoryLimit(String category) {
        if (category == null) return 0.0;
        return categoryLimits.getOrDefault(category.trim(), 0.0);
    }

    /** Turns an allowance into a daily amount: weekly is split over 7 days, monthly over 30. */
    public static double perDay(double amount, String frequency) {
        if ("Weekly".equalsIgnoreCase(frequency)) return Math.round(amount / 7.0);
        if ("Monthly".equalsIgnoreCase(frequency)) return Math.round(amount / 30.0);
        return Math.round(amount);
    }
}
