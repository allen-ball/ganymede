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
import ganymede.util.ObjectMappers;
import java.io.File;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Stream;
import javax.script.ScriptContext;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;
import jdk.jshell.JShell;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.jooq.DSLContext;
import org.jooq.Query;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.impl.DSL;

import static java.lang.reflect.Modifier.isPublic;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static jdk.jshell.Snippet.SubKind.TEMP_VAR_EXPRESSION_SUBKIND;

/**
 * {@link NotebookContext} for {@link Notebook} {@link Shell}
 * {@link JShell} instance.  Bound to {@value #NAME} in the {@link JShell}
 * instance.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@NoArgsConstructor @ToString(callSuper = true, onlyExplicitlyIncluded = true)
public class NotebookContext {
    private static final Base64.Decoder DECODER = Base64.getDecoder();
    private static final Base64.Encoder ENCODER = Base64.getEncoder();

    /**
     * The name ({@value #NAME}) the {@link NotebookContext} instance is
     * bound to in the {@link JShell} instance.
     */
    public static final String NAME = "$$";

    /**
     * Common {@link ScriptContext} supplied to
     * {@link ganymede.shell.Magic#execute(String,String)}.
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
     * {@link ganymede.shell.magic.SQL}-specific context.
     * See {@link NotebookContext.SQL SQL}.
     */
    public final SQL sql = new SQL();

    private final MagicMap magics = new MagicMap(t -> t.configure(this));

    /**
     * Provide access to the {@link NotebookContext} {@link MagicMap}.
     *
     * @return  The {@link MagicMap}.
     */
    public MagicMap magics() { return magics; }

    /**
     * Method to receive a {@link ganymede.shell.Magic} request in the
     * {@link JShell} instance.  See
     * {@link magic(Shell,String,String,String)}.
     *
     * @param   name            The magic name.
     * @param   line0           The initial magic line.
     * @param   code            The remainder of the cell.
     */
    public void magic(String name, String line0, String code) {
        try {
            var magic = magics.reload().get(name);

            if (magic != null) {
                magic.execute(decode(line0), decode(code));
            } else {
                System.err.format("Magic '%s' not found\n", name);
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace(System.err);
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
        return ObjectMappers.JSON.valueToTree(object);
    }

    /**
     * {@link NotebookFunction} to convert an {@link Object} to YAML
     * representation.
     *
     * @param   object          The {@link Object} to convert.
     *
     * @return  The YAML (as a {@link String}) representation.
     */
    @NotebookFunction
    public String asYaml(Object object) {
        var yaml = "";

        try {
            yaml = ObjectMappers.YAML.writeValueAsString(object);
        } catch (Exception exception) {
            exception.printStackTrace(System.err);
        }

        return yaml;
    }

    /**
     * Method to generate the bootstrap code for a new {@link JShell}
     * instance.
     *
     * @return  The boostrap code.
     */
    public static String bootstrap() {
        var code =
            String.format("var %1$s = %2$s.newNotebookContext();\n",
                          NAME, Notebook.class.getCanonicalName());

        for (var method : getNotebookFunctions()) {
            code += makeWrapperFor(NAME, method);
        }

        return code;
    }

    private static String makeWrapperFor(String instance, Method method) {
        var types = method.getGenericParameterTypes();
        var arguments = new String[types.length];
        var parameters = new String[types.length];

        for (int i = 0; i < arguments.length; i += 1) {
            arguments[i] = String.format("argument%1$d", i);
        }

        for (int i = 0; i < parameters.length; i += 1) {
            parameters[i] =
                String.format("%1$s %2$s",
                              types[i].getTypeName(), arguments[i]);
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
     * Method to get the {@link NotebookContext} {@link Method}s annotated
     * with {@link NotebookFunction} that should be linked into the
     * {@link Notebook} environment.
     *
     * @return  The array of {@link Method}s.
     */
    public static Method[] getNotebookFunctions() {
        var functions =
            Stream.of(NotebookContext.class.getDeclaredMethods())
            .filter(t -> t.isAnnotationPresent(NotebookFunction.class))
            .filter(t -> isPublic(t.getModifiers()))
            .toArray(Method[]::new);

        return functions;
    }

    /**
     * Static method used by the {@link Shell} REPL to update the
     * {@link NotebookContext} instance before execution.
     *
     * @param   shell           The {@link Shell}.
     */
    public static void preExecute(Shell shell) {
        var jshell = shell.jshell();
        var classpath =
            shell.classpath().stream()
            .map(File::getAbsolutePath)
            .collect(toList());

        evaluate(jshell, "%1$s.classpath.clear()", NAME);

        if (! classpath.isEmpty()) {
            evaluate(jshell,
                     "java.util.Collections.addAll(%1$s.classpath, \"%2$s\".split(\",\"))",
                     NAME, String.join(",", classpath));
        }

        var imports =
            jshell.imports()
            .map(t -> t.source())
            .map(String::strip)
            .collect(toCollection(LinkedHashSet::new));

        evaluate(jshell, "%1$s.imports.clear()", NAME);

        if (! imports.isEmpty()) {
            evaluate(jshell,
                     "java.util.Collections.addAll(%1$s.imports, \"%2$s\".split(\",\"))",
                     NAME, String.join(",", imports));
        }

        var types =
            jshell.variables()
            .filter(t -> (! t.subKind().equals(TEMP_VAR_EXPRESSION_SUBKIND)))
            .collect(toMap(k -> k.name(), v -> v.typeName()));

        evaluate(jshell, "%1$s.types.clear()", NAME);

        for (var entry : types.entrySet()) {
            evaluate(jshell,
                     "%1$s.context.getBindings(%2$d).put(\"%3$s\", %3$s)",
                     NAME, ScriptContext.ENGINE_SCOPE, entry.getKey());
            evaluate(jshell,
                     "%1$s.types.put(\"%2$s\", \"%3$s\")",
                     NAME, entry.getKey(), entry.getValue());
        }
    }

    /**
     * Static method used by the {@link Shell} REPL to update the
     * {@link Shell} and {@link JShell} after execution.
     *
     * @param   shell           The {@link Shell}.
     */
    public static void postExecute(Shell shell) {
        /* var jshell = shell.jshell(); */
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
                 "%1$s.magic(\"%2$s\", \"%3$s\", \"%4$s\")",
                 NAME, name, encode(line0), encode(code));
    }

    /**
     * Method to unescape a Java-escaped string literal.
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

    /**
     * {@link ganymede.shell.magic.SQL}-specific context.  Implements
     * {@link Map} of JDBC URLs to {@link DSLContext}s.
     */
    @NoArgsConstructor
    public class SQL extends LinkedHashMap<String,DSLContext> {
        private static final long serialVersionUID = -4901551333824542142L;

        /**
         * {@link List} of most recent {@link ganymede.shell.magic.SQL}
         * {@link Query Queries}.
         *
         * @serial
         */
        public final List<Query> queries = new ArrayList<>();

        /**
         * {@link List} of most recent {@link ganymede.shell.magic.SQL}
         * {@link Result}s.
         *
         * @serial
         */
        public final List<Result<Record>> results = new ArrayList<>();

        /**
         * Target of the {@link ganymede.shell.magic.SQL}
         * {@link ganymede.shell.Magic}.
         *
         * @param       url             The JDBC URL.
         * @param       username        The JDBC Username.
         * @param       password        The JDBC Password.
         *
         * @return      The {@link DSLContext} corresponding to the URL.
         */
        public DSLContext connect(String url, String username, String password) {
            return computeIfAbsent(toKey(url), k -> DSL.using(url, username, password));
        }

        private String toKey(String url) {
            var key = url;

            if (key != null) {
                try {
                    var list = new LinkedList<URI>();

                    list.add(URI.create(key));

                    for (;;) {
                        var last = list.getLast();
                        var ssp = last.getSchemeSpecificPart();

                        if (last.isOpaque() && ssp.contains(":")) {
                            list.add(URI.create(ssp));
                        } else {
                            break;
                        }
                    }

                    var uri = list.removeLast();

                    if (uri.isOpaque()) {
                        uri = new URI(uri.getScheme(), uri.getSchemeSpecificPart().split(";", 2)[0], null);
                    } else {
                        uri = new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), uri.getPath(), null, null);
                    }

                    while (! list.isEmpty()) {
                        uri = new URI(list.removeLast().getScheme(), uri.toString(), null);
                    }

                    key = uri.toString();
                } catch (URISyntaxException exception) {
                    throw new IllegalArgumentException(exception);
                }
            }

            return key;
        }
    }
}
