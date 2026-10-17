package com.codejar.app.service;

import com.codejar.app.dto.RunCodeRequest;
import com.codejar.app.dto.RunCodeResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class CodeRunnerService {

    @Value("${codejar.execution.timeout-seconds:5}")
    private int timeoutSeconds = 5;

    @Value("${codejar.execution.java-command:java}")
    private String javaCommand = "java";

    @Value("${codejar.execution.javac-command:javac}")
    private String javacCommand = "javac";

    @Value("${codejar.execution.python-command:python}")
    private String pythonCommand = "python";

    public RunCodeResponse run(RunCodeRequest request) {

        String language = request.language()
                .toLowerCase(Locale.ROOT)
                .trim();

        LanguageSpec spec = switch (language) {
            case "java" ->
                    new LanguageSpec("java", "Main.java", "Main", true);

            case "python" ->
                    new LanguageSpec("python", "main.py", "main", false);

            default ->
                    throw new IllegalArgumentException(
                            "Only Java and Python are supported."
                    );
        };

        Path workspace = null;
        long startedAt = System.nanoTime();

        System.out.println(
                "RUN REQUEST RECEIVED language=" + language
                        + " inputPresent="
                        + (request.input() != null
                        && !request.input().isBlank())
        );

        try {

            // Create temporary execution directory
            workspace = Files.createTempDirectory(
                    "codejar-execution-"
            );

            // Create source file
            Path sourceFile =
                    workspace.resolve(spec.fileName());

            Files.writeString(
                    sourceFile,
                    request.code(),
                    StandardCharsets.UTF_8
            );

            // =====================================================
            // JAVA
            // =====================================================

            if (spec.isJava()) {

                String resolvedJavac =
                        resolveExecutable(javacCommand, "javac");

                if (resolvedJavac == null
                        || !commandExists(resolvedJavac)) {

                    return new RunCodeResponse(
                            "",
                            "Java compiler is not available on this system.",
                            -1,
                            elapsedMillis(startedAt),
                            false
                    );
                }

                // Compile
                ProcessExecutionResult compileResult =
                        executeProcess(
                                List.of(
                                        resolvedJavac,
                                        sourceFile.getFileName().toString()
                                ),
                                null,
                                workspace,
                                timeoutSeconds
                        );

                System.out.println(
                        "COMPILE EXIT="
                                + compileResult.exitCode()
                                + " TIMEOUT="
                                + compileResult.timedOut()
                );

                if (compileResult.timedOut()) {

                    return new RunCodeResponse(
                            "",
                            "Compilation timed out.",
                            -1,
                            elapsedMillis(startedAt),
                            true
                    );
                }

                if (compileResult.exitCode() != 0) {

                    String compileMessage =
                            (compileResult.stderr()
                                    + compileResult.stdout())
                                    .trim();

                    return new RunCodeResponse(
                            "",
                            "Compilation Error\n"
                                    + compileMessage,
                            compileResult.exitCode(),
                            elapsedMillis(startedAt),
                            false
                    );
                }

                // Check Java runtime
                String resolvedJava =
                        resolveExecutable(javaCommand, "java");

                if (resolvedJava == null
                        || !commandExists(resolvedJava)) {

                    return new RunCodeResponse(
                            "",
                            "Java runtime is not available on this system.",
                            -1,
                            elapsedMillis(startedAt),
                            false
                    );
                }

                // Run Java program
                ProcessExecutionResult runResult =
                        executeProcess(
                                List.of(
                                        resolvedJava,
                                        "Main"
                                ),
                                request.input(),
                                workspace,
                                timeoutSeconds
                        );

                System.out.println(
                        "JAVA RUN EXIT="
                                + runResult.exitCode()
                                + " TIMEOUT="
                                + runResult.timedOut()
                );

                if (runResult.timedOut()) {

                    return new RunCodeResponse(
                            "",
                            "Execution timed out.",
                            -1,
                            elapsedMillis(startedAt),
                            true
                    );
                }

                if (runResult.exitCode() != 0) {

                    String message =
                            (runResult.stderr()
                                    + runResult.stdout())
                                    .trim();

                    return new RunCodeResponse(
                            runResult.stdout(),
                            "Runtime Error\n" + message,
                            runResult.exitCode(),
                            elapsedMillis(startedAt),
                            false
                    );
                }

                System.out.println(
                        "RESPONSE RETURNED for Java outputLength="
                                + runResult.stdout().length()
                );

                return new RunCodeResponse(
                        runResult.stdout(),
                        "",
                        runResult.exitCode(),
                        elapsedMillis(startedAt),
                        false
                );
            }

            // =====================================================
            // PYTHON
            // =====================================================

            String resolvedPython =
                    resolveExecutable(
                            pythonCommand,
                            "python",
                            "python3",
                            "py"
                    );

            if (resolvedPython == null
                    || !commandExists(resolvedPython)) {

                return new RunCodeResponse(
                        "",
                        "Python interpreter is not available on this system.",
                        -1,
                        elapsedMillis(startedAt),
                        false
                );
            }

            ProcessExecutionResult result =
                    executeProcess(
                            List.of(
                                    resolvedPython,
                                    sourceFile.getFileName().toString()
                            ),
                            request.input(),
                            workspace,
                            timeoutSeconds
                    );

            System.out.println(
                    "PYTHON EXIT="
                            + result.exitCode()
                            + " TIMEOUT="
                            + result.timedOut()
            );

            if (result.timedOut()) {

                return new RunCodeResponse(
                        "",
                        "Execution timed out.",
                        -1,
                        elapsedMillis(startedAt),
                        true
                );
            }

            if (result.exitCode() != 0) {

                String message =
                        (result.stderr()
                                + result.stdout())
                                .trim();

                return new RunCodeResponse(
                        result.stdout(),
                        "Runtime Error\n" + message,
                        result.exitCode(),
                        elapsedMillis(startedAt),
                        false
                );
            }

            System.out.println(
                    "RESPONSE RETURNED for Python outputLength="
                            + result.stdout().length()
            );

            return new RunCodeResponse(
                    result.stdout(),
                    "",
                    result.exitCode(),
                    elapsedMillis(startedAt),
                    false
            );

        } catch (IOException e) {

            return new RunCodeResponse(
                    "",
                    "Unable to execute the program locally. "
                            + "Please check the Java or Python runtime configuration.",
                    -1,
                    elapsedMillis(startedAt),
                    false
            );

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            return new RunCodeResponse(
                    "",
                    "Execution interrupted.",
                    -1,
                    elapsedMillis(startedAt),
                    false
            );

        } finally {

            if (workspace != null) {
                deleteWorkspace(workspace);
            }
        }
    }

    // =============================================================
    // PROCESS EXECUTION
    // =============================================================

    private ProcessExecutionResult executeProcess(
            List<String> command,
            String input,
            Path directory,
            int timeoutSeconds
    ) throws IOException, InterruptedException {

        System.out.println(
                "PROCESS STARTED: "
                        + String.join(" ", command)
        );

        Process process =
                new ProcessBuilder(command)
                        .directory(directory.toFile())
                        .redirectErrorStream(false)
                        .start();

        // ---------------------------------------------------------
        // IMPORTANT:
        // Always close stdin, even when input is empty.
        // Programs using Scanner/System.in otherwise keep waiting.
        // ---------------------------------------------------------

        try (var stdin = process.getOutputStream()) {

            if (input != null && !input.isBlank()) {

                System.out.println("INPUT SENT");

                stdin.write(
                        input.getBytes(StandardCharsets.UTF_8)
                );

                stdin.flush();
            }
        }

        System.out.println("INPUT CLOSED");

        // Read stdout/stderr concurrently
        CompletableFuture<String> stdoutFuture =
                CompletableFuture.supplyAsync(
                        () -> readStream(
                                process.getInputStream()
                        )
                );

        CompletableFuture<String> stderrFuture =
                CompletableFuture.supplyAsync(
                        () -> readStream(
                                process.getErrorStream()
                        )
                );

        // Wait for process with timeout
        boolean finished =
                process.waitFor(
                        timeoutSeconds,
                        TimeUnit.SECONDS
                );

        // ---------------------------------------------------------
        // TIMEOUT
        // ---------------------------------------------------------

        if (!finished) {

            System.out.println("PROCESS TIMEOUT");

            process.destroy();

            if (!process.waitFor(1, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }

            return new ProcessExecutionResult(
                    "",
                    "Execution timed out.",
                    -1,
                    true
            );
        }

        String stdout;
        String stderr;

        // ---------------------------------------------------------
        // Read captured output
        // ---------------------------------------------------------

        try {

            stdout = stdoutFuture.get(
                    2,
                    TimeUnit.SECONDS
            );

            stderr = stderrFuture.get(
                    2,
                    TimeUnit.SECONDS
            );

        } catch (ExecutionException | TimeoutException e) {

            process.destroyForcibly();

            return new ProcessExecutionResult(
                    "",
                    "Unable to read program output.",
                    -1,
                    false
            );
        }

        System.out.println(
                "STDOUT RECEIVED bytes="
                        + stdout.length()
                        + " STDERR RECEIVED bytes="
                        + stderr.length()
        );

        int exitCode = process.exitValue();

        System.out.println(
                "PROCESS EXIT CODE=" + exitCode
        );

        return new ProcessExecutionResult(
                stdout,
                stderr,
                exitCode,
                false
        );
    }

    // =============================================================
    // STREAM READER
    // =============================================================

    private String readStream(
            java.io.InputStream stream
    ) {

        try {

            return new String(
                    stream.readAllBytes(),
                    StandardCharsets.UTF_8
            );

        } catch (IOException e) {

            return "";
        }
    }

    // =============================================================
    // EXECUTABLE RESOLUTION
    // =============================================================

    private String resolveExecutable(
            String configuredCommand,
            String... fallbackCommands
    ) {

        List<String> candidates =
                new ArrayList<>();

        if (configuredCommand != null
                && !configuredCommand.isBlank()) {

            candidates.add(configuredCommand);
        }

        for (String fallback : fallbackCommands) {

            if (fallback != null
                    && !fallback.isBlank()
                    && !candidates.contains(fallback)) {

                candidates.add(fallback);
            }
        }

        for (String candidate : candidates) {

            if (commandExists(candidate)) {
                return candidate;
            }
        }

        return candidates.isEmpty()
                ? null
                : candidates.get(0);
    }

    // =============================================================
    // CHECK COMMAND
    // =============================================================

    private boolean commandExists(
            String command
    ) {

        if (command == null
                || command.isBlank()) {

            return false;
        }

        List<String> versionArgs =
                switch (command.toLowerCase(Locale.ROOT)) {

                    case "java" ->
                            List.of(command, "-version");

                    case "javac" ->
                            List.of(command, "-version");

                    case "python", "python3" ->
                            List.of(command, "--version");

                    case "py" ->
                            List.of(command, "-V");

                    default ->
                            List.of(command, "--version");
                };

        try {

            Process process =
                    new ProcessBuilder(versionArgs)
                            .redirectErrorStream(true)
                            .start();

            boolean completed =
                    process.waitFor(
                            10,
                            TimeUnit.SECONDS
                    );

            if (!completed) {

                process.destroyForcibly();

                return false;
            }

            return process.exitValue() == 0;

        } catch (IOException | InterruptedException e) {

            return false;
        }
    }

    // =============================================================
    // EXECUTION TIME
    // =============================================================

    private long elapsedMillis(
            long startedAt
    ) {

        return TimeUnit.NANOSECONDS.toMillis(
                System.nanoTime() - startedAt
        );
    }

    // =============================================================
    // DELETE TEMP WORKSPACE
    // =============================================================

    private void deleteWorkspace(
            Path workspace
    ) {

        try (var paths = Files.walk(workspace)) {

            paths
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {

                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }

                    });

        } catch (IOException ignored) {
        }
    }

    // =============================================================
    // RECORDS
    // =============================================================

    private record ProcessExecutionResult(
            String stdout,
            String stderr,
            int exitCode,
            boolean timedOut
    ) {
    }

    private record LanguageSpec(
            String language,
            String fileName,
            String className,
            boolean java
    ) {

        boolean isJava() {
            return java;
        }
    }
}