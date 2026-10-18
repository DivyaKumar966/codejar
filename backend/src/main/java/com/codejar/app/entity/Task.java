
package com.codejar.app.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Language language;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Difficulty difficulty = Difficulty.EASY;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String topic;

    @Column(nullable = false, length = 4000)
    private String description;

    @Column(nullable = false, length = 4000)
    private String starterCode;

    protected Task() {
    }

    public Task(
            Language language,
            String title,
            String topic,
            String description,
            String starterCode) {
        this(
            language,
            title,
            topic,
            description,
            starterCode,
            Difficulty.EASY
        );
    }

    public Task(
            Language language,
            String title,
            String topic,
            String description,
            String starterCode,
            Difficulty difficulty) {
        this.language = language;
        this.title = title;
        this.topic = topic;
        this.description = description;
        this.starterCode = starterCode;
        this.difficulty = difficulty;
    }

    public Long getId() {
        return id;
    }

    public Language getLanguage() {
        return language;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public String getTitle() {
        return title;
    }

    public String getTopic() {
        return topic;
    }

    public String getDescription() {
        return description;
    }

    public String getStarterCode() {
        return starterCode;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStarterCode(String starterCode) {
        this.starterCode = starterCode;
    }
}
