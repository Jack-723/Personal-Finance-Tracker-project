package com.financetracker.service;

import com.financetracker.model.Expense;
import com.financetracker.util.SupabaseClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service class for Expense database operations
 */
public class ExpenseService {
    private static final Logger logger = LoggerFactory.getLogger(ExpenseService.class);
    private final SupabaseClient supabaseClient;

    public ExpenseService() {
        this.supabaseClient = SupabaseClient.getInstance();
    }

    /**
     * Create a new expense
     */
    public boolean createExpense(Expense expense) {
        String sql = "INSERT INTO expenses (expense_id, user_id, category_id, amount, vendor, " +
                "description, expense_date, payment_method, receipt_url) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, expense.getExpenseId());
            pstmt.setObject(2, expense.getUserId());
            pstmt.setObject(3, expense.getCategoryId());
            pstmt.setBigDecimal(4, expense.getAmount());
            pstmt.setString(5, expense.getVendor());
            pstmt.setString(6, expense.getDescription());
            pstmt.setDate(7, Date.valueOf(expense.getExpenseDate()));
            pstmt.setString(8, expense.getPaymentMethod());
            pstmt.setString(9, expense.getReceiptUrl());

            int rowsAffected = pstmt.executeUpdate();
            logger.info("Expense created: {}", expense.getExpenseId());
            return rowsAffected > 0;

        } catch (SQLException e) {
            logger.error("Error creating expense", e);
            return false;
        }
    }

    /**
     * Get all expenses for a user
     */
    public List<Expense> getExpensesByUser(UUID userId) {
        List<Expense> expenses = new ArrayList<>();
        String sql = "SELECT e.*, c.category_name " +
                "FROM expenses e " +
                "LEFT JOIN categories c ON e.category_id = c.category_id " +
                "WHERE e.user_id = ? " +
                "ORDER BY e.expense_date DESC";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                expenses.add(mapResultSetToExpense(rs));
            }

        } catch (SQLException e) {
            logger.error("Error getting expenses by user", e);
        }

        return expenses;
    }

    /**
     * Get expenses by date range
     */
    public List<Expense> getExpensesByDateRange(UUID userId, LocalDate startDate, LocalDate endDate) {
        List<Expense> expenses = new ArrayList<>();
        String sql = "SELECT e.*, c.category_name " +
                "FROM expenses e " +
                "LEFT JOIN categories c ON e.category_id = c.category_id " +
                "WHERE e.user_id = ? AND e.expense_date BETWEEN ? AND ? " +
                "ORDER BY e.expense_date DESC";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, userId);
            pstmt.setDate(2, Date.valueOf(startDate));
            pstmt.setDate(3, Date.valueOf(endDate));
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                expenses.add(mapResultSetToExpense(rs));
            }

        } catch (SQLException e) {
            logger.error("Error getting expenses by date range", e);
        }

        return expenses;
    }

    /**
     * Get expenses by category
     */
    public List<Expense> getExpensesByCategory(UUID userId, UUID categoryId) {
        List<Expense> expenses = new ArrayList<>();
        String sql = "SELECT e.*, c.category_name " +
                "FROM expenses e " +
                "LEFT JOIN categories c ON e.category_id = c.category_id " +
                "WHERE e.user_id = ? AND e.category_id = ? " +
                "ORDER BY e.expense_date DESC";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, userId);
            pstmt.setObject(2, categoryId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                expenses.add(mapResultSetToExpense(rs));
            }

        } catch (SQLException e) {
            logger.error("Error getting expenses by category", e);
        }

        return expenses;
    }

    /**
     * Update an expense
     */
    public boolean updateExpense(Expense expense) {
        String sql = "UPDATE expenses SET category_id = ?, amount = ?, vendor = ?, " +
                "description = ?, expense_date = ?, payment_method = ?, receipt_url = ?, " +
                "updated_at = CURRENT_TIMESTAMP " +
                "WHERE expense_id = ?";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, expense.getCategoryId());
            pstmt.setBigDecimal(2, expense.getAmount());
            pstmt.setString(3, expense.getVendor());
            pstmt.setString(4, expense.getDescription());
            pstmt.setDate(5, Date.valueOf(expense.getExpenseDate()));
            pstmt.setString(6, expense.getPaymentMethod());
            pstmt.setString(7, expense.getReceiptUrl());
            pstmt.setObject(8, expense.getExpenseId());

            int rowsAffected = pstmt.executeUpdate();
            logger.info("Expense updated: {}", expense.getExpenseId());
            return rowsAffected > 0;

        } catch (SQLException e) {
            logger.error("Error updating expense", e);
            return false;
        }
    }

    /**
     * Delete an expense
     */
    public boolean deleteExpense(UUID expenseId) {
        String sql = "DELETE FROM expenses WHERE expense_id = ?";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, expenseId);
            int rowsAffected = pstmt.executeUpdate();
            logger.info("Expense deleted: {}", expenseId);
            return rowsAffected > 0;

        } catch (SQLException e) {
            logger.error("Error deleting expense", e);
            return false;
        }
    }

    /**
     * Get total expenses for a user in a date range
     */
    public double getTotalExpenses(UUID userId, LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT COALESCE(SUM(amount), 0) as total " +
                "FROM expenses " +
                "WHERE user_id = ? AND expense_date BETWEEN ? AND ?";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, userId);
            pstmt.setDate(2, Date.valueOf(startDate));
            pstmt.setDate(3, Date.valueOf(endDate));
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getDouble("total");
            }

        } catch (SQLException e) {
            logger.error("Error getting total expenses", e);
        }

        return 0.0;
    }

    /**
     * Map ResultSet to Expense object
     */
    private Expense mapResultSetToExpense(ResultSet rs) throws SQLException {
        Expense expense = new Expense();
        expense.setExpenseId((UUID) rs.getObject("expense_id"));
        expense.setUserId((UUID) rs.getObject("user_id"));
        expense.setCategoryId((UUID) rs.getObject("category_id"));
        expense.setAmount(rs.getBigDecimal("amount"));
        expense.setVendor(rs.getString("vendor"));
        expense.setDescription(rs.getString("description"));
        expense.setExpenseDate(rs.getDate("expense_date").toLocalDate());
        expense.setPaymentMethod(rs.getString("payment_method"));
        expense.setReceiptUrl(rs.getString("receipt_url"));
        expense.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        expense.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        expense.setCategoryName(rs.getString("category_name"));
        return expense;
    }
}
