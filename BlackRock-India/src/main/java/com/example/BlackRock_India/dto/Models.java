package com.example.BlackRock_India.dto;

import java.util.List;

public class Models {

    // ---- Inputs ----
    public record Expense(String date, long amount) {}

    public record QRule(String startDate, String endDate, long fixed) {}
    public record PRule(String startDate, String endDate, long extra) {}
    public record KWindow(String startDate, String endDate) {}

    public record ParseRequest(List<Expense> expenses) {}

    public record ValidatorRequest(List<ParsedTransaction> transactions) {}

    public record FilterRequest(
            List<ParsedTransaction> transactions,
            List<QRule> qPeriods,
            List<PRule> pPeriods,
            List<KWindow> kPeriods
    ) {}

    public record ReturnsRequest(
            List<ParsedTransaction> transactions,
            List<QRule> qPeriods,
            List<PRule> pPeriods,
            List<KWindow> kPeriods,
            int age,
            long wage,          // monthly wage (as per statement vibe)
            double inflation    // e.g. 0.055 for 5.5%
    ) {}

    // ---- Outputs ----
    public record ParsedTransaction(String date, long amount, long ceiling, long remanent) {}

    public record InvalidItem(ParsedTransaction transaction, String reason) {}

    public record ParseResponse(List<ParsedTransaction> transactions) {}

    public record ValidatorResponse(List<ParsedTransaction> valid, List<InvalidItem> invalid) {}

    public record FilteredTransaction(String date, long amount, long ceiling, long baseRemanent, long finalRemanent) {}

    public record KSum(String startDate, String endDate, long amount) {}

    public record FilterResponse(
            List<FilteredTransaction> transactions,
            List<KSum> savingsByDates,
            long totalInvested
    ) {}

    public record ReturnRow(String startDate, String endDate, long invested, double nominalFV, double realFV, double profit, Double taxBenefit) {}

    public record ReturnsResponse(
            String instrument,
            List<ReturnRow> rows,
            double totalNominalFV,
            double totalRealFV
    ) {}
}