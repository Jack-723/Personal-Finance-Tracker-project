package com.financetracker;

import com.financetracker.util.ConnectionTester;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main JavaFX Application Class
 */
public class MainApp extends Application {
    private static final Logger logger = LoggerFactory.getLogger(MainApp.class);
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) {
        try {
            primaryStage = stage;
            primaryStage.setTitle("Finance Tracker - Personal Budget Manager");

            // Test Supabase connection on startup
            logger.info("Testing Supabase connection...");
            ConnectionTester.validateConfiguration();
            ConnectionTester.testConnection();

            // Load the login screen
            showLoginScreen();

            primaryStage.show();
            logger.info("Application started successfully");

        } catch (Exception e) {
            logger.error("Error starting application", e);
            e.printStackTrace();
        }
    }

    /**
     * Show the login screen
     */
    public static void showLoginScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/fxml/LoginView.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 400, 500);
            scene.getStylesheets().add(MainApp.class.getResource("/css/style.css").toExternalForm());

            primaryStage.setScene(scene);
            primaryStage.setResizable(false);

        } catch (Exception e) {
            logger.error("Error loading login screen", e);
            e.printStackTrace();
        }
    }

    /**
     * Show the main dashboard
     */
    public static void showDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/fxml/DashboardView.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(MainApp.class.getResource("/css/style.css").toExternalForm());

            primaryStage.setScene(scene);
            primaryStage.setResizable(true);
            primaryStage.centerOnScreen();

        } catch (Exception e) {
            logger.error("Error loading dashboard", e);
            e.printStackTrace();
        }
    }

    /**
     * Get the primary stage
     */
    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}