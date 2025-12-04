package com.financetracker.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Abstract base class for financial transactions
 * Demonstrates polymorphism - Income and Expense extend this class
 */
public abstract class Transaction {
    protected UUID transactionId;
    protected UUID userId;
    protected UUID categoryId;
    protected BigDecimal amount;
    protected String description;
    protected LocalDate transactionDate;
    protected LocalDateTime createdAt;
    protected LocalDateTime updatedAt;

    // Transient field for display
    protected String categoryName;

    // Constructor
    public Transaction() {
        this.transactionId = UUID.randomUUID();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Transaction(BigDecimal amount, LocalDate transactionDate) {
        this();
        this.amount = amount;
        this.transactionDate = transactionDate;
    }

    // Abstract methods - must be implemented by subclasses (Polymorphism)

    /**
     * Get the type of transaction (INCOME or EXPENSE)
     */
    public abstract TransactionType getTransactionType();

    /**
     * Get the display name for the transaction source/vendor
     */
    public abstract String getDisplayName();

    /**
     * Get the effect on balance (positive for income, negative for expense)
     */
    public abstract BigDecimal getBalanceEffect();

    /**
     * Get a formatted summary of the transaction
     */
    public abstract String getSummary();

    /**
     * Validate the transaction data
     */
    public abstract boolean isValid();

    // Enum for transaction types
    public enum TransactionType {
        INCOME("Income", "+"),
        EXPENSE("Expense", "-");

        private final String displayName;
        private final String symbol;

        TransactionType(String displayName, String symbol) {
            this.displayName = displayName;
            this.symbol = symbol;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getSymbol() {
            return symbol;
        }
    }

    // Common getters and setters
    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    /**
     * Get formatted amount with currency symbol
     */
    public String getFormattedAmount() {
        return String.format("$%,.2f", amount);
    }

    /**
     * Get formatted amount with sign
     */
    public String getFormattedAmountWithSign() {
        String symbol = getTransactionType().getSymbol();
        return symbol + String.format("$%,.2f", amount);
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "type=" + getTransactionType() +
                ", amount=" + amount +
                ", date=" + transactionDate +
                ", name='" + getDisplayName() + '\'' +
                '}';
    }
}