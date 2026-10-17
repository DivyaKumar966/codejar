package com.codejar.app.repository;
import com.codejar.app.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface TaskRepository extends JpaRepository<Task, Long> {
	List<Task> findByLanguageCode(ProgrammingLanguage language);
	List<Task> findByLanguageCodeAndDifficulty(ProgrammingLanguage language, Difficulty difficulty);
	List<Task> findByLanguageCodeAndDifficultyOrderByIdAsc(ProgrammingLanguage language, Difficulty difficulty);
	List<Task> findByDifficultyIsNull();
	boolean existsByLanguageCodeAndTitle(ProgrammingLanguage language, String title);
	long countByLanguageCode(ProgrammingLanguage language);
	long countByLanguageCodeAndDifficulty(ProgrammingLanguage language, Difficulty difficulty);
}
