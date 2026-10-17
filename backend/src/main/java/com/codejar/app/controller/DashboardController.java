package com.codejar.app.controller;

import com.codejar.app.entity.*;
import com.codejar.app.repository.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final TaskRepository tasks; private final SubmissionRepository submissions;
    public DashboardController(TaskRepository tasks, SubmissionRepository submissions) { this.tasks=tasks; this.submissions=submissions; }
    @GetMapping public Map<String,Object> dashboard(Authentication authentication) {
        User user=(User) authentication.getPrincipal(); List<Submission> history=submissions.findByUserIdOrderBySubmittedAtDesc(user.getId());
        Map<String,Object> result=new LinkedHashMap<>(); result.put("java", progress(user.getId(), ProgrammingLanguage.JAVA)); result.put("python", progress(user.getId(), ProgrammingLanguage.PYTHON));
        long total=tasks.countByLanguageCode(ProgrammingLanguage.JAVA)+tasks.countByLanguageCode(ProgrammingLanguage.PYTHON), completed=submissions.countCompletedTasks(user.getId());
        result.put("completed",completed); result.put("pending",Math.max(0,total-completed)); result.put("averageScore",history.isEmpty()?0:Math.round(history.stream().mapToDouble(Submission::getScore).average().orElse(0)*100.0)/100.0); result.put("currentLanguage",history.isEmpty()?null:history.get(0).getLanguage().name()); result.put("streak",currentStreak(history)); result.put("recentSubmissions",history.stream().limit(5).map(this::submission).toList()); return result;
    }
    private Map<String,Object> progress(Long userId, ProgrammingLanguage language) { long total=tasks.countByLanguageCode(language), completed=submissions.countCompletedTasks(userId,language); Map<String,Object> value=new LinkedHashMap<>(); value.put("completed",completed); value.put("total",total); value.put("percentage",total==0?0:Math.round(completed*10000.0/total)/100.0); return value; }
    private long currentStreak(List<Submission> history) { ZoneId zone=ZoneId.systemDefault(); Set<LocalDate> activeDays=new HashSet<>(); for (Submission submission : history) if ("ACCEPTED".equals(submission.getStatus()) && submission.getScore() >= 100.0) activeDays.add(submission.getSubmittedAt().atZone(zone).toLocalDate()); long streak=0; for (LocalDate day=LocalDate.now(zone); activeDays.contains(day); day=day.minusDays(1)) streak++; return streak; }
    private Map<String,Object> submission(Submission s) { Map<String,Object> value=new LinkedHashMap<>(); value.put("id",s.getId()); value.put("taskTitle",s.getTask().getTitle()); value.put("language",s.getLanguage().name()); value.put("status",s.getStatus()); value.put("score",s.getScore()); value.put("passedTests",s.getPassedTests()); value.put("totalTests",s.getTotalTests()); value.put("executionTime",s.getExecutionTime()); value.put("submittedAt",s.getSubmittedAt()); return value; }
}
