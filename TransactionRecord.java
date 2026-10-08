package app;

import java.time.LocalDate;

/** One row of the transactions table: money in or out of the wallet on a given day. */
public class TransactionRecord {

    // Values stored in the flow_type column.
    public static final String MONEY_IN = "Money In";
    public static final String MONEY_OUT = "Money Out";

    // Values stored in category_source that aren't spending categories.
    public static final String SAVINGS = "Savings";
    public static final String ALLOWANCE = "Allowance";
    public static final String OTHER_FUNDS = "Other Funds";

    private final LocalDate date;
    private final String flowType;
    private final String categorySource;
    private final String description;
    private final double amount;

    /** Money in has a positive amount and money out a negative one. */
    public TransactionRecord(LocalDate date, String flowType, String categorySource,
                             String description, double amount) {
        this.date = date;
        this.flowType = flowType;
        this.categorySource = categorySource;
        this.description = description == null ? "" : description;
        this.amount = amount;
    }

    public LocalDate getDate() {
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

    /** Spending is money out, except money moved into savings (that money is still yours). */
    public boolean isSpending() {
        return MONEY_OUT.equals(flowType) && !SAVINGS.equals(categorySource);
    }
}
