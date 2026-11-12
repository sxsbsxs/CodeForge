package dev.codeforge;

import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

public final class EmbeddedJavaScriptRunner {
    private EmbeddedJavaScriptRunner() {}

    public static String run(String source) {
        final StringBuilder output = new StringBuilder();
        Context context = Context.enter();
        try {
            // Interpreter mode is required on Android; Rhino must not generate JVM bytecode.
            context.setOptimizationLevel(-1);
            context.setLanguageVersion(Context.VERSION_ES6);
            Scriptable scope = context.initStandardObjects();

            BaseFunction logFunction = new BaseFunction() {
                @Override
                public Object call(Context cx, Scriptable callScope, Scriptable thisObject, Object[] args) {
                    for (int i = 0; i < args.length; i++) {
                        if (i > 0) output.append(' ');
                        output.append(Context.toString(args[i]));
                    }
                    output.append('\n');
                    return Undefined.instance;
                }
            };
            logFunction.setParentScope(scope);
            logFunction.setPrototype(ScriptableObject.getFunctionPrototype(scope));

            Scriptable console = context.newObject(scope);
            ScriptableObject.putProperty(console, "log", logFunction);
            ScriptableObject.putProperty(console, "error", logFunction);
            ScriptableObject.putProperty(scope, "console", console);
            ScriptableObject.putProperty(scope, "print", logFunction);

            Object result = context.evaluateString(scope, source, "CodeForge", 1, null);
            if (result != null && result != Undefined.instance) {
                output.append(Context.toString(result)).append('\n');
            }
            return output.length() == 0 ? "Finished (no output)." : output.toString().trim();
        } catch (Throwable error) {
            return "JavaScript error: " + error.getMessage();
        } finally {
            Context.exit();
        }
    }
}
