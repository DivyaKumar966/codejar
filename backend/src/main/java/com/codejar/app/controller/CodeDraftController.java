package com.codejar.app.controller;

import com.codejar.app.dto.CodeDraftRequest;
import com.codejar.app.dto.CodeDraftResponse;
import com.codejar.app.entity.CodeDraft;
import com.codejar.app.entity.Task;
import com.codejar.app.entity.User;
import com.codejar.app.repository.CodeDraftRepository;
import com.codejar.app.repository.TaskRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Optional;

@RestController
@RequestMapping("/api/code-drafts")
public class CodeDraftController {
    private final CodeDraftRepository codeDraftRepository;
    private final TaskRepository taskRepository;

    public CodeDraftController(CodeDraftRepository codeDraftRepository, TaskRepository taskRepository) {
        this.codeDraftRepository = codeDraftRepository;
        this.taskRepository = taskRepository;
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<CodeDraftResponse> getDraft(Authentication authentication, @PathVariable Long taskId) {
        User user = (User) authentication.getPrincipal();
        Task task = taskRepository.findById(taskId).orElseThrow(() -> new IllegalArgumentException("Problem not found."));

        Optional<CodeDraft> draft = codeDraftRepository.findByUserAndTask(user, task);
        if (draft.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        CodeDraft found = draft.get();
        return ResponseEntity.ok(new CodeDraftResponse(found.getTask().getId(), found.getLanguage().name(), found.getSourceCode(), found.getUpdatedAt()));
    }

    @PutMapping("/{taskId}")
    public ResponseEntity<CodeDraftResponse> saveDraft(Authentication authentication, @PathVariable Long taskId, @Valid @RequestBody CodeDraftRequest request) {
        User user = (User) authentication.getPrincipal();
        Task task = taskRepository.findById(request.taskId()).orElseThrow(() -> new IllegalArgumentException("Problem not found."));

        if (!taskId.equals(request.taskId())) {
            throw new IllegalArgumentException("Problem mismatch.");
        }

        CodeDraft existing = codeDraftRepository.findByUserAndTask(user, task).orElse(null);
        if (existing == null) {
            CodeDraft saved = codeDraftRepository.save(new CodeDraft(user, task, com.codejar.app.entity.ProgrammingLanguage.valueOf(request.language().trim().toUpperCase()), request.sourceCode()));
            return ResponseEntity.ok(new CodeDraftResponse(saved.getTask().getId(), saved.getLanguage().name(), saved.getSourceCode(), saved.getUpdatedAt()));
        }

        existing.setLanguage(com.codejar.app.entity.ProgrammingLanguage.valueOf(request.language().trim().toUpperCase()));
        existing.setSourceCode(request.sourceCode());
        existing.setUpdatedAt(Instant.now());
        CodeDraft saved = codeDraftRepository.save(existing);
        return ResponseEntity.ok(new CodeDraftResponse(saved.getTask().getId(), saved.getLanguage().name(), saved.getSourceCode(), saved.getUpdatedAt()));
    }
}
