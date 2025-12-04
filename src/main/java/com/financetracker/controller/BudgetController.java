package com.financetracker.controller;

import com.financetracker.model.Budget;
import com.financetracker.model.Category;
import com.financetracker.service.BudgetService;
import com.financetracker.service.CategoryService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Controller for Budget Management View
 */
public class BudgetController {

    private static final Logger logger = LoggerFactory.getLogger(BudgetController.class);

    // FXML Components
    @FXML private Label totalBudgetLabel;
    @FXML private Label activeBudgetsLabel;
    @FXML private Label warningBudgetsLabel;
    @FXML private TableView<Budget> budgetTable;
    @FXML private TableColumn<Budget, String> nameColumn;
    @FXML private TableColumn<Budget, String> categoryColumn;
    @FXML private TableColumn<Budget, String> limitColumn;
    @FXML private TableColumn<Budget, String> spentColumn;
    @FXML private TableColumn<Budget, String> remainingColumn;
    @FXML private TableColumn<Budget, String> statusColumn;
    @FXML private TableColumn<Budget, String> periodColumn;
    @FXML private ComboBox<String> filterComboBox;
    @FXML private TextField searchField;

    // Services
    private BudgetService budgetService;
    private CategoryService categoryService;

    // Data
    private ObservableList<Budget> budgetList;
    private List<Budget> allBudgets; // Store all budgets for filtering
    private UUID currentUserId;
    private List<Category> expenseCategories;

    @FXML
    public void initialize() {
        logger.info("Initializing BudgetController");

        budgetService = new BudgetService();
        categoryService = new CategoryService();
        budgetList = FXCollections.observableArrayList();

        if (LoginController.getCurrentUser() != null) {
            currentUserId = LoginController.getCurrentUser().getUserId();
        }

        setupTable();
        setupFilters();
        setupSearch();
        loadBudgetData();
        updateSummary();

        logger.info("BudgetController initialized");
    }

    /**
     * Setup table columns
     */
    private void setupTable() {
        // Name column
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("budgetName"));

        // Category column
        categoryColumn.setCellValueFactory(cellData -> {
            String categoryName = cellData.getValue().getCategoryName();
            return new SimpleStringProperty(categoryName != null ? categoryName : "All Categories");
        });

        // Limit column
        limitColumn.setCellValueFactory(cellData -> {
            BigDecimal limit = cellData.getValue().getAmountLimit();
            return new SimpleStringProperty(String.format("$%,.2f", limit));
        });
        limitColumn.setStyle("-fx-alignment: CENTER-RIGHT;");

        // Spent column
        spentColumn.setCellValueFactory(cellData -> {
            BigDecimal spent = cellData.getValue().getSpentAmount();
            return new SimpleStringProperty(String.format("$%,.2f", spent != null ? spent : BigDecimal.ZERO));
        });
        spentColumn.setStyle("-fx-alignment: CENTER-RIGHT;");

        // Remaining column
        remainingColumn.setCellValueFactory(cellData -> {
            BigDecimal remaining = cellData.getValue().getRemainingAmount();
            return new SimpleStringProperty(String.format("$%,.2f", remaining != null ? remaining : BigDecimal.ZERO));
        });
        remainingColumn.setStyle("-fx-alignment: CENTER-RIGHT;");

        // Status column with color coding
        statusColumn.setCellValueFactory(cellData -> {
            String status = cellData.getValue().getStatus();
            double percentage = cellData.getValue().getPercentageUsed();
            return new SimpleStringProperty(String.format("%s (%.0f%%)", status, percentage));
        });
        statusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.startsWith("EXCEEDED")) {
                        setStyle("-fx-text-fill: #D9534F; -fx-font-weight: bold;");
                    } else if (item.startsWith("DANGER")) {
                        setStyle("-fx-text-fill: #F0AD4E; -fx-font-weight: bold;");
                    } else if (item.startsWith("WARNING")) {
                        setStyle("-fx-text-fill: #5BC0DE;");
                    } else {
                        setStyle("-fx-text-fill: #5CB85C;");
                    }
                }
            }
        });

        // Period column
        periodColumn.setCellValueFactory(cellData -> {
            Budget b = cellData.getValue();
            String period = b.getPeriod();
            return new SimpleStringProperty(period != null ? period : "");
        });

        // Attach list
        budgetTable.setItems(budgetList);

        // Double-click to edit
        budgetTable.setRowFactory(tv -> {
            TableRow<Budget> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    handleEditBudget();
                }
            });
            return row;
        });

        // Context menu
        ContextMenu ctx = new ContextMenu();
        MenuItem edit = new MenuItem("Edit");
        edit.setOnAction(e -> handleEditBudget());
        MenuItem delete = new MenuItem("Delete");
        delete.setOnAction(e -> handleDeleteBudget());
        ctx.getItems().addAll(edit, delete);
        budgetTable.setContextMenu(ctx);
    }

    /**
     * Setup filter section
     */
    private void setupFilters() {
        if (filterComboBox != null) {
            filterComboBox.setItems(FXCollections.observableArrayList(
                    "All Budgets", "Active Only", "Exceeded", "Warning"
            ));
            filterComboBox.setValue("Active Only");
            filterComboBox.setOnAction(e -> applyFilter());
        }
    }

    /**
     * Setup real-time search functionality
     */
    private void setupSearch() {
        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                filterBySearch(newValue);
            });
        }
    }

    /**
     * Load budget data from database
     */
    private void loadBudgetData() {
        if (currentUserId == null) return;

        try {
            expenseCategories = categoryService.getExpenseCategories(currentUserId);
            applyFilter();
        } catch (Exception e) {
            logger.error("Error loading budgets", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load budgets: " + e.getMessage());
        }
    }

    /**
     * Apply filter
     */
    private void applyFilter() {
        if (currentUserId == null || filterComboBox == null) return;

        String filter = filterComboBox.getValue();

        switch (filter) {
            case "Active Only":
                allBudgets = budgetService.getActiveBudgets(currentUserId);
                break;
            case "Exceeded":
                allBudgets = budgetService.getActiveBudgets(currentUserId).stream()
                        .filter(b -> b.getStatus().equals("EXCEEDED"))
                        .toList();
                break;
            case "Warning":
                allBudgets = budgetService.getActiveBudgets(currentUserId).stream()
                        .filter(b -> b.getStatus().equals("WARNING") || b.getStatus().equals("DANGER"))
                        .toList();
                break;
            case "All Budgets":
            default:
                allBudgets = budgetService.getBudgetsByUser(currentUserId);
                break;
        }

        // Apply search filter if there's text in the search field
        if (searchField != null && !searchField.getText().isEmpty()) {
            filterBySearch(searchField.getText());
        } else {
            budgetList.setAll(allBudgets);
        }

        updateSummary();
    }

    /**
     * Filter budgets by search text (real-time)
     */
    private void filterBySearch(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            // No search text, show all budgets from current filter
            if (allBudgets != null) {
                budgetList.setAll(allBudgets);
            }
            return;
        }

        String searchLower = searchText.toLowerCase().trim();

        // Filter the allBudgets list based on search text
        List<Budget> filteredList;
        if (allBudgets != null) {
            filteredList = allBudgets.stream()
                    .filter(budget -> {
                        // Search by budget name
                        if (budget.getBudgetName() != null &&
                                budget.getBudgetName().toLowerCase().contains(searchLower)) {
                            return true;
                        }
                        // Search by category name
                        if (budget.getCategoryName() != null &&
                                budget.getCategoryName().toLowerCase().contains(searchLower)) {
                            return true;
                        }
                        // Search by period
                        if (budget.getPeriod() != null &&
                                budget.getPeriod().toLowerCase().contains(searchLower)) {
                            return true;
                        }
                        // Search by status
                        if (budget.getStatus() != null &&
                                budget.getStatus().toLowerCase().contains(searchLower)) {
                            return true;
                        }
                        return false;
                    })
                    .toList();
        } else {
            filteredList = List.of();
        }

        budgetList.setAll(filteredList);
    }

    /**
     * Update the summary cards
     */
    private void updateSummary() {
        if (currentUserId == null) return;

        BigDecimal totalLimit = budgetService.getTotalBudgetLimit(currentUserId);
        totalBudgetLabel.setText(String.format("$%,.2f", totalLimit));

        List<Budget> activeBudgets = budgetService.getActiveBudgets(currentUserId);
        activeBudgetsLabel.setText(String.valueOf(activeBudgets.size()));

        long warningCount = activeBudgets.stream()
                .filter(b -> !b.getStatus().equals("OK"))
                .count();
        warningBudgetsLabel.setText(String.valueOf(warningCount));
    }

    /**
     * Add Budget
     */
    @FXML
    private void handleAddBudget() {
        showBudgetDialog(null);
    }

    /**
     * Edit Budget
     */
    @FXML
    private void handleEditBudget() {
        Budget sel = budgetTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a budget to edit.");
            return;
        }
        showBudgetDialog(sel);
    }

    /**
     * Delete Budget
     */
    @FXML
    private void handleDeleteBudget() {
        Budget sel = budgetTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a budget to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Budget");
        confirm.setContentText("Are you sure you want to delete the budget: " + sel.getBudgetName() + "?");

        if (confirm.showAndWait().filter(btn -> btn == ButtonType.OK).isPresent()) {
            if (budgetService.deleteBudget(sel.getBudgetId())) {
                budgetList.remove(sel);
                if (allBudgets != null) {
                    allBudgets = allBudgets.stream()
                            .filter(b -> !b.getBudgetId().equals(sel.getBudgetId()))
                            .toList();
                }
                updateSummary();
                showAlert(Alert.AlertType.INFORMATION, "Success", "Budget deleted successfully.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete budget.");
            }
        }
    }

    @FXML
    private void handleRefresh() {
        if (searchField != null) {
            searchField.clear();
        }
        loadBudgetData();
    }

    /**
     * Add/Edit dialog for budgets
     */
    private void showBudgetDialog(Budget existing) {
        Dialog<Budget> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Budget" : "Edit Budget");
        dialog.setHeaderText(existing == null ? "Create a new budget" : "Update budget details");

        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField();
        nameField.setPromptText("e.g., Monthly Groceries");

        TextField limitField = new TextField();
        limitField.setPromptText("0.00");

        ComboBox<Category> categoryCombo = new ComboBox<>();
        categoryCombo.setItems(FXCollections.observableArrayList(expenseCategories));
        categoryCombo.setConverter(new StringConverter<>() {
            public String toString(Category c) { return c != null ? c.getCategoryName() : ""; }
            public Category fromString(String s) { return null; }
        });

        ComboBox<String> periodCombo = new ComboBox<>();
        periodCombo.setItems(FXCollections.observableArrayList(
                "WEEKLY", "MONTHLY", "QUARTERLY", "YEARLY"
        ));
        periodCombo.setValue("MONTHLY");

        DatePicker startPicker = new DatePicker(LocalDate.now().withDayOfMonth(1));
        DatePicker endPicker = new DatePicker(LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()));

        Spinner<Integer> thresholdSpinner = new Spinner<>(50, 100, 80, 5);
        thresholdSpinner.setEditable(true);

        CheckBox activeCheck = new CheckBox("Active");
        activeCheck.setSelected(true);

        // Auto-adjust end date based on period
        periodCombo.setOnAction(e -> {
            LocalDate start = startPicker.getValue();
            if (start != null) {
                switch (periodCombo.getValue()) {
                    case "WEEKLY":
                        endPicker.setValue(start.plusWeeks(1).minusDays(1));
                        break;
                    case "MONTHLY":
                        endPicker.setValue(start.plusMonths(1).minusDays(1));
                        break;
                    case "QUARTERLY":
                        endPicker.setValue(start.plusMonths(3).minusDays(1));
                        break;
                    case "YEARLY":
                        endPicker.setValue(start.plusYears(1).minusDays(1));
                        break;
                }
            }
        });

        if (existing != null) {
            nameField.setText(existing.getBudgetName());
            limitField.setText(existing.getAmountLimit().toString());
            periodCombo.setValue(existing.getPeriod());
            startPicker.setValue(existing.getStartDate());
            endPicker.setValue(existing.getEndDate());
            thresholdSpinner.getValueFactory().setValue(existing.getAlertThreshold());
            activeCheck.setSelected(existing.isActive());

            for (Category c : expenseCategories) {
                if (c.getCategoryId().equals(existing.getCategoryId())) {
                    categoryCombo.setValue(c);
                    break;
                }
            }
        }

        grid.add(new Label("Budget Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Limit Amount:"), 0, 1);
        grid.add(limitField, 1, 1);
        grid.add(new Label("Category:"), 0, 2);
        grid.add(categoryCombo, 1, 2);
        grid.add(new Label("Period:"), 0, 3);
        grid.add(periodCombo, 1, 3);
        grid.add(new Label("Start Date:"), 0, 4);
        grid.add(startPicker, 1, 4);
        grid.add(new Label("End Date:"), 0, 5);
        grid.add(endPicker, 1, 5);
        grid.add(new Label("Alert at (%):"), 0, 6);
        grid.add(thresholdSpinner, 1, 6);
        grid.add(activeCheck, 1, 7);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                try {
                    if (nameField.getText().isEmpty()) {
                        showAlert(Alert.AlertType.ERROR, "Validation Error", "Budget name is required.");
                        return null;
                    }

                    BigDecimal limit;
                    try {
                        limit = new BigDecimal(limitField.getText().replace(",", ""));
                        if (limit.compareTo(BigDecimal.ZERO) <= 0) {
                            showAlert(Alert.AlertType.ERROR, "Validation Error", "Limit must be greater than 0.");
                            return null;
                        }
                    } catch (Exception e) {
                        showAlert(Alert.AlertType.ERROR, "Validation Error", "Invalid limit amount.");
                        return null;
                    }

                    if (startPicker.getValue().isAfter(endPicker.getValue())) {
                        showAlert(Alert.AlertType.ERROR, "Validation Error", "Start date must be before end date.");
                        return null;
                    }

                    Budget budget = existing != null ? existing : new Budget();
                    budget.setUserId(currentUserId);
                    budget.setBudgetName(nameField.getText().trim());
                    budget.setAmountLimit(limit);
                    budget.setPeriod(periodCombo.getValue());
                    budget.setStartDate(startPicker.getValue());
                    budget.setEndDate(endPicker.getValue());
                    budget.setAlertThreshold(thresholdSpinner.getValue());
                    budget.setActive(activeCheck.isSelected());

                    if (categoryCombo.getValue() != null) {
                        budget.setCategoryId(categoryCombo.getValue().getCategoryId());
                        budget.setCategoryName(categoryCombo.getValue().getCategoryName());
                    }

                    return budget;
                } catch (Exception ex) {
                    logger.error("Error saving budget", ex);
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to save budget");
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(budget -> {
            boolean success = (existing == null)
                    ? budgetService.createBudget(budget)
                    : budgetService.updateBudget(budget);

            if (success) {
                loadBudgetData();
                showAlert(Alert.AlertType.INFORMATION, "Success",
                        "Budget " + (existing == null ? "created" : "updated") + " successfully.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to save budget.");
            }
        });
    }

    /**
     * Set user ID externally
     */
    public void setCurrentUserId(UUID userId) {
        this.currentUserId = userId;
        loadBudgetData();
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