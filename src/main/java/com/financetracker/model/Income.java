package com.financetracker.model;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Income Model Class
 */
public class Income {
    private UUID incomeId;
    private UUID userId;
    private UUID categoryId;
    private BigDecimal amount;
    private String source;
    private String description;
    private LocalDate incomeDate;
    private boolean isRecurring;
    private String recurringFrequency;
    private Integer recurringDay;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Transient field for display
    private String categoryName;
    
    // Constructors
    public Income() {
        this.incomeId = UUID.randomUUID();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isRecurring = false;
    }
    
    public Income(BigDecimal amount, String source, LocalDate incomeDate) {
        this();
        this.amount = amount;
        this.source = source;
        this.incomeDate = incomeDate;
    }
    
    // Getters and Setters
    public UUID getIncomeId() {
        return incomeId;
    }
    
    public void setIncomeId(UUID incomeId) {
        this.incomeId = incomeId;
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
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public LocalDate getIncomeDate() {
        return incomeDate;
    }
    
    public void setIncomeDate(LocalDate incomeDate) {
        this.incomeDate = incomeDate;
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
    
    @Override
    public String toString() {
        return "Income{" +
                "source='" + source + '\'' +
                ", amount=" + amount +
                ", date=" + incomeDate +
                '}';
    }
}
