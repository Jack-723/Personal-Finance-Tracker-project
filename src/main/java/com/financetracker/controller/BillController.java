package com.financetracker.controller;

import com.financetracker.model.Bill;
import com.financetracker.model.Category;
import com.financetracker.service.BillService;
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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Controller for Bills & Subscriptions Management View
 */
public class BillController {

    private static final Logger logger = LoggerFactory.getLogger(BillController.class);

    // FXML Components
    @FXML private Label monthlyTotalLabel;
    @FXML private Label activeBillsLabel;
    @FXML private Label dueSoonLabel;
    @FXML private TableView<Bill> billTable;
    @FXML private TableColumn<Bill, String> nameColumn;
    @FXML private TableColumn<Bill, String> vendorColumn;
    @FXML private TableColumn<Bill, String> amountColumn;
    @FXML private TableColumn<Bill, String> cycleColumn;
    @FXML private TableColumn<Bill, String> nextDueColumn;
    @FXML private TableColumn<Bill, String> statusColumn;
    @FXML private TableColumn<Bill, String> categoryColumn;
    @FXML private ComboBox<String> filterComboBox;
    @FXML private TextField searchField;

    // Services
    private BillService billService;
    private CategoryService categoryService;

    // Data
    private ObservableList<Bill> billList;
    private List<Bill> allBills; // Store all bills for filtering
    private UUID currentUserId;
    private List<Category> expenseCategories;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    @FXML
    public void initialize() {
        logger.info("Initializing BillController");

        billService = new BillService();
        categoryService = new CategoryService();
        billList = FXCollections.observableArrayList();

        if (LoginController.getCurrentUser() != null) {
            currentUserId = LoginController.getCurrentUser().getUserId();
        }

        setupTable();
        setupFilters();
        setupSearch();
        loadBillData();
        updateSummary();

        logger.info("BillController initialized");
    }

    /**
     * Setup table columns
     */
    private void setupTable() {
        // Name column
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        // Vendor column
        vendorColumn.setCellValueFactory(cellData -> {
            String vendor = cellData.getValue().getVendor();
            return new SimpleStringProperty(vendor != null ? vendor : "-");
        });

        // Amount column
        amountColumn.setCellValueFactory(cellData -> {
            BigDecimal amount = cellData.getValue().getAmount();
            return new SimpleStringProperty(String.format("$%,.2f", amount));
        });
        amountColumn.setStyle("-fx-alignment: CENTER-RIGHT;");

        // Billing cycle column
        cycleColumn.setCellValueFactory(cellData -> {
            Bill.BillingCycle cycle = cellData.getValue().getBillingCycle();
            return new SimpleStringProperty(cycle != null ? formatCycle(cycle.name()) : "-");
        });

        // Next due date column
        nextDueColumn.setCellValueFactory(cellData -> {
            LocalDate nextDue = cellData.getValue().getNextPaymentDate();
            if (nextDue == null) {
                return new SimpleStringProperty("-");
            }
            long daysUntil = cellData.getValue().getDaysUntilDue();
            String dateStr = nextDue.format(DATE_FORMATTER);
            if (daysUntil == 0) {
                return new SimpleStringProperty(dateStr + " (Today)");
            } else if (daysUntil == 1) {
                return new SimpleStringProperty(dateStr + " (Tomorrow)");
            } else if (daysUntil < 0) {
                return new SimpleStringProperty(dateStr + " (Overdue)");
            } else if (daysUntil <= 7) {
                return new SimpleStringProperty(dateStr + " (" + daysUntil + " days)");
            }
            return new SimpleStringProperty(dateStr);
        });

        // Status column with color coding
        statusColumn.setCellValueFactory(cellData -> {
            return new SimpleStringProperty(cellData.getValue().getStatus());
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
                    switch (item) {
                        case "OVERDUE":
                            setStyle("-fx-text-fill: #D9534F; -fx-font-weight: bold;");
                            break;
                        case "DUE TODAY":
                            setStyle("-fx-text-fill: #F0AD4E; -fx-font-weight: bold;");
                            break;
                        case "DUE SOON":
                            setStyle("-fx-text-fill: #5BC0DE; -fx-font-weight: bold;");
                            break;
                        case "INACTIVE":
                        case "EXPIRED":
                            setStyle("-fx-text-fill: #999999;");
                            break;
                        default:
                            setStyle("-fx-text-fill: #5CB85C;");
                            break;
                    }
                }
            }
        });

        // Category column
        categoryColumn.setCellValueFactory(cellData -> {
            String categoryName = cellData.getValue().getCategoryName();
            return new SimpleStringProperty(categoryName != null ? categoryName : "-");
        });

        // Attach list
        billTable.setItems(billList);

        // Double-click to edit
        billTable.setRowFactory(tv -> {
            TableRow<Bill> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    handleEditBill();
                }
            });
            return row;
        });

        // Context menu
        ContextMenu ctx = new ContextMenu();
        MenuItem edit = new MenuItem("Edit");
        edit.setOnAction(e -> handleEditBill());
        MenuItem markPaid = new MenuItem("Mark as Paid");
        markPaid.setOnAction(e -> handleMarkAsPaid());
        MenuItem delete = new MenuItem("Delete");
        delete.setOnAction(e -> handleDeleteBill());
        ctx.getItems().addAll(edit, markPaid, new SeparatorMenuItem(), delete);
        billTable.setContextMenu(ctx);
    }

    /**
     * Format billing cycle for display
     */
    private String formatCycle(String cycle) {
        if (cycle == null) return "-";
        return cycle.substring(0, 1).toUpperCase() + cycle.substring(1).toLowerCase();
    }

    /**
     * Setup filter section
     */
    private void setupFilters() {
        if (filterComboBox != null) {
            filterComboBox.setItems(FXCollections.observableArrayList(
                    "Active Bills", "All Bills", "Due Soon", "Overdue", "Inactive"
            ));
            filterComboBox.setValue("Active Bills");
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
     * Load bill data from database
     */
    private void loadBillData() {
        if (currentUserId == null) return;

        try {
            expenseCategories = categoryService.getExpenseCategories(currentUserId);
            applyFilter();
        } catch (Exception e) {
            logger.error("Error loading bills", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load bills: " + e.getMessage());
        }
    }

    /**
     * Apply filter
     */
    private void applyFilter() {
        if (currentUserId == null || filterComboBox == null) return;

        String filter = filterComboBox.getValue();

        switch (filter) {
            case "All Bills":
                allBills = billService.getBillsByUser(currentUserId);
                break;
            case "Due Soon":
                allBills = billService.getBillsDueSoon(currentUserId, 7);
                break;
            case "Overdue":
                allBills = billService.getOverdueBills(currentUserId);
                break;
            case "Inactive":
                allBills = billService.getBillsByUser(currentUserId).stream()
                        .filter(b -> !b.isActive() || "EXPIRED".equals(b.getStatus()))
                        .toList();
                break;
            case "Active Bills":
            default:
                allBills = billService.getActiveBills(currentUserId);
                break;
        }

        // Apply search filter if there's text in the search field
        if (searchField != null && !searchField.getText().isEmpty()) {
            filterBySearch(searchField.getText());
        } else {
            billList.setAll(allBills);
        }

        updateSummary();
    }

    /**
     * Filter bills by search text (real-time)
     */
    private void filterBySearch(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            // No search text, show all bills from current filter
            if (allBills != null) {
                billList.setAll(allBills);
            }
            return;
        }

        String searchLower = searchText.toLowerCase().trim();

        // Filter the allBills list based on search text
        List<Bill> filteredList;
        if (allBills != null) {
            filteredList = allBills.stream()
                    .filter(bill -> {
                        // Search by bill name
                        if (bill.getName() != null &&
                                bill.getName().toLowerCase().contains(searchLower)) {
                            return true;
                        }
                        // Search by vendor
                        if (bill.getVendor() != null &&
                                bill.getVendor().toLowerCase().contains(searchLower)) {
                            return true;
                        }
                        // Search by category name
                        if (bill.getCategoryName() != null &&
                                bill.getCategoryName().toLowerCase().contains(searchLower)) {
                            return true;
                        }
                        // Search by billing cycle
                        if (bill.getBillingCycle() != null &&
                                bill.getBillingCycle().name().toLowerCase().contains(searchLower)) {
                            return true;
                        }
                        // Search by status
                        if (bill.getStatus() != null &&
                                bill.getStatus().toLowerCase().contains(searchLower)) {
                            return true;
                        }
                        // Search by description
                        if (bill.getDescription() != null &&
                                bill.getDescription().toLowerCase().contains(searchLower)) {
                            return true;
                        }
                        return false;
                    })
                    .toList();
        } else {
            filteredList = List.of();
        }

        billList.setAll(filteredList);
    }

    /**
     * Update the summary cards
     */
    private void updateSummary() {
        if (currentUserId == null) return;

        BigDecimal monthlyTotal = billService.getTotalMonthlyCost(currentUserId);
        monthlyTotalLabel.setText(String.format("$%,.2f", monthlyTotal));

        List<Bill> activeBills = billService.getActiveBills(currentUserId);
        activeBillsLabel.setText(String.valueOf(activeBills.size()));

        List<Bill> dueSoon = billService.getBillsDueSoon(currentUserId, 7);
        int overdueCount = billService.getOverdueBills(currentUserId).size();
        dueSoonLabel.setText(String.valueOf(dueSoon.size() + overdueCount));
    }

    /**
     * Add Bill
     */
    @FXML
    private void handleAddBill() {
        showBillDialog(null);
    }

    /**
     * Edit Bill
     */
    @FXML
    private void handleEditBill() {
        Bill sel = billTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a bill to edit.");
            return;
        }
        showBillDialog(sel);
    }

    /**
     * Mark bill as paid
     */
    @FXML
    private void handleMarkAsPaid() {
        Bill sel = billTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a bill to mark as paid.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Payment");
        confirm.setHeaderText("Mark Bill as Paid");
        confirm.setContentText("Mark '" + sel.getName() + "' as paid?\n" +
                "Amount: " + String.format("$%,.2f", sel.getAmount()) + "\n\n" +
                "This will update the last payment date to today and calculate the next due date.");

        if (confirm.showAndWait().filter(btn -> btn == ButtonType.OK).isPresent()) {
            if (billService.markBillAsPaid(sel.getBillId())) {
                loadBillData();
                showAlert(Alert.AlertType.INFORMATION, "Success", "Bill marked as paid. Next payment date updated.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to update bill.");
            }
        }
    }

    /**
     * Delete Bill
     */
    @FXML
    private void handleDeleteBill() {
        Bill sel = billTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a bill to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Bill/Subscription");
        confirm.setContentText("Are you sure you want to delete '" + sel.getName() + "'?\n" +
                "This action cannot be undone.");

        if (confirm.showAndWait().filter(btn -> btn == ButtonType.OK).isPresent()) {
            if (billService.deleteBill(sel.getBillId())) {
                billList.remove(sel);
                if (allBills != null) {
                    allBills = allBills.stream()
                            .filter(b -> !b.getBillId().equals(sel.getBillId()))
                            .toList();
                }
                updateSummary();
                showAlert(Alert.AlertType.INFORMATION, "Success", "Bill deleted successfully.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete bill.");
            }
        }
    }

    @FXML
    private void handleRefresh() {
        if (searchField != null) {
            searchField.clear();
        }
        loadBillData();
    }

    /**
     * Add/Edit dialog for bills
     */
    private void showBillDialog(Bill existing) {
        Dialog<Bill> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Bill/Subscription" : "Edit Bill/Subscription");
        dialog.setHeaderText(existing == null ? "Create a new recurring bill" : "Update bill details");

        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // Form fields
        TextField nameField = new TextField();
        nameField.setPromptText("e.g., Netflix, Rent, Insurance");

        TextField vendorField = new TextField();
        vendorField.setPromptText("e.g., Company name");

        TextField amountField = new TextField();
        amountField.setPromptText("0.00");

        ComboBox<String> cycleCombo = new ComboBox<>();
        cycleCombo.setItems(FXCollections.observableArrayList(
                "DAILY", "WEEKLY", "MONTHLY", "QUARTERLY", "YEARLY"
        ));
        cycleCombo.setValue("MONTHLY");

        Spinner<Integer> dueDaySpinner = new Spinner<>(1, 31, 1);
        dueDaySpinner.setEditable(true);
        dueDaySpinner.setPrefWidth(80);

        DatePicker startPicker = new DatePicker(LocalDate.now());

        DatePicker endPicker = new DatePicker();
        endPicker.setPromptText("Optional");

        ComboBox<Category> categoryCombo = new ComboBox<>();
        categoryCombo.setItems(FXCollections.observableArrayList(expenseCategories));
        categoryCombo.setConverter(new StringConverter<>() {
            public String toString(Category c) { return c != null ? c.getCategoryName() : ""; }
            public Category fromString(String s) { return null; }
        });

        Spinner<Integer> reminderSpinner = new Spinner<>(0, 30, 3);
        reminderSpinner.setEditable(true);
        reminderSpinner.setPrefWidth(80);

        CheckBox activeCheck = new CheckBox("Active");
        activeCheck.setSelected(true);

        TextArea descArea = new TextArea();
        descArea.setPromptText("Optional notes");
        descArea.setPrefRowCount(2);

        // Populate fields if editing
        if (existing != null) {
            nameField.setText(existing.getName());
            vendorField.setText(existing.getVendor());
            amountField.setText(existing.getAmount().toString());
            cycleCombo.setValue(existing.getBillingCycleString());
            dueDaySpinner.getValueFactory().setValue(existing.getDueDay());
            startPicker.setValue(existing.getStartDate());
            endPicker.setValue(existing.getEndDate());
            reminderSpinner.getValueFactory().setValue(existing.getReminderDays());
            activeCheck.setSelected(existing.isActive());
            descArea.setText(existing.getDescription());

            for (Category c : expenseCategories) {
                if (c.getCategoryId().equals(existing.getCategoryId())) {
                    categoryCombo.setValue(c);
                    break;
                }
            }
        }

        // Layout
        int row = 0;
        grid.add(new Label("Bill Name:"), 0, row);
        grid.add(nameField, 1, row++);
        grid.add(new Label("Vendor:"), 0, row);
        grid.add(vendorField, 1, row++);
        grid.add(new Label("Amount:"), 0, row);
        grid.add(amountField, 1, row++);
        grid.add(new Label("Billing Cycle:"), 0, row);
        grid.add(cycleCombo, 1, row++);
        grid.add(new Label("Due Day (1-31):"), 0, row);
        grid.add(dueDaySpinner, 1, row++);
        grid.add(new Label("Start Date:"), 0, row);
        grid.add(startPicker, 1, row++);
        grid.add(new Label("End Date:"), 0, row);
        grid.add(endPicker, 1, row++);
        grid.add(new Label("Category:"), 0, row);
        grid.add(categoryCombo, 1, row++);
        grid.add(new Label("Remind (days before):"), 0, row);
        grid.add(reminderSpinner, 1, row++);
        grid.add(activeCheck, 1, row++);
        grid.add(new Label("Notes:"), 0, row);
        grid.add(descArea, 1, row);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                try {
                    // Validation
                    if (nameField.getText().trim().isEmpty()) {
                        showAlert(Alert.AlertType.ERROR, "Validation Error", "Bill name is required.");
                        return null;
                    }

                    BigDecimal amount;
                    try {
                        amount = new BigDecimal(amountField.getText().replace(",", ""));
                        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                            showAlert(Alert.AlertType.ERROR, "Validation Error", "Amount must be greater than 0.");
                            return null;
                        }
                    } catch (Exception e) {
                        showAlert(Alert.AlertType.ERROR, "Validation Error", "Invalid amount.");
                        return null;
                    }

                    Bill bill = existing != null ? existing : new Bill();
                    bill.setUserId(currentUserId);
                    bill.setName(nameField.getText().trim());
                    bill.setVendor(vendorField.getText().trim());
                    bill.setAmount(amount);
                    bill.setBillingCycleFromString(cycleCombo.getValue());
                    bill.setDueDay(dueDaySpinner.getValue());
                    bill.setStartDate(startPicker.getValue());
                    bill.setEndDate(endPicker.getValue());
                    bill.setReminderDays(reminderSpinner.getValue());
                    bill.setActive(activeCheck.isSelected());
                    bill.setDescription(descArea.getText().trim());

                    if (categoryCombo.getValue() != null) {
                        bill.setCategoryId(categoryCombo.getValue().getCategoryId());
                        bill.setCategoryName(categoryCombo.getValue().getCategoryName());
                    }

                    // Calculate next payment date if not set
                    if (bill.getNextPaymentDate() == null || existing == null) {
                        bill.setNextPaymentDate(bill.calculateNextPaymentDate());
                    }

                    return bill;
                } catch (Exception ex) {
                    logger.error("Error saving bill", ex);
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to save bill");
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(bill -> {
            boolean success = (existing == null)
                    ? billService.createBill(bill)
                    : billService.updateBill(bill);

            if (success) {
                loadBillData();
                showAlert(Alert.AlertType.INFORMATION, "Success",
                        "Bill " + (existing == null ? "created" : "updated") + " successfully.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to save bill.");
            }
        });
    }

    /**
     * Set user ID externally
     */
    public void setCurrentUserId(UUID userId) {
        this.currentUserId = userId;
        loadBillData();
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