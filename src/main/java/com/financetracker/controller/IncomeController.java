package com.financetracker.controller;

import com.financetracker.model.Category;
import com.financetracker.model.Income;
import com.financetracker.service.CategoryService;
import com.financetracker.service.IncomeService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Controller for Income Management View
 */
public class IncomeController {
    private static final Logger logger = LoggerFactory.getLogger(IncomeController.class);

    // FXML Components
    @FXML private Label totalIncomeLabel;
    @FXML private Label monthlyIncomeLabel;
    @FXML private TableView<Income> incomeTable;
    @FXML private TableColumn<Income, String> dateColumn;
    @FXML private TableColumn<Income, String> sourceColumn;
    @FXML private TableColumn<Income, String> categoryColumn;
    @FXML private TableColumn<Income, String> amountColumn;
    @FXML private TableColumn<Income, String> recurringColumn;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterComboBox;
    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;

    // Services
    private IncomeService incomeService;
    private CategoryService categoryService;

    // Data
    private ObservableList<Income> incomeList;
    private UUID currentUserId;
    private List<Category> incomeCategories;

    // Date formatter
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    @FXML
    public void initialize() {
        logger.info("Initializing IncomeController");

        incomeService = new IncomeService();
        categoryService = new CategoryService();
        incomeList = FXCollections.observableArrayList();

        // Get current user ID from LoginController
        if (LoginController.getCurrentUser() != null) {
            currentUserId = LoginController.getCurrentUser().getUserId();
        }

        setupTable();
        setupFilters();
        loadIncomeData();
        updateSummary();

        logger.info("IncomeController initialized");
    }

    /**
     * Setup table columns
     */
    private void setupTable() {
        // Date column
        dateColumn.setCellValueFactory(cellData -> {
            LocalDate date = cellData.getValue().getIncomeDate();
            return new SimpleStringProperty(date != null ? date.format(DATE_FORMATTER) : "");
        });

        // Source column
        sourceColumn.setCellValueFactory(new PropertyValueFactory<>("source"));

        // Category column
        categoryColumn.setCellValueFactory(cellData -> {
            String categoryName = cellData.getValue().getCategoryName();
            return new SimpleStringProperty(categoryName != null ? categoryName : "Uncategorized");
        });

        // Amount column with currency formatting
        amountColumn.setCellValueFactory(cellData -> {
            BigDecimal amount = cellData.getValue().getAmount();
            return new SimpleStringProperty(String.format("$%,.2f", amount));
        });
        amountColumn.setStyle("-fx-alignment: CENTER-RIGHT;");

        // Recurring column
        recurringColumn.setCellValueFactory(cellData -> {
            boolean recurring = cellData.getValue().isRecurring();
            String frequency = cellData.getValue().getRecurringFrequency();
            if (recurring && frequency != null) {
                return new SimpleStringProperty("Yes (" + frequency + ")");
            }
            return new SimpleStringProperty(recurring ? "Yes" : "No");
        });

        // Set table data
        incomeTable.setItems(incomeList);

        // Double-click to edit
        incomeTable.setRowFactory(tv -> {
            TableRow<Income> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    handleEditIncome();
                }
            });
            return row;
        });

        // Add context menu
        ContextMenu contextMenu = new ContextMenu();
        MenuItem editItem = new MenuItem("Edit");
        editItem.setOnAction(e -> handleEditIncome());
        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(e -> handleDeleteIncome());
        contextMenu.getItems().addAll(editItem, deleteItem);
        incomeTable.setContextMenu(contextMenu);
    }

    /**
     * Setup filter controls
     */
    private void setupFilters() {
        // Filter combo box options
        if (filterComboBox != null) {
            filterComboBox.setItems(FXCollections.observableArrayList(
                    "All Time", "This Month", "Last Month", "This Year", "Custom Range"
            ));
            filterComboBox.setValue("This Month");
            filterComboBox.setOnAction(e -> applyFilter());
        }

        // Date pickers - default to current month
        LocalDate now = LocalDate.now();
        if (fromDatePicker != null) {
            fromDatePicker.setValue(now.withDayOfMonth(1));
            fromDatePicker.setOnAction(e -> applyFilter());
        }
        if (toDatePicker != null) {
            toDatePicker.setValue(now.withDayOfMonth(now.lengthOfMonth()));
            toDatePicker.setOnAction(e -> applyFilter());
        }

        // Search field listener
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                filterBySearch(newVal);
            });
        }
    }

    /**
     * Load income data from database
     */
    private void loadIncomeData() {
        if (currentUserId == null) {
            logger.warn("No user logged in, cannot load income data");
            return;
        }

        try {
            // Load categories for dropdown
            incomeCategories = categoryService.getIncomeCategories(currentUserId);

            // Apply current filter
            applyFilter();

        } catch (Exception e) {
            logger.error("Error loading income data", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load income data: " + e.getMessage());
        }
    }

    /**
     * Apply date filter
     */
    private void applyFilter() {
        if (currentUserId == null || filterComboBox == null) return;

        LocalDate startDate;
        LocalDate endDate;
        LocalDate now = LocalDate.now();

        String filter = filterComboBox.getValue();

        switch (filter) {
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
                if (startDate == null) startDate = now.minusYears(1);
                if (endDate == null) endDate = now;
                break;
            case "All Time":
            default:
                startDate = LocalDate.of(2000, 1, 1);
                endDate = now.plusYears(1);
                break;
        }

        // Update date pickers
        if (fromDatePicker != null) fromDatePicker.setValue(startDate);
        if (toDatePicker != null) toDatePicker.setValue(endDate);

        // Load filtered data
        List<Income> filteredIncome = incomeService.getIncomeByDateRange(currentUserId, startDate, endDate);
        incomeList.setAll(filteredIncome);

        updateSummary();
    }

    /**
     * Filter by search text
     */
    private void filterBySearch(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            applyFilter();
            return;
        }

        List<Income> searchResults = incomeService.searchIncomeBySource(currentUserId, searchText);
        incomeList.setAll(searchResults);
    }

    /**
     * Update summary labels
     */
    private void updateSummary() {
        if (currentUserId == null) return;

        LocalDate now = LocalDate.now();
        LocalDate monthStart = now.withDayOfMonth(1);
        LocalDate monthEnd = now.withDayOfMonth(now.lengthOfMonth());

        // Calculate monthly income
        BigDecimal monthlyTotal = incomeService.getTotalIncome(currentUserId, monthStart, monthEnd);
        if (monthlyIncomeLabel != null) {
            monthlyIncomeLabel.setText(String.format("$%,.2f", monthlyTotal));
        }

        // Calculate total from current filter/table
        BigDecimal tableTotal = incomeList.stream()
                .map(Income::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalIncomeLabel != null) {
            totalIncomeLabel.setText(String.format("$%,.2f", tableTotal));
        }
    }

    /**
     * Handle Add Income button click
     */
    @FXML
    private void handleAddIncome() {
        logger.info("Add Income clicked");
        showIncomeDialog(null);
    }

    /**
     * Handle Edit Income
     */
    @FXML
    private void handleEditIncome() {
        Income selected = incomeTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select an income entry to edit.");
            return;
        }
        showIncomeDialog(selected);
    }

    /**
     * Handle Delete Income
     */
    @FXML
    private void handleDeleteIncome() {
        Income selected = incomeTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select an income entry to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Income Entry");
        confirm.setContentText("Are you sure you want to delete this income entry?\n\n" +
                "Source: " + selected.getSource() + "\n" +
                "Amount: " + String.format("$%,.2f", selected.getAmount()));

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (incomeService.deleteIncome(selected.getIncomeId())) {
                incomeList.remove(selected);
                updateSummary();
                showAlert(Alert.AlertType.INFORMATION, "Success", "Income entry deleted successfully.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete income entry.");
            }
        }
    }

    /**
     * Handle Refresh button click
     */
    @FXML
    private void handleRefresh() {
        loadIncomeData();
    }

    /**
     * Show Income Add/Edit Dialog
     */
    private void showIncomeDialog(Income existingIncome) {
        Dialog<Income> dialog = new Dialog<>();
        dialog.setTitle(existingIncome == null ? "Add Income" : "Edit Income");
        dialog.setHeaderText(existingIncome == null ? "Enter income details" : "Update income details");

        // Set button types
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Create form
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // Form fields
        TextField sourceField = new TextField();
        sourceField.setPromptText("e.g., Salary, Freelance");

        TextField amountField = new TextField();
        amountField.setPromptText("0.00");

        DatePicker datePicker = new DatePicker(LocalDate.now());

        ComboBox<Category> categoryCombo = new ComboBox<>();
        categoryCombo.setItems(FXCollections.observableArrayList(incomeCategories));
        categoryCombo.setConverter(new StringConverter<Category>() {
            @Override
            public String toString(Category category) {
                return category != null ? category.getCategoryName() : "";
            }
            @Override
            public Category fromString(String string) {
                return null;
            }
        });

        TextArea descriptionArea = new TextArea();
        descriptionArea.setPromptText("Optional description");
        descriptionArea.setPrefRowCount(2);

        CheckBox recurringCheck = new CheckBox("Recurring Income");

        ComboBox<String> frequencyCombo = new ComboBox<>();
        frequencyCombo.setItems(FXCollections.observableArrayList(
                "DAILY", "WEEKLY", "MONTHLY", "QUARTERLY", "YEARLY"
        ));
        frequencyCombo.setDisable(true);

        // Link recurring checkbox to frequency combo
        recurringCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            frequencyCombo.setDisable(!newVal);
            if (!newVal) frequencyCombo.setValue(null);
        });

        // Populate fields if editing
        if (existingIncome != null) {
            sourceField.setText(existingIncome.getSource());
            amountField.setText(existingIncome.getAmount().toString());
            datePicker.setValue(existingIncome.getIncomeDate());
            descriptionArea.setText(existingIncome.getDescription());
            recurringCheck.setSelected(existingIncome.isRecurring());
            frequencyCombo.setValue(existingIncome.getRecurringFrequency());

            // Find and select category
            if (existingIncome.getCategoryId() != null) {
                for (Category cat : incomeCategories) {
                    if (cat.getCategoryId().equals(existingIncome.getCategoryId())) {
                        categoryCombo.setValue(cat);
                        break;
                    }
                }
            }
        }

        // Add fields to grid
        grid.add(new Label("Source:"), 0, 0);
        grid.add(sourceField, 1, 0);
        grid.add(new Label("Amount:"), 0, 1);
        grid.add(amountField, 1, 1);
        grid.add(new Label("Date:"), 0, 2);
        grid.add(datePicker, 1, 2);
        grid.add(new Label("Category:"), 0, 3);
        grid.add(categoryCombo, 1, 3);
        grid.add(new Label("Description:"), 0, 4);
        grid.add(descriptionArea, 1, 4);
        grid.add(recurringCheck, 0, 5);
        grid.add(frequencyCombo, 1, 5);

        dialog.getDialogPane().setContent(grid);

        // Convert result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    // Validate
                    if (sourceField.getText().isEmpty()) {
                        showAlert(Alert.AlertType.ERROR, "Validation Error", "Source is required.");
                        return null;
                    }

                    BigDecimal amount;
                    try {
                        amount = new BigDecimal(amountField.getText().replace(",", ""));
                        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                            showAlert(Alert.AlertType.ERROR, "Validation Error", "Amount must be greater than 0.");
                            return null;
                        }
                    } catch (NumberFormatException e) {
                        showAlert(Alert.AlertType.ERROR, "Validation Error", "Invalid amount format.");
                        return null;
                    }

                    Income income = existingIncome != null ? existingIncome : new Income();
                    income.setUserId(currentUserId);
                    income.setSource(sourceField.getText().trim());
                    income.setAmount(amount);
                    income.setIncomeDate(datePicker.getValue());
                    income.setDescription(descriptionArea.getText().trim());
                    income.setRecurring(recurringCheck.isSelected());
                    income.setRecurringFrequency(frequencyCombo.getValue());

                    if (categoryCombo.getValue() != null) {
                        income.setCategoryId(categoryCombo.getValue().getCategoryId());
                        income.setCategoryName(categoryCombo.getValue().getCategoryName());
                    }

                    return income;
                } catch (Exception e) {
                    logger.error("Error creating income object", e);
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to save income: " + e.getMessage());
                    return null;
                }
            }
            return null;
        });

        // Show dialog and process result
        Optional<Income> result = dialog.showAndWait();
        result.ifPresent(income -> {
            boolean success;
            if (existingIncome == null) {
                success = incomeService.createIncome(income);
            } else {
                success = incomeService.updateIncome(income);
            }

            if (success) {
                loadIncomeData();
                showAlert(Alert.AlertType.INFORMATION, "Success",
                        "Income " + (existingIncome == null ? "added" : "updated") + " successfully.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Error",
                        "Failed to " + (existingIncome == null ? "add" : "update") + " income.");
            }
        });
    }

    /**
     * Show alert dialog
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Set the current user ID (called from DashboardController)
     */
    public void setCurrentUserId(UUID userId) {
        this.currentUserId = userId;
        loadIncomeData();
    }
}