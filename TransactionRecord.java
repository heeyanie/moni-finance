package app;

public class TransactionRecord {
    private final String date;
    private final String flowType;
    private final String categorySource;
    private final String description;
    private final double amount;

    public TransactionRecord(String date, String flowType, String categorySource,
                             String description, double amount) {
        this.date = date;
        this.flowType = flowType;
        this.categorySource = categorySource;
        this.description = description;
        this.amount = amount;
    }

    public String getDate() {
        return date;
    }

    public String getFlowType() {
        return flowType;
    }

    public String getCategorySource() {
        return categorySource;
    }

    public String getDescription() {
        return description;
    }

    public double getAmount() {
        return amount;
    }
}