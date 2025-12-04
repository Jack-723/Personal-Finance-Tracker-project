package com.financetracker.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Bill/Subscription Model Class
 * Represents recurring bills and subscriptions
 */
public class Bill {
    private UUID billId;
    private UUID userId;
    private UUID categoryId;
    private String name;
    private BigDecimal amount;
    private BillingCycle billingCycle;
    private int dueDay;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean isActive;
    private int reminderDays;
    private LocalDate lastPaymentDate;
    private LocalDate nextPaymentDate;
    private String description;
    private String vendor;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Transient field for display
    private String categoryName;

    public enum BillingCycle {
        DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY
    }

    // Constructors
    public Bill() {
        this.billId = UUID.randomUUID();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isActive = true;
        this.reminderDays = 3;
        this.billingCycle = BillingCycle.MONTHLY;
    }

    public Bill(String name, BigDecimal amount, BillingCycle billingCycle) {
        this();
        this.name = name;
        this.amount = amount;
        this.billingCycle = billingCycle;
    }

    // Getters and Setters
    public UUID getBillId() {
        return billId;
    }

    public void setBillId(UUID billId) {
        this.billId = billId;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BillingCycle getBillingCycle() {
        return billingCycle;
    }

    public void setBillingCycle(BillingCycle billingCycle) {
        this.billingCycle = billingCycle;
    }

    public String getBillingCycleString() {
        return billingCycle != null ? billingCycle.name() : null;
    }

    public void setBillingCycleFromString(String cycle) {
        if (cycle != null) {
            this.billingCycle = BillingCycle.valueOf(cycle);
        }
    }

    public int getDueDay() {
        return dueDay;
    }

    public void setDueDay(int dueDay) {
        this.dueDay = dueDay;
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

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public int getReminderDays() {
        return reminderDays;
    }

    public void setReminderDays(int reminderDays) {
        this.reminderDays = reminderDays;
    }

    public LocalDate getLastPaymentDate() {
        return lastPaymentDate;
    }

    public void setLastPaymentDate(LocalDate lastPaymentDate) {
        this.lastPaymentDate = lastPaymentDate;
    }

    public LocalDate getNextPaymentDate() {
        return nextPaymentDate;
    }

    public void setNextPaymentDate(LocalDate nextPaymentDate) {
        this.nextPaymentDate = nextPaymentDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
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
     * Calculate the next payment date based on billing cycle
     */
    public LocalDate calculateNextPaymentDate() {
        LocalDate today = LocalDate.now();
        LocalDate next;

        if (lastPaymentDate == null) {
            if (startDate != null && startDate.isAfter(today)) {
                return startDate;
            }
            next = today;
        } else {
            next = lastPaymentDate;
        }

        switch (billingCycle) {
            case DAILY:
                next = next.plusDays(1);
                break;
            case WEEKLY:
                next = next.plusWeeks(1);
                break;
            case MONTHLY:
                next = next.plusMonths(1);
                if (dueDay > 0 && dueDay <= 28) {
                    next = next.withDayOfMonth(dueDay);
                }
                break;
            case QUARTERLY:
                next = next.plusMonths(3);
                break;
            case YEARLY:
                next = next.plusYears(1);
                break;
        }

        while (next.isBefore(today) || next.isEqual(today)) {
            switch (billingCycle) {
                case DAILY:
                    next = next.plusDays(1);
                    break;
                case WEEKLY:
                    next = next.plusWeeks(1);
                    break;
                case MONTHLY:
                    next = next.plusMonths(1);
                    break;
                case QUARTERLY:
                    next = next.plusMonths(3);
                    break;
                case YEARLY:
                    next = next.plusYears(1);
                    break;
            }
        }

        return next;
    }

    /**
     * Get days until next payment
     */
    public long getDaysUntilDue() {
        if (nextPaymentDate == null) {
            return -1;
        }
        return ChronoUnit.DAYS.between(LocalDate.now(), nextPaymentDate);
    }

    /**
     * Get status based on next payment date
     */
    public String getStatus() {
        if (!isActive) {
            return "INACTIVE";
        }
        if (endDate != null && LocalDate.now().isAfter(endDate)) {
            return "EXPIRED";
        }

        long daysUntil = getDaysUntilDue();
        if (daysUntil < 0) {
            return "OVERDUE";
        } else if (daysUntil == 0) {
            return "DUE TODAY";
        } else if (daysUntil <= reminderDays) {
            return "DUE SOON";
        } else {
            return "UPCOMING";
        }
    }

    /**
     * Calculate monthly cost (normalized)
     */
    public BigDecimal getMonthlyCost() {
        if (amount == null) return BigDecimal.ZERO;

        switch (billingCycle) {
            case DAILY:
                return amount.multiply(BigDecimal.valueOf(30));
            case WEEKLY:
                return amount.multiply(BigDecimal.valueOf(4.33));
            case MONTHLY:
                return amount;
            case QUARTERLY:
                return amount.divide(BigDecimal.valueOf(3), 2, java.math.RoundingMode.HALF_UP);
            case YEARLY:
                return amount.divide(BigDecimal.valueOf(12), 2, java.math.RoundingMode.HALF_UP);
            default:
                return amount;
        }
    }

    /**
     * Calculate yearly cost
     */
    public BigDecimal getYearlyCost() {
        if (amount == null) return BigDecimal.ZERO;

        switch (billingCycle) {
            case DAILY:
                return amount.multiply(BigDecimal.valueOf(365));
            case WEEKLY:
                return amount.multiply(BigDecimal.valueOf(52));
            case MONTHLY:
                return amount.multiply(BigDecimal.valueOf(12));
            case QUARTERLY:
                return amount.multiply(BigDecimal.valueOf(4));
            case YEARLY:
                return amount;
            default:
                return amount;
        }
    }

    @Override
    public String toString() {
        return "Bill{" +
                "name='" + name + '\'' +
                ", amount=" + amount +
                ", billingCycle=" + billingCycle +
                ", nextPaymentDate=" + nextPaymentDate +
                '}';
    }
}