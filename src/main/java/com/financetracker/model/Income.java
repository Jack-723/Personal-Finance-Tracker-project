package com.financetracker.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Income Model Class - Extends Transaction (Polymorphism)
 * Represents money coming in (salary, investments, etc.)
 */
public class Income extends Transaction {
    private UUID incomeId;
    private String source;
    private LocalDate incomeDate;
    private boolean isRecurring;
    private String recurringFrequency;
    private Integer recurringDay;

    // Constructors
    public Income() {
        super();
        this.incomeId = UUID.randomUUID();
        this.isRecurring = false;
    }

    public Income(BigDecimal amount, String source, LocalDate incomeDate) {
        super(amount, incomeDate);
        this.incomeId = UUID.randomUUID();
        this.source = source;
        this.incomeDate = incomeDate;
        this.isRecurring = false;
    }

    // ===== POLYMORPHIC METHODS (Override abstract methods from Transaction) =====

    @Override
    public TransactionType getTransactionType() {
        return TransactionType.INCOME;
    }

    @Override
    public String getDisplayName() {
        return source != null ? source : "Unknown Source";
    }

    @Override
    public BigDecimal getBalanceEffect() {
        // Income adds to balance (positive effect)
        return amount != null ? amount : BigDecimal.ZERO;
    }

    @Override
    public String getSummary() {
        return String.format("Income from %s: +$%,.2f on %s",
                getDisplayName(),
                amount != null ? amount : BigDecimal.ZERO,
                incomeDate != null ? incomeDate.toString() : "N/A");
    }

    @Override
    public boolean isValid() {
        return amount != null &&
                amount.compareTo(BigDecimal.ZERO) > 0 &&
                source != null &&
                !source.trim().isEmpty() &&
                incomeDate != null;
    }

    // ===== INCOME-SPECIFIC METHODS =====

    /**
     * Calculate projected annual income if this is recurring
     */
    public BigDecimal getProjectedAnnualAmount() {
        if (!isRecurring || amount == null || recurringFrequency == null) {
            return amount;
        }

        switch (recurringFrequency.toUpperCase()) {
            case "DAILY":
                return amount.multiply(BigDecimal.valueOf(365));
            case "WEEKLY":
                return amount.multiply(BigDecimal.valueOf(52));
            case "BIWEEKLY":
                return amount.multiply(BigDecimal.valueOf(26));
            case "MONTHLY":
                return amount.multiply(BigDecimal.valueOf(12));
            case "QUARTERLY":
                return amount.multiply(BigDecimal.valueOf(4));
            case "YEARLY":
                return amount;
            default:
                return amount;
        }
    }

    /**
     * Calculate projected monthly income if this is recurring
     */
    public BigDecimal getProjectedMonthlyAmount() {
        if (!isRecurring || amount == null || recurringFrequency == null) {
            return amount;
        }

        switch (recurringFrequency.toUpperCase()) {
            case "DAILY":
                return amount.multiply(BigDecimal.valueOf(30));
            case "WEEKLY":
                return amount.multiply(BigDecimal.valueOf(4.33));
            case "BIWEEKLY":
                return amount.multiply(BigDecimal.valueOf(2.17));
            case "MONTHLY":
                return amount;
            case "QUARTERLY":
                return amount.divide(BigDecimal.valueOf(3), 2, java.math.RoundingMode.HALF_UP);
            case "YEARLY":
                return amount.divide(BigDecimal.valueOf(12), 2, java.math.RoundingMode.HALF_UP);
            default:
                return amount;
        }
    }

    // ===== GETTERS AND SETTERS =====

    public UUID getIncomeId() {
        return incomeId;
    }

    public void setIncomeId(UUID incomeId) {
        this.incomeId = incomeId;
        this.transactionId = incomeId; // Keep in sync with parent
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public LocalDate getIncomeDate() {
        return incomeDate;
    }

    public void setIncomeDate(LocalDate incomeDate) {
        this.incomeDate = incomeDate;
        this.transactionDate = incomeDate; // Keep in sync with parent
    }

    public boolean isRecurring() {
        return isRecurring;
    }

    public void setRecurring(boolean recurring) {
        isRecurring = recurring;
    }

    public String getRecurringFrequency() {
        return recurringFrequency;
    }

    public void setRecurringFrequency(String recurringFrequency) {
        this.recurringFrequency = recurringFrequency;
    }

    public Integer getRecurringDay() {
        return recurringDay;
    }

    public void setRecurringDay(Integer recurringDay) {
        this.recurringDay = recurringDay;
    }

    @Override
    public String toString() {
        return "Income{" +
                "source='" + source + '\'' +
                ", amount=" + amount +
                ", date=" + incomeDate +
                ", recurring=" + isRecurring +
                '}';
    }
}