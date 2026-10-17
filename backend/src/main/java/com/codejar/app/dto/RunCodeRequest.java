package com.codejar.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RunCodeRequest(
        @NotBlank(message = "Language is required") String language,
        @NotBlank(message = "Code is required") @Size(max = 20000, message = "Code is limited to 20,000 characters") String code,
        @Size(max = 5000, message = "Input is limited to 5,000 characters") String input
) { }
