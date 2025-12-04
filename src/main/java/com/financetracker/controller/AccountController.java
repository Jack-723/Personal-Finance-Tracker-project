package com.financetracker.controller;

import com.financetracker.model.Account;
import com.financetracker.service.AccountService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Controller for Account Management View
 */
public class AccountController {

    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

    // FXML Components
    @FXML private Label netWorthLabel;
    @FXML private Label totalAssetsLabel;
    @FXML private Label totalLiabilitiesLabel;
    @FXML private TableView<Account> accountTable;
    @FXML private TableColumn<Account, String> nameColumn;
    @FXML private TableColumn<Account, String> typeColumn;
    @FXML private TableColumn<Account, String> institutionColumn;
    @FXML private TableColumn<Account, String> accountNumberColumn;
    @FXML private TableColumn<Account, String> balanceColumn;
    @FXML private TableColumn<Account, String> currencyColumn;
    @FXML private TableColumn<Account, String> statusColumn;
    @FXML private ComboBox<String> filterComboBox;
    @FXML private TextField searchField;

    // Services
    private AccountService accountService;

    // Data
    private ObservableList<Account> accountList;
    private List<Account> allAccounts; // Store all accounts for filtering
    private UUID currentUserId;

    @FXML
    public void initialize() {
        logger.info("Initializing AccountController");

        accountService = new AccountService();
        accountList = FXCollections.observableArrayList();

        if (LoginController.getCurrentUser() != null) {
            currentUserId = LoginController.getCurrentUser().getUserId();
        }

        setupTable();
        setupFilters();
        setupSearch();
        loadAccountData();
        updateSummary();

        logger.info("AccountController initialized");
    }

    /**
     * Setup table columns
     */
    private void setupTable() {
        // Name column
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("accountName"));

        // Type column
        typeColumn.setCellValueFactory(cellData -> {
            return new SimpleStringProperty(cellData.getValue().getAccountTypeDisplayName());
        });

        // Institution column
        institutionColumn.setCellValueFactory(cellData -> {
            String institution = cellData.getValue().getInstitutionName();
            return new SimpleStringProperty(institution != null ? institution : "-");
        });

        // Account number column (masked)
        accountNumberColumn.setCellValueFactory(cellData -> {
            String masked = cellData.getValue().getMaskedAccountNumber();
            return new SimpleStringProperty(masked != null ? masked : "-");
        });

        // Balance column with color coding
        balanceColumn.setCellValueFactory(cellData -> {
            BigDecimal balance = cellData.getValue().getBalance();
            boolean isCredit = cellData.getValue().getAccountType() == Account.AccountType.CREDIT_CARD;
            String prefix = isCredit ? "-" : "";
            return new SimpleStringProperty(prefix + String.format("$%,.2f", balance.abs()));
        });
        balanceColumn.setStyle("-fx-alignment: CENTER-RIGHT;");
        balanceColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-alignment: CENTER-RIGHT;");
                } else {
                    setText(item);
                    if (item.startsWith("-")) {
                        setStyle("-fx-alignment: CENTER-RIGHT; -fx-text-fill: #D9534F;");
                    } else {
                        setStyle("-fx-alignment: CENTER-RIGHT; -fx-text-fill: #5CB85C;");
                    }
                }
            }
        });

        // Currency column
        currencyColumn.setCellValueFactory(new PropertyValueFactory<>("currency"));

        // Status column
        statusColumn.setCellValueFactory(cellData -> {
            return new SimpleStringProperty(cellData.getValue().isActive() ? "Active" : "Inactive");
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
                    if ("Active".equals(item)) {
                        setStyle("-fx-text-fill: #5CB85C;");
                    } else {
                        setStyle("-fx-text-fill: #999999;");
                    }
                }
            }
        });

        // Attach list
        accountTable.setItems(accountList);

        // Double-click to edit
        accountTable.setRowFactory(tv -> {
            TableRow<Account> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    handleEditAccount();
                }
            });
            return row;
        });

        // Context menu
        ContextMenu ctx = new ContextMenu();
        MenuItem edit = new MenuItem("Edit");
        edit.setOnAction(e -> handleEditAccount());
        MenuItem updateBalance = new MenuItem("Update Balance");
        updateBalance.setOnAction(e -> handleUpdateBalance());
        MenuItem deactivate = new MenuItem("Deactivate");
        deactivate.setOnAction(e -> handleDeactivateAccount());
        MenuItem delete = new MenuItem("Delete");
        delete.setOnAction(e -> handleDeleteAccount());
        ctx.getItems().addAll(edit, updateBalance, new SeparatorMenuItem(), deactivate, delete);
        accountTable.setContextMenu(ctx);
    }

    /**
     * Setup filter section
     */
    private void setupFilters() {
        if (filterComboBox != null) {
            filterComboBox.setItems(FXCollections.observableArrayList(
                    "Active Accounts", "All Accounts", "Checking", "Savings", "Credit Cards", "Cash", "Investment"
            ));
            filterComboBox.setValue("Active Accounts");
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
     * Load account data from database
     */
    private void loadAccountData() {
        if (currentUserId == null) return;

        try {
            applyFilter();
        } catch (Exception e) {
            logger.error("Error loading accounts", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load accounts: " + e.getMessage());
        }
    }

    /**
     * Apply filter
     */
    private void applyFilter() {
        if (currentUserId == null || filterComboBox == null) return;

        String filter = filterComboBox.getValue();

        switch (filter) {
            case "All Accounts":
                allAccounts = accountService.getAccountsByUser(currentUserId);
                break;
            case "Checking":
                allAccounts = accountService.getAccountsByType(currentUserId, Account.AccountType.CHECKING);
                break;
            case "Savings":
                allAccounts = accountService.getAccountsByType(currentUserId, Account.AccountType.SAVINGS);
                break;
            case "Credit Cards":
                allAccounts = accountService.getAccountsByType(currentUserId, Account.AccountType.CREDIT_CARD);
                break;
            case "Cash":
                allAccounts = accountService.getAccountsByType(currentUserId, Account.AccountType.CASH);
                break;
            case "Investment":
                allAccounts = accountService.getAccountsByType(currentUserId, Account.AccountType.INVESTMENT);
                break;
            case "Active Accounts":
            default:
                allAccounts = accountService.getActiveAccounts(currentUserId);
                break;
        }

        // Apply search filter if there's text in the search field
        if (searchField != null && !searchField.getText().isEmpty()) {
            filterBySearch(searchField.getText());
        } else {
            accountList.setAll(allAccounts);
        }

        updateSummary();
    }

    /**
     * Filter accounts by search text (real-time)
     */
    private void filterBySearch(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            // No search text, show all accounts from current filter
            if (allAccounts != null) {
                accountList.setAll(allAccounts);
            }
            return;
        }

        String searchLower = searchText.toLowerCase().trim();

        // Filter the allAccounts list based on search text
        List<Account> filteredList;
        if (allAccounts != null) {
            filteredList = allAccounts.stream()
                    .filter(account -> {
                        // Search by account name
                        if (account.getAccountName() != null &&
                                account.getAccountName().toLowerCase().contains(searchLower)) {
                            return true;
                        }
                        // Search by institution name
                        if (account.getInstitutionName() != null &&
                                account.getInstitutionName().toLowerCase().contains(searchLower)) {
                            return true;
                        }
                        // Search by account type
                        if (account.getAccountTypeDisplayName() != null &&
                                account.getAccountTypeDisplayName().toLowerCase().contains(searchLower)) {
                            return true;
                        }
                        // Search by account type enum
                        if (account.getAccountType() != null &&
                                account.getAccountType().name().toLowerCase().contains(searchLower)) {
                            return true;
                        }
                        // Search by currency
                        if (account.getCurrency() != null &&
                                account.getCurrency().toLowerCase().contains(searchLower)) {
                            return true;
                        }
                        // Search by account number (masked)
                        if (account.getMaskedAccountNumber() != null &&
                                account.getMaskedAccountNumber().toLowerCase().contains(searchLower)) {
                            return true;
                        }
                        // Search by status
                        String status = account.isActive() ? "active" : "inactive";
                        if (status.contains(searchLower)) {
                            return true;
                        }
                        return false;
                    })
                    .toList();
        } else {
            filteredList = List.of();
        }

        accountList.setAll(filteredList);
    }

    /**
     * Update the summary cards
     */
    private void updateSummary() {
        if (currentUserId == null) return;

        BigDecimal netWorth = accountService.getNetWorth(currentUserId);
        BigDecimal assets = accountService.getTotalAssets(currentUserId);
        BigDecimal liabilities = accountService.getTotalLiabilities(currentUserId);

        netWorthLabel.setText(String.format("$%,.2f", netWorth));
        if (netWorth.compareTo(BigDecimal.ZERO) >= 0) {
            netWorthLabel.setStyle("-fx-text-fill: #5CB85C;");
        } else {
            netWorthLabel.setStyle("-fx-text-fill: #D9534F;");
        }

        totalAssetsLabel.setText(String.format("$%,.2f", assets));
        totalLiabilitiesLabel.setText(String.format("$%,.2f", liabilities));
    }

    /**
     * Add Account
     */
    @FXML
    private void handleAddAccount() {
        showAccountDialog(null);
    }

    /**
     * Edit Account
     */
    @FXML
    private void handleEditAccount() {
        Account sel = accountTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select an account to edit.");
            return;
        }
        showAccountDialog(sel);
    }

    /**
     * Update Balance
     */
    @FXML
    private void handleUpdateBalance() {
        Account sel = accountTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select an account to update.");
            return;
        }
        showUpdateBalanceDialog(sel);
    }

    /**
     * Deactivate Account
     */
    @FXML
    private void handleDeactivateAccount() {
        Account sel = accountTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select an account to deactivate.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deactivation");
        confirm.setHeaderText("Deactivate Account");
        confirm.setContentText("Are you sure you want to deactivate '" + sel.getAccountName() + "'?\n" +
                "The account will be hidden but can be reactivated later.");

        if (confirm.showAndWait().filter(btn -> btn == ButtonType.OK).isPresent()) {
            if (accountService.deactivateAccount(sel.getAccountId())) {
                loadAccountData();
                showAlert(Alert.AlertType.INFORMATION, "Success", "Account deactivated successfully.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to deactivate account.");
            }
        }
    }

    /**
     * Delete Account
     */
    @FXML
    private void handleDeleteAccount() {
        Account sel = accountTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select an account to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Account");
        confirm.setContentText("Are you sure you want to permanently delete '" + sel.getAccountName() + "'?\n\n" +
                "WARNING: This action cannot be undone and may affect linked transactions.");

        if (confirm.showAndWait().filter(btn -> btn == ButtonType.OK).isPresent()) {
            if (accountService.deleteAccount(sel.getAccountId())) {
                accountList.remove(sel);
                if (allAccounts != null) {
                    allAccounts = allAccounts.stream()
                            .filter(a -> !a.getAccountId().equals(sel.getAccountId()))
                            .toList();
                }
                updateSummary();
                showAlert(Alert.AlertType.INFORMATION, "Success", "Account deleted successfully.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete account.");
            }
        }
    }

    @FXML
    private void handleRefresh() {
        if (searchField != null) {
            searchField.clear();
        }
        loadAccountData();
    }

    /**
     * Add/Edit dialog for accounts
     */
    private void showAccountDialog(Account existing) {
        Dialog<Account> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Account" : "Edit Account");
        dialog.setHeaderText(existing == null ? "Create a new account" : "Update account details");

        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // Form fields
        TextField nameField = new TextField();
        nameField.setPromptText("e.g., Chase Checking, Savings Account");

        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.setItems(FXCollections.observableArrayList(
                "CHECKING", "SAVINGS", "CREDIT_CARD", "CASH", "INVESTMENT", "OTHER"
        ));
        typeCombo.setValue("CHECKING");

        TextField institutionField = new TextField();
        institutionField.setPromptText("e.g., Chase, Bank of America");

        TextField accountNumberField = new TextField();
        accountNumberField.setPromptText("Optional - Last 4 digits recommended");

        TextField balanceField = new TextField();
        balanceField.setPromptText("0.00");

        ComboBox<String> currencyCombo = new ComboBox<>();
        currencyCombo.setItems(FXCollections.observableArrayList(
                "USD", "EUR", "GBP", "CAD", "AUD", "JPY", "CHF", "NOK", "SEK", "DKK"
        ));
        currencyCombo.setValue("USD");

        CheckBox activeCheck = new CheckBox("Active");
        activeCheck.setSelected(true);

        // Populate fields if editing
        if (existing != null) {
            nameField.setText(existing.getAccountName());
            typeCombo.setValue(existing.getAccountTypeString());
            institutionField.setText(existing.getInstitutionName());
            accountNumberField.setText(existing.getAccountNumber());
            balanceField.setText(existing.getBalance().toString());
            currencyCombo.setValue(existing.getCurrency());
            activeCheck.setSelected(existing.isActive());
        }

        // Layout
        int row = 0;
        grid.add(new Label("Account Name:"), 0, row);
        grid.add(nameField, 1, row++);
        grid.add(new Label("Account Type:"), 0, row);
        grid.add(typeCombo, 1, row++);
        grid.add(new Label("Institution:"), 0, row);
        grid.add(institutionField, 1, row++);
        grid.add(new Label("Account Number:"), 0, row);
        grid.add(accountNumberField, 1, row++);
        grid.add(new Label("Current Balance:"), 0, row);
        grid.add(balanceField, 1, row++);
        grid.add(new Label("Currency:"), 0, row);
        grid.add(currencyCombo, 1, row++);
        grid.add(activeCheck, 1, row);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                try {
                    // Validation
                    if (nameField.getText().trim().isEmpty()) {
                        showAlert(Alert.AlertType.ERROR, "Validation Error", "Account name is required.");
                        return null;
                    }

                    BigDecimal balance;
                    try {
                        balance = new BigDecimal(balanceField.getText().replace(",", ""));
                    } catch (Exception e) {
                        showAlert(Alert.AlertType.ERROR, "Validation Error", "Invalid balance amount.");
                        return null;
                    }

                    Account account = existing != null ? existing : new Account();
                    account.setUserId(currentUserId);
                    account.setAccountName(nameField.getText().trim());
                    account.setAccountTypeFromString(typeCombo.getValue());
                    account.setInstitutionName(institutionField.getText().trim());
                    account.setAccountNumber(accountNumberField.getText().trim());
                    account.setBalance(balance);
                    account.setCurrency(currencyCombo.getValue());
                    account.setActive(activeCheck.isSelected());

                    return account;
                } catch (Exception ex) {
                    logger.error("Error saving account", ex);
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to save account");
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(account -> {
            boolean success = (existing == null)
                    ? accountService.createAccount(account)
                    : accountService.updateAccount(account);

            if (success) {
                loadAccountData();
                showAlert(Alert.AlertType.INFORMATION, "Success",
                        "Account " + (existing == null ? "created" : "updated") + " successfully.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to save account.");
            }
        });
    }

    /**
     * Quick balance update dialog
     */
    private void showUpdateBalanceDialog(Account account) {
        Dialog<BigDecimal> dialog = new Dialog<>();
        dialog.setTitle("Update Balance");
        dialog.setHeaderText("Update balance for: " + account.getAccountName());

        ButtonType saveBtn = new ButtonType("Update", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        Label currentLabel = new Label(String.format("Current Balance: $%,.2f", account.getBalance()));
        TextField newBalanceField = new TextField();
        newBalanceField.setPromptText("Enter new balance");
        newBalanceField.setText(account.getBalance().toString());

        grid.add(currentLabel, 0, 0, 2, 1);
        grid.add(new Label("New Balance:"), 0, 1);
        grid.add(newBalanceField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                try {
                    return new BigDecimal(newBalanceField.getText().replace(",", ""));
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Validation Error", "Invalid balance amount.");
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(newBalance -> {
            if (accountService.updateBalance(account.getAccountId(), newBalance)) {
                loadAccountData();
                showAlert(Alert.AlertType.INFORMATION, "Success", "Balance updated successfully.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to update balance.");
            }
        });
    }

    /**
     * Set user ID externally
     */
    public void setCurrentUserId(UUID userId) {
        this.currentUserId = userId;
        loadAccountData();
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