package com.financetracker.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Expense Model Class - Extends Transaction (Polymorphism)
 * Represents money going out (purchases, bills, etc.)
 */
public class Expense extends Transaction {
    private UUID expenseId;
    private String vendor;
    private LocalDate expenseDate;
    private String paymentMethod;
    private String receiptUrl;

    // Constructors
    public Expense() {
        super();
        this.expenseId = UUID.randomUUID();
    }

    public Expense(BigDecimal amount, String vendor, LocalDate expenseDate) {
        super(amount, expenseDate);
        this.expenseId = UUID.randomUUID();
        this.vendor = vendor;
        this.expenseDate = expenseDate;
    }

    // ===== POLYMORPHIC METHODS (Override abstract methods from Transaction) =====

    @Override
    public TransactionType getTransactionType() {
        return TransactionType.EXPENSE;
    }

    @Override
    public String getDisplayName() {
        return vendor != null ? vendor : "Unknown Vendor";
    }

    @Override
    public BigDecimal getBalanceEffect() {
        // Expense subtracts from balance (negative effect)
        return amount != null ? amount.negate() : BigDecimal.ZERO;
    }

    @Override
    public String getSummary() {
        return String.format("Expense at %s: -$%,.2f on %s",
                getDisplayName(),
                amount != null ? amount : BigDecimal.ZERO,
                expenseDate != null ? expenseDate.toString() : "N/A");
    }

    @Override
    public boolean isValid() {
        return amount != null &&
                amount.compareTo(BigDecimal.ZERO) > 0 &&
                vendor != null &&
                !vendor.trim().isEmpty() &&
                expenseDate != null;
    }

    // ===== EXPENSE-SPECIFIC METHODS =====

    /**
     * Check if this expense has a receipt
     */
    public boolean hasReceipt() {
        return receiptUrl != null && !receiptUrl.trim().isEmpty();
    }

    /**
     * Get payment method display name
     */
    public String getPaymentMethodDisplayName() {
        if (paymentMethod == null) return "Unknown";

        switch (paymentMethod.toUpperCase()) {
            case "CREDIT_CARD":
                return "Credit Card";
            case "DEBIT_CARD":
                return "Debit Card";
            case "CASH":
                return "Cash";
            case "BANK_TRANSFER":
                return "Bank Transfer";
            case "CHECK":
                return "Check";
            case "DIGITAL_WALLET":
                return "Digital Wallet";
            default:
                return paymentMethod;
        }
    }

    /**
     * Check if this is a large expense (over threshold)
     */
    public boolean isLargeExpense(BigDecimal threshold) {
        return amount != null && amount.compareTo(threshold) > 0;
    }

    // ===== GETTERS AND SETTERS =====

    public UUID getExpenseId() {
        return expenseId;
    }

    public void setExpenseId(UUID expenseId) {
        this.expenseId = expenseId;
        this.transactionId = expenseId; // Keep in sync with parent
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public void setExpenseDate(LocalDate expenseDate) {
        this.expenseDate = expenseDate;
        this.transactionDate = expenseDate; // Keep in sync with parent
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getReceiptUrl() {
        return receiptUrl;
    }

    public void setReceiptUrl(String receiptUrl) {
        this.receiptUrl = receiptUrl;
    }

    @Override
    public String toString() {
        return "Expense{" +
                "vendor='" + vendor + '\'' +
                ", amount=" + amount +
                ", date=" + expenseDate +
                ", paymentMethod='" + paymentMethod + '\'' +
                '}';
    }
}
