package com.financetracker.controller;

import com.financetracker.model.Category;
import com.financetracker.model.Expense;
import com.financetracker.service.CategoryService;
import com.financetracker.service.ExpenseService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Controller for Expense Management View
 */
public class ExpenseController {

    private static final Logger logger = LoggerFactory.getLogger(ExpenseController.class);

    // FXML Components
    @FXML private Label totalExpenseLabel;
    @FXML private Label monthlyExpenseLabel;
    @FXML private TableView<Expense> expenseTable;
    @FXML private TableColumn<Expense, String> dateColumn;
    @FXML private TableColumn<Expense, String> merchantColumn;
    @FXML private TableColumn<Expense, String> categoryColumn;
    @FXML private TableColumn<Expense, String> amountColumn;
    @FXML private TableColumn<Expense, String> paymentMethodColumn;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterComboBox;
    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;

    // Services
    private ExpenseService expenseService;
    private CategoryService categoryService;

    // Data
    private ObservableList<Expense> expenseList;
    private UUID currentUserId;
    private List<Category> expenseCategories;

    // Date formatter
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    @FXML
    public void initialize() {
        logger.info("Initializing ExpenseController");

        expenseService = new ExpenseService();
        categoryService = new CategoryService();
        expenseList = FXCollections.observableArrayList();

        if (LoginController.getCurrentUser() != null) {
            currentUserId = LoginController.getCurrentUser().getUserId();
        }

        setupTable();
        setupFilters();
        loadExpenseData();
        updateSummary();

        logger.info("ExpenseController initialized");
    }

    /**
     * Setup table columns
     */
    private void setupTable() {
        // Date column
        dateColumn.setCellValueFactory(cellData -> {
            LocalDate date = cellData.getValue().getExpenseDate();
            return new SimpleStringProperty(date != null ? date.format(DATE_FORMATTER) : "");
        });

        // Merchant/Vendor column
        merchantColumn.setCellValueFactory(new PropertyValueFactory<>("vendor"));

        // Category column
        categoryColumn.setCellValueFactory(cellData -> {
            String categoryName = cellData.getValue().getCategoryName();
            return new SimpleStringProperty(categoryName != null ? categoryName : "Uncategorized");
        });

        // Amount column — currency formatting
        amountColumn.setCellValueFactory(cellData -> {
            BigDecimal amount = cellData.getValue().getAmount();
            return new SimpleStringProperty(String.format("$%,.2f", amount));
        });
        amountColumn.setStyle("-fx-alignment: CENTER-RIGHT;");

        // Payment Method
        paymentMethodColumn.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));

        // Attach list
        expenseTable.setItems(expenseList);

        // Double-click to edit
        expenseTable.setRowFactory(tv -> {
            TableRow<Expense> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    handleEditExpense();
                }
            });
            return row;
        });

        // Context menu
        ContextMenu ctx = new ContextMenu();
        MenuItem edit = new MenuItem("Edit");
        edit.setOnAction(e -> handleEditExpense());
        MenuItem delete = new MenuItem("Delete");
        delete.setOnAction(e -> handleDeleteExpense());
        ctx.getItems().addAll(edit, delete);

        expenseTable.setContextMenu(ctx);
    }

    /**
     * Setup filter section
     */
    private void setupFilters() {
        if (filterComboBox != null) {
            filterComboBox.setItems(FXCollections.observableArrayList(
                    "All Time", "This Month", "Last Month", "This Year", "Custom Range"
            ));
            filterComboBox.setValue("This Month");
            filterComboBox.setOnAction(e -> applyFilter());
        }

        LocalDate now = LocalDate.now();

        if (fromDatePicker != null) {
            fromDatePicker.setValue(now.withDayOfMonth(1));
            fromDatePicker.setOnAction(e -> applyFilter());
        }
        if (toDatePicker != null) {
            toDatePicker.setValue(now.withDayOfMonth(now.lengthOfMonth()));
            toDatePicker.setOnAction(e -> applyFilter());
        }

        if (searchField != null) {
            searchField.textProperty().addListener(
                    (obs, o, n) -> filterBySearch(n)
            );
        }
    }

    /**
     * Load expenses from database
     */
    private void loadExpenseData() {
        if (currentUserId == null) return;

        try {
            expenseCategories = categoryService.getExpenseCategories(currentUserId);
            applyFilter();
        } catch (Exception e) {
            logger.error("Error loading expenses", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load expenses: " + e.getMessage());
        }
    }

    /**
     * Apply date filter
     */
    private void applyFilter() {
        if (currentUserId == null || filterComboBox == null) return;

        LocalDate now = LocalDate.now();
        LocalDate startDate;
        LocalDate endDate;

        switch (filterComboBox.getValue()) {
            case "This Month":
                startDate = now.withDayOfMonth(1);
                endDate = now.withDayOfMonth(now.lengthOfMonth());
                break;
            case "Last Month":
                LocalDate lastMonth = now.minusMonths(1);
                startDate = lastMonth.withDayOfMonth(1);
                endDate = lastMonth.withDayOfMonth(lastMonth.lengthOfMonth());
                break;
            case "This Year":
                startDate = now.withDayOfYear(1);
                endDate = now.withDayOfYear(now.lengthOfYear());
                break;
            case "Custom Range":
                startDate = fromDatePicker.getValue();
                endDate = toDatePicker.getValue();
                break;
            case "All Time":
            default:
                startDate = LocalDate.of(2000, 1, 1);
                endDate = now.plusYears(1);
                break;
        }

        List<Expense> filtered = expenseService.getExpensesByDateRange(currentUserId, startDate, endDate);
        expenseList.setAll(filtered);

        updateSummary();
    }

    /**
     * Search filter
     */
    private void filterBySearch(String text) {
        if (text == null || text.isEmpty()) {
            applyFilter();
            return;
        }
        // Filter the current list by vendor name (case-insensitive)
        String searchLower = text.toLowerCase();
        List<Expense> allExpenses = expenseService.getExpensesByUser(currentUserId);
        List<Expense> filtered = allExpenses.stream()
                .filter(e -> e.getVendor() != null && e.getVendor().toLowerCase().contains(searchLower))
                .toList();
        expenseList.setAll(filtered);
    }

    /**
     * Update the summary cards
     */
    private void updateSummary() {
        if (currentUserId == null) return;

        LocalDate now = LocalDate.now();
        LocalDate start = now.withDayOfMonth(1);
        LocalDate end = now.withDayOfMonth(now.lengthOfMonth());

        BigDecimal monthlyTotal = BigDecimal.valueOf(expenseService.getTotalExpenses(currentUserId, start, end));
        monthlyExpenseLabel.setText(String.format("$%,.2f", monthlyTotal));

        BigDecimal tableTotal = expenseList.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        totalExpenseLabel.setText(String.format("$%,.2f", tableTotal));
    }

    /**
     * Add Expense
     */
    @FXML
    private void handleAddExpense() {
        showExpenseDialog(null);
    }

    /**
     * Edit Expense
     */
    @FXML
    private void handleEditExpense() {
        Expense sel = expenseTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select an expense to edit.");
            return;
        }
        showExpenseDialog(sel);
    }

    /**
     * Delete Expense
     */
    @FXML
    private void handleDeleteExpense() {
        Expense sel = expenseTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select an expense to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Expense Entry");
        confirm.setContentText(
                "Vendor: " + sel.getVendor() + "\n" +
                        "Amount: " + String.format("$%,.2f", sel.getAmount())
        );

        if (confirm.showAndWait().filter(btn -> btn == ButtonType.OK).isPresent()) {
            if (expenseService.deleteExpense(sel.getExpenseId())) {
                expenseList.remove(sel);
                updateSummary();
                showAlert(Alert.AlertType.INFORMATION, "Success", "Expense deleted successfully.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete expense.");
            }
        }
    }

    @FXML
    private void handleRefresh() {
        loadExpenseData();
    }

    /**
     * Add/Edit dialog for expenses
     */
    private void showExpenseDialog(Expense existing) {
        Dialog<Expense> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Expense" : "Edit Expense");
        dialog.setHeaderText(existing == null ? "Enter expense details" : "Update expense details");

        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField merchantField = new TextField();
        merchantField.setPromptText("e.g., Starbucks, Amazon");

        TextField amountField = new TextField();
        amountField.setPromptText("0.00");

        DatePicker datePicker = new DatePicker(LocalDate.now());

        ComboBox<Category> categoryCombo = new ComboBox<>();
        categoryCombo.setItems(FXCollections.observableArrayList(expenseCategories));
        categoryCombo.setConverter(new StringConverter<>() {
            public String toString(Category c) { return c != null ? c.getCategoryName() : ""; }
            public Category fromString(String s) { return null; }
        });

        TextField paymentField = new TextField();
        paymentField.setPromptText("Card, Cash, Transfer...");

        TextArea notes = new TextArea();
        notes.setPromptText("Optional notes");
        notes.setPrefRowCount(2);

        if (existing != null) {
            merchantField.setText(existing.getVendor());
            amountField.setText(existing.getAmount().toString());
            datePicker.setValue(existing.getExpenseDate());
            notes.setText(existing.getDescription());
            paymentField.setText(existing.getPaymentMethod());

            for (Category c : expenseCategories) {
                if (c.getCategoryId().equals(existing.getCategoryId())) {
                    categoryCombo.setValue(c);
                    break;
                }
            }
        }

        grid.add(new Label("Merchant:"), 0, 0);
        grid.add(merchantField, 1, 0);
        grid.add(new Label("Amount:"), 0, 1);
        grid.add(amountField, 1, 1);
        grid.add(new Label("Date:"), 0, 2);
        grid.add(datePicker, 1, 2);
        grid.add(new Label("Category:"), 0, 3);
        grid.add(categoryCombo, 1, 3);
        grid.add(new Label("Payment Method:"), 0, 4);
        grid.add(paymentField, 1, 4);
        grid.add(new Label("Notes:"), 0, 5);
        grid.add(notes, 1, 5);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                try {
                    if (merchantField.getText().isEmpty()) {
                        showAlert(Alert.AlertType.ERROR, "Validation Error", "Merchant is required.");
                        return null;
                    }

                    BigDecimal amount;
                    try {
                        amount = new BigDecimal(amountField.getText().replace(",", ""));
                    } catch (Exception e) {
                        showAlert(Alert.AlertType.ERROR, "Validation Error", "Invalid amount.");
                        return null;
                    }

                    Expense exp = existing != null ? existing : new Expense();
                    exp.setUserId(currentUserId);
                    exp.setVendor(merchantField.getText().trim());
                    exp.setAmount(amount);
                    exp.setExpenseDate(datePicker.getValue());
                    exp.setDescription(notes.getText().trim());
                    exp.setPaymentMethod(paymentField.getText().trim());

                    if (categoryCombo.getValue() != null) {
                        exp.setCategoryId(categoryCombo.getValue().getCategoryId());
                        exp.setCategoryName(categoryCombo.getValue().getCategoryName());
                    }

                    return exp;
                } catch (Exception ex) {
                    logger.error("Error saving expense", ex);
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to save expense");
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(exp -> {
            boolean success = (existing == null)
                    ? expenseService.createExpense(exp)
                    : expenseService.updateExpense(exp);

            if (success) {
                loadExpenseData();
                showAlert(Alert.AlertType.INFORMATION, "Success",
                        "Expense " + (existing == null ? "added" : "updated") + " successfully.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to save expense.");
            }
        });
    }

    /**
     * Set user ID externally
     */
    public void setCurrentUserId(UUID userId) {
        this.currentUserId = userId;
        loadExpenseData();
    }

    /**
     * Quick alert utility
     */
    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}