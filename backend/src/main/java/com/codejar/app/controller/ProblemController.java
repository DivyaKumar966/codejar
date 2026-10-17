package com.codejar.app.controller;

import com.codejar.app.entity.*;
import com.codejar.app.repository.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import java.util.*;

@RestController @RequestMapping("/api")
public class ProblemController {
    private final LanguageRepository languages; private final TaskRepository tasks; private final TestCaseRepository testCases; private final SubmissionRepository submissions;
    public ProblemController(LanguageRepository languages, TaskRepository tasks, TestCaseRepository testCases, SubmissionRepository submissions) { this.languages=languages; this.tasks=tasks; this.testCases=testCases; this.submissions=submissions; }
    @GetMapping("/languages") public List<Map<String,String>> languages() { return languages.findAll().stream().map(l -> Map.of("code",l.getCode().name(),"name",l.getDisplayName())).toList(); }
    @GetMapping("/problems") public List<Map<String,Object>> problems(@RequestParam String language, @RequestParam(required = false) Difficulty difficulty, Authentication authentication) { ProgrammingLanguage selected=parse(language); User user=(User) authentication.getPrincipal(); Map<Long,String> statuses=new HashMap<>(); for (Submission s : submissions.findByUserIdOrderBySubmittedAtDesc(user.getId())) { Long taskId=s.getTask().getId(); if ("ACCEPTED".equals(s.getStatus()) || !statuses.containsKey(taskId)) statuses.put(taskId, s.getStatus()); } List<Task> matching=difficulty == null ? tasks.findByLanguageCode(selected) : tasks.findByLanguageCodeAndDifficultyOrderByIdAsc(selected,difficulty); return matching.stream().map(t -> { Map<String,Object> item=new LinkedHashMap<>(); item.put("id",t.getId()); item.put("title",t.getTitle()); item.put("topic",t.getTopic()); item.put("difficulty",t.getDifficulty().name()); item.put("status",statuses.getOrDefault(t.getId(),"PENDING")); return item; }).toList(); }
    @GetMapping("/problems/{id}") public Map<String,Object> problem(@PathVariable Long id) { Task t=tasks.findById(id).orElseThrow(() -> new IllegalArgumentException("Problem not found.")); List<Map<String,String>> visible=testCases.findByTaskId(id).stream().filter(c -> !c.isHidden()).map(c -> Map.of("input",c.getInput(),"expectedOutput",c.getExpectedOutput())).toList(); Map<String,Object> result=new LinkedHashMap<>(); result.put("id",t.getId()); result.put("title",t.getTitle()); result.put("topic",t.getTopic()); result.put("description",t.getDescription()); result.put("language",t.getLanguage().getCode().name()); result.put("difficulty",t.getDifficulty().name()); result.put("starterCode",t.getStarterCode()); result.put("examples",visible); return result; }
    private ProgrammingLanguage parse(String value) { try { return ProgrammingLanguage.valueOf(value.toUpperCase()); } catch(Exception e) { throw new IllegalArgumentException("Only Java and Python are supported."); } }
}
