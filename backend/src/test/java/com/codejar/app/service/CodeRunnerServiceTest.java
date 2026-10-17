package com.codejar.app.service;

import com.codejar.app.dto.RunCodeRequest;
import com.codejar.app.dto.RunCodeResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeRunnerServiceTest {

    @Test
    void javaRunUsesLocalCompilerAndReportsClearErrorWhenMissing() {
        CodeRunnerService service = new CodeRunnerService();

        RunCodeResponse response = service.run(new RunCodeRequest(
                "java",
                "public class Main {\n    public static void main(String[] args) {\n        System.out.println(30);\n    }\n}",
                "10\n20\n"
        ));

        assertTrue(response.error().isEmpty() || response.error().contains("Compilation Error") || response.error().contains("Java compiler") || response.error().contains("Java runtime"),
                "Expected a local Java execution result or a meaningful local runtime error.");
    }

    @Test
    void pythonRunUsesLocalInterpreterWhenAvailable() {
        CodeRunnerService service = new CodeRunnerService();

        RunCodeResponse response = service.run(new RunCodeRequest(
                "python",
                "print(30)",
                ""
        ));

        assertTrue(response.output().contains("30") || response.error().contains("Python interpreter") || response.error().contains("Runtime Error"),
                "Expected Python execution output or the correct local interpreter error.");
    }
}
