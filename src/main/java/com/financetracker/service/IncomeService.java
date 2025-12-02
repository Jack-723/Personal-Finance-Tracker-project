package com.financetracker.service;

import com.financetracker.model.Income;
import com.financetracker.util.SupabaseClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service class for Income database operations
 */
public class IncomeService {
    private static final Logger logger = LoggerFactory.getLogger(IncomeService.class);
    private final SupabaseClient supabaseClient;

    public IncomeService() {
        this.supabaseClient = SupabaseClient.getInstance();
    }

    /**
     * Create a new income entry
     */
    public boolean createIncome(Income income) {
        String sql = "INSERT INTO income (income_id, user_id, category_id, amount, source, " +
                "description, income_date, is_recurring, recurring_frequency, recurring_day) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, income.getIncomeId());
            pstmt.setObject(2, income.getUserId());
            pstmt.setObject(3, income.getCategoryId());
            pstmt.setBigDecimal(4, income.getAmount());
            pstmt.setString(5, income.getSource());
            pstmt.setString(6, income.getDescription());
            pstmt.setDate(7, Date.valueOf(income.getIncomeDate()));
            pstmt.setBoolean(8, income.isRecurring());
            pstmt.setString(9, income.getRecurringFrequency());

            if (income.getRecurringDay() != null) {
                pstmt.setInt(10, income.getRecurringDay());
            } else {
                pstmt.setNull(10, Types.INTEGER);
            }

            int rowsAffected = pstmt.executeUpdate();
            logger.info("Income created: {} - ${}", income.getSource(), income.getAmount());
            return rowsAffected > 0;

        } catch (SQLException e) {
            logger.error("Error creating income", e);
            return false;
        }
    }

    /**
     * Get all income entries for a user
     */
    public List<Income> getIncomeByUser(UUID userId) {
        List<Income> incomes = new ArrayList<>();
        String sql = "SELECT i.*, c.category_name, c.color_code " +
                "FROM income i " +
                "LEFT JOIN categories c ON i.category_id = c.category_id " +
                "WHERE i.user_id = ? " +
                "ORDER BY i.income_date DESC";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                incomes.add(mapResultSetToIncome(rs));
            }

        } catch (SQLException e) {
            logger.error("Error getting income by user", e);
        }

        return incomes;
    }

    /**
     * Get income by date range
     */
    public List<Income> getIncomeByDateRange(UUID userId, LocalDate startDate, LocalDate endDate) {
        List<Income> incomes = new ArrayList<>();
        String sql = "SELECT i.*, c.category_name, c.color_code " +
                "FROM income i " +
                "LEFT JOIN categories c ON i.category_id = c.category_id " +
                "WHERE i.user_id = ? AND i.income_date BETWEEN ? AND ? " +
                "ORDER BY i.income_date DESC";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, userId);
            pstmt.setDate(2, Date.valueOf(startDate));
            pstmt.setDate(3, Date.valueOf(endDate));
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                incomes.add(mapResultSetToIncome(rs));
            }

        } catch (SQLException e) {
            logger.error("Error getting income by date range", e);
        }

        return incomes;
    }

    /**
     * Update an income entry
     */
    public boolean updateIncome(Income income) {
        String sql = "UPDATE income SET category_id = ?, amount = ?, source = ?, " +
                "description = ?, income_date = ?, is_recurring = ?, " +
                "recurring_frequency = ?, recurring_day = ?, updated_at = CURRENT_TIMESTAMP " +
                "WHERE income_id = ?";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, income.getCategoryId());
            pstmt.setBigDecimal(2, income.getAmount());
            pstmt.setString(3, income.getSource());
            pstmt.setString(4, income.getDescription());
            pstmt.setDate(5, Date.valueOf(income.getIncomeDate()));
            pstmt.setBoolean(6, income.isRecurring());
            pstmt.setString(7, income.getRecurringFrequency());

            if (income.getRecurringDay() != null) {
                pstmt.setInt(8, income.getRecurringDay());
            } else {
                pstmt.setNull(8, Types.INTEGER);
            }

            pstmt.setObject(9, income.getIncomeId());

            int rowsAffected = pstmt.executeUpdate();
            logger.info("Income updated: {}", income.getIncomeId());
            return rowsAffected > 0;

        } catch (SQLException e) {
            logger.error("Error updating income", e);
            return false;
        }
    }

    /**
     * Delete an income entry
     */
    public boolean deleteIncome(UUID incomeId) {
        String sql = "DELETE FROM income WHERE income_id = ?";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, incomeId);
            int rowsAffected = pstmt.executeUpdate();
            logger.info("Income deleted: {}", incomeId);
            return rowsAffected > 0;

        } catch (SQLException e) {
            logger.error("Error deleting income", e);
            return false;
        }
    }

    /**
     * Get total income for a user in a date range
     */
    public BigDecimal getTotalIncome(UUID userId, LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT COALESCE(SUM(amount), 0) as total " +
                "FROM income " +
                "WHERE user_id = ? AND income_date BETWEEN ? AND ?";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, userId);
            pstmt.setDate(2, Date.valueOf(startDate));
            pstmt.setDate(3, Date.valueOf(endDate));
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getBigDecimal("total");
            }

        } catch (SQLException e) {
            logger.error("Error getting total income", e);
        }

        return BigDecimal.ZERO;
    }

    /**
     * Get income grouped by category
     */
    public Map<String, BigDecimal> getIncomeByCategoryGrouped(UUID userId, LocalDate startDate, LocalDate endDate) {
        Map<String, BigDecimal> result = new HashMap<>();
        String sql = "SELECT c.category_name, COALESCE(SUM(i.amount), 0) as total " +
                "FROM income i " +
                "LEFT JOIN categories c ON i.category_id = c.category_id " +
                "WHERE i.user_id = ? AND i.income_date BETWEEN ? AND ? " +
                "GROUP BY c.category_name " +
                "ORDER BY total DESC";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, userId);
            pstmt.setDate(2, Date.valueOf(startDate));
            pstmt.setDate(3, Date.valueOf(endDate));
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String category = rs.getString("category_name");
                if (category == null) category = "Uncategorized";
                result.put(category, rs.getBigDecimal("total"));
            }

        } catch (SQLException e) {
            logger.error("Error getting income by category grouped", e);
        }

        return result;
    }

    /**
     * Map ResultSet to Income object
     */
    private Income mapResultSetToIncome(ResultSet rs) throws SQLException {
        Income income = new Income();
        income.setIncomeId((UUID) rs.getObject("income_id"));
        income.setUserId((UUID) rs.getObject("user_id"));

        Object categoryIdObj = rs.getObject("category_id");
        if (categoryIdObj != null) {
            income.setCategoryId((UUID) categoryIdObj);
        }

        income.setAmount(rs.getBigDecimal("amount"));
        income.setSource(rs.getString("source"));
        income.setDescription(rs.getString("description"));
        income.setIncomeDate(rs.getDate("income_date").toLocalDate());
        income.setRecurring(rs.getBoolean("is_recurring"));
        income.setRecurringFrequency(rs.getString("recurring_frequency"));

        int recurringDay = rs.getInt("recurring_day");
        if (!rs.wasNull()) {
            income.setRecurringDay(recurringDay);
        }

        income.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        income.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());

        try {
            income.setCategoryName(rs.getString("category_name"));
        } catch (SQLException e) {
            // Field not present in query
        }

        return income;
    }
}
