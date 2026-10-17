package com.codejar.app.controller;

import com.codejar.app.entity.*;
import com.codejar.app.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final TaskRepository tasks;
    private final TestCaseRepository testCases;
    private final LanguageRepository languages;
    private final UserRepository users;
    private final SubmissionRepository submissions;
    private final CodeDraftRepository drafts;

    public AdminController(TaskRepository tasks, TestCaseRepository testCases, LanguageRepository languages,
                           UserRepository users, SubmissionRepository submissions, CodeDraftRepository drafts) {
        this.tasks = tasks;
        this.testCases = testCases;
        this.languages = languages;
        this.users = users;
        this.submissions = submissions;
        this.drafts = drafts;
    }

    @GetMapping("/problems")
    public List<Map<String, Object>> problems() {
        return tasks.findAll().stream().map(this::problem).toList();
    }

    @PostMapping("/problems")
    public Map<String, Object> addProblem(@RequestBody Map<String, String> body) {
        return problem(tasks.save(new Task(language(body), required(body, "title"), required(body, "topic"),
                required(body, "description"), required(body, "starterCode"), difficulty(body))));
    }

    @PutMapping("/problems/{id}")
    public Map<String, Object> editProblem(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Task task = tasks.findById(id).orElseThrow(() -> new IllegalArgumentException("Problem not found."));
        task.setLanguage(language(body));
        task.setDifficulty(difficulty(body));
        task.setTitle(required(body, "title"));
        task.setTopic(required(body, "topic"));
        task.setDescription(required(body, "description"));
        task.setStarterCode(required(body, "starterCode"));
        return problem(tasks.save(task));
    }

    @DeleteMapping("/problems/{id}")
    @Transactional
    public void deleteProblem(@PathVariable Long id) {
        if (!tasks.existsById(id)) throw new IllegalArgumentException("Problem not found.");
        drafts.deleteByTaskId(id);
        submissions.deleteByTaskId(id);
        testCases.deleteAll(testCases.findByTaskId(id));
        tasks.deleteById(id);
    }

    @GetMapping("/problems/{taskId}/test-cases")
    public List<Map<String, Object>> testCases(@PathVariable Long taskId) {
        return testCases.findByTaskId(taskId).stream().map(this::testCase).toList();
    }

    @PostMapping("/problems/{taskId}/test-cases")
    public Map<String, Object> addTestCase(@PathVariable Long taskId, @RequestBody Map<String, Object> body) {
        Task task = task(taskId);
        return testCase(testCases.save(new TestCase(task, string(body, "input"), string(body, "expectedOutput"), bool(body, "hidden"))));
    }

    @PutMapping("/test-cases/{id}")
    public Map<String, Object> editTestCase(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        TestCase testCase = testCases.findById(id).orElseThrow(() -> new IllegalArgumentException("Test case not found."));
        testCase.setInput(string(body, "input"));
        testCase.setExpectedOutput(string(body, "expectedOutput"));
        testCase.setHidden(bool(body, "hidden"));
        return testCase(testCases.save(testCase));
    }

    @DeleteMapping("/test-cases/{id}")
    public void deleteTestCase(@PathVariable Long id) { testCases.deleteById(id); }

    @GetMapping("/users")
    public List<Map<String, Object>> users() {
        return users.findAll().stream().map(user -> {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("id", user.getId()); value.put("name", user.getName()); value.put("email", user.getEmail());
            value.put("role", user.getRole()); value.put("createdAt", user.getCreatedAt()); return value;
        }).toList();
    }

    @GetMapping("/submissions")
    public List<Map<String, Object>> submissions() {
        return submissions.findAll().stream().map(s -> {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("id", s.getId()); value.put("user", s.getUser().getName()); value.put("email", s.getUser().getEmail());
            value.put("problem", s.getTask().getTitle()); value.put("language", s.getLanguage().name()); value.put("status", s.getStatus());
            value.put("score", s.getScore()); value.put("passedTests", s.getPassedTests()); value.put("totalTests", s.getTotalTests());
            value.put("submittedAt", s.getSubmittedAt()); return value;
        }).toList();
    }

    private Task task(Long id) { return tasks.findById(id).orElseThrow(() -> new IllegalArgumentException("Problem not found.")); }
    private Language language(Map<String, String> body) { try { return languages.findByCode(ProgrammingLanguage.valueOf(required(body, "language").toUpperCase())).orElseThrow(() -> new IllegalArgumentException("Language not found.")); } catch (IllegalArgumentException e) { throw new IllegalArgumentException("Only Java and Python are supported."); } }
    private String required(Map<String, String> body, String key) { String value = body.get(key); if (value == null || value.isBlank()) throw new IllegalArgumentException(key + " is required."); return value; }
    private Difficulty difficulty(Map<String, String> body) { try { return Difficulty.valueOf(body.getOrDefault("difficulty", "EASY").toUpperCase()); } catch (IllegalArgumentException e) { throw new IllegalArgumentException("Difficulty must be EASY or HARD."); } }
    private String string(Map<String, Object> body, String key) { Object value = body.get(key); return value == null ? "" : value.toString(); }
    private boolean bool(Map<String, Object> body, String key) { return Boolean.parseBoolean(String.valueOf(body.getOrDefault(key, false))); }
    private Map<String, Object> problem(Task task) { Map<String, Object> value = new LinkedHashMap<>(); value.put("id", task.getId()); value.put("language", task.getLanguage().getCode().name()); value.put("difficulty", task.getDifficulty().name()); value.put("title", task.getTitle()); value.put("topic", task.getTopic()); value.put("description", task.getDescription()); value.put("starterCode", task.getStarterCode()); return value; }
    private Map<String, Object> testCase(TestCase testCase) { return Map.of("id", testCase.getId(), "taskId", testCase.getTask().getId(), "input", testCase.getInput(), "expectedOutput", testCase.getExpectedOutput(), "hidden", testCase.isHidden()); }
}
