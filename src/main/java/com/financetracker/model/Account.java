package com.financetracker.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Account Model Class
 * Represents bank accounts, credit cards, cash, and other financial accounts
 */
public class Account {
    private UUID accountId;
    private UUID userId;
    private String accountName;
    private AccountType accountType;
    private BigDecimal balance;
    private String currency;
    private String institutionName;
    private String accountNumber;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum AccountType {
        CHECKING, SAVINGS, CREDIT_CARD, CASH, INVESTMENT, OTHER
    }

    // Constructors
    public Account() {
        this.accountId = UUID.randomUUID();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isActive = true;
        this.balance = BigDecimal.ZERO;
        this.currency = "USD";
        this.accountType = AccountType.CHECKING;
    }

    public Account(String accountName, AccountType accountType, BigDecimal balance) {
        this();
        this.accountName = accountName;
        this.accountType = accountType;
        this.balance = balance;
    }

    // Getters and Setters
    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public String getAccountTypeString() {
        return accountType != null ? accountType.name() : null;
    }

    public void setAccountTypeFromString(String type) {
        if (type != null) {
            this.accountType = AccountType.valueOf(type);
        }
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    /**
     * Get masked account number for display (shows last 4 digits only)
     */
    public String getMaskedAccountNumber() {
        if (accountNumber == null || accountNumber.length() < 4) {
            return accountNumber;
        }
        String lastFour = accountNumber.substring(accountNumber.length() - 4);
        return "****" + lastFour;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
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

    /**
     * Check if this is a liability account (negative impact on net worth)
     */
    public boolean isLiability() {
        return accountType == AccountType.CREDIT_CARD;
    }

    /**
     * Check if this is an asset account (positive impact on net worth)
     */
    public boolean isAsset() {
        return accountType != AccountType.CREDIT_CARD;
    }

    /**
     * Get display-friendly account type name
     */
    public String getAccountTypeDisplayName() {
        if (accountType == null) return "Unknown";
        switch (accountType) {
            case CHECKING:
                return "Checking";
            case SAVINGS:
                return "Savings";
            case CREDIT_CARD:
                return "Credit Card";
            case CASH:
                return "Cash";
            case INVESTMENT:
                return "Investment";
            case OTHER:
                return "Other";
            default:
                return accountType.name();
        }
    }

    /**
     * Get icon name based on account type (for UI)
     */
    public String getIconName() {
        if (accountType == null) return "account";
        switch (accountType) {
            case CHECKING:
                return "bank";
            case SAVINGS:
                return "piggy-bank";
            case CREDIT_CARD:
                return "credit-card";
            case CASH:
                return "cash";
            case INVESTMENT:
                return "chart-line";
            case OTHER:
            default:
                return "wallet";
        }
    }

    @Override
    public String toString() {
        return "Account{" +
                "accountName='" + accountName + '\'' +
                ", accountType=" + accountType +
                ", balance=" + balance +
                ", currency='" + currency + '\'' +
                '}';
    }
}