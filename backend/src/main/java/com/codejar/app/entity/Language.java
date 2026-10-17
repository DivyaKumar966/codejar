package com.codejar.app.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "languages")
public class Language {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Enumerated(EnumType.STRING) @Column(nullable = false, unique = true) private ProgrammingLanguage code;
    @Column(nullable = false) private String displayName;
    protected Language() { }
    public Language(ProgrammingLanguage code, String displayName) { this.code = code; this.displayName = displayName; }
    public Long getId() { return id; }
    public ProgrammingLanguage getCode() { return code; }
    public String getDisplayName() { return displayName; }
}
