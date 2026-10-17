package com.codejar.app.controller;

import com.codejar.app.dto.RunCodeRequest;
import com.codejar.app.dto.RunCodeResponse;
import com.codejar.app.service.CodeRunnerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/runner")
public class CodeRunnerController {
    private final CodeRunnerService codeRunnerService;

    public CodeRunnerController(CodeRunnerService codeRunnerService) {
        this.codeRunnerService = codeRunnerService;
    }

    @PostMapping("/run")
    public ResponseEntity<RunCodeResponse> run(@Valid @RequestBody RunCodeRequest request) {
        return ResponseEntity.ok(codeRunnerService.run(request));
    }
}
