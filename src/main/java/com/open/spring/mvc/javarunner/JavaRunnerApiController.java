package com.open.spring.mvc.javarunner;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

@RestController
@RequestMapping("/run")
@CrossOrigin(origins = {"http://127.0.0.1:4500","https://pages.opencodingsociety.com"}, allowCredentials = "true")
public class JavaRunnerApiController {

    @PostMapping("/java")
    public ResponseEntity<Map<String, String>> runJava(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        if (code == null || code.trim().isEmpty()) {
            return new ResponseEntity<>(Map.of("output", "⚠️ No code provided."), HttpStatus.BAD_REQUEST);
        }

        try {
            // Create a temporary directory and file
            Path tempDir = Files.createTempDirectory("java-run-");
            Path javaFile = tempDir.resolve("Main.java");
            Files.writeString(javaFile, code);

            // Step 1: Compile Java code
            Process compileProcess = new ProcessBuilder("javac", javaFile.toString())
                    .directory(tempDir.toFile())
                    .redirectErrorStream(true)
                    .start();

            String compileOutput = new String(compileProcess.getInputStream().readAllBytes());
            compileProcess.waitFor();

            if (compileProcess.exitValue() != 0) {
                cleanup(tempDir);
                return new ResponseEntity<>(Map.of("output", "❌ Compilation error:\n" + compileOutput), HttpStatus.OK);
            }

            // Step 2: Run compiled Java code
            Process runProcess = new ProcessBuilder("java", "-cp", tempDir.toString(), "Main")
                    .redirectErrorStream(true)
                    .start();

            boolean finished = runProcess.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            String runOutput = new String(runProcess.getInputStream().readAllBytes());

            if (!finished) {
                runProcess.destroyForcibly();
                cleanup(tempDir);
                return new ResponseEntity<>(Map.of("output", "⏱️ Execution timed out (5 s limit)."), HttpStatus.OK);
            }

            cleanup(tempDir);
            return new ResponseEntity<>(Map.of("output", runOutput), HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>(Map.of("output", "⚠️ Error running code: " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Helper: safely delete temporary files
    private void cleanup(Path dir) {
        try {
            Files.walk(dir)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try { Files.delete(path); } catch (IOException ignored) {}
                });
        } catch (IOException ignored) {}
    }
}
