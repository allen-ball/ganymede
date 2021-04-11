package ganymede.shell.magic;
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
import ganymede.shell.Magic;
import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import scala.tools.nsc.Settings;
import scala.tools.nsc.interpreter.Scripted;

import static java.util.Collections.disjoint;

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
    protected ScriptEngine engine() {
        if (engine == null) {
            /*
             * Can't simply extend AbstractScriptEngineMagic because:
             *
             *     new ScriptEngineManager().getEngineByName("scala");
             *
             * tickles https://github.com/scala/bug/issues/11754.
             */
            try {
                var manager = new ScriptEngineManager(getClass().getClassLoader());
                var factory =
                    (Scripted.Factory)
                    manager.getEngineFactories().stream()
                    .filter(t -> (! disjoint(t.getExtensions(), getExtensions())))
                    .findFirst().orElse(null);
                var settings = new Settings();

                settings.usejavacp().value_$eq(true);

                context.classpath.stream()
                    .forEach(t -> settings.classpath().append(t));

                engine = new Scripted(factory, settings, Scripted.apply$default$3());
            } catch (NoClassDefFoundError exception) {
            } catch (Exception exception) {
                exception.printStackTrace(System.err);
            }
        }

        return engine;
    }
}
