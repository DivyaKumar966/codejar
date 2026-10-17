package com.codejar.app.service;

import com.codejar.app.dto.*;
import com.codejar.app.entity.*;
import com.codejar.app.repository.*;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class SubmissionService {
    private final TaskRepository tasks; private final TestCaseRepository testCases; private final SubmissionRepository submissions; private final CodeRunnerService runner;
    public SubmissionService(TaskRepository tasks, TestCaseRepository testCases, SubmissionRepository submissions, CodeRunnerService runner) { this.tasks=tasks; this.testCases=testCases; this.submissions=submissions; this.runner=runner; }
    public SubmissionResponse submit(User user, SubmitCodeRequest request) {
        ProgrammingLanguage language = parseLanguage(request.language());
        Task task = tasks.findById(request.taskId()).orElseThrow(() -> new IllegalArgumentException("Problem not found."));
        if (task.getLanguage().getCode() != language) throw new IllegalArgumentException("This problem is not available for the selected language.");

        Optional<Submission> existingCompleted = submissions.findTopByUserIdAndTaskIdAndStatusAndScoreGreaterThanEqualOrderBySubmittedAtDesc(user.getId(), task.getId(), "ACCEPTED", 100.0);
        if (existingCompleted.isPresent()) {
            Submission completed = existingCompleted.get();
            return toResponse(submissions.save(new Submission(user, task, language, request.code(), completed.getStatus(), completed.getScore(), completed.getPassedTests(), completed.getTotalTests(), completed.getExecutionTime())));
        }

        List<TestCase> cases = testCases.findByTaskId(task.getId()); int passed=0; long maxTime=0; String status="ACCEPTED";
        for (TestCase testCase : cases) {
            RunCodeResponse result=runner.run(new RunCodeRequest(language.name().toLowerCase(), request.code(), testCase.getInput())); maxTime=Math.max(maxTime,result.executionTime());
            if (result.timedOut()) status="TIME_LIMIT_EXCEEDED";
            else if (result.exitCode()!=0) status="RUNTIME_OR_COMPILATION_ERROR";
            if (!result.timedOut() && result.exitCode()==0 && normalize(result.output()).equals(normalize(testCase.getExpectedOutput()))) passed++;
        }
        double score = calculateScore(passed, cases.size());
        if (passed != cases.size() && status.equals("ACCEPTED")) status="WRONG_ANSWER";
        Submission submission=submissions.save(new Submission(user,task,language,request.code(),status,score,passed,cases.size(),maxTime));
        return toResponse(submission);
    }
    public List<SubmissionResponse> history(User user) { return submissions.findByUserIdOrderBySubmittedAtDesc(user.getId()).stream().map(this::toResponse).toList(); }
    private ProgrammingLanguage parseLanguage(String value) { try { return ProgrammingLanguage.valueOf(value.trim().toUpperCase()); } catch(Exception e) { throw new IllegalArgumentException("Only Java and Python are supported."); } }
    private String normalize(String value) { return value.replace("\r\n", "\n").trim(); }
    private double calculateScore(int passedTestCases, int totalTestCases) {
        return totalTestCases == 0 ? 0.0 : (passedTestCases * 100.0) / totalTestCases;
    }
    private SubmissionResponse toResponse(Submission s) { return new SubmissionResponse(s.getId(),s.getTask().getTitle(),s.getLanguage().name(),s.getSourceCode(),s.getStatus(),calculateScore(s.getPassedTests(),s.getTotalTests()),s.getPassedTests(),s.getTotalTests(),s.getExecutionTime(),s.getSubmittedAt()); }
}
