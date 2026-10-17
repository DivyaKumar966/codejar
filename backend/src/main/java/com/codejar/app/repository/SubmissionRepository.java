package com.codejar.app.repository;
import com.codejar.app.entity.Submission;
import com.codejar.app.entity.ProgrammingLanguage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface SubmissionRepository extends JpaRepository<Submission, Long> {
 List<Submission> findByUserIdOrderBySubmittedAtDesc(Long userId);
 void deleteByTaskId(Long taskId);
 Optional<Submission> findTopByUserIdAndTaskIdAndStatusAndScoreGreaterThanEqualOrderBySubmittedAtDesc(Long userId, Long taskId, String status, double score);
 @Query("select count(distinct s.task.id) from Submission s where s.user.id = :userId and s.task.language.code = :language and s.status = 'ACCEPTED'")
 long countCompletedTasks(@Param("userId") Long userId, @Param("language") ProgrammingLanguage language);
 @Query("select count(distinct s.task.id) from Submission s where s.user.id = :userId and s.status = 'ACCEPTED'")
 long countCompletedTasks(@Param("userId") Long userId);
 @Query("select count(s) from Submission s where s.user.id = :userId")
 long countByUserId(@Param("userId") Long userId);
}
