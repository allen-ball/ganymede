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
import com.fasterxml.jackson.databind.JsonNode;
import ganymede.kernel.KernelRestClient;
import ganymede.server.Message;
import ganymede.shell.MagicMap;
import ganymede.shell.Shell;
import java.io.File;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.Base64;
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

import static ganymede.server.Server.OBJECT_MAPPER;
import static java.lang.reflect.Modifier.isPublic;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static jdk.jshell.Snippet.SubKind.TEMP_VAR_EXPRESSION_SUBKIND;

/**
 * {@link NotebookContext} for {@link Notebook} {@link Shell}
 * {@link JShell} instance.  Bound to {@code __} in the {@link JShell}
 * instance.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@NoArgsConstructor @ToString
public class NotebookContext {
    private static final Base64.Decoder DECODER = Base64.getDecoder();
    private static final Base64.Encoder ENCODER = Base64.getEncoder();

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

    private final MagicMap magics = new MagicMap();

    /**
     * Method to receive a {@link ganymede.shell.Magic} request in the
     * {@link JShell} instance.  See
     * {@link magic(Shell,String,String,String)}.
     *
     * @param   name            The magic name.
     * @param   line0           The initial magic line.
     * @param   code            The remainder of the cell.
     */
    public void magic(String name, String line0, String code) throws Exception {
        magics.reload();

        if (magics.containsKey(name)) {
            magics.get(name).execute(this, decode(line0), decode(code));
        } else {
            throw new IllegalStateException("Magic '" + name + "' not found");
        }
    }

    /**
     * {@link NotebookFunction} to display from a Notebook cell.
     *
     * @param   object          The {@link Object} to display.
     */
    @NotebookFunction
    public void display(Object object) {
        try {
            new KernelRestClient().display(Message.mime_bundle(object));
        } catch (Exception exception) {
            System.out.println(object);
            exception.printStackTrace(System.err);
        }
    }

    /**
     * {@link NotebookFunction} to print from a Notebook cell.
     *
     * @param   object          The {@link Object} to print.
     */
    @NotebookFunction
    public void print(Object object) {
        try {
            new KernelRestClient().print(Message.mime_bundle(object));
        } catch (Exception exception) {
            System.out.println(object);
            exception.printStackTrace(System.err);
        }
    }

    /**
     * {@link NotebookFunction} to convert an {@link Object} to
     * {@link JsonNode JSON} representation.
     *
     * @param   object          The {@link Object} to convert.
     *
     * @return  The {@link JsonNode} representation.
     */
    @NotebookFunction
    public JsonNode asJson(Object object) {
        return OBJECT_MAPPER.valueToTree(object);
    }

    /**
     * Method to generate the bootstrap code for a new {@link JShell}
     * instance.
     *
     * @return  The boostrap code.
     */
    public static String bootstrap() {
        var code =
            String.format("var __ = %s.newNotebookContext();\n",
                          Notebook.class.getCanonicalName());

        for (var method : NotebookContext.class.getDeclaredMethods()) {
            if (method.isAnnotationPresent(NotebookFunction.class)) {
                if (isPublic(method.getModifiers())) {
                    code += makeWrapperFor("__", method);
                }
            }
        }

        return code;
    }

    private static String makeWrapperFor(String instance, Method method) {
        var types = method.getGenericParameterTypes();
        var arguments = new String[types.length];
        var parameters = new String[types.length];

        for (int i = 0; i < arguments.length; i += 1) {
            arguments[i] = String.format("argument%d", i);
        }

        for (int i = 0; i < parameters.length; i += 1) {
            parameters[i] =
                String.format("%s %s", types[i].getTypeName(), arguments[i]);
        }

        var plist = String.join(", ", parameters);
        var alist = String.join(", ", arguments);

        return String.format("%1$s %2$s(%3$s) { %4$s%5$s.%2$s(%6$s); }\n",
                             method.getGenericReturnType().getTypeName(),
                             method.getName(), plist,
                             Void.TYPE.equals(method.getReturnType()) ? "" : "return ",
                             instance, alist);
    }

    /**
     * Static method used by the {@link Shell} to update the
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
                     ScriptContext.ENGINE_SCOPE, entry.getKey());
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
     * Static method invoke a {@link ganymede.shell.Magic} in a
     * {@link JShell} instance.  See {@link #magic(String,String,String)}.
     *
     * @param   shell           The {@link Shell}.
     * @param   name            The magic name.
     * @param   line0           The initial magic line.
     * @param   code            The remainder of the cell.
     */
    public static void magic(Shell shell, String name, String line0, String code) {
        var jshell = shell.jshell();

        evaluate(jshell,
                 "__.magic(\"%s\", \"%s\", \"%s\")",
                 name, encode(line0), encode(code));
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

    private static String decode(String string) {
        return new String(DECODER.decode(string), UTF_8);
    }

    private static String encode(String string) {
        return ENCODER.encodeToString(((string != null) ? string : "").getBytes(UTF_8));
    }
}
