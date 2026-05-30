package com.cache.controller;

import com.cache.dto.LoadTestRequest;
import com.cache.dto.LoadTestResult;
import com.cache.service.LoadTestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/load-test")
public class LoadTestController {

    private final LoadTestService loadTestService;

    public LoadTestController(LoadTestService loadTestService) {
        this.loadTestService = loadTestService;
    }

    @PostMapping("/run")
    public ResponseEntity<LoadTestResult> run(@RequestBody LoadTestRequest request) {
        LoadTestResult result = loadTestService.run(request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/run/quick")
    public ResponseEntity<LoadTestResult> quickRun(@RequestParam String cacheName) {
        LoadTestResult result = loadTestService.run(LoadTestRequest.defaults(cacheName));
        return ResponseEntity.ok(result);
    }
}
