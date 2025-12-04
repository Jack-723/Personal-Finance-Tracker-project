package com.financetracker.service;

import com.financetracker.model.Budget;
import com.financetracker.util.SupabaseClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service class for Budget database operations
 */
public class BudgetService {
    private static final Logger logger = LoggerFactory.getLogger(BudgetService.class);
    private final SupabaseClient supabaseClient;

    public BudgetService() {
        this.supabaseClient = SupabaseClient.getInstance();
    }

    /**
     * Create a new budget
     */
    public boolean createBudget(Budget budget) {
        String sql = "INSERT INTO budgets (budget_id, user_id, category_id, budget_name, amount_limit, " +
                "period, start_date, end_date, alert_threshold, is_active) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, budget.getBudgetId());
            pstmt.setObject(2, budget.getUserId());
            pstmt.setObject(3, budget.getCategoryId());
            pstmt.setString(4, budget.getBudgetName());
            pstmt.setBigDecimal(5, budget.getAmountLimit());
            pstmt.setString(6, budget.getPeriod());
            pstmt.setDate(7, Date.valueOf(budget.getStartDate()));
            pstmt.setDate(8, Date.valueOf(budget.getEndDate()));
            pstmt.setInt(9, budget.getAlertThreshold());
            pstmt.setBoolean(10, budget.isActive());

            int rowsAffected = pstmt.executeUpdate();
            logger.info("Budget created: {} - ${}", budget.getBudgetName(), budget.getAmountLimit());
            return rowsAffected > 0;

        } catch (SQLException e) {
            logger.error("Error creating budget", e);
            return false;
        }
    }

    /**
     * Get all budgets for a user
     */
    public List<Budget> getBudgetsByUser(UUID userId) {
        List<Budget> budgets = new ArrayList<>();
        String sql = "SELECT b.*, c.category_name " +
                "FROM budgets b " +
                "LEFT JOIN categories c ON b.category_id = c.category_id " +
                "WHERE b.user_id = ? " +
                "ORDER BY b.created_at DESC";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Budget budget = mapResultSetToBudget(rs);
                // Calculate spent amount for this budget
                BigDecimal spent = getSpentAmount(userId, budget.getCategoryId(),
                        budget.getStartDate(), budget.getEndDate());
                budget.setSpentAmount(spent);
                budgets.add(budget);
            }

        } catch (SQLException e) {
            logger.error("Error getting budgets by user", e);
        }

        return budgets;
    }

    /**
     * Get active budgets for a user
     */
    public List<Budget> getActiveBudgets(UUID userId) {
        List<Budget> budgets = new ArrayList<>();
        String sql = "SELECT b.*, c.category_name " +
                "FROM budgets b " +
                "LEFT JOIN categories c ON b.category_id = c.category_id " +
                "WHERE b.user_id = ? AND b.is_active = true " +
                "AND b.start_date <= CURRENT_DATE AND b.end_date >= CURRENT_DATE " +
                "ORDER BY b.budget_name";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Budget budget = mapResultSetToBudget(rs);
                BigDecimal spent = getSpentAmount(userId, budget.getCategoryId(),
                        budget.getStartDate(), budget.getEndDate());
                budget.setSpentAmount(spent);
                budgets.add(budget);
            }

        } catch (SQLException e) {
            logger.error("Error getting active budgets", e);
        }

        return budgets;
    }

    /**
     * Get budget by ID
     */
    public Budget getBudgetById(UUID budgetId) {
        String sql = "SELECT b.*, c.category_name " +
                "FROM budgets b " +
                "LEFT JOIN categories c ON b.category_id = c.category_id " +
                "WHERE b.budget_id = ?";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, budgetId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Budget budget = mapResultSetToBudget(rs);
                BigDecimal spent = getSpentAmount(budget.getUserId(), budget.getCategoryId(),
                        budget.getStartDate(), budget.getEndDate());
                budget.setSpentAmount(spent);
                return budget;
            }

        } catch (SQLException e) {
            logger.error("Error getting budget by ID", e);
        }

        return null;
    }

    /**
     * Update a budget
     */
    public boolean updateBudget(Budget budget) {
        String sql = "UPDATE budgets SET category_id = ?, budget_name = ?, amount_limit = ?, " +
                "period = ?, start_date = ?, end_date = ?, alert_threshold = ?, " +
                "is_active = ?, updated_at = CURRENT_TIMESTAMP " +
                "WHERE budget_id = ?";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, budget.getCategoryId());
            pstmt.setString(2, budget.getBudgetName());
            pstmt.setBigDecimal(3, budget.getAmountLimit());
            pstmt.setString(4, budget.getPeriod());
            pstmt.setDate(5, Date.valueOf(budget.getStartDate()));
            pstmt.setDate(6, Date.valueOf(budget.getEndDate()));
            pstmt.setInt(7, budget.getAlertThreshold());
            pstmt.setBoolean(8, budget.isActive());
            pstmt.setObject(9, budget.getBudgetId());

            int rowsAffected = pstmt.executeUpdate();
            logger.info("Budget updated: {}", budget.getBudgetId());
            return rowsAffected > 0;

        } catch (SQLException e) {
            logger.error("Error updating budget", e);
            return false;
        }
    }

    /**
     * Delete a budget
     */
    public boolean deleteBudget(UUID budgetId) {
        String sql = "DELETE FROM budgets WHERE budget_id = ?";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, budgetId);
            int rowsAffected = pstmt.executeUpdate();
            logger.info("Budget deleted: {}", budgetId);
            return rowsAffected > 0;

        } catch (SQLException e) {
            logger.error("Error deleting budget", e);
            return false;
        }
    }

    /**
     * Get spent amount for a category within a date range
     */
    public BigDecimal getSpentAmount(UUID userId, UUID categoryId, LocalDate startDate, LocalDate endDate) {
        String sql;
        if (categoryId != null) {
            sql = "SELECT COALESCE(SUM(amount), 0) as total " +
                    "FROM expenses " +
                    "WHERE user_id = ? AND category_id = ? AND expense_date BETWEEN ? AND ?";
        } else {
            sql = "SELECT COALESCE(SUM(amount), 0) as total " +
                    "FROM expenses " +
                    "WHERE user_id = ? AND expense_date BETWEEN ? AND ?";
        }

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, userId);
            if (categoryId != null) {
                pstmt.setObject(2, categoryId);
                pstmt.setDate(3, Date.valueOf(startDate));
                pstmt.setDate(4, Date.valueOf(endDate));
            } else {
                pstmt.setDate(2, Date.valueOf(startDate));
                pstmt.setDate(3, Date.valueOf(endDate));
            }

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getBigDecimal("total");
            }

        } catch (SQLException e) {
            logger.error("Error getting spent amount", e);
        }

        return BigDecimal.ZERO;
    }

    /**
     * Get total budget limit for a user
     */
    public BigDecimal getTotalBudgetLimit(UUID userId) {
        String sql = "SELECT COALESCE(SUM(amount_limit), 0) as total " +
                "FROM budgets " +
                "WHERE user_id = ? AND is_active = true " +
                "AND start_date <= CURRENT_DATE AND end_date >= CURRENT_DATE";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getBigDecimal("total");
            }

        } catch (SQLException e) {
            logger.error("Error getting total budget limit", e);
        }

        return BigDecimal.ZERO;
    }

    /**
     * Get count of budgets by status
     */
    public int getBudgetCountByStatus(UUID userId, String status) {
        List<Budget> budgets = getActiveBudgets(userId);
        return (int) budgets.stream()
                .filter(b -> b.getStatus().equals(status))
                .count();
    }

    /**
     * Map ResultSet to Budget object
     */
    private Budget mapResultSetToBudget(ResultSet rs) throws SQLException {
        Budget budget = new Budget();
        budget.setBudgetId((UUID) rs.getObject("budget_id"));
        budget.setUserId((UUID) rs.getObject("user_id"));

        Object categoryIdObj = rs.getObject("category_id");
        if (categoryIdObj != null) {
            budget.setCategoryId((UUID) categoryIdObj);
        }

        budget.setBudgetName(rs.getString("budget_name"));
        budget.setAmountLimit(rs.getBigDecimal("amount_limit"));
        budget.setPeriod(rs.getString("period"));
        budget.setStartDate(rs.getDate("start_date").toLocalDate());
        budget.setEndDate(rs.getDate("end_date").toLocalDate());
        budget.setAlertThreshold(rs.getInt("alert_threshold"));
        budget.setActive(rs.getBoolean("is_active"));
        budget.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        budget.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());

        try {
            budget.setCategoryName(rs.getString("category_name"));
        } catch (SQLException e) {
            // Field not present in query
        }

        return budget;
    }
}