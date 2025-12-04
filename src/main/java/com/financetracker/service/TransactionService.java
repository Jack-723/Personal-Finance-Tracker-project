package com.financetracker.service;

import com.financetracker.model.Expense;
import com.financetracker.model.Income;
import com.financetracker.model.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service class demonstrating polymorphism with transactions
 * Works with both Income and Expense objects through the Transaction interface
 */
public class TransactionService {

    private final IncomeService incomeService;
    private final ExpenseService expenseService;

    public TransactionService() {
        this.incomeService = new IncomeService();
        this.expenseService = new ExpenseService();
    }

    /**
     * Get all transactions (both income and expenses) for a user
     * Demonstrates polymorphism - returns list of Transaction base type
     */
    public List<Transaction> getAllTransactions(UUID userId) {
        List<Transaction> allTransactions = new ArrayList<>();

        // Add all income transactions (polymorphism - Income IS-A Transaction)
        List<Income> incomes = incomeService.getIncomeByUser(userId);
        allTransactions.addAll(incomes);

        // Add all expense transactions (polymorphism - Expense IS-A Transaction)
        List<Expense> expenses = expenseService.getExpensesByUser(userId);
        allTransactions.addAll(expenses);

        // Sort by date (newest first)
        allTransactions.sort(Comparator.comparing(Transaction::getTransactionDate).reversed());

        return allTransactions;
    }

    /**
     * Get transactions within a date range
     * Polymorphic method - works with any Transaction subclass
     */
    public List<Transaction> getTransactionsByDateRange(UUID userId, LocalDate startDate, LocalDate endDate) {
        List<Transaction> allTransactions = new ArrayList<>();

        // Get income for date range
        List<Income> incomes = incomeService.getIncomeByDateRange(userId, startDate, endDate);
        allTransactions.addAll(incomes);

        // Get expenses for date range
        List<Expense> expenses = expenseService.getExpensesByDateRange(userId, startDate, endDate);
        allTransactions.addAll(expenses);

        // Sort by date
        allTransactions.sort(Comparator.comparing(Transaction::getTransactionDate).reversed());

        return allTransactions;
    }

    /**
     * Calculate net balance from a list of transactions
     * Demonstrates polymorphism - getBalanceEffect() behaves differently for Income vs Expense
     */
    public BigDecimal calculateNetBalance(List<Transaction> transactions) {
        return transactions.stream()
                .map(Transaction::getBalanceEffect)  // Polymorphic call!
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Get total income from transactions
     * Filters by transaction type using polymorphic method
     */
    public BigDecimal getTotalIncome(List<Transaction> transactions) {
        return transactions.stream()
                .filter(t -> t.getTransactionType() == Transaction.TransactionType.INCOME)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Get total expenses from transactions
     * Filters by transaction type using polymorphic method
     */
    public BigDecimal getTotalExpenses(List<Transaction> transactions) {
        return transactions.stream()
                .filter(t -> t.getTransactionType() == Transaction.TransactionType.EXPENSE)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Get recent transactions
     */
    public List<Transaction> getRecentTransactions(UUID userId, int limit) {
        return getAllTransactions(userId).stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Print transaction summaries
     * Demonstrates polymorphism - getSummary() returns different format for Income vs Expense
     */
    public void printTransactionSummaries(List<Transaction> transactions) {
        for (Transaction t : transactions) {
            System.out.println(t.getSummary());  // Polymorphic call!
        }
    }

    /**
     * Validate all transactions
     * Demonstrates polymorphism - isValid() checks different fields for Income vs Expense
     */
    public List<Transaction> getInvalidTransactions(List<Transaction> transactions) {
        return transactions.stream()
                .filter(t -> !t.isValid())  // Polymorphic call!
                .collect(Collectors.toList());
    }

    /**
     * Get transactions by type
     */
    public List<Transaction> getTransactionsByType(List<Transaction> transactions,
                                                   Transaction.TransactionType type) {
        return transactions.stream()
                .filter(t -> t.getTransactionType() == type)
                .collect(Collectors.toList());
    }

    /**
     * Calculate savings rate
     */
    public BigDecimal calculateSavingsRate(List<Transaction> transactions) {
        BigDecimal income = getTotalIncome(transactions);
        BigDecimal expenses = getTotalExpenses(transactions);

        if (income.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal savings = income.subtract(expenses);
        return savings.divide(income, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Group transactions by type and return formatted amounts
     * Uses polymorphic getFormattedAmountWithSign()
     */
    public List<String> getFormattedTransactionList(List<Transaction> transactions) {
        return transactions.stream()
                .map(t -> String.format("[%s] %s - %s",
                        t.getTransactionType().getDisplayName(),
                        t.getDisplayName(),
                        t.getFormattedAmountWithSign()))
                .collect(Collectors.toList());
    }
}