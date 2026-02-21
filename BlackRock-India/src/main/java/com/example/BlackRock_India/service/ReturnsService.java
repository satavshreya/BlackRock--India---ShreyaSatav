package com.example.BlackRock_India.service;

import com.example.BlackRock_India.dto.Models;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ReturnsService {

    private static final double NPS_RATE = 0.0711;
    private static final double INDEX_RATE = 0.1449;

    public Models.ReturnsResponse index(Models.ReturnsRequest req, Models.FilterResponse filtered) {
        return compute(req, filtered, "index", INDEX_RATE, false);
    }

    public Models.ReturnsResponse nps(Models.ReturnsRequest req, Models.FilterResponse filtered) {
        return compute(req, filtered, "nps", NPS_RATE, true);
    }

    private Models.ReturnsResponse compute(
            Models.ReturnsRequest req,
            Models.FilterResponse filtered,
            String instrument,
            double rate,
            boolean includeTaxBenefit
    ) {
        int years = (req.age() < 60) ? (60 - req.age()) : 5;
        double infl = req.inflation();

        List<Models.ReturnRow> rows = new ArrayList<>();
        double totalNom = 0;
        double totalReal = 0;

        List<Models.KSum> ks = filtered.savingsByDates();
        if (ks == null || ks.isEmpty()) {
            // If no k windows provided, treat the whole-year total as one row
            long invested = filtered.totalInvested();
            Models.ReturnRow row = rowFor(null, null, invested, years, rate, infl, includeTaxBenefit, req.wage());
            rows.add(row);
            totalNom += row.nominalFV();
            totalReal += row.realFV();
            return new Models.ReturnsResponse(instrument, rows, totalNom, totalReal);
        }

        for (Models.KSum k : ks) {
            Models.ReturnRow row = rowFor(k.startDate(), k.endDate(), k.amount(), years, rate, infl, includeTaxBenefit, req.wage());
            rows.add(row);
            totalNom += row.nominalFV();
            totalReal += row.realFV();
        }

        return new Models.ReturnsResponse(instrument, rows, totalNom, totalReal);
    }

    private Models.ReturnRow rowFor(
            String startDate, String endDate,
            long invested, int years, double rate, double infl,
            boolean includeTaxBenefit, long wageMonthly
    ) {
        double nominal = invested * Math.pow(1.0 + rate, years);
        double real = nominal / Math.pow(1.0 + infl, years);
        double profit = real - invested;

        Double taxBenefit = null;
        if (includeTaxBenefit) {
            long annualIncome = wageMonthly * 12;
            long cap10pct = (long) Math.floor(0.10 * annualIncome);
            long deduction = Math.min(invested, Math.min(cap10pct, 200_000L));
            double before = tax(annualIncome);
            double after = tax(Math.max(0, annualIncome - deduction));
            taxBenefit = before - after;
        }

        return new Models.ReturnRow(startDate, endDate, invested, nominal, real, profit, taxBenefit);
    }

    // Tax slabs per screenshot (progressive)
    private double tax(long income) {
        if (income <= 700_000L) return 0.0;

        double t = 0.0;

        // 7L–10L: 10%
        t += slab(income, 700_000L, 1_000_000L, 0.10);

        // 10L–12L: 15%
        t += slab(income, 1_000_000L, 1_200_000L, 0.15);

        // 12L–15L: 20%
        t += slab(income, 1_200_000L, 1_500_000L, 0.20);

        // >15L: 30%
        if (income > 1_500_000L) {
            t += (income - 1_500_000L) * 0.30;
        }

        return t;
    }

    private double slab(long income, long lo, long hi, double rate) {
        if (income <= lo) return 0.0;
        long upper = Math.min(income, hi);
        return (upper - lo) * rate;
    }
}