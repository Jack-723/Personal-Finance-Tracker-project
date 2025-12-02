package com.financetracker.util;

import okhttp3.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

/**
 * Supabase Client Manager for handling authentication and database connections
 */
public class SupabaseClient {
    private static final Logger logger = LoggerFactory.getLogger(SupabaseClient.class);
    private static SupabaseClient instance;

    private final String supabaseUrl;
    private final String supabaseKey;
    private final String dbUrl;
    private final String dbUsername;
    private final String dbPassword;

    private OkHttpClient httpClient;
    private Gson gson;
    private String currentUserToken;
    private String currentUserId;
    private String currentUserEmail;

    private SupabaseClient() {
        ConfigManager config = ConfigManager.getInstance();
        this.supabaseUrl = config.getSupabaseUrl();
        this.supabaseKey = config.getSupabaseKey();
        this.dbUrl = config.getDbUrl();
        this.dbUsername = config.getDbUsername();
        this.dbPassword = config.getDbPassword();

        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        this.gson = new Gson();

        logger.info("SupabaseClient initialized");
    }

    public static SupabaseClient getInstance() {
        if (instance == null) {
            synchronized (SupabaseClient.class) {
                if (instance == null) {
                    instance = new SupabaseClient();
                }
            }
        }
        return instance;
    }

    /**
     * Sign up a new user (original method with full name)
     */
    public JsonObject signUp(String email, String password, String fullName) throws IOException {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("email", email);
        requestBody.addProperty("password", password);

        JsonObject metadata = new JsonObject();
        metadata.addProperty("full_name", fullName);
        requestBody.add("data", metadata);

        String url = supabaseUrl + "/auth/v1/signup";

        RequestBody body = RequestBody.create(
                requestBody.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", supabaseKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body().string();
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

            if (response.isSuccessful()) {
                if (jsonResponse.has("access_token")) {
                    currentUserToken = jsonResponse.get("access_token").getAsString();
                }
                if (jsonResponse.has("user")) {
                    JsonObject user = jsonResponse.getAsJsonObject("user");
                    if (user.has("id")) {
                        currentUserId = user.get("id").getAsString();
                    }
                    if (user.has("email")) {
                        currentUserEmail = user.get("email").getAsString();
                    }
                }
                logger.info("User signed up successfully: {}", email);
                return jsonResponse;
            } else {
                logger.error("Sign up failed: {}", responseBody);
                throw new IOException("Sign up failed: " + responseBody);
            }
        }
    }

    /**
     * Sign up a new user (simplified - returns boolean)
     */
    public boolean signUp(String email, String password) throws IOException {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("email", email);
        requestBody.addProperty("password", password);

        String url = supabaseUrl + "/auth/v1/signup";

        RequestBody body = RequestBody.create(
                requestBody.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", supabaseKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body().string();
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

            if (response.isSuccessful()) {
                if (jsonResponse.has("access_token")) {
                    currentUserToken = jsonResponse.get("access_token").getAsString();
                }
                if (jsonResponse.has("user")) {
                    JsonObject user = jsonResponse.getAsJsonObject("user");
                    if (user.has("id")) {
                        currentUserId = user.get("id").getAsString();
                    }
                    if (user.has("email")) {
                        currentUserEmail = user.get("email").getAsString();
                    }
                }
                logger.info("User signed up successfully: {}", email);
                return true;
            } else {
                logger.error("Sign up failed: {}", responseBody);
                if (jsonResponse.has("error_description")) {
                    throw new IOException(jsonResponse.get("error_description").getAsString());
                } else if (jsonResponse.has("msg")) {
                    throw new IOException(jsonResponse.get("msg").getAsString());
                } else if (jsonResponse.has("error")) {
                    throw new IOException(jsonResponse.get("error").getAsString());
                }
                throw new IOException("Sign up failed: " + responseBody);
            }
        }
    }

    /**
     * Sign in an existing user
     */
    public boolean signIn(String email, String password) throws IOException {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("email", email);
        requestBody.addProperty("password", password);

        String url = supabaseUrl + "/auth/v1/token?grant_type=password";

        RequestBody body = RequestBody.create(
                requestBody.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", supabaseKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body().string();
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

            if (response.isSuccessful()) {
                if (jsonResponse.has("access_token")) {
                    currentUserToken = jsonResponse.get("access_token").getAsString();
                }
                if (jsonResponse.has("user")) {
                    JsonObject user = jsonResponse.getAsJsonObject("user");
                    if (user.has("id")) {
                        currentUserId = user.get("id").getAsString();
                    }
                    if (user.has("email")) {
                        currentUserEmail = user.get("email").getAsString();
                    }
                }
                logger.info("User signed in successfully: {}", email);
                return true;
            } else {
                logger.error("Sign in failed: {}", responseBody);
                if (jsonResponse.has("error_description")) {
                    throw new IOException(jsonResponse.get("error_description").getAsString());
                } else if (jsonResponse.has("msg")) {
                    throw new IOException(jsonResponse.get("msg").getAsString());
                } else if (jsonResponse.has("error")) {
                    throw new IOException(jsonResponse.get("error").getAsString());
                }
                throw new IOException("Invalid login credentials");
            }
        }
    }

    /**
     * Sign out the current user
     */
    public void signOut() {
        currentUserToken = null;
        currentUserId = null;
        currentUserEmail = null;
        logger.info("User signed out");
    }

    /**
     * Request password reset email
     */
    public boolean resetPassword(String email) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("email", email);

        String url = supabaseUrl + "/auth/v1/recover";

        RequestBody body = RequestBody.create(
                requestBody.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", supabaseKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                logger.info("Password reset email sent to: {}", email);
                return true;
            } else {
                String responseBody = response.body().string();
                logger.error("Password reset failed: {}", responseBody);
                return false;
            }
        } catch (IOException e) {
            logger.error("Password reset request failed", e);
            return false;
        }
    }

    /**
     * Get the current user's access token
     */
    public String getCurrentUserToken() {
        return currentUserToken;
    }

    /**
     * Get the current user's ID
     */
    public String getCurrentUserId() {
        return currentUserId;
    }

    /**
     * Get the current user's email
     */
    public String getCurrentUserEmail() {
        return currentUserEmail;
    }

    /**
     * Check if a user is currently signed in
     */
    public boolean isUserSignedIn() {
        return currentUserToken != null && currentUserId != null;
    }

    /**
     * Check if authenticated
     */
    public boolean isAuthenticated() {
        return isUserSignedIn();
    }

    /**
     * Get a database connection
     */
    public Connection getConnection() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
            Connection conn = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
            logger.debug("Database connection established");
            return conn;
        } catch (ClassNotFoundException e) {
            logger.error("PostgreSQL JDBC Driver not found", e);
            throw new SQLException("PostgreSQL JDBC Driver not found", e);
        }
    }

    /**
     * Check if configuration is valid
     */
    public boolean isConfigured() {
        return supabaseUrl != null && !supabaseUrl.isEmpty() &&
                dbUrl != null && !dbUrl.isEmpty();
    }
}
