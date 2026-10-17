package com.codejar.app.controller;

import com.codejar.app.entity.*;
import com.codejar.app.repository.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserRepository userRepo;
    private final SubmissionRepository submissionRepo;
    private final TaskRepository taskRepo;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepo, SubmissionRepository submissionRepo, 
                         TaskRepository taskRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.submissionRepo = submissionRepo;
        this.taskRepo = taskRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/profile")
    public Map<String, Object> getProfile(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<Submission> submissions = submissionRepo.findByUserIdOrderBySubmittedAtDesc(user.getId());
        
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", user.getId());
        profile.put("name", user.getName());
        profile.put("email", user.getEmail());
        profile.put("createdAt", user.getCreatedAt());
        
        // Stats
        long totalSolved = submissionRepo.countCompletedTasks(user.getId());
        long totalAttempts = submissionRepo.countByUserId(user.getId());
        double avgScore = submissions.isEmpty() ? 0 : 
            Math.round(submissions.stream().mapToDouble(Submission::getScore).average().orElse(0) * 100.0) / 100.0;
        
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalSolved", totalSolved);
        stats.put("totalAttempts", totalAttempts);
        stats.put("averageScore", avgScore);
        
        // Language breakdown
        long javaCompleted = submissionRepo.countCompletedTasks(user.getId(), ProgrammingLanguage.JAVA);
        long pythonCompleted = submissionRepo.countCompletedTasks(user.getId(), ProgrammingLanguage.PYTHON);
        long javaTotal = taskRepo.countByLanguageCode(ProgrammingLanguage.JAVA);
        long pythonTotal = taskRepo.countByLanguageCode(ProgrammingLanguage.PYTHON);
        
        Map<String, Object> languages = new LinkedHashMap<>();
        languages.put("java", Map.of(
            "completed", javaCompleted,
            "total", javaTotal,
            "percentage", javaTotal == 0 ? 0 : Math.round(javaCompleted * 10000.0 / javaTotal) / 100.0
        ));
        languages.put("python", Map.of(
            "completed", pythonCompleted,
            "total", pythonTotal,
            "percentage", pythonTotal == 0 ? 0 : Math.round(pythonCompleted * 10000.0 / pythonTotal) / 100.0
        ));
        
        // Achievements
        Map<String, Object> achievements = new LinkedHashMap<>();
        achievements.put("firstSolve", totalSolved > 0);
        achievements.put("tenSolves", totalSolved >= 10);
        achievements.put("twentySolves", totalSolved >= 20);
        achievements.put("perfectScore", submissions.stream().anyMatch(s -> s.getScore() == 100));
        achievements.put("polyglot", javaCompleted > 0 && pythonCompleted > 0);
        achievements.put("streakData", calculateStreak(submissions));
        
        profile.put("stats", stats);
        profile.put("languages", languages);
        profile.put("achievements", achievements);
        
        return profile;
    }

    @PutMapping("/profile")
    public Map<String, Object> updateProfile(Authentication authentication, 
                                              @RequestBody Map<String, String> updates) {
        User user = (User) authentication.getPrincipal();
        
        if (updates.containsKey("name") && !updates.get("name").isBlank()) {
            user.setName(updates.get("name"));
        }
        if (updates.containsKey("email") && !updates.get("email").isBlank()) {
            user.setEmail(updates.get("email"));
        }
        
        userRepo.save(user);
        
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Profile updated successfully");
        response.put("user", Map.of("id", user.getId(), "name", user.getName(), "email", user.getEmail()));
        return response;
    }

    @PostMapping("/change-password")
    public Map<String, Object> changePassword(Authentication authentication,
                                               @RequestBody Map<String, String> passwordData) {
        User user = (User) authentication.getPrincipal();
        String oldPassword = passwordData.get("oldPassword");
        String newPassword = passwordData.get("newPassword");
        
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters");
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);
        
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Password changed successfully");
        return response;
    }

    private Map<String, Object> calculateStreak(List<Submission> submissions) {
        Map<String, Object> streak = new LinkedHashMap<>();
        streak.put("current", submissions.isEmpty() ? 0 : 1);
        streak.put("longest", submissions.isEmpty() ? 0 : 1);
        return streak;
    }
}