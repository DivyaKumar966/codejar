package com.codejar.app.controller;
import com.codejar.app.dto.*;
import com.codejar.app.entity.User;
import com.codejar.app.service.SubmissionService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/submissions")
public class SubmissionController {
 private final SubmissionService service; public SubmissionController(SubmissionService service) {this.service=service;}
 @PostMapping public SubmissionResponse submit(Authentication authentication,@Valid @RequestBody SubmitCodeRequest request) { return service.submit((User) authentication.getPrincipal(),request); }
 @GetMapping public List<SubmissionResponse> history(Authentication authentication) { return service.history((User) authentication.getPrincipal()); }
}
