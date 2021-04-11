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
import ganymede.shell.Magic;
import java.util.Objects;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

import static javax.script.ScriptContext.ENGINE_SCOPE;
import static lombok.AccessLevel.PROTECTED;

/**
 * Abstract
 * {@link.uri https://www.jcp.org/en/jsr/detail?id=223 target=newtab JSR 223}
 * {@link ScriptEngine} {@link Magic} base class.
 *
 * @see ScriptEngineManager
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@NoArgsConstructor(access = PROTECTED) @ToString @Log4j2
public abstract class AbstractScriptEngineMagic extends AbstractMagic
                                                implements AnnotatedScriptEngineMagic {
    protected ScriptEngine engine = null;

    /**
     * Method to get the {@link ScriptEngine}.
     *
     * @return  The {@link ScriptEngine} if it can be instantiated;
     *          {@code null} otherwise.
     */
    protected ScriptEngine engine() {
        if (engine == null) {
            var manager = new ScriptEngineManager(getClass().getClassLoader());

            engine = manager.getEngineByName(getScriptEngineName());

            if (engine == null) {
                engine =
                    getExtensions().stream()
                    .map(t -> manager.getEngineByExtension(t))
                    .filter(Objects::nonNull)
                    .findFirst().orElse(null);
            }
        }

        return engine;
    }

    @Override
    public void execute(String line0, String code) throws Exception {
        execute(Magic.getCellMagicCommand(line0), code);
    }

    /**
     * Method provided for subclass implementations to intercept calculation
     * of {@link ScriptEngine#ARGV} binding.
     *
     * @param   argv            The first line parsed as an array of
     *                          {@link String}s.
     * @param   code            The remainder of the cell.
     */
    protected void execute(String[] argv, String code) throws Exception {
        var bindings = context.context.getBindings(ENGINE_SCOPE);

        try {
            bindings.put(ScriptEngine.ARGV, argv);
            execute(code);
        } finally {
            bindings.remove(ScriptEngine.ARGV);
        }
    }

    /**
     * Target of {@link #execute(String,String)}.  The
     * {@code argv} is available in the {@link javax.script.ScriptContext}
     * {@code ENGINE_SCOPE} {@link javax.script.Bindings} as
     * {@link ScriptEngine#ARGV ScriptEngine.ARGV}.
     *
     * @param   code            The remainder of the cell.
     */
    protected void execute(String code) throws Exception {
        var engine = engine();

        if (engine != null) {
            render(engine.eval(code, context.context));
        } else {
            System.err.format("No %s REPL available\n", getMagicNames()[0]);
        }
    }

    /**
     * Callback to render the results of
     * {@link ScriptEngine#eval(String,ScriptContext)}.  Made available for
     * template engines.  Default implementation does nothing.
     *
     * @param   object          The result of
     *                          {@link ScriptEngine#eval(String,ScriptContext)}.
     */
    protected void render(Object object) { }
}
