package com.example.airtimebackend.model;

import java.util.List;
import java.util.Map;

public class BrainDumpResponse {
    public int urgency;          // 1-5
    public int priority;         // 1-5
    public String advice;
    public String motivation;
    public List<String> organizedNotes;


    // ADHD-specific additions
    public List<Map<String, Object>> calendarEvents;
    public List<String> timeManagementTips;
    public String quickWin;
    public int estimatedTime;
    public String energyLevel;
    public String celebration;
    public List<String> nextSteps;
    public int dopamineScore;


    public BrainDumpResponse(
            int urgency,
            int priority,
            String advice,
            String motivation,
            List<String> organizedNotes,
            String quickWin,
            int estimatedTime,
            String energyLevel,
            String celebration,
            List<String> nextSteps,
            int dopamineScore,
            List<Map<String, Object>> calendarEvents, // Added 12
            List<String> timeManagementTips          // Added 13
    ) {
        this.urgency = urgency;
        this.priority = priority;
        this.advice = advice;
        this.motivation = motivation;
        this.organizedNotes = organizedNotes;
        this.quickWin = quickWin;
        this.estimatedTime = estimatedTime;
        this.energyLevel = energyLevel;
        this.celebration = celebration;
        this.nextSteps = nextSteps;
        this.dopamineScore = dopamineScore;
        this.calendarEvents = calendarEvents;
        this.timeManagementTips = timeManagementTips;
    }

}
