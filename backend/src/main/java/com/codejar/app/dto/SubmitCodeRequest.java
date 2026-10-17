package com.codejar.app.dto;
import jakarta.validation.constraints.*;
public record SubmitCodeRequest(@NotNull Long taskId, @NotBlank String language, @NotBlank @Size(max=20000) String code) { }
