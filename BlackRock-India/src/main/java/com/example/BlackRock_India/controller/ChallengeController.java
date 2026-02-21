package com.example.BlackRock_India.controller;

import com.example.BlackRock_India.dto.Models;
import com.example.BlackRock_India.service.PerformanceService;
import com.example.BlackRock_India.service.ReturnsService;
import com.example.BlackRock_India.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/blackrock/challenge/v1")
public class ChallengeController {

    private final TransactionService txService;
    private final ReturnsService returnsService;
    private final PerformanceService perfService;

    public ChallengeController(TransactionService txService, ReturnsService returnsService, PerformanceService perfService) {
        this.txService = txService;
        this.returnsService = returnsService;
        this.perfService = perfService;
    }

    @PostMapping("/transactions:parse")
    public ResponseEntity<Models.ParseResponse> parse(@RequestBody Models.ParseRequest req) {
        return ResponseEntity.ok(txService.parse(req));
    }

    @PostMapping("/transactions:validator")
    public ResponseEntity<Models.ValidatorResponse> validate(@RequestBody Models.ValidatorRequest req) {
        return ResponseEntity.ok(txService.validate(req));
    }

    @PostMapping("/transactions:filter")
    public ResponseEntity<Models.FilterResponse> filter(@RequestBody Models.FilterRequest req) {
        return ResponseEntity.ok(txService.filter(req));
    }

    @PostMapping("/returns:index")
    public ResponseEntity<Models.ReturnsResponse> index(@RequestBody Models.ReturnsRequest req) {
        Models.FilterResponse filtered = txService.filter(
                new Models.FilterRequest(req.transactions(), req.qPeriods(), req.pPeriods(), req.kPeriods())
        );
        return ResponseEntity.ok(returnsService.index(req, filtered));
    }

    @PostMapping("/returns:nps")
    public ResponseEntity<Models.ReturnsResponse> nps(@RequestBody Models.ReturnsRequest req) {
        Models.FilterResponse filtered = txService.filter(
                new Models.FilterRequest(req.transactions(), req.qPeriods(), req.pPeriods(), req.kPeriods())
        );
        return ResponseEntity.ok(returnsService.nps(req, filtered));
    }

    @GetMapping("/performance")
    public ResponseEntity<Map<String, Object>> performance() {
        return ResponseEntity.ok(perfService.performance());
    }
}