package com.financetracker.model;


import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Category Model Class
 */
public class Category {
    private UUID categoryId;
    private UUID userId;
    private String categoryName;
    private CategoryType categoryType;
    private UUID parentCategoryId;
    private String colorCode;
    private String iconName;
    private boolean isDefault;
    private LocalDateTime createdAt;
    
    public enum CategoryType {
        INCOME, EXPENSE
    }
    
    // Constructors
    public Category() {
        this.categoryId = UUID.randomUUID();
        this.createdAt = LocalDateTime.now();
        this.isDefault = false;
    }
    
    public Category(String categoryName, CategoryType categoryType) {
        this();
        this.categoryName = categoryName;
        this.categoryType = categoryType;
    }
    
    // Getters and Setters
    public UUID getCategoryId() {
        return categoryId;
    }
    
    public void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
    }
    
    public UUID getUserId() {
        return userId;
    }
    
    public void setUserId(UUID userId) {
        this.userId = userId;
    }
    
    public String getCategoryName() {
        return categoryName;
    }
    
    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }
    
    public CategoryType getCategoryType() {
        return categoryType;
    }
    
    public void setCategoryType(CategoryType categoryType) {
        this.categoryType = categoryType;
    }
    
    public UUID getParentCategoryId() {
        return parentCategoryId;
    }
    
    public void setParentCategoryId(UUID parentCategoryId) {
        this.parentCategoryId = parentCategoryId;
    }
    
    public String getColorCode() {
        return colorCode;
    }
    
    public void setColorCode(String colorCode) {
        this.colorCode = colorCode;
    }
    
    public String getIconName() {
        return iconName;
    }
    
    public void setIconName(String iconName) {
        this.iconName = iconName;
    }
    
    public boolean isDefault() {
        return isDefault;
    }
    
    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    @Override
    public String toString() {
        return categoryName;
    }
}
