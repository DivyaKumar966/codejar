package com.codejar.app.dto;

import java.time.Instant;

public record CodeDraftResponse(Long taskId, String language, String sourceCode, Instant updatedAt) {
}
