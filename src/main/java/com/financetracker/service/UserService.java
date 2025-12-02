package com.financetracker.service;

import com.financetracker.model.User;
import com.financetracker.util.SupabaseClient;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.UUID;

/**
 * Service class for User database operations
 */
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final SupabaseClient supabaseClient;

    public UserService() {
        this.supabaseClient = SupabaseClient.getInstance();
    }

    /**
     * Create a new user in the database
     */
    public boolean createUser(User user, String password) {
        String sql = "INSERT INTO users (user_id, email, password_hash, full_name, phone, " +
                "profile_picture_url, currency_preference) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Hash the password
            String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());

            pstmt.setObject(1, user.getUserId());
            pstmt.setString(2, user.getEmail());
            pstmt.setString(3, passwordHash);
            pstmt.setString(4, user.getFullName());
            pstmt.setString(5, user.getPhone());
            pstmt.setString(6, user.getProfilePictureUrl());
            pstmt.setString(7, user.getCurrencyPreference());

            int rowsAffected = pstmt.executeUpdate();
            logger.info("User created: {}", user.getEmail());
            return rowsAffected > 0;

        } catch (SQLException e) {
            logger.error("Error creating user", e);
            return false;
        }
    }

    /**
     * Create a new user without password (for Supabase Auth users)
     * Password is managed by Supabase Auth, not stored locally
     */
    public boolean createUserWithoutPassword(User user) {
        String sql = "INSERT INTO users (user_id, email, password_hash, full_name, phone, " +
                "currency_preference, is_active) VALUES (?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT (email) DO NOTHING";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, user.getUserId());
            pstmt.setString(2, user.getEmail());
            pstmt.setString(3, "SUPABASE_AUTH"); // Placeholder - password managed by Supabase
            pstmt.setString(4, user.getFullName());
            pstmt.setString(5, user.getPhone());
            pstmt.setString(6, user.getCurrencyPreference() != null ? user.getCurrencyPreference() : "USD");
            pstmt.setBoolean(7, true);

            int rowsAffected = pstmt.executeUpdate();
            logger.info("User created (Supabase Auth): {}", user.getEmail());
            return rowsAffected > 0;

        } catch (SQLException e) {
            logger.error("Error creating user", e);
            return false;
        }
    }

    /**
     * Authenticate user - Supabase Auth already verified password
     * This method just retrieves the user from database
     */
    public User authenticateUser(String email, String password) {
        String sql = "SELECT * FROM users WHERE email = ? AND is_active = true";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                User user = mapResultSetToUser(rs);
                logger.info("User authenticated and found in database: {}", email);
                return user;
            } else {
                logger.warn("User authenticated by Supabase but not found in database: {}", email);
            }

        } catch (SQLException e) {
            logger.error("Error authenticating user", e);
        }

        return null;
    }

    /**
     * Get user by ID
     */
    public User getUserById(UUID userId) {
        String sql = "SELECT * FROM users WHERE user_id = ?";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToUser(rs);
            }

        } catch (SQLException e) {
            logger.error("Error getting user by ID", e);
        }

        return null;
    }

    /**
     * Get user by email
     */
    public User getUserByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToUser(rs);
            }

        } catch (SQLException e) {
            logger.error("Error getting user by email", e);
        }

        return null;
    }

    /**
     * Check if email already exists in database
     */
    public boolean emailExists(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            logger.error("Error checking email existence", e);
        }

        return false;
    }

    /**
     * Update user information
     */
    public boolean updateUser(User user) {
        String sql = "UPDATE users SET full_name = ?, phone = ?, profile_picture_url = ?, " +
                "currency_preference = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getFullName());
            pstmt.setString(2, user.getPhone());
            pstmt.setString(3, user.getProfilePictureUrl());
            pstmt.setString(4, user.getCurrencyPreference());
            pstmt.setObject(5, user.getUserId());

            int rowsAffected = pstmt.executeUpdate();
            logger.info("User updated: {}", user.getEmail());
            return rowsAffected > 0;

        } catch (SQLException e) {
            logger.error("Error updating user", e);
            return false;
        }
    }

    /**
     * Change user password
     */
    public boolean changePassword(UUID userId, String oldPassword, String newPassword) {
        User user = getUserById(userId);
        if (user == null) {
            return false;
        }

        String checkSql = "SELECT password_hash FROM users WHERE user_id = ?";
        String updateSql = "UPDATE users SET password_hash = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {

            checkStmt.setObject(1, userId);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password_hash");

                // Only verify if hash exists and is not the Supabase placeholder
                if (storedHash != null && !storedHash.isEmpty() && !storedHash.equals("SUPABASE_AUTH")) {
                    if (!BCrypt.checkpw(oldPassword, storedHash)) {
                        logger.warn("Old password incorrect for user: {}", userId);
                        return false;
                    }
                }

                // Update with new password
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    String newHash = BCrypt.hashpw(newPassword, BCrypt.gensalt());
                    updateStmt.setString(1, newHash);
                    updateStmt.setObject(2, userId);

                    int rowsAffected = updateStmt.executeUpdate();
                    logger.info("Password changed for user: {}", userId);
                    return rowsAffected > 0;
                }
            }

        } catch (SQLException e) {
            logger.error("Error changing password", e);
        }

        return false;
    }

    /**
     * Deactivate user account
     */
    public boolean deactivateUser(UUID userId) {
        String sql = "UPDATE users SET is_active = false, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, userId);
            int rowsAffected = pstmt.executeUpdate();
            logger.info("User deactivated: {}", userId);
            return rowsAffected > 0;

        } catch (SQLException e) {
            logger.error("Error deactivating user", e);
        }

        return false;
    }

    /**
     * Map ResultSet to User object
     */
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId((UUID) rs.getObject("user_id"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setFullName(rs.getString("full_name"));
        user.setPhone(rs.getString("phone"));
        user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        user.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        user.setActive(rs.getBoolean("is_active"));
        user.setProfilePictureUrl(rs.getString("profile_picture_url"));
        user.setCurrencyPreference(rs.getString("currency_preference"));
        return user;
    }
}
