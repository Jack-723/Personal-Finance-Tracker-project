package com.financetracker.service;

import com.financetracker.model.Bill;
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
 * Service class for Bill/Subscription database operations
 */
public class BillService {
    private static final Logger logger = LoggerFactory.getLogger(BillService.class);
    private final SupabaseClient supabaseClient;

    public BillService() {
        this.supabaseClient = SupabaseClient.getInstance();
    }

    /**
     * Create a new bill/subscription
     */
    public boolean createBill(Bill bill) {
        String sql = "INSERT INTO bills_subscriptions (bill_id, user_id, category_id, name, amount, " +
                "billing_cycle, due_day, start_date, end_date, is_active, reminder_days, " +
                "last_payment_date, next_payment_date, description, vendor) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, bill.getBillId());
            pstmt.setObject(2, bill.getUserId());
            pstmt.setObject(3, bill.getCategoryId());
            pstmt.setString(4, bill.getName());
            pstmt.setBigDecimal(5, bill.getAmount());
            pstmt.setString(6, bill.getBillingCycleString());
            pstmt.setInt(7, bill.getDueDay());
            pstmt.setDate(8, Date.valueOf(bill.getStartDate()));

            if (bill.getEndDate() != null) {
                pstmt.setDate(9, Date.valueOf(bill.getEndDate()));
            } else {
                pstmt.setNull(9, Types.DATE);
            }

            pstmt.setBoolean(10, bill.isActive());
            pstmt.setInt(11, bill.getReminderDays());

            if (bill.getLastPaymentDate() != null) {
                pstmt.setDate(12, Date.valueOf(bill.getLastPaymentDate()));
            } else {
                pstmt.setNull(12, Types.DATE);
            }

            if (bill.getNextPaymentDate() != null) {
                pstmt.setDate(13, Date.valueOf(bill.getNextPaymentDate()));
            } else {
                pstmt.setDate(13, Date.valueOf(bill.calculateNextPaymentDate()));
            }

            pstmt.setString(14, bill.getDescription());
            pstmt.setString(15, bill.getVendor());

            int rowsAffected = pstmt.executeUpdate();
            logger.info("Bill created: {} - ${}", bill.getName(), bill.getAmount());
            return rowsAffected > 0;

        } catch (SQLException e) {
            logger.error("Error creating bill", e);
            return false;
        }
    }

    /**
     * Get all bills for a user
     */
    public List<Bill> getBillsByUser(UUID userId) {
        List<Bill> bills = new ArrayList<>();
        String sql = "SELECT b.*, c.category_name " +
                "FROM bills_subscriptions b " +
                "LEFT JOIN categories c ON b.category_id = c.category_id " +
                "WHERE b.user_id = ? " +
                "ORDER BY b.next_payment_date ASC NULLS LAST, b.name";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                bills.add(mapResultSetToBill(rs));
            }

        } catch (SQLException e) {
            logger.error("Error getting bills by user", e);
        }

        return bills;
    }

    /**
     * Get active bills for a user
     */
    public List<Bill> getActiveBills(UUID userId) {
        List<Bill> bills = new ArrayList<>();
        String sql = "SELECT b.*, c.category_name " +
                "FROM bills_subscriptions b " +
                "LEFT JOIN categories c ON b.category_id = c.category_id " +
                "WHERE b.user_id = ? AND b.is_active = true " +
                "AND (b.end_date IS NULL OR b.end_date >= CURRENT_DATE) " +
                "ORDER BY b.next_payment_date ASC NULLS LAST, b.name";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                bills.add(mapResultSetToBill(rs));
            }

        } catch (SQLException e) {
            logger.error("Error getting active bills", e);
        }

        return bills;
    }

    /**
     * Get bills due soon (within reminder period)
     */
    public List<Bill> getBillsDueSoon(UUID userId, int daysAhead) {
        List<Bill> bills = new ArrayList<>();
        String sql = "SELECT b.*, c.category_name " +
                "FROM bills_subscriptions b " +
                "LEFT JOIN categories c ON b.category_id = c.category_id " +
                "WHERE b.user_id = ? AND b.is_active = true " +
                "AND b.next_payment_date <= CURRENT_DATE + INTERVAL '" + daysAhead + " days' " +
                "AND (b.end_date IS NULL OR b.end_date >= CURRENT_DATE) " +
                "ORDER BY b.next_payment_date ASC";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                bills.add(mapResultSetToBill(rs));
            }

        } catch (SQLException e) {
            logger.error("Error getting bills due soon", e);
        }

        return bills;
    }

    /**
     * Get overdue bills
     */
    public List<Bill> getOverdueBills(UUID userId) {
        List<Bill> bills = new ArrayList<>();
        String sql = "SELECT b.*, c.category_name " +
                "FROM bills_subscriptions b " +
                "LEFT JOIN categories c ON b.category_id = c.category_id " +
                "WHERE b.user_id = ? AND b.is_active = true " +
                "AND b.next_payment_date < CURRENT_DATE " +
                "AND (b.end_date IS NULL OR b.end_date >= CURRENT_DATE) " +
                "ORDER BY b.next_payment_date ASC";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                bills.add(mapResultSetToBill(rs));
            }

        } catch (SQLException e) {
            logger.error("Error getting overdue bills", e);
        }

        return bills;
    }

    /**
     * Get bill by ID
     */
    public Bill getBillById(UUID billId) {
        String sql = "SELECT b.*, c.category_name " +
                "FROM bills_subscriptions b " +
                "LEFT JOIN categories c ON b.category_id = c.category_id " +
                "WHERE b.bill_id = ?";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, billId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToBill(rs);
            }

        } catch (SQLException e) {
            logger.error("Error getting bill by ID", e);
        }

        return null;
    }

    /**
     * Update a bill
     */
    public boolean updateBill(Bill bill) {
        String sql = "UPDATE bills_subscriptions SET category_id = ?, name = ?, amount = ?, " +
                "billing_cycle = ?, due_day = ?, start_date = ?, end_date = ?, " +
                "is_active = ?, reminder_days = ?, last_payment_date = ?, " +
                "next_payment_date = ?, description = ?, vendor = ?, " +
                "updated_at = CURRENT_TIMESTAMP " +
                "WHERE bill_id = ?";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, bill.getCategoryId());
            pstmt.setString(2, bill.getName());
            pstmt.setBigDecimal(3, bill.getAmount());
            pstmt.setString(4, bill.getBillingCycleString());
            pstmt.setInt(5, bill.getDueDay());
            pstmt.setDate(6, Date.valueOf(bill.getStartDate()));

            if (bill.getEndDate() != null) {
                pstmt.setDate(7, Date.valueOf(bill.getEndDate()));
            } else {
                pstmt.setNull(7, Types.DATE);
            }

            pstmt.setBoolean(8, bill.isActive());
            pstmt.setInt(9, bill.getReminderDays());

            if (bill.getLastPaymentDate() != null) {
                pstmt.setDate(10, Date.valueOf(bill.getLastPaymentDate()));
            } else {
                pstmt.setNull(10, Types.DATE);
            }

            if (bill.getNextPaymentDate() != null) {
                pstmt.setDate(11, Date.valueOf(bill.getNextPaymentDate()));
            } else {
                pstmt.setNull(11, Types.DATE);
            }

            pstmt.setString(12, bill.getDescription());
            pstmt.setString(13, bill.getVendor());
            pstmt.setObject(14, bill.getBillId());

            int rowsAffected = pstmt.executeUpdate();
            logger.info("Bill updated: {}", bill.getBillId());
            return rowsAffected > 0;

        } catch (SQLException e) {
            logger.error("Error updating bill", e);
            return false;
        }
    }

    /**
     * Mark bill as paid and update next payment date
     */
    public boolean markBillAsPaid(UUID billId) {
        Bill bill = getBillById(billId);
        if (bill == null) {
            return false;
        }

        bill.setLastPaymentDate(LocalDate.now());
        bill.setNextPaymentDate(bill.calculateNextPaymentDate());

        return updateBill(bill);
    }

    /**
     * Delete a bill
     */
    public boolean deleteBill(UUID billId) {
        String sql = "DELETE FROM bills_subscriptions WHERE bill_id = ?";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, billId);
            int rowsAffected = pstmt.executeUpdate();
            logger.info("Bill deleted: {}", billId);
            return rowsAffected > 0;

        } catch (SQLException e) {
            logger.error("Error deleting bill", e);
            return false;
        }
    }

    /**
     * Get total monthly cost of all active bills
     */
    public BigDecimal getTotalMonthlyCost(UUID userId) {
        List<Bill> activeBills = getActiveBills(userId);
        return activeBills.stream()
                .map(Bill::getMonthlyCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Get total yearly cost of all active bills
     */
    public BigDecimal getTotalYearlyCost(UUID userId) {
        List<Bill> activeBills = getActiveBills(userId);
        return activeBills.stream()
                .map(Bill::getYearlyCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Get count of bills by status
     */
    public int getBillCountByStatus(UUID userId, String status) {
        List<Bill> bills = getActiveBills(userId);
        return (int) bills.stream()
                .filter(b -> b.getStatus().equals(status))
                .count();
    }

    /**
     * Map ResultSet to Bill object
     */
    private Bill mapResultSetToBill(ResultSet rs) throws SQLException {
        Bill bill = new Bill();
        bill.setBillId((UUID) rs.getObject("bill_id"));
        bill.setUserId((UUID) rs.getObject("user_id"));

        Object categoryIdObj = rs.getObject("category_id");
        if (categoryIdObj != null) {
            bill.setCategoryId((UUID) categoryIdObj);
        }

        bill.setName(rs.getString("name"));
        bill.setAmount(rs.getBigDecimal("amount"));

        String billingCycle = rs.getString("billing_cycle");
        if (billingCycle != null) {
            bill.setBillingCycleFromString(billingCycle);
        }

        bill.setDueDay(rs.getInt("due_day"));
        bill.setStartDate(rs.getDate("start_date").toLocalDate());

        Date endDate = rs.getDate("end_date");
        if (endDate != null) {
            bill.setEndDate(endDate.toLocalDate());
        }

        bill.setActive(rs.getBoolean("is_active"));
        bill.setReminderDays(rs.getInt("reminder_days"));

        Date lastPayment = rs.getDate("last_payment_date");
        if (lastPayment != null) {
            bill.setLastPaymentDate(lastPayment.toLocalDate());
        }

        Date nextPayment = rs.getDate("next_payment_date");
        if (nextPayment != null) {
            bill.setNextPaymentDate(nextPayment.toLocalDate());
        }

        bill.setDescription(rs.getString("description"));
        bill.setVendor(rs.getString("vendor"));
        bill.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        bill.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());

        try {
            bill.setCategoryName(rs.getString("category_name"));
        } catch (SQLException e) {
            // Field not present in query
        }

        return bill;
    }
}