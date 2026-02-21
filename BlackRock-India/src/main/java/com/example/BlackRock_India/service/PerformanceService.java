package com.example.BlackRock_India.service;

import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class PerformanceService {
    public Map<String, Object> performance() {
        var rt = Runtime.getRuntime();
        long used = rt.totalMemory() - rt.freeMemory();

        Map<String, Object> m = new HashMap<>();
        m.put("timeUtc", Instant.now().toString());
        m.put("uptimeSeconds", ManagementFactory.getRuntimeMXBean().getUptime() / 1000.0);
        m.put("heapUsedMB", used / (1024.0 * 1024.0));
        m.put("heapMaxMB", rt.maxMemory() / (1024.0 * 1024.0));
        m.put("availableProcessors", rt.availableProcessors());
        m.put("threadCount", ManagementFactory.getThreadMXBean().getThreadCount());
        return m;
    }
}