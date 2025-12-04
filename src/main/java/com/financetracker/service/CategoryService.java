package com.financetracker.service;

import com.financetracker.model.Category;
import com.financetracker.model.Category.CategoryType;
import com.financetracker.util.SupabaseClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service class for Category database operations
 */
public class CategoryService {
    private static final Logger logger = LoggerFactory.getLogger(CategoryService.class);
    private final SupabaseClient supabaseClient;

    public CategoryService() {
        this.supabaseClient = SupabaseClient.getInstance();
    }

    /**
     * Create a new category
     */
    public boolean createCategory(Category category) {
        String sql = "INSERT INTO categories (category_id, user_id, category_name, category_type, " +
                "parent_category_id, color_code, icon_name, is_default) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, category.getCategoryId());
            pstmt.setObject(2, category.getUserId());
            pstmt.setString(3, category.getCategoryName());
            pstmt.setString(4, category.getCategoryType().name());
            pstmt.setObject(5, category.getParentCategoryId());
            pstmt.setString(6, category.getColorCode());
            pstmt.setString(7, category.getIconName());
            pstmt.setBoolean(8, category.isDefault());

            int rowsAffected = pstmt.executeUpdate();
            logger.info("Category created: {}", category.getCategoryName());
            return rowsAffected > 0;

        } catch (SQLException e) {
            logger.error("Error creating category", e);
            return false;
        }
    }

    /**
     * Get all categories for a user (including default categories)
     */
    public List<Category> getCategoriesByUser(UUID userId) {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT * FROM categories WHERE user_id = ? OR is_default = true ORDER BY category_name";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                categories.add(mapResultSetToCategory(rs));
            }

        } catch (SQLException e) {
            logger.error("Error getting categories by user", e);
        }

        return categories;
    }

    /**
     * Get categories by type for a user
     */
    public List<Category> getCategoriesByType(UUID userId, CategoryType type) {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT * FROM categories WHERE (user_id = ? OR is_default = true) " +
                "AND category_type = ? ORDER BY category_name";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, userId);
            pstmt.setString(2, type.name());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                categories.add(mapResultSetToCategory(rs));
            }

        } catch (SQLException e) {
            logger.error("Error getting categories by type", e);
        }

        return categories;
    }

    /**
     * Get income categories for a user
     */
    public List<Category> getIncomeCategories(UUID userId) {
        return getCategoriesByType(userId, CategoryType.INCOME);
    }

    /**
     * Get expense categories for a user
     */
    public List<Category> getExpenseCategories(UUID userId) {
        return getCategoriesByType(userId, CategoryType.EXPENSE);
    }

    /**
     * Get category by ID
     */
    public Category getCategoryById(UUID categoryId) {
        String sql = "SELECT * FROM categories WHERE category_id = ?";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, categoryId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToCategory(rs);
            }

        } catch (SQLException e) {
            logger.error("Error getting category by ID", e);
        }

        return null;
    }

    /**
     * Get subcategories for a parent category
     */
    public List<Category> getSubcategories(UUID parentCategoryId) {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT * FROM categories WHERE parent_category_id = ? ORDER BY category_name";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, parentCategoryId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                categories.add(mapResultSetToCategory(rs));
            }

        } catch (SQLException e) {
            logger.error("Error getting subcategories", e);
        }

        return categories;
    }

    /**
     * Update a category (only non-default categories can be updated)
     */
    public boolean updateCategory(Category category) {
        String sql = "UPDATE categories SET category_name = ?, color_code = ?, icon_name = ?, " +
                "parent_category_id = ? WHERE category_id = ? AND is_default = false";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, category.getCategoryName());
            pstmt.setString(2, category.getColorCode());
            pstmt.setString(3, category.getIconName());
            pstmt.setObject(4, category.getParentCategoryId());
            pstmt.setObject(5, category.getCategoryId());

            int rowsAffected = pstmt.executeUpdate();
            logger.info("Category updated: {}", category.getCategoryName());
            return rowsAffected > 0;

        } catch (SQLException e) {
            logger.error("Error updating category", e);
            return false;
        }
    }

    /**
     * Delete a category (only non-default categories can be deleted)
     */
    public boolean deleteCategory(UUID categoryId) {
        String sql = "DELETE FROM categories WHERE category_id = ? AND is_default = false";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, categoryId);
            int rowsAffected = pstmt.executeUpdate();
            logger.info("Category deleted: {}", categoryId);
            return rowsAffected > 0;

        } catch (SQLException e) {
            logger.error("Error deleting category", e);
            return false;
        }
    }

    /**
     * Check if category name exists for user
     */
    public boolean categoryNameExists(UUID userId, String categoryName, CategoryType type) {
        String sql = "SELECT COUNT(*) FROM categories WHERE (user_id = ? OR is_default = true) " +
                "AND LOWER(category_name) = LOWER(?) AND category_type = ?";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, userId);
            pstmt.setString(2, categoryName);
            pstmt.setString(3, type.name());
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            logger.error("Error checking category name existence", e);
        }

        return false;
    }

    /**
     * Get default categories
     */
    public List<Category> getDefaultCategories() {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT * FROM categories WHERE is_default = true ORDER BY category_type, category_name";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                categories.add(mapResultSetToCategory(rs));
            }

        } catch (SQLException e) {
            logger.error("Error getting default categories", e);
        }

        return categories;
    }

    /**
     * Map ResultSet to Category object
     */
    private Category mapResultSetToCategory(ResultSet rs) throws SQLException {
        Category category = new Category();
        category.setCategoryId((UUID) rs.getObject("category_id"));

        Object userIdObj = rs.getObject("user_id");
        if (userIdObj != null) {
            category.setUserId((UUID) userIdObj);
        }

        category.setCategoryName(rs.getString("category_name"));

        String typeStr = rs.getString("category_type");
        if (typeStr != null) {
            category.setCategoryType(CategoryType.valueOf(typeStr));
        }

        Object parentIdObj = rs.getObject("parent_category_id");
        if (parentIdObj != null) {
            category.setParentCategoryId((UUID) parentIdObj);
        }

        category.setColorCode(rs.getString("color_code"));
        category.setIconName(rs.getString("icon_name"));
        category.setDefault(rs.getBoolean("is_default"));
        category.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());

        return category;
    }
}