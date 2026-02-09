package com.example.airtimebackend.controller;

import com.example.airtimebackend.model.BrainDumpRequest;
import com.example.airtimebackend.model.BrainDumpResponse;
import com.example.airtimebackend.service.BrainDumpService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class BrainDumpController {

    private final BrainDumpService service;

    public BrainDumpController(BrainDumpService service) {
        this.service = service;
    }

    @GetMapping("/ping")
    public String ping() {
        return "Backend is alive 🚀";
    }
//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

    @PostMapping("/brain-dump")
    public BrainDumpResponse analyze(@RequestBody BrainDumpRequest request) {
        return service.analyze(request.text);
    }
}