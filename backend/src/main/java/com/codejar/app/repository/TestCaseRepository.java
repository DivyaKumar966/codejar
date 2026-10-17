package com.codejar.app.repository;
import com.codejar.app.entity.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface TestCaseRepository extends JpaRepository<TestCase, Long> { List<TestCase> findByTaskId(Long taskId); }
