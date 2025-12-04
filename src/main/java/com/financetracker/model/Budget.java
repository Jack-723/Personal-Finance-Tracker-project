package com.financetracker.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Budget Model Class
 */
public class Budget {
    private UUID budgetId;
    private UUID userId;
    private UUID categoryId;
    private String budgetName;
    private BigDecimal amountLimit;
    private String period;
    private LocalDate startDate;
    private LocalDate endDate;
    private int alertThreshold;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Transient fields for display and calculations
    private String categoryName;
    private BigDecimal spentAmount;
    private BigDecimal remainingAmount;
    private double percentageUsed;
    
    // Constructors
    public Budget() {
        this.budgetId = UUID.randomUUID();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isActive = true;
        this.alertThreshold = 80;
        this.spentAmount = BigDecimal.ZERO;
    }
    
    public Budget(String budgetName, BigDecimal amountLimit, String period) {
        this();
        this.budgetName = budgetName;
        this.amountLimit = amountLimit;
        this.period = period;
    }
    
    // Getters and Setters
    public UUID getBudgetId() {
        return budgetId;
    }
    
    public void setBudgetId(UUID budgetId) {
        this.budgetId = budgetId;
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
    
    public String getBudgetName() {
        return budgetName;
    }
    
    public void setBudgetName(String budgetName) {
        this.budgetName = budgetName;
    }
    
    public BigDecimal getAmountLimit() {
        return amountLimit;
    }
    
    public void setAmountLimit(BigDecimal amountLimit) {
        this.amountLimit = amountLimit;
    }
    
    public String getPeriod() {
        return period;
    }
    
    public void setPeriod(String period) {
        this.period = period;
    }
    
    public LocalDate getStartDate() {
        return startDate;
    }
    
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }
    
    public LocalDate getEndDate() {
        return endDate;
    }
    
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
    
    public int getAlertThreshold() {
        return alertThreshold;
    }
    
    public void setAlertThreshold(int alertThreshold) {
        this.alertThreshold = alertThreshold;
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
    
    public String getCategoryName() {
        return categoryName;
    }
    
    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }
    
    public BigDecimal getSpentAmount() {
        return spentAmount;
    }
    
    public void setSpentAmount(BigDecimal spentAmount) {
        this.spentAmount = spentAmount;
        calculateRemainingAndPercentage();
    }
    
    public BigDecimal getRemainingAmount() {
        return remainingAmount;
    }
    
    public double getPercentageUsed() {
        return percentageUsed;
    }
    
    private void calculateRemainingAndPercentage() {
        if (amountLimit != null && spentAmount != null) {
            remainingAmount = amountLimit.subtract(spentAmount);
            if (amountLimit.compareTo(BigDecimal.ZERO) > 0) {
                percentageUsed = spentAmount.divide(amountLimit, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue();
            }
        }
    }
    
    /**
     * Get budget status based on percentage used
     */
    public String getStatus() {
        if (percentageUsed >= 100) {
            return "EXCEEDED";
        } else if (percentageUsed >= 90) {
            return "DANGER";
        } else if (percentageUsed >= alertThreshold) {
            return "WARNING";
        } else {
            return "OK";
        }
    }
    
    @Override
    public String toString() {
        return "Budget{" +
                "budgetName='" + budgetName + '\'' +
                ", amountLimit=" + amountLimit +
                ", spentAmount=" + spentAmount +
                ", percentageUsed=" + percentageUsed +
                '}';
    }
}
