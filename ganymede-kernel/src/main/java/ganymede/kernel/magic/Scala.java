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
import java.io.InputStreamReader;
import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;
import scala.tools.nsc.Settings;
import scala.tools.nsc.interpreter.Scripted;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.disjoint;
import static javax.script.ScriptContext.ENGINE_SCOPE;

/**
 * {@link Scala} {@link Magic}.
 *
 * @see Scripted
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@ServiceProviderFor({ Magic.class })
@Description("Execute code in scala REPL")
@NoArgsConstructor @ToString @Log4j2
public class Scala extends AbstractScriptEngineMagic {
    @Override
    protected Scripted engine() {
        if (engine == null) {
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
                    (Scripted.Factory)
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

                var scripted = Scripted.apply(factory, settings, Scripted.apply$default$3());
                var bindings = context.context.getBindings(ENGINE_SCOPE);

                scripted.setBindings(bindings, ENGINE_SCOPE);

                if (initialize(scripted)) {
                    engine = scripted;
                }
            } catch (NoClassDefFoundError error) {
            } catch (Throwable throwable) {
                throwable.printStackTrace(System.err);
            }
        }

        return (Scripted) engine;
    }

    @Override
    protected boolean initialize(ScriptEngine engine) {
        var initialized = true;
        var scripted = (Scripted) engine;
        var name = getClass().getSimpleName() + "." + "scala";
        var resource = new ClassPathResource(name, getClass());

        if (initialized && resource.exists()) {
            try (var in = new InputStreamReader(resource.getInputStream(), UTF_8)) {
                scripted.compile(in).eval(context.context);
            } catch (Throwable throwable) {
                initialized = false;
                throwable.printStackTrace(System.err);
            }
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
                 * TBD: Add paths to Scripted classpath.
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
                     * engine.compile(statement).eval(context.context);
                     */
                } catch (Throwable throwable) {
                    throwable.printStackTrace(System.err);
                }
            }

            try {
                if (! code.isBlank()) {
                    render(engine.compile(code).eval(context.context));
                } else {
                    show();
                }
            } catch (Throwable throwable) {
                throwable.printStackTrace(System.err);
            }
        } else {
            super.execute(code);
        }
    }
}
