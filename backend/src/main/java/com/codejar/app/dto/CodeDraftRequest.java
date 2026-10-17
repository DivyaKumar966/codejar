package com.codejar.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CodeDraftRequest(
        @NotNull Long taskId,
        @NotBlank String language,
        @NotBlank @Size(max = 20000) String sourceCode
) {
}
