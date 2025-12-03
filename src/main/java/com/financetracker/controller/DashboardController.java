package com.financetracker.controller;

import com.financetracker.model.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Controller for the main Dashboard view
 */
public class DashboardController {
    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    // FXML Components
    @FXML private Label welcomeLabel;
    @FXML private Label dateLabel;
    @FXML private StackPane contentArea;
    @FXML private VBox navMenu;

    // Navigation buttons
    @FXML private Button dashboardBtn;
    @FXML private Button incomeBtn;
    @FXML private Button expensesBtn;
    @FXML private Button budgetsBtn;
    @FXML private Button billsBtn;
    @FXML private Button accountsBtn;
    @FXML private Button analyticsBtn;
    @FXML private Button settingsBtn;

    private User currentUser;
    private Button activeButton;

    @FXML
    public void initialize() {
        logger.info("Initializing DashboardController");

        // Set current date
        if (dateLabel != null) {
            dateLabel.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));
        }

        // Get current user from LoginController
        currentUser = LoginController.getCurrentUser();
        if (currentUser != null && welcomeLabel != null) {
            String name = currentUser.getFullName();
            if (name == null || name.isEmpty()) {
                name = currentUser.getEmail().split("@")[0];
            }
            welcomeLabel.setText("Welcome, " + name + "!");
        }

        // Load dashboard home by default
        loadDashboardHome();
        setActiveButton(dashboardBtn);

        logger.info("DashboardController initialized");
    }

    /**
     * Set the currently active navigation button
     */
    private void setActiveButton(Button button) {
        // Remove active style from previous button
        if (activeButton != null) {
            activeButton.getStyleClass().remove("nav-button-active");
            if (!activeButton.getStyleClass().contains("nav-button")) {
                activeButton.getStyleClass().add("nav-button");
            }
        }

        // Add active style to new button
        if (button != null) {
            button.getStyleClass().remove("nav-button");
            if (!button.getStyleClass().contains("nav-button-active")) {
                button.getStyleClass().add("nav-button-active");
            }
            activeButton = button;
        }
    }

    /**
     * Load a view into the content area
     */
    private void loadView(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/" + fxmlFile));
            Node view = loader.load();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
            logger.info("Loaded view: {}", fxmlFile);
        } catch (IOException e) {
            logger.error("Error loading view: {}", fxmlFile, e);
            showErrorInContent("Failed to load " + fxmlFile + ": " + e.getMessage());
        }
    }

    /**
     * Show error message in content area
     */
    private void showErrorInContent(String message) {
        Label errorLabel = new Label(message);
        errorLabel.setStyle("-fx-text-fill: #D9534F; -fx-font-size: 14px;");
        contentArea.getChildren().clear();
        contentArea.getChildren().add(errorLabel);
    }

    /**
     * Load dashboard home view
     */
    private void loadDashboardHome() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/DashboardHome.fxml"));
            Node view = loader.load();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
        } catch (IOException e) {
            logger.warn("DashboardHome.fxml not found, showing placeholder");
            showPlaceholder("Dashboard", "Overview of your finances will appear here.");
        }
    }

    /**
     * Show placeholder content for views not yet implemented
     */
    private void showPlaceholder(String title, String message) {
        VBox placeholder = new VBox(10);
        placeholder.setStyle("-fx-alignment: center; -fx-padding: 50;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #333;");

        Label messageLabel = new Label(message);
        messageLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");

        placeholder.getChildren().addAll(titleLabel, messageLabel);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(placeholder);
    }

    // Navigation handlers
    @FXML
    private void handleDashboard() {
        logger.info("Dashboard clicked");
        loadDashboardHome();
        setActiveButton(dashboardBtn);
    }

    @FXML
    private void handleIncome() {
        logger.info("Income clicked");
        loadView("IncomeView.fxml");
        setActiveButton(incomeBtn);
    }

    @FXML
    private void handleExpenses() {
        logger.info("Expenses clicked");
        try {
            loadView("ExpenseView.fxml");
        } catch (Exception e) {
            showPlaceholder("Expenses", "Track your spending and categorize expenses.");
        }
        setActiveButton(expensesBtn);
    }

    @FXML
    private void handleBudgets() {
        logger.info("Budgets clicked");
        try {
            loadView("BudgetView.fxml");
        } catch (Exception e) {
            showPlaceholder("Budgets", "Set spending limits and monitor your progress.");
        }
        setActiveButton(budgetsBtn);
    }

    @FXML
    private void handleBills() {
        logger.info("Bills clicked");
        try {
            loadView("BillView.fxml");
        } catch (Exception e) {
            showPlaceholder("Bills & Subscriptions", "Manage recurring bills and subscriptions.");
        }
        setActiveButton(billsBtn);
    }

    @FXML
    private void handleAccounts() {
        logger.info("Accounts clicked");
        try {
            loadView("AccountView.fxml");
        } catch (Exception e) {
            showPlaceholder("Accounts", "Manage your bank accounts and balances.");
        }
        setActiveButton(accountsBtn);
    }

    @FXML
    private void handleAnalytics() {
        logger.info("Analytics clicked");
        try {
            loadView("AnalyticsView.fxml");
        } catch (Exception e) {
            showPlaceholder("Analytics", "View charts and reports of your financial data.");
        }
        setActiveButton(analyticsBtn);
    }

    @FXML
    private void handleSettings() {
        logger.info("Settings clicked");
        try {
            loadView("SettingsView.fxml");
        } catch (Exception e) {
            showPlaceholder("Settings", "Customize your preferences and account settings.");
        }
        setActiveButton(settingsBtn);
    }

    @FXML
    private void handleLogout() {
        logger.info("Logout clicked");

        // Clear current user
        LoginController.clearCurrentUser();

        // Return to  screen
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LoginView.fxml"));
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.getScene().setRoot(loader.load());
            stage.setTitle("Finance Tracker - Login");
            logger.info("Logged out successfully");
        } catch (IOException e) {
            logger.error("Error returning to login", e);
        }
    }

    /**
     * Get the current user
     */
    public User getCurrentUser() {
        return currentUser;
    }
}