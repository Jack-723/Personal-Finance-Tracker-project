package com.financetracker.controller;

import com.financetracker.service.ExpenseService;
import com.financetracker.service.IncomeService;
import com.financetracker.service.BudgetService;
import com.financetracker.service.AccountService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;

/**
 * Controller for Analytics & Data Visualization View
 * Provides charts and summaries of financial data
 */
public class AnalyticsController {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsController.class);

    // FXML Components - Summary Labels
    @FXML private Label totalIncomeLabel;
    @FXML private Label totalExpensesLabel;
    @FXML private Label netSavingsLabel;
    @FXML private Label savingsRateLabel;

    // FXML Components - Charts
    @FXML private PieChart expenseByCategoryChart;
    @FXML private BarChart<String, Number> incomeExpenseBarChart;
    @FXML private CategoryAxis barChartXAxis;
    @FXML private NumberAxis barChartYAxis;
    @FXML private LineChart<String, Number> trendLineChart;
    @FXML private CategoryAxis lineChartXAxis;
    @FXML private NumberAxis lineChartYAxis;
    @FXML private PieChart incomeByCategoryChart;

    // FXML Components - Filters
    @FXML private ComboBox<String> periodComboBox;
    @FXML private ComboBox<Integer> yearComboBox;

    // Services
    private IncomeService incomeService;
    private ExpenseService expenseService;
    private BudgetService budgetService;
    private AccountService accountService;

    // Data
    private UUID currentUserId;
    private int selectedYear;
    private String selectedPeriod;

    @FXML
    public void initialize() {
        logger.info("Initializing AnalyticsController");

        incomeService = new IncomeService();
        expenseService = new ExpenseService();
        budgetService = new BudgetService();
        accountService = new AccountService();

        if (LoginController.getCurrentUser() != null) {
            currentUserId = LoginController.getCurrentUser().getUserId();
        }

        selectedYear = LocalDate.now().getYear();
        selectedPeriod = "This Year";

        setupFilters();
        setupCharts();
        loadAnalyticsData();

        logger.info("AnalyticsController initialized");
    }

    /**
     * Setup filter controls
     */
    private void setupFilters() {
        // Period filter
        if (periodComboBox != null) {
            periodComboBox.setItems(FXCollections.observableArrayList(
                    "This Month", "Last 3 Months", "Last 6 Months", "This Year", "Last Year", "All Time"
            ));
            periodComboBox.setValue("This Year");
            periodComboBox.setOnAction(e -> {
                selectedPeriod = periodComboBox.getValue();
                loadAnalyticsData();
            });
        }

        // Year filter
        if (yearComboBox != null) {
            int currentYear = LocalDate.now().getYear();
            ObservableList<Integer> years = FXCollections.observableArrayList();
            for (int i = currentYear; i >= currentYear - 5; i--) {
                years.add(i);
            }
            yearComboBox.setItems(years);
            yearComboBox.setValue(currentYear);
            yearComboBox.setOnAction(e -> {
                selectedYear = yearComboBox.getValue();
                loadAnalyticsData();
            });
        }
    }

    /**
     * Setup chart configurations
     */
    private void setupCharts() {
        // Expense by Category Pie Chart
        if (expenseByCategoryChart != null) {
            expenseByCategoryChart.setTitle("Expenses by Category");
            expenseByCategoryChart.setLabelsVisible(true);
            expenseByCategoryChart.setLegendVisible(true);
        }

        // Income by Category Pie Chart
        if (incomeByCategoryChart != null) {
            incomeByCategoryChart.setTitle("Income by Category");
            incomeByCategoryChart.setLabelsVisible(true);
            incomeByCategoryChart.setLegendVisible(true);
        }

        // Income vs Expense Bar Chart
        if (incomeExpenseBarChart != null) {
            incomeExpenseBarChart.setTitle("Monthly Income vs Expenses");
            barChartXAxis.setLabel("Month");
            barChartYAxis.setLabel("Amount ($)");
            incomeExpenseBarChart.setLegendVisible(true);
        }

        // Trend Line Chart
        if (trendLineChart != null) {
            trendLineChart.setTitle("Financial Trends");
            lineChartXAxis.setLabel("Month");
            lineChartYAxis.setLabel("Amount ($)");
            trendLineChart.setLegendVisible(true);
            trendLineChart.setCreateSymbols(true);
        }
    }

    /**
     * Load all analytics data
     */
    private void loadAnalyticsData() {
        if (currentUserId == null) {
            logger.warn("No user logged in, cannot load analytics data");
            return;
        }

        try {
            // Get date range based on selected period
            LocalDate[] dateRange = getDateRange();
            LocalDate startDate = dateRange[0];
            LocalDate endDate = dateRange[1];

            // Load summary data
            loadSummaryData(startDate, endDate);

            // Load chart data
            loadExpenseByCategoryChart(startDate, endDate);
            loadIncomeByCategoryChart(startDate, endDate);
            loadIncomeExpenseBarChart();
            loadTrendLineChart();

        } catch (Exception e) {
            logger.error("Error loading analytics data", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load analytics data: " + e.getMessage());
        }
    }

    /**
     * Get date range based on selected period
     */
    private LocalDate[] getDateRange() {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate;

        switch (selectedPeriod) {
            case "This Month":
                startDate = endDate.withDayOfMonth(1);
                break;
            case "Last 3 Months":
                startDate = endDate.minusMonths(3).withDayOfMonth(1);
                break;
            case "Last 6 Months":
                startDate = endDate.minusMonths(6).withDayOfMonth(1);
                break;
            case "This Year":
                startDate = LocalDate.of(selectedYear, 1, 1);
                endDate = LocalDate.of(selectedYear, 12, 31);
                break;
            case "Last Year":
                startDate = LocalDate.of(selectedYear - 1, 1, 1);
                endDate = LocalDate.of(selectedYear - 1, 12, 31);
                break;
            case "All Time":
            default:
                startDate = LocalDate.of(2000, 1, 1);
                break;
        }

        return new LocalDate[]{startDate, endDate};
    }

    /**
     * Load summary data (totals and rates)
     */
    private void loadSummaryData(LocalDate startDate, LocalDate endDate) {
        // Get totals
        BigDecimal totalIncome = incomeService.getTotalIncome(currentUserId, startDate, endDate);
        BigDecimal totalExpenses = BigDecimal.valueOf(expenseService.getTotalExpenses(currentUserId, startDate, endDate));
        BigDecimal netSavings = totalIncome.subtract(totalExpenses);

        // Calculate savings rate
        BigDecimal savingsRate = BigDecimal.ZERO;
        if (totalIncome.compareTo(BigDecimal.ZERO) > 0) {
            savingsRate = netSavings.divide(totalIncome, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        // Update labels
        totalIncomeLabel.setText(String.format("$%,.2f", totalIncome));
        totalExpensesLabel.setText(String.format("$%,.2f", totalExpenses));
        netSavingsLabel.setText(String.format("$%,.2f", netSavings));

        // Color code net savings
        if (netSavings.compareTo(BigDecimal.ZERO) >= 0) {
            netSavingsLabel.setStyle("-fx-text-fill: #5CB85C;");
        } else {
            netSavingsLabel.setStyle("-fx-text-fill: #D9534F;");
        }

        savingsRateLabel.setText(String.format("%.1f%%", savingsRate));
        if (savingsRate.compareTo(BigDecimal.valueOf(20)) >= 0) {
            savingsRateLabel.setStyle("-fx-text-fill: #5CB85C;");
        } else if (savingsRate.compareTo(BigDecimal.ZERO) >= 0) {
            savingsRateLabel.setStyle("-fx-text-fill: #F0AD4E;");
        } else {
            savingsRateLabel.setStyle("-fx-text-fill: #D9534F;");
        }
    }

    /**
     * Load Expense by Category Pie Chart
     */
    private void loadExpenseByCategoryChart(LocalDate startDate, LocalDate endDate) {
        if (expenseByCategoryChart == null) return;

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();

        // Get expenses grouped by category
        Map<String, BigDecimal> expensesByCategory = getExpensesByCategory(startDate, endDate);

        // Add data to pie chart
        for (Map.Entry<String, BigDecimal> entry : expensesByCategory.entrySet()) {
            String category = entry.getKey();
            double amount = entry.getValue().doubleValue();
            if (amount > 0) {
                pieData.add(new PieChart.Data(category + " ($" + String.format("%,.0f", amount) + ")", amount));
            }
        }

        expenseByCategoryChart.setData(pieData);

        // Add tooltips
        for (PieChart.Data data : pieData) {
            Tooltip tooltip = new Tooltip(data.getName() + "\n" +
                    String.format("%.1f%%", (data.getPieValue() / getTotalFromPieData(pieData)) * 100));
            Tooltip.install(data.getNode(), tooltip);
        }
    }

    /**
     * Load Income by Category Pie Chart
     */
    private void loadIncomeByCategoryChart(LocalDate startDate, LocalDate endDate) {
        if (incomeByCategoryChart == null) return;

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();

        // Get income grouped by category
        Map<String, BigDecimal> incomeByCategory = incomeService.getIncomeByCategoryGrouped(currentUserId, startDate, endDate);

        // Add data to pie chart
        for (Map.Entry<String, BigDecimal> entry : incomeByCategory.entrySet()) {
            String category = entry.getKey() != null ? entry.getKey() : "Uncategorized";
            double amount = entry.getValue().doubleValue();
            if (amount > 0) {
                pieData.add(new PieChart.Data(category + " ($" + String.format("%,.0f", amount) + ")", amount));
            }
        }

        incomeByCategoryChart.setData(pieData);

        // Add tooltips
        for (PieChart.Data data : pieData) {
            Tooltip tooltip = new Tooltip(data.getName() + "\n" +
                    String.format("%.1f%%", (data.getPieValue() / getTotalFromPieData(pieData)) * 100));
            Tooltip.install(data.getNode(), tooltip);
        }
    }

    /**
     * Load Income vs Expense Bar Chart (Monthly comparison)
     */
    private void loadIncomeExpenseBarChart() {
        if (incomeExpenseBarChart == null) return;

        incomeExpenseBarChart.getData().clear();

        XYChart.Series<String, Number> incomeSeries = new XYChart.Series<>();
        incomeSeries.setName("Income");

        XYChart.Series<String, Number> expenseSeries = new XYChart.Series<>();
        expenseSeries.setName("Expenses");

        // Get monthly data for selected year
        Map<String, BigDecimal> monthlyIncome = incomeService.getMonthlyIncomeTotals(currentUserId, selectedYear);
        Map<String, BigDecimal> monthlyExpenses = getMonthlyExpenseTotals(selectedYear);

        // Add data for each month
        for (int month = 1; month <= 12; month++) {
            String monthKey = String.format("%d-%02d", selectedYear, month);
            String monthName = Month.of(month).getDisplayName(TextStyle.SHORT, Locale.getDefault());

            BigDecimal income = monthlyIncome.getOrDefault(monthKey, BigDecimal.ZERO);
            BigDecimal expense = monthlyExpenses.getOrDefault(monthKey, BigDecimal.ZERO);

            incomeSeries.getData().add(new XYChart.Data<>(monthName, income.doubleValue()));
            expenseSeries.getData().add(new XYChart.Data<>(monthName, expense.doubleValue()));
        }

        incomeExpenseBarChart.getData().addAll(incomeSeries, expenseSeries);

        // Add tooltips to bars
        for (XYChart.Series<String, Number> series : incomeExpenseBarChart.getData()) {
            for (XYChart.Data<String, Number> data : series.getData()) {
                Tooltip tooltip = new Tooltip(series.getName() + " - " + data.getXValue() +
                        ": $" + String.format("%,.2f", data.getYValue()));
                Tooltip.install(data.getNode(), tooltip);
            }
        }
    }

    /**
     * Load Trend Line Chart (Cumulative savings over time)
     */
    private void loadTrendLineChart() {
        if (trendLineChart == null) return;

        trendLineChart.getData().clear();

        XYChart.Series<String, Number> incomeTrendSeries = new XYChart.Series<>();
        incomeTrendSeries.setName("Income");

        XYChart.Series<String, Number> expenseTrendSeries = new XYChart.Series<>();
        expenseTrendSeries.setName("Expenses");

        XYChart.Series<String, Number> savingsTrendSeries = new XYChart.Series<>();
        savingsTrendSeries.setName("Net Savings");

        // Get monthly data for selected year
        Map<String, BigDecimal> monthlyIncome = incomeService.getMonthlyIncomeTotals(currentUserId, selectedYear);
        Map<String, BigDecimal> monthlyExpenses = getMonthlyExpenseTotals(selectedYear);

        BigDecimal cumulativeIncome = BigDecimal.ZERO;
        BigDecimal cumulativeExpenses = BigDecimal.ZERO;

        // Add data for each month (cumulative)
        for (int month = 1; month <= 12; month++) {
            String monthKey = String.format("%d-%02d", selectedYear, month);
            String monthName = Month.of(month).getDisplayName(TextStyle.SHORT, Locale.getDefault());

            BigDecimal income = monthlyIncome.getOrDefault(monthKey, BigDecimal.ZERO);
            BigDecimal expense = monthlyExpenses.getOrDefault(monthKey, BigDecimal.ZERO);

            cumulativeIncome = cumulativeIncome.add(income);
            cumulativeExpenses = cumulativeExpenses.add(expense);
            BigDecimal cumulativeSavings = cumulativeIncome.subtract(cumulativeExpenses);

            incomeTrendSeries.getData().add(new XYChart.Data<>(monthName, cumulativeIncome.doubleValue()));
            expenseTrendSeries.getData().add(new XYChart.Data<>(monthName, cumulativeExpenses.doubleValue()));
            savingsTrendSeries.getData().add(new XYChart.Data<>(monthName, cumulativeSavings.doubleValue()));
        }

        trendLineChart.getData().addAll(incomeTrendSeries, expenseTrendSeries, savingsTrendSeries);

        // Add tooltips to data points
        for (XYChart.Series<String, Number> series : trendLineChart.getData()) {
            for (XYChart.Data<String, Number> data : series.getData()) {
                Tooltip tooltip = new Tooltip(series.getName() + " - " + data.getXValue() +
                        ": $" + String.format("%,.2f", data.getYValue()));
                if (data.getNode() != null) {
                    Tooltip.install(data.getNode(), tooltip);
                }
            }
        }
    }

    /**
     * Get expenses grouped by category
     */
    private Map<String, BigDecimal> getExpensesByCategory(LocalDate startDate, LocalDate endDate) {
        Map<String, BigDecimal> result = new HashMap<>();

        // Get all expenses for the period and group by category
        var expenses = expenseService.getExpensesByDateRange(currentUserId, startDate, endDate);

        for (var expense : expenses) {
            String category = expense.getCategoryName() != null ? expense.getCategoryName() : "Uncategorized";
            result.merge(category, expense.getAmount(), BigDecimal::add);
        }

        return result;
    }

    /**
     * Get monthly expense totals for a year
     */
    private Map<String, BigDecimal> getMonthlyExpenseTotals(int year) {
        Map<String, BigDecimal> result = new HashMap<>();

        for (int month = 1; month <= 12; month++) {
            LocalDate startDate = LocalDate.of(year, month, 1);
            LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

            double total = expenseService.getTotalExpenses(currentUserId, startDate, endDate);
            String monthKey = String.format("%d-%02d", year, month);
            result.put(monthKey, BigDecimal.valueOf(total));
        }

        return result;
    }

    /**
     * Calculate total from pie chart data
     */
    private double getTotalFromPieData(ObservableList<PieChart.Data> pieData) {
        return pieData.stream().mapToDouble(PieChart.Data::getPieValue).sum();
    }

    /**
     * Refresh all data
     */
    @FXML
    private void handleRefresh() {
        loadAnalyticsData();
    }

    /**
     * Set user ID externally
     */
    public void setCurrentUserId(UUID userId) {
        this.currentUserId = userId;
        loadAnalyticsData();
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
