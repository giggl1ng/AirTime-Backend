package com.example.airtimebackend.service;

import com.example.airtimebackend.ai.AIProvider;
import com.example.airtimebackend.model.BrainDumpResponse;
import org.springframework.stereotype.Service;

@Service
public class BrainDumpService {

    private final AIProvider aiProvider;

    public BrainDumpService(AIProvider aiProvider) {
        this.aiProvider = aiProvider;
    }

    public BrainDumpResponse analyze(String text) {
        return aiProvider.analyzeBrainDump(text);
    }
}