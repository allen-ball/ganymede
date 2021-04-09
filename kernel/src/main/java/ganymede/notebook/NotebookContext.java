package ganymede.notebook;
/*-
 * ##########################################################################
 * Ganymede
 * %%
 * Copyright (C) 2021 Allen D. Ball
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ##########################################################################
 */
import ganymede.shell.Shell;
import java.io.File;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import javax.script.ScriptContext;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;
import jdk.jshell.JShell;
import lombok.NoArgsConstructor;
import lombok.ToString;

import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static javax.script.ScriptContext.ENGINE_SCOPE;
import static javax.script.ScriptContext.GLOBAL_SCOPE;
import static jdk.jshell.Snippet.SubKind.TEMP_VAR_EXPRESSION_SUBKIND;

/**
 * {@link NotebookContext} for {@link Notebook} {@link ganymede.shell.Shell}
 * {@link JShell} instance.  Bound to {@code __} in the {@link JShell}
 * instance.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@NoArgsConstructor @ToString
public class NotebookContext {

    /**
     * Common {@link ScriptContext} supplied to
     * {@link ganymede.shell.Magic#execute(NotebookContext,String,String)}.
     */
    public final ScriptContext context =
        new SimpleScriptContext() {
            {
                setBindings(new SimpleBindings(new ConcurrentSkipListMap<>()), GLOBAL_SCOPE);
                setBindings(new SimpleBindings(new ConcurrentSkipListMap<>()), ENGINE_SCOPE);
            }
        };

    /**
     * {@link List} of {@code classpath} entries.
     */
    public final List<String> classpath = new LinkedList<>();

    /**
     * {@link List} of accumulated {@code import}s.
     */
    public final List<String> imports = new LinkedList<>();

    /**
     * {@link Map} of known bindings' {@link Class types}.
     */
    public final Map<String,String> types = new ConcurrentSkipListMap<>();

    /**
     * Method to construct a call to a static
     * {@link java.lang.reflect.Method}.
     *
     * @param   type            The containing {@link Class}.
     * @param   method          The {@link java.lang.reflect.Method} name.
     * @param   parameters      The parameter types ({@link Class}es).
     * @param   arguments       The argument {@link Object} array.
     *
     * @return  The {@link Object result}.
     */
    public Object invokeStaticMethod(String type, String method,
                                     Class<?>[] parameters, Object... arguments) {
        Object object = null;

        try {
            object =
                Class.forName(type)
                .getDeclaredMethod(method, parameters)
                .invoke(null, arguments);
        } catch (Throwable throwable) {
            throwable.printStackTrace(System.err);
        }

        return object;
    }

    /**
     * Method to construct a call to a static
     * {@link java.lang.reflect.Method} with an empty argument list.
     *
     * @param   type            The containing {@link Class}.
     * @param   method          The {@link java.lang.reflect.Method} name.
     *
     * @return  The {@link Object result}.
     */
    public Object invokeStaticMethod(String type, String method) {
        return invokeStaticMethod(type, method,
                                  new Class<?>[] { }, new Object[] { });
    }

    /**
     * Static method used by the {@link ganymede.shell.Shell} to update the
     * {@link NotebookContext} members.
     *
     * @param   shell           The {@link Shell}.
     */
    public static void update(Shell shell) {
        var jshell = shell.jshell();
        var classpath =
            shell.classpath().stream()
            .map(File::getAbsolutePath)
            .collect(toList());

        evaluate(jshell, "__.classpath.clear()");

        if (! classpath.isEmpty()) {
            evaluate(jshell,
                     "java.util.Collections.addAll(__.classpath, \"%1$s\".split(\",\"))",
                     String.join(",", classpath));
        }

        var imports =
            jshell.imports()
            .map(t -> t.source())
            .map(String::strip)
            .collect(toCollection(LinkedHashSet::new));

        evaluate(jshell, "__.imports.clear()");

        if (! imports.isEmpty()) {
            evaluate(jshell,
                     "java.util.Collections.addAll(__.imports, \"%1$s\".split(\",\"))",
                     String.join(",", imports));
        }

        var types =
            jshell.variables()
            .filter(t -> (! t.subKind().equals(TEMP_VAR_EXPRESSION_SUBKIND)))
            .filter(t -> (! t.name().equals("__")))
            .collect(toMap(k -> k.name(), v -> v.typeName()));

        evaluate(jshell, "__.types.clear()");

        for (var entry : types.entrySet()) {
            evaluate(jshell,
                     "__.context.getBindings(%1d).put(\"%2$s\", %2$s)",
                     ENGINE_SCOPE, entry.getKey());
            evaluate(jshell,
                     "__.types.put(\"%1$s\", \"%2$s\")",
                     entry.getKey(), entry.getValue());
        }
    }

    private static String evaluate(JShell jshell, String expression, Object... argv) {
        var analyzer = jshell.sourceCodeAnalysis();
        var info = analyzer.analyzeCompletion(String.format(expression, argv));

        return unescape(jshell.eval(info.source()).get(0).value());
    }

    /**
     * Method to unescape a Java-escaped spring literal.
     *
     * @param   literal         The {@link String} to unescape.
     *
     * @return  The unescaped {@link String}.
     */
    public static String unescape(String literal) {
        var string = literal;
        /*
         * https://stackoverflow.com/questions/3537706/how-to-unescape-a-java-string-literal-in-java
         */
        if (literal != null) {
            try (var reader = new StringReader(literal)) {
                var tokenizer = new StreamTokenizer(reader);

                tokenizer.nextToken();

                if (tokenizer.ttype == '"') {
                    string = tokenizer.sval;
                }
            } catch (IOException exception) {
            }
        }

        return string;
    }
}
