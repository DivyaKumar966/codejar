package com.codejar.app.dto;

public record RunCodeResponse(
        String output,
        String error,
        int exitCode,
        long executionTime,
        boolean timedOut
) { }
