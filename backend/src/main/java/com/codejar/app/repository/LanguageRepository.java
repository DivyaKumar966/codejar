package com.codejar.app.repository;
import com.codejar.app.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface LanguageRepository extends JpaRepository<Language, Long> { Optional<Language> findByCode(ProgrammingLanguage code); }
