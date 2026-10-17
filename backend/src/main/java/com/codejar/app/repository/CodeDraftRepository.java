package com.codejar.app.repository;

import com.codejar.app.entity.CodeDraft;
import com.codejar.app.entity.Task;
import com.codejar.app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CodeDraftRepository extends JpaRepository<CodeDraft, Long> {
    Optional<CodeDraft> findByUserAndTask(User user, Task task);
    void deleteByUserIdAndTaskId(Long userId, Long taskId);
    void deleteByTaskId(Long taskId);
}
