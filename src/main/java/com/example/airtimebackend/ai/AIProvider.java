package com.example.airtimebackend.ai;

import com.example.airtimebackend.model.BrainDumpResponse;

public interface AIProvider {
    BrainDumpResponse analyzeBrainDump(String text);
}