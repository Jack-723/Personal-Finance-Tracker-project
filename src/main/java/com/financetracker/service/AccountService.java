package com.financetracker.service;

import com.financetracker.model.Account;
import com.financetracker.util.SupabaseClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service class for Account database operations
 */
public class AccountService {
    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);
    private final SupabaseClient supabaseClient;

    public AccountService() {
        this.supabaseClient = SupabaseClient.getInstance();
    }

    /**
     * Create a new account
     */
    public boolean createAccount(Account account) {
        String sql = "INSERT INTO accounts (account_id, user_id, account_name, account_type, " +
                "balance, currency, institution_name, account_number, is_active) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, account.getAccountId());
            pstmt.setObject(2, account.getUserId());
            pstmt.setString(3, account.getAccountName());
            pstmt.setString(4, account.getAccountTypeString());
            pstmt.setBigDecimal(5, account.getBalance());
            pstmt.setString(6, account.getCurrency());
            pstmt.setString(7, account.getInstitutionName());
            pstmt.setString(8, account.getAccountNumber());
            pstmt.setBoolean(9, account.isActive());

            int rowsAffected = pstmt.executeUpdate();
            logger.info("Account created: {} - {}", account.getAccountName(), account.getAccountTypeString());
            return rowsAffected > 0;

        } catch (SQLException e) {
            logger.error("Error creating account", e);
            return false;
        }
    }

    /**
     * Get all accounts for a user
     */
    public List<Account> getAccountsByUser(UUID userId) {
        List<Account> accounts = new ArrayList<>();
        String sql = "SELECT * FROM accounts WHERE user_id = ? ORDER BY account_name";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                accounts.add(mapResultSetToAccount(rs));
            }

        } catch (SQLException e) {
            logger.error("Error getting accounts by user", e);
        }

        return accounts;
    }

    /**
     * Get active accounts for a user
     */
    public List<Account> getActiveAccounts(UUID userId) {
        List<Account> accounts = new ArrayList<>();
        String sql = "SELECT * FROM accounts WHERE user_id = ? AND is_active = true ORDER BY account_name";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                accounts.add(mapResultSetToAccount(rs));
            }

        } catch (SQLException e) {
            logger.error("Error getting active accounts", e);
        }

        return accounts;
    }

    /**
     * Get accounts by type
     */
    public List<Account> getAccountsByType(UUID userId, Account.AccountType accountType) {
        List<Account> accounts = new ArrayList<>();
        String sql = "SELECT * FROM accounts WHERE user_id = ? AND account_type = ? AND is_active = true ORDER BY account_name";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, userId);
            pstmt.setString(2, accountType.name());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                accounts.add(mapResultSetToAccount(rs));
            }

        } catch (SQLException e) {
            logger.error("Error getting accounts by type", e);
        }

        return accounts;
    }

    /**
     * Get account by ID
     */
    public Account getAccountById(UUID accountId) {
        String sql = "SELECT * FROM accounts WHERE account_id = ?";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, accountId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToAccount(rs);
            }

        } catch (SQLException e) {
            logger.error("Error getting account by ID", e);
        }

        return null;
    }

    /**
     * Update an account
     */
    public boolean updateAccount(Account account) {
        String sql = "UPDATE accounts SET account_name = ?, account_type = ?, balance = ?, " +
                "currency = ?, institution_name = ?, account_number = ?, is_active = ?, " +
                "updated_at = CURRENT_TIMESTAMP WHERE account_id = ?";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, account.getAccountName());
            pstmt.setString(2, account.getAccountTypeString());
            pstmt.setBigDecimal(3, account.getBalance());
            pstmt.setString(4, account.getCurrency());
            pstmt.setString(5, account.getInstitutionName());
            pstmt.setString(6, account.getAccountNumber());
            pstmt.setBoolean(7, account.isActive());
            pstmt.setObject(8, account.getAccountId());

            int rowsAffected = pstmt.executeUpdate();
            logger.info("Account updated: {}", account.getAccountId());
            return rowsAffected > 0;

        } catch (SQLException e) {
            logger.error("Error updating account", e);
            return false;
        }
    }

    /**
     * Update account balance
     */
    public boolean updateBalance(UUID accountId, BigDecimal newBalance) {
        String sql = "UPDATE accounts SET balance = ?, updated_at = CURRENT_TIMESTAMP WHERE account_id = ?";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setBigDecimal(1, newBalance);
            pstmt.setObject(2, accountId);

            int rowsAffected = pstmt.executeUpdate();
            logger.info("Account balance updated: {} -> {}", accountId, newBalance);
            return rowsAffected > 0;

        } catch (SQLException e) {
            logger.error("Error updating account balance", e);
            return false;
        }
    }

    /**
     * Adjust account balance (add or subtract)
     */
    public boolean adjustBalance(UUID accountId, BigDecimal adjustment) {
        Account account = getAccountById(accountId);
        if (account == null) {
            return false;
        }

        BigDecimal newBalance = account.getBalance().add(adjustment);
        return updateBalance(accountId, newBalance);
    }

    /**
     * Delete an account
     */
    public boolean deleteAccount(UUID accountId) {
        String sql = "DELETE FROM accounts WHERE account_id = ?";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, accountId);
            int rowsAffected = pstmt.executeUpdate();
            logger.info("Account deleted: {}", accountId);
            return rowsAffected > 0;

        } catch (SQLException e) {
            logger.error("Error deleting account", e);
            return false;
        }
    }

    /**
     * Deactivate an account (soft delete)
     */
    public boolean deactivateAccount(UUID accountId) {
        String sql = "UPDATE accounts SET is_active = false, updated_at = CURRENT_TIMESTAMP WHERE account_id = ?";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, accountId);
            int rowsAffected = pstmt.executeUpdate();
            logger.info("Account deactivated: {}", accountId);
            return rowsAffected > 0;

        } catch (SQLException e) {
            logger.error("Error deactivating account", e);
            return false;
        }
    }

    /**
     * Get total balance of all active accounts (assets)
     */
    public BigDecimal getTotalAssets(UUID userId) {
        String sql = "SELECT COALESCE(SUM(balance), 0) as total FROM accounts " +
                "WHERE user_id = ? AND is_active = true AND account_type != 'CREDIT_CARD'";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getBigDecimal("total");
            }

        } catch (SQLException e) {
            logger.error("Error getting total assets", e);
        }

        return BigDecimal.ZERO;
    }

    /**
     * Get total liabilities (credit card balances)
     */
    public BigDecimal getTotalLiabilities(UUID userId) {
        String sql = "SELECT COALESCE(SUM(balance), 0) as total FROM accounts " +
                "WHERE user_id = ? AND is_active = true AND account_type = 'CREDIT_CARD'";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getBigDecimal("total");
            }

        } catch (SQLException e) {
            logger.error("Error getting total liabilities", e);
        }

        return BigDecimal.ZERO;
    }

    /**
     * Get net worth (assets - liabilities)
     */
    public BigDecimal getNetWorth(UUID userId) {
        BigDecimal assets = getTotalAssets(userId);
        BigDecimal liabilities = getTotalLiabilities(userId);
        return assets.subtract(liabilities);
    }

    /**
     * Get total balance across all active accounts
     */
    public BigDecimal getTotalBalance(UUID userId) {
        String sql = "SELECT COALESCE(SUM(CASE WHEN account_type = 'CREDIT_CARD' THEN -balance ELSE balance END), 0) as total " +
                "FROM accounts WHERE user_id = ? AND is_active = true";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getBigDecimal("total");
            }

        } catch (SQLException e) {
            logger.error("Error getting total balance", e);
        }

        return BigDecimal.ZERO;
    }

    /**
     * Get count of active accounts
     */
    public int getActiveAccountCount(UUID userId) {
        String sql = "SELECT COUNT(*) as count FROM accounts WHERE user_id = ? AND is_active = true";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("count");
            }

        } catch (SQLException e) {
            logger.error("Error getting active account count", e);
        }

        return 0;
    }

    /**
     * Map ResultSet to Account object
     */
    private Account mapResultSetToAccount(ResultSet rs) throws SQLException {
        Account account = new Account();
        account.setAccountId((UUID) rs.getObject("account_id"));
        account.setUserId((UUID) rs.getObject("user_id"));
        account.setAccountName(rs.getString("account_name"));

        String accountType = rs.getString("account_type");
        if (accountType != null) {
            account.setAccountTypeFromString(accountType);
        }

        account.setBalance(rs.getBigDecimal("balance"));
        account.setCurrency(rs.getString("currency"));
        account.setInstitutionName(rs.getString("institution_name"));
        account.setAccountNumber(rs.getString("account_number"));
        account.setActive(rs.getBoolean("is_active"));
        account.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        account.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());

        return account;
    }
}