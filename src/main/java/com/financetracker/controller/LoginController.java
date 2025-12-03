package com.financetracker.controller;

import com.financetracker.MainApp;
import com.financetracker.model.User;
import com.financetracker.service.UserService;
import com.financetracker.util.SupabaseClient;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Controller for Login and Registration views
 * Handles Supabase Auth authentication and local database user management
 */
public class LoginController {
    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    // Static reference to current user (shared across controllers)
    private static User currentUser;

    // FXML Components - Login
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;
    @FXML private Label signupLabel;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Label connectionStatusLabel;

    // FXML Components - Registration
    @FXML private VBox loginForm;
    @FXML private VBox registerForm;
    @FXML private TextField regEmailField;
    @FXML private TextField regNameField;
    @FXML private PasswordField regPasswordField;
    @FXML private PasswordField regConfirmPasswordField;

    // Services
    private UserService userService;
    private SupabaseClient supabaseClient;

    @FXML
    public void initialize() {
        logger.info("Initializing LoginController");

        userService = new UserService();
        supabaseClient = SupabaseClient.getInstance();

        // Hide error label and loading indicator initially
        if (errorLabel != null) {
            errorLabel.setVisible(false);
        }
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(false);
        }

        // Add enter key listener for password field
        if (passwordField != null) {
            passwordField.setOnAction(event -> handleLogin());
        }

        // Test Supabase connection on startup
        testSupabaseConnection();

        logger.info("LoginController initialized");
    }

    /**
     * Test Supabase connection and display status
     */
    private void testSupabaseConnection() {
        if (connectionStatusLabel != null) {
            connectionStatusLabel.setText("● Testing connection...");
            connectionStatusLabel.setStyle("-fx-text-fill: #FFA500;"); // Orange
        }

        Task<Boolean> connectionTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                Connection conn = supabaseClient.getConnection();
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                    return true;
                }
                return false;
            }
        };

        connectionTask.setOnSucceeded(event -> {
            boolean connected = connectionTask.getValue();
            Platform.runLater(() -> {
                if (connectionStatusLabel != null) {
                    if (connected) {
                        connectionStatusLabel.setText("● Connected to Supabase");
                        connectionStatusLabel.setStyle("-fx-text-fill: #5CB85C;"); // Green
                        logger.info("✓ Supabase connection successful");
                    } else {
                        connectionStatusLabel.setText("● Connection failed");
                        connectionStatusLabel.setStyle("-fx-text-fill: #D9534F;"); // Red
                    }
                }
            });
        });

        connectionTask.setOnFailed(event -> {
            Throwable ex = connectionTask.getException();
            logger.error("✗ Supabase connection failed", ex);
            Platform.runLater(() -> {
                if (connectionStatusLabel != null) {
                    connectionStatusLabel.setText("● Connection failed");
                    connectionStatusLabel.setStyle("-fx-text-fill: #D9534F;"); // Red
                }
            });
        });

        new Thread(connectionTask).start();
    }

    /**
     * Handle login button click
     *
     * Supabase Auth Flow:
     * 1. Authenticate with Supabase Auth API (email/password)
     * 2. On success, Supabase returns JWT token and user info
     * 3. Fetch additional user data from our 'users' table
     * 4. Store user session for the application
     */
    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        // Validate input
        if (email.isEmpty() || password.isEmpty()) {
            showError("Please enter email and password");
            return;
        }

        if (!isValidEmail(email)) {
            showError("Please enter a valid email address");
            return;
        }

        // Show loading state
        setLoading(true);
        hideError();

        // Perform authentication in background thread
        Task<User> loginTask = new Task<>() {
            @Override
            protected User call() throws Exception {
                logger.info("Attempting login for: {}", email);

                // Step 1: Authenticate with Supabase Auth
                logger.info("Step 1: Authenticating with Supabase Auth...");
                try {
                    boolean authSuccess = supabaseClient.signIn(email, password);
                    if (!authSuccess) {
                        throw new IOException("Invalid login credentials");
                    }
                    logger.info("✓ Supabase Auth successful, token received");
                } catch (IOException e) {
                    logger.error("✗ Supabase Auth failed: {}", e.getMessage());
                    throw new IOException("Invalid email or password");
                }

                // Step 2: Get user from our application's users table
                logger.info("Step 2: Fetching user profile from database...");
                User user = userService.getUserByEmail(email);

                if (user == null) {
                    // User exists in Supabase Auth but not in our users table
                    logger.warn("User authenticated but not found in users table");

                    // Auto-create user profile in our table
                    logger.info("Step 3: Creating user profile in database...");
                    user = new User();
                    user.setEmail(email);
                    user.setFullName(extractNameFromEmail(email));
                    user.setCurrencyPreference("USD");
                    user.setActive(true);

                    boolean created = userService.createUserWithoutPassword(user);

                    if (created) {
                        user = userService.getUserByEmail(email);
                        logger.info("✓ User profile created in database");
                    } else {
                        throw new SQLException("Failed to create user profile");
                    }
                }

                logger.info("✓ Login successful for: {}", user.getEmail());
                return user;
            }
        };

        loginTask.setOnSucceeded(event -> {
            User user = loginTask.getValue();
            Platform.runLater(() -> {
                setLoading(false);
                if (user != null) {
                    currentUser = user;
                    logger.info("Navigating to dashboard for user: {}", user.getDisplayName());
                    MainApp.showDashboard();
                } else {
                    showError("Login failed. Please try again.");
                }
            });
        });

        loginTask.setOnFailed(event -> {
            Throwable ex = loginTask.getException();
            logger.error("Login failed", ex);
            Platform.runLater(() -> {
                setLoading(false);

                String errorMessage = "Login failed";
                if (ex != null) {
                    String msg = ex.getMessage();
                    if (msg.contains("Invalid login credentials") || msg.contains("Invalid email or password")) {
                        errorMessage = "Invalid email or password";
                    } else if (msg.contains("connection") || msg.contains("Connection")) {
                        errorMessage = "Cannot connect to server. Check your internet connection.";
                    } else if (msg.contains("Email not confirmed")) {
                        errorMessage = "Please confirm your email address first.";
                    } else {
                        errorMessage = msg;
                    }
                }
                showError(errorMessage);
            });
        });

        new Thread(loginTask).start();
    }

    /**
     * Handle signup/register link click
     */
    @FXML
    private void handleSignup(MouseEvent event) {
        handleShowRegister();
    }

    /**
     * Show registration form
     */
    @FXML
    private void handleShowRegister() {
        logger.info("Showing registration form");

        if (loginForm != null && registerForm != null) {
            loginForm.setVisible(false);
            loginForm.setManaged(false);
            registerForm.setVisible(true);
            registerForm.setManaged(true);
            hideError();
        }
    }

    /**
     * Show login form (hide registration)
     */
    @FXML
    private void handleShowLogin() {
        logger.info("Showing login form");

        if (loginForm != null && registerForm != null) {
            registerForm.setVisible(false);
            registerForm.setManaged(false);
            loginForm.setVisible(true);
            loginForm.setManaged(true);
            hideError();
        }
    }

    /**
     * Handle registration form submission
     *
     * Supabase Auth Registration Flow:
     * 1. Validate all input fields
     * 2. Create user in Supabase Auth (handles password hashing, email verification)
     * 3. Create corresponding user profile in our 'users' table
     * 4. User may need to confirm email before logging in (depending on Supabase settings)
     */
    @FXML
    private void handleRegister() {
        logger.info("handleRegister method called");

        // Get values from registration form
        String email = regEmailField.getText().trim();
        String name = regNameField.getText().trim();
        String password = regPasswordField.getText();
        String confirmPassword = regConfirmPasswordField.getText();

        // Validate all fields are filled
        if (email.isEmpty() || name.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showError("Please fill in all fields");
            return;
        }

        // Validate email format
        if (!isValidEmail(email)) {
            showError("Please enter a valid email address");
            return;
        }

        // Validate password length
        if (password.length() < 8) {
            showError("Password must be at least 8 characters long");
            return;
        }

        // Validate passwords match
        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match");
            return;
        }

        // Show loading state
        setLoading(true);
        hideError();

        // Perform registration in background thread
        Task<Boolean> registerTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                logger.info("Attempting registration for: {}", email);

                // Step 1: Create auth user in Supabase
                logger.info("Step 1: Creating Supabase Auth user...");
                boolean authCreated = supabaseClient.signUp(email, password);

                if (!authCreated) {
                    throw new IOException("Failed to create auth user");
                }
                logger.info("✓ Supabase Auth user created");

                // Step 2: Create user profile in local database
                logger.info("Step 2: Creating user profile in database...");
                User newUser = new User();
                newUser.setEmail(email);
                newUser.setFullName(name);
                newUser.setCurrencyPreference("USD");
                newUser.setActive(true);

                boolean profileCreated = userService.createUserWithoutPassword(newUser);

                if (!profileCreated) {
                    logger.warn("User profile creation returned false, but may already exist");
                } else {
                    logger.info("✓ User profile created in database");
                }

                logger.info("✓ Registration successful for: {}", email);
                return true;
            }
        };

        registerTask.setOnSucceeded(event -> {
            Boolean success = registerTask.getValue();
            Platform.runLater(() -> {
                setLoading(false);
                if (success) {
                    showSuccess("Account created successfully! Please check your email to verify your account.");

                    // Pre-fill login email for convenience
                    if (emailField != null) {
                        emailField.setText(email);
                    }

                    // Clear registration form
                    regEmailField.clear();
                    regNameField.clear();
                    regPasswordField.clear();
                    regConfirmPasswordField.clear();

                    // Show login form after 2 seconds
                    PauseTransition pause = new PauseTransition(Duration.seconds(2));
                    pause.setOnFinished(e -> handleShowLogin());
                    pause.play();
                } else {
                    showError("Registration failed. Please try again.");
                }
            });
        });

        registerTask.setOnFailed(event -> {
            Throwable ex = registerTask.getException();
            logger.error("Registration failed", ex);
            Platform.runLater(() -> {
                setLoading(false);
                String errorMsg = ex.getMessage();

                // Parse common Supabase error messages
                if (errorMsg.contains("User already registered")) {
                    showError("This email is already registered. Try signing in instead.");
                } else if (errorMsg.contains("Invalid email")) {
                    showError("Please enter a valid email address");
                } else if (errorMsg.contains("Password should be at least")) {
                    showError("Password must be at least 8 characters long");
                } else {
                    showError("Registration failed: " + errorMsg);
                }
            });
        });

        // Start the registration task
        new Thread(registerTask).start();
    }

    /**
     * Handle forgot password link click
     */
    @FXML
    private void handleForgotPassword(MouseEvent event) {
        handleForgotPassword();
    }

    /**
     * Handle forgot password - sends reset email via Supabase
     */
    @FXML
    private void handleForgotPassword() {
        String email = emailField.getText().trim();

        if (email.isEmpty()) {
            showError("Please enter your email address first");
            return;
        }

        if (!isValidEmail(email)) {
            showError("Please enter a valid email address");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Reset Password");
        confirm.setHeaderText("Password Reset");
        confirm.setContentText("Send password reset email to:\n" + email + "?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                setLoading(true);

                Task<Boolean> resetTask = new Task<>() {
                    @Override
                    protected Boolean call() {
                        return supabaseClient.resetPassword(email);
                    }
                };

                resetTask.setOnSucceeded(e -> {
                    setLoading(false);
                    if (resetTask.getValue()) {
                        showSuccess("Password reset email sent! Check your inbox.");
                    } else {
                        showError("Failed to send reset email. Please try again.");
                    }
                });

                resetTask.setOnFailed(e -> {
                    setLoading(false);
                    showError("Failed to send reset email.");
                });

                new Thread(resetTask).start();
            }
        });
    }

    /**
     * Extract a display name from email address
     */
    private String extractNameFromEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "User";
        }
        String namePart = email.split("@")[0];
        // Capitalize first letter and replace dots/underscores with spaces
        namePart = namePart.replace(".", " ").replace("_", " ");
        if (!namePart.isEmpty()) {
            namePart = namePart.substring(0, 1).toUpperCase() + namePart.substring(1);
        }
        return namePart;
    }

    /**
     * Show error message
     */
    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setStyle("-fx-text-fill: #D9534F;"); // Red
            errorLabel.setVisible(true);
        }
    }

    /**
     * Show success message
     */
    private void showSuccess(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setStyle("-fx-text-fill: #5CB85C;"); // Green
            errorLabel.setVisible(true);
        }
    }

    /**
     * Hide error/success message
     */
    private void hideError() {
        if (errorLabel != null) {
            errorLabel.setVisible(false);
        }
    }

    /**
     * Set loading state
     */
    private void setLoading(boolean loading) {
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(loading);
        }
        if (loginButton != null) {
            loginButton.setDisable(loading);
        }
    }

    /**
     * Validate email format
     */
    private boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }

    // ============================================
    // Static methods for session management
    // ============================================

    /**
     * Get current logged-in user (static access for other controllers)
     */
    public static User getCurrentUser() {
        return currentUser;
    }

    /**
     * Set current user (for testing or programmatic login)
     */
    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    /**
     * Clear current user (for logout)
     */
    public static void clearCurrentUser() {
        currentUser = null;
        // Also sign out from Supabase
        SupabaseClient.getInstance().signOut();
    }

    /**
     * Check if a user is currently logged in
     */
    public static boolean isLoggedIn() {
        return currentUser != null;
    }
}