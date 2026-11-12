package dev.codeforge;

import bsh.EvalError;
import bsh.Interpreter;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public final class EmbeddedJavaRunner {
    private EmbeddedJavaRunner() {}

    /**
     * Runs Java statements with BeanShell. This is an interpreter, not javac:
     * complete Android apps, annotation processing and all JDK APIs are not supported.
     */
    public static String run(String source) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (PrintStream stream = new PrintStream(buffer, true, StandardCharsets.UTF_8.name())) {
            Interpreter interpreter = new Interpreter();
            interpreter.setOut(stream);
            interpreter.setErr(stream);
            Object result = interpreter.eval(source);
            stream.flush();
            String printed = buffer.toString(StandardCharsets.UTF_8.name()).trim();
            if (!printed.isEmpty()) return printed;
            return result == null ? "Finished (no output)." : String.valueOf(result);
        } catch (EvalError error) {
            return "Java interpreter error: " + error.getMessage();
        } catch (Exception error) {
            return "Java runner error: " + error.getMessage();
        }
    }
}
