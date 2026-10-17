package com.codejar.app.dto;
import java.time.Instant;
public record SubmissionResponse(Long id, String taskTitle, String language, String sourceCode, String status, double score, int passedTests, int totalTests, long executionTime, Instant submittedAt) { }
