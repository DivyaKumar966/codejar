package com.codejar.app.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "test_cases")
public class TestCase {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) private Task task;
    @Column(nullable = false, length = 4000) private String input;
    @Column(nullable = false, length = 4000) private String expectedOutput;
    @Column(nullable = false) private boolean hidden;
    protected TestCase() { }
    public TestCase(Task task, String input, String expectedOutput, boolean hidden) { this.task=task; this.input=input; this.expectedOutput=expectedOutput; this.hidden=hidden; }
    public Long getId() { return id; } public Task getTask() { return task; } public String getInput() { return input; } public String getExpectedOutput() { return expectedOutput; } public boolean isHidden() { return hidden; }
    public void setTask(Task task) { this.task = task; } public void setInput(String input) { this.input = input; } public void setExpectedOutput(String expectedOutput) { this.expectedOutput = expectedOutput; } public void setHidden(boolean hidden) { this.hidden = hidden; }
}
