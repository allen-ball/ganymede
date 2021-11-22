package ganymede.kernel.magic;
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
import ball.annotation.ServiceProviderFor;
import ganymede.notebook.AbstractScriptEngineMagic;
import ganymede.notebook.Description;
import ganymede.notebook.Magic;
import ganymede.notebook.NotebookContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.ClassPathResource;
import scala.tools.nsc.Settings;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.disjoint;
import static javax.script.ScriptContext.ENGINE_SCOPE;

/**
 * {@link Scala} {@link Magic}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@ServiceProviderFor({ Magic.class })
@Description("Execute code in scala REPL")
@NoArgsConstructor @ToString @Log4j2
public class Scala extends AbstractScriptEngineMagic {
    private boolean isScriptedScriptEngine = false;

    private Object eval(ScriptEngine engine, ClassPathResource resource) throws IOException, ScriptException {
        Object object = null;

        try (var in = resource.getInputStream()) {
            object = eval(engine, in);
        }

        return object;
    }

    private Object eval(ScriptEngine engine, InputStream in) throws IOException, ScriptException {
        Object object = null;

        try (var reader = new InputStreamReader(in, UTF_8)) {
            var script =
                engine.getClass()
                .getMethod("compile", Reader.class)
                .invoke(engine, reader);

            object =
                script.getClass()
                .getMethod("eval", ScriptContext.class)
                .invoke(script, context.context);
        } catch (IOException exception) {
            throw exception;
        } catch (InvocationTargetException exception) {
            var cause = exception.getCause();

            if (cause instanceof IOException) {
                throw (IOException) cause;
            } else if (cause instanceof ScriptException) {
                throw (ScriptException) cause;
            } else {
                throw new IllegalStateException(exception);
            }
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Error error) {
            throw error;
        } catch (Throwable throwable) {
            throw new IllegalStateException(throwable);
        }

        return object;
    }

    private Object eval(ScriptEngine engine, String code) throws ScriptException {
        Object object = null;

        try {
            var script =
                engine.getClass()
                .getMethod("compile", String.class)
                .invoke(engine, code);

            object =
                script.getClass()
                .getMethod("eval", ScriptContext.class)
                .invoke(script, context.context);
        } catch (InvocationTargetException exception) {
            var cause = exception.getCause();

            if (cause instanceof ScriptException) {
                throw (ScriptException) cause;
            } else {
                throw new IllegalStateException(exception);
            }
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Error error) {
            throw error;
        } catch (Throwable throwable) {
            throw new IllegalStateException(throwable);
        }

        return object;
    }

    @Override
    protected ScriptEngine engine() {
        if (engine == null) {
            Class<? extends ScriptEngine> type = null;

            try {
                type =
                    Class.forName("scala.tools.nsc.interpreter.Scripted",
                                  false, getClass().getClassLoader())
                    .asSubclass(ScriptEngine.class);
            } catch (ClassNotFoundException exception) {
            }

            isScriptedScriptEngine = (type != null);

            if (isScriptedScriptEngine) {
                /*
                 * Can't simply extend AbstractScriptEngineMagic because:
                 *
                 *     new ScriptEngineManager().getEngineByName("scala");
                 *
                 * tickles https://github.com/scala/bug/issues/11754.
                 *
                 * The following workaround is required to use println():
                 *
                 *     Console.withOut(System.out) { println($ctx.greeting) }
                 */
                try {
                    var manager = new ScriptEngineManager(getClass().getClassLoader());
                    var factory =
                        manager.getEngineFactories().stream()
                        .filter(t -> (! disjoint(t.getExtensions(), getExtensions())))
                        .findFirst().orElse(null);
                    var settings = new Settings();
                    /*
                     * settings.Yreplclassbased().value_$eq(true);
                     * settings.usejavacp().value_$eq(true);
                     */
                    context.classpath.stream()
                        .forEach(t -> settings.classpath().append(t));

                    var out = type.getMethod("apply$default$3").invoke(null);
                    var scripted =
                        type.cast(type.getMethod("apply",
                                                 ScriptEngineFactory.class, Settings.class, PrintWriter.class)
                                  .invoke(null, factory, settings, out));
                    var bindings = context.context.getBindings(ENGINE_SCOPE);

                    scripted.setBindings(bindings, ENGINE_SCOPE);

                    if (initialize(scripted)) {
                        engine = scripted;
                    }
                } catch (NoClassDefFoundError error) {
                } catch (Throwable throwable) {
                    throwable.printStackTrace(System.err);
                }
            } else {
                super.engine();
            }
        } else {
            super.engine();
        }

        return engine;
    }

    @Override
    protected boolean initialize(ScriptEngine engine) {
        var initialized = true;

        if (isScriptedScriptEngine) {
            var resource = getInitScript(getExtensions());

            if (initialized && resource != null) {
                try {
                    eval(engine, resource);
                } catch (Throwable throwable) {
                    initialized = false;
                    throwable.printStackTrace(System.err);
                }
            }
        } else {
            initialized = super.initialize(engine);
        }

        if (initialized) {
            for (var method : NotebookContext.getNotebookFunctions()) {
                /*
                 * TBD: Generate Scala wrappers through reflection.
                 */
            }
        }

        return initialized;
    }

    @Override
    protected void execute(String code) {
        var engine = engine();

        if (engine != null) {
            for (var path : context.classpath) {
                /*
                 * TBD: Add paths to ScriptEngine classpath.
                 */
            }

            for (var statement : context.imports) {
                try {
                    statement =
                        statement
                        .replaceAll("[;]", "")
                        .replaceAll("[\\p{Space}]+", " ")
                        .replaceAll("import static ", "import ")
                        .replaceAll("[*]", "_")
                        .strip();
                    /*
                     * TBD: Add imports (depends on successful classpath
                     * updates).
                     *
                     * eval(engine, statement);
                     */
                } catch (Throwable throwable) {
                    throwable.printStackTrace(System.err);
                }
            }

            if (isScriptedScriptEngine && (! code.isBlank())) {
                try {
                    render(eval(engine, code));
                } catch (Throwable throwable) {
                    throwable.printStackTrace(System.err);
                }
            } else {
                super.execute(code);
            }
        } else {
            super.execute(code);
        }
    }
}
