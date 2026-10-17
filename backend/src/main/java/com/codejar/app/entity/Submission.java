package com.codejar.app.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "submissions")
public class Submission {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "user_id") private User user;
    @ManyToOne(optional = false) @JoinColumn(name = "task_id") private Task task;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ProgrammingLanguage language;
    @Column(nullable = false, length = 20000) private String sourceCode;
    @Column(nullable = false) private String status;
    @Column(nullable = false) private double score;
    @Column(nullable = false) private int passedTests;
    @Column(nullable = false) private int totalTests;
    @Column(nullable = false) private long executionTime;
    @Column(nullable = false) private Instant submittedAt = Instant.now();
    protected Submission() { }
    public Submission(User user, Task task, ProgrammingLanguage language, String sourceCode, String status, double score, int passedTests, int totalTests, long executionTime) { this.user=user; this.task=task; this.language=language; this.sourceCode=sourceCode; this.status=status; this.score=score; this.passedTests=passedTests; this.totalTests=totalTests; this.executionTime=executionTime; }
    public Long getId() { return id; } public User getUser() { return user; } public Task getTask() { return task; } public ProgrammingLanguage getLanguage() { return language; } public String getSourceCode() { return sourceCode; } public String getStatus() { return status; } public double getScore() { return score; } public int getPassedTests() { return passedTests; } public int getTotalTests() { return totalTests; } public long getExecutionTime() { return executionTime; } public Instant getSubmittedAt() { return submittedAt; }
}
