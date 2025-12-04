package com.financetracker.model;


import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User model representing a finance tracker user
 */
public class User {
    private UUID userId;
    private String email;
    private String passwordHash;
    private String fullName;
    private String phone;
    private String profilePictureUrl;
    private String currencyPreference;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public User() {
        this.userId = UUID.randomUUID();
        this.currencyPreference = "USD";
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    public String getCurrencyPreference() {
        return currencyPreference;
    }

    public void setCurrencyPreference(String currencyPreference) {
        this.currencyPreference = currencyPreference;
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
     * Get display name (full name or email prefix)
     */
    public String getDisplayName() {
        if (fullName != null && !fullName.isEmpty()) {
            return fullName;
        }
        if (email != null && email.contains("@")) {
            return email.split("@")[0];
        }
        return "User";
    }

    /**
     * Get currency symbol based on preference
     */
    public String getCurrencySymbol() {
        if (currencyPreference == null) return "$";
        switch (currencyPreference) {
            case "EUR": return "€";
            case "GBP": return "£";
            case "JPY": return "¥";
            case "NOK":
            case "SEK":
            case "DKK": return "kr";
            default: return "$";
        }
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", email='" + email + '\'' +
                ", fullName='" + fullName + '\'' +
                ", currencyPreference='" + currencyPreference + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}