package com.codejar.app.config;

import com.codejar.app.entity.*;
import com.codejar.app.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner seedLanguages(LanguageRepository languages, TaskRepository tasks, TestCaseRepository testCases) {
        return args -> {
            Language java = languages.findByCode(ProgrammingLanguage.JAVA)
                    .orElseGet(() -> languages.save(new Language(ProgrammingLanguage.JAVA, "Java")));
            Language python = languages.findByCode(ProgrammingLanguage.PYTHON)
                    .orElseGet(() -> languages.save(new Language(ProgrammingLanguage.PYTHON, "Python")));
            tasks.findByDifficultyIsNull().forEach(task -> {
                task.setDifficulty(legacyDifficulty(task));
                tasks.save(task);
            });
            if (tasks.count() == 0) {
                Task javaTask = tasks.save(new Task(java, "Sum of Two Numbers", "Basics",
                        "Read two integers from standard input and print their sum.",
                        "public class Main {\n    public static void main(String[] args) {\n        // Write your solution here\n    }\n}"));
                testCases.save(new TestCase(javaTask, "10\n20\n", "30", false));
                testCases.save(new TestCase(javaTask, "-5\n8\n", "3", true));
                Task pythonTask = tasks.save(new Task(python, "Sum of Two Numbers", "Basics",
                        "Read two integers from standard input and print their sum.",
                        "a = int(input())\nb = int(input())\nprint(a + b)"));
                testCases.save(new TestCase(pythonTask, "10\n20\n", "30", false));
                testCases.save(new TestCase(pythonTask, "-5\n8\n", "3", true));
            }
            seedEasy(tasks, testCases, java, ProgrammingLanguage.JAVA);
            seedEasy(tasks, testCases, python, ProgrammingLanguage.PYTHON);
            seedHard(tasks, testCases, java, ProgrammingLanguage.JAVA);
            seedHard(tasks, testCases, python, ProgrammingLanguage.PYTHON);
        };
    }

    private void seedEasy(TaskRepository tasks, TestCaseRepository testCases, Language language,
            ProgrammingLanguage code) {
        if (hasFiveUnique(tasks, code, Difficulty.EASY))
            return;
        String[] titles = { "Sum of Two Numbers", "Double a Number", "Square a Number", "Positive or Negative",
                "Print a Greeting" };
        String[] descriptions = { "Read two integers and print their sum.",
                "Read an integer and print twice its value.", "Read an integer and print its square.",
                "Read an integer. Print Positive when it is zero or greater, otherwise print Negative.",
                "Read a name and print Hello followed by the name." };
        String[] inputs = { "10\n20\n", "7\n", "6\n", "-2\n", "CodeJar\n" };
        String[] outputs = { "30", "14", "36", "Negative", "Hello CodeJar" };
        for (int i = 0; i < titles.length && !hasFiveUnique(tasks, code, Difficulty.EASY); i++) {
            if (tasks.existsByLanguageCodeAndTitle(code, titles[i]))
                continue;
            String starter = code == ProgrammingLanguage.JAVA
                    ? "public class Main {\n    public static void main(String[] args) {\n        // Write your solution here\n    }\n}"
                    : "# Write your solution here";
            Task task = tasks.save(new Task(language, titles[i], "Basics", descriptions[i], starter, Difficulty.EASY));
            testCases.save(new TestCase(task, inputs[i], outputs[i], false));
        }
    }

    private void seedHard(TaskRepository tasks, TestCaseRepository testCases, Language language,
            ProgrammingLanguage code) {
        String[] titles = { "Electricity Bill Calculator", "Student Grade Result System", "ATM Transaction System",
                "Employee Salary Calculator", "Shopping Discount Calculator" };
        String[] descriptions = {
                "Read electricity units. The first 100 units cost 2 each and remaining units cost 5 each. Print the bill.",
                "Read marks for three subjects. Print Pass when every mark is at least 35, otherwise print Fail.",
                "Read account balance and withdrawal amount. Print the remaining balance when funds are sufficient, otherwise print Insufficient Funds.",
                "Read basic salary and years of service. Add a 10 percent bonus for 5 or more years, otherwise add 5 percent. Print final salary.",
                "Read bill amount. Apply 10 percent discount for bills of 1000 or more, otherwise print the original amount." };
        String[] inputs = { "120\n", "40\n50\n60\n", "1000\n300\n", "10000\n5\n", "1200\n" };
        String[] outputs = { "300", "Pass", "700", "11000", "1080" };
        for (int i = 0; i < titles.length && !hasFiveUnique(tasks, code, Difficulty.HARD); i++) {
            if (tasks.existsByLanguageCodeAndTitle(code, titles[i]))
                continue;
            String starter = code == ProgrammingLanguage.JAVA
                    ? "public class Main {\n    public static void main(String[] args) {\n        // Write your solution here\n    }\n}"
                    : "# Write your solution here";
            Task task = tasks
                    .save(new Task(language, titles[i], "Scenario", descriptions[i], starter, Difficulty.HARD));
            testCases.save(new TestCase(task, inputs[i], outputs[i], false));
        }
    }

    private Difficulty legacyDifficulty(Task task) {
        String title = (task.getTitle() + " " + task.getTopic()).toLowerCase();
        return title.contains("bill") || title.contains("grade") || title.contains("result") || title.contains("atm")
                || title.contains("salary") || title.contains("discount") || title.contains("parking")
                || title.contains("library") || title.contains("delivery") ? Difficulty.HARD : Difficulty.EASY;
    }

    private boolean hasFiveUnique(TaskRepository tasks, ProgrammingLanguage code, Difficulty difficulty) {
        return tasks.findByLanguageCodeAndDifficulty(code, difficulty).stream().map(Task::getTitle).distinct()
                .count() >= 5;
    }

    @Bean
    CommandLineRunner seedAdmin(UserRepository users, PasswordEncoder encoder,
            @Value("${codejar.admin.email:}") String adminEmail,
            @Value("${codejar.admin.password:}") String adminPassword) {
        return args -> {
            users.findAll().forEach(user -> {
                if (!"ADMIN".equalsIgnoreCase(user.getRole()))
                    user.setRole("USER");
                else
                    user.setRole("ADMIN");
                users.save(user);
            });
            if (adminEmail.isBlank() || adminPassword.isBlank() || users.findByRole("ADMIN").isPresent())
                return;
            User admin = users.findByEmail(adminEmail).orElseGet(User::new);
            if (admin.getId() == null) {
                admin.setName("CodeJar Administrator");
                admin.setEmail(adminEmail);
                admin.setPassword(encoder.encode(adminPassword));
            }
            admin.setRole("ADMIN");
            users.save(admin);
        };
    }
}
