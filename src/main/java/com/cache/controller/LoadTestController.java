package com.cache.controller;

import com.cache.dto.LoadTestRequest;
import com.cache.dto.LoadTestResult;
import com.cache.service.LoadTestService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/caches/{name}/loadtest")
@CrossOrigin(origins = "${cache.cors.allowed-origin:http://localhost:4200}")
public class LoadTestController {
    private final LoadTestService service;
    public LoadTestController(LoadTestService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<LoadTestResult> start(@PathVariable String name, @RequestBody LoadTestRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(service.start(name, request));
    }

    @GetMapping("/{testId}")
    public LoadTestResult result(@PathVariable String name, @PathVariable String testId) {
        return service.get(name, testId);
    }
}
