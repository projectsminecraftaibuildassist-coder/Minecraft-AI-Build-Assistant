package com.example.ai.script;

import com.example.ai.api.MinecraftAI;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.MethodPointerExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.stmt.SynchronizedStatement;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.SecureASTCustomizer;
import org.codehaus.groovy.runtime.MethodClosure;

import java.io.File;
import java.math.BigDecimal;
import java.net.Socket;
import java.net.URL;
import java.util.Collections;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Builds a GroovyShell locked down with {@link SecureASTCustomizer}.
 *
 * <p><b>Limitations of SecureASTCustomizer</b> (documented by the Groovy project):
 * <ul>
 *   <li>Groovy is dynamic; AST checks cannot catch every runtime escape (e.g. crafted metaClass usage).</li>
 *   <li>Disallowed-receiver lists can be bypassed via {@code MethodPointerExpression} unless explicitly blocked.</li>
 *   <li>This is a strong compile-time filter, not a JVM SecurityManager sandbox.</li>
 *   <li>We combine it with {@link ScriptSecurityValidator} (pre-parse) and a minimal binding surface.</li>
 * </ul>
 *
 * <p>Test checklist (expected results):
 * <ul>
 *   <li>Allowed: ai.placeBlock(...), for loops, if statements, local variables</li>
 *   <li>Rejected: System.getenv(), Runtime.getRuntime().exec(...), new File(...),
 *       new Socket(...), Class.forName(...), new Thread(...), import java.io.File,
 *       class Evil {}</li>
 * </ul>
 */
public final class SafeGroovyShellFactory {

    private static final Set<String> FORBIDDEN_CONSTRUCTOR_TYPES = Set.of(
        File.class.getName(),
        Socket.class.getName(),
        URL.class.getName(),
        ProcessBuilder.class.getName(),
        Thread.class.getName(),
        Runtime.class.getName(),
        GroovyShell.class.getName(),
        Binding.class.getName(),
        Class.class.getName()
    );

    private static final Set<String> FORBIDDEN_METHOD_NAMES = Set.of(
        "getenv",
        "getproperties",
        "getruntime",
        "exec",
        "forname",
        "getbinding",
        "evaluate",
        "run",
        "exit"
    );

    private SafeGroovyShellFactory() {
    }

    public static GroovyShell createShell(Binding binding) {
        CompilerConfiguration configuration = new CompilerConfiguration();
        configuration.addCompilationCustomizers(buildSecureAstCustomizer());
        return new GroovyShell(binding, configuration);
    }

    private static SecureASTCustomizer buildSecureAstCustomizer() {
        SecureASTCustomizer secure = new SecureASTCustomizer();

        // Groovy 3.0.x: deny imports by using empty whitelists (there is no setImportsAllowed flag).
        secure.setAllowedImports(Collections.emptyList());
        secure.setAllowedStaticImports(Collections.emptyList());
        secure.setAllowedStarImports(Collections.emptyList());
        secure.setAllowedStaticStarImports(Collections.emptyList());
        secure.setIndirectImportCheckEnabled(true);

        // Scripts must be a single compilation unit without package statements.
        secure.setPackageAllowed(false);

        // Prevent user-defined methods (def foo() { ... }) inside the script.
        secure.setMethodDefinitionAllowed(false);

        // Closures enable meta-programming escapes; building scripts only need loops/ifs.
        secure.setClosuresAllowed(false);

        // Block synchronized blocks at the statement level.
        secure.setStatementsBlacklist(List.of(SynchronizedStatement.class));

        // Block method pointers at the expression level (known SecureASTCustomizer bypass vector).
        secure.setExpressionsBlacklist(List.of(MethodPointerExpression.class));

        // Blacklist dangerous packages even if an import slips through indirect resolution.
        secure.setImportsBlacklist(Arrays.asList(
            "java.io",
            "java.net",
            "java.nio",
            "java.lang.reflect",
            "java.lang.System",
            "java.lang.Runtime",
            "java.lang.ProcessBuilder",
            "java.lang.Thread",
            "java.lang.Class",
            "groovy.lang.GroovyShell",
            "groovy.lang.Binding",
            "groovy.util.Eval"
        ));

        // Only allow method calls on the Minecraft AI surface and plain data types.
        secure.setReceiversClassesWhiteList(List.of(
            AiScriptFacade.class,
            MethodClosure.class,
            MinecraftAI.class,
            String.class,
            Integer.class,
            Long.class,
            Double.class,
            Float.class,
            Boolean.class,
            BigDecimal.class,
            Number.class,
            Math.class,
            groovy.lang.IntRange.class,
            groovy.lang.ObjectRange.class
        ));

        // Literals used in loops and coordinates.
        secure.setConstantTypesClassesWhiteList(List.of(
            String.class,
            Integer.class,
            Long.class,
            Double.class,
            Float.class,
            Boolean.class,
            BigDecimal.class
        ));

        secure.addExpressionCheckers(SafeGroovyShellFactory::isExpressionAuthorized);
        return secure;
    }

    /**
     * @return {@code true} when the expression may remain in the script AST
     */
    private static boolean isExpressionAuthorized(Expression expression) {
        if (expression instanceof MethodPointerExpression) {
            return false;
        }

        if (expression instanceof ConstructorCallExpression constructorCall) {
            ClassNode type = constructorCall.getType();
            if (type != null && isForbiddenConstructor(type.getName())) {
                return false;
            }
        }

        if (expression instanceof MethodCallExpression methodCall) {
            String methodName = methodCall.getMethodAsString();
            if (methodName != null && isForbiddenMethod(methodName)) {
                return false;
            }
        }

        if (expression instanceof PropertyExpression propertyExpression) {
            String propertyName = propertyExpression.getPropertyAsString();
            if (propertyName != null && isForbiddenProperty(propertyName)) {
                return false;
            }
        }

        return true;
    }

    private static boolean isForbiddenConstructor(String typeName) {
        if (typeName == null) {
            return false;
        }
        String normalized = typeName.toLowerCase(Locale.ROOT);
        if (FORBIDDEN_CONSTRUCTOR_TYPES.contains(typeName)) {
            return true;
        }
        return normalized.startsWith("java.io.")
            || normalized.startsWith("java.net.")
            || normalized.startsWith("java.nio.")
            || normalized.startsWith("java.lang.reflect.");
    }

    private static boolean isForbiddenMethod(String methodName) {
        return FORBIDDEN_METHOD_NAMES.contains(methodName.toLowerCase(Locale.ROOT));
    }

    private static boolean isForbiddenProperty(String propertyName) {
        String normalized = propertyName.toLowerCase(Locale.ROOT);
        return normalized.equals("binding")
            || normalized.equals("metaClass")
            || normalized.equals("class");
    }
}
