package com.example.BlackRock_India.service;

import com.example.BlackRock_India.dto.Models;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class TransactionService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static long toEpoch(String s) {
        return LocalDateTime.parse(s, FMT).toEpochSecond(ZoneOffset.UTC);
    }

    public Models.ParseResponse parse(Models.ParseRequest req) {
        List<Models.ParsedTransaction> out = new ArrayList<>();
        if (req == null || req.expenses() == null) return new Models.ParseResponse(out);

        for (Models.Expense e : req.expenses()) {
            long amount = e.amount();
            long ceiling = ((amount + 99) / 100) * 100;
            long rem = ceiling - amount;
            out.add(new Models.ParsedTransaction(e.date(), amount, ceiling, rem));
        }
        return new Models.ParseResponse(out);
    }

    public Models.ValidatorResponse validate(Models.ValidatorRequest req) {
        List<Models.ParsedTransaction> valid = new ArrayList<>();
        List<Models.InvalidItem> invalid = new ArrayList<>();
        if (req == null || req.transactions() == null) return new Models.ValidatorResponse(valid, invalid);

        Set<String> seen = new HashSet<>();
        for (Models.ParsedTransaction t : req.transactions()) {
            if (t.amount() < 0) {
                invalid.add(new Models.InvalidItem(t, "Negative amounts are not allowed"));
                continue;
            }
            String key = t.date() + "|" + t.amount();
            if (!seen.add(key)) {
                invalid.add(new Models.InvalidItem(t, "Duplicate transaction"));
                continue;
            }
            valid.add(t);
        }
        return new Models.ValidatorResponse(valid, invalid);
    }

    // Internal sortable tx
    private record Tx(int idx, String date, long amount, long ceiling, long baseRem, long time) {}

    private record Q(long start, long end, long fixed, int idx) {}
    private record P(long start, long end, long extra) {}
    private record K(long start, long end, String s, String e) {}

    public Models.FilterResponse filter(Models.FilterRequest req) {
        if (req == null || req.transactions() == null) {
            return new Models.FilterResponse(List.of(), List.of(), 0);
        }

        // Build tx list
        int n = req.transactions().size();
        Tx[] txs = new Tx[n];
        for (int i = 0; i < n; i++) {
            Models.ParsedTransaction t = req.transactions().get(i);
            long time = toEpoch(t.date());
            txs[i] = new Tx(i, t.date(), t.amount(), t.ceiling(), t.remanent(), time);
        }
        Arrays.sort(txs, Comparator.comparingLong(Tx::time));

        // Build q list (preserve input order for tie-break)
        List<Q> qList = new ArrayList<>();
        if (req.qPeriods() != null) {
            for (int i = 0; i < req.qPeriods().size(); i++) {
                Models.QRule r = req.qPeriods().get(i);
                qList.add(new Q(toEpoch(r.startDate()), toEpoch(r.endDate()), r.fixed(), i));
            }
        }
        qList.sort(Comparator.comparingLong(Q::start)); // start asc for pointer

        // Build p list
        List<P> pList = new ArrayList<>();
        if (req.pPeriods() != null) {
            for (Models.PRule r : req.pPeriods()) {
                pList.add(new P(toEpoch(r.startDate()), toEpoch(r.endDate()), r.extra()));
            }
        }
        pList.sort(Comparator.comparingLong(P::start));

        // Build k list
        List<K> kList = new ArrayList<>();
        if (req.kPeriods() != null) {
            for (Models.KWindow w : req.kPeriods()) {
                kList.add(new K(toEpoch(w.startDate()), toEpoch(w.endDate()), w.startDate(), w.endDate()));
            }
        }

        // qHeap: latest start wins, tie -> first input index
        PriorityQueue<Q> qHeap = new PriorityQueue<>((a, b) -> {
            if (a.start != b.start) return Long.compare(b.start, a.start);
            return Integer.compare(a.idx, b.idx);
        });

        // pHeap by end for sumExtra
        record PActive(long end, long extra) {}
        PriorityQueue<PActive> pHeap = new PriorityQueue<>(Comparator.comparingLong(PActive::end));
        long sumExtra = 0;

        int qPtr = 0, pPtr = 0;

        long[] finalRemByIdx = new long[n];
        long[] timesSorted = new long[n];
        long[] remSorted = new long[n];

        for (int j = 0; j < n; j++) {
            Tx tx = txs[j];
            long t = tx.time();

            // activate q
            while (qPtr < qList.size() && qList.get(qPtr).start <= t) {
                qHeap.add(qList.get(qPtr++));
            }
            while (!qHeap.isEmpty() && qHeap.peek().end < t) qHeap.poll();

            // activate p
            while (pPtr < pList.size() && pList.get(pPtr).start <= t) {
                P pr = pList.get(pPtr++);
                pHeap.add(new PActive(pr.end, pr.extra));
                sumExtra += pr.extra;
            }
            while (!pHeap.isEmpty() && pHeap.peek().end < t) {
                sumExtra -= pHeap.poll().extra;
            }

            long rem = tx.baseRem();
            if (!qHeap.isEmpty()) rem = qHeap.peek().fixed;
            rem += sumExtra;

            finalRemByIdx[tx.idx()] = rem;
            timesSorted[j] = t;
            remSorted[j] = rem;
        }

        // Build filtered tx list back in original order (nice for debugging)
        Models.FilteredTransaction[] filtered = new Models.FilteredTransaction[n];
        for (Tx tx : txs) {
            long fin = finalRemByIdx[tx.idx()];
            filtered[tx.idx()] = new Models.FilteredTransaction(
                    tx.date(), tx.amount(), tx.ceiling(), tx.baseRem(), fin
            );
        }

        // Prefix sums for k
        long[] pref = new long[n + 1];
        for (int i = 0; i < n; i++) pref[i + 1] = pref[i] + remSorted[i];

        List<Models.KSum> sums = new ArrayList<>();
        long total = pref[n];

        for (K w : kList) {
            int L = lowerBound(timesSorted, w.start);
            int R = upperBound(timesSorted, w.end);
            long amount = pref[R] - pref[L];
            sums.add(new Models.KSum(w.s, w.e, amount));
        }

        return new Models.FilterResponse(Arrays.asList(filtered), sums, total);
    }

    private static int lowerBound(long[] a, long x) {
        int lo = 0, hi = a.length;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (a[mid] >= x) hi = mid; else lo = mid + 1;
        }
        return lo;
    }

    private static int upperBound(long[] a, long x) {
        int lo = 0, hi = a.length;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (a[mid] > x) hi = mid; else lo = mid + 1;
        }
        return lo;
    }
}