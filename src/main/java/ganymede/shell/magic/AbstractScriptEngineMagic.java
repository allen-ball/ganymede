package ganymede.shell.magic;
/*-
 * ##########################################################################
 * Ganymede
 * $Id$
 * $HeadURL$
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
import ganymede.notebook.NotebookContext;
import ganymede.shell.Magic;
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
 * @version $Revision$
 */
@NoArgsConstructor(access = PROTECTED) @ToString @Log4j2
public abstract class AbstractScriptEngineMagic extends AbstractMagic {
    protected ScriptEngine engine = null;

    /**
     * Method to get the script extension.
     *
     * @return  The script extension.
     */
    public String getExtension() { return getMagicNames()[0]; }

    /**
     * Method to get the {@link ScriptEngine}.
     *
     * @return  The {@link ScriptEngine} if it can be instantiated;
     *          {@code null} otherwise.
     */
    protected ScriptEngine engine() {
        if (engine == null) {
            var manager = new ScriptEngineManager(getClass().getClassLoader());

            engine = manager.getEngineByExtension(getExtension());

            if (engine == null) {
                engine =
                    manager.getEngineFactories().stream()
                    .filter(t -> t.getExtensions().contains(getExtension()))
                    .map(t -> t.getScriptEngine())
                    .findFirst().orElse(null);
            }
        }

        return engine;
    }

    @Override
    public void execute(NotebookContext __, String line0, String code) throws Exception {
        var bindings = __.context.getBindings(ENGINE_SCOPE);
        var argv = Magic.getCellMagicCommand(line0);

        try {
            bindings.put(ScriptEngine.ARGV, argv);

            execute(__, code);
        } finally {
            bindings.remove(ScriptEngine.ARGV);
        }
    }

    /**
     * Target of {@link #execute(NotebookContext,String,String)}.  The
     * {@code argv} is available in the {@link javax.script.ScriptContext}
     * {@code ENGINE_SCOPE} {@link javax.script.Bindings} as
     * {@link ScriptEngine#ARGV ScriptEngine.ARGV}.
     *
     * @param   __              The {@link NotebookContext}.
     * @param   code            The remainder of the cell.
     */
    protected void execute(NotebookContext __, String code) throws Exception {
        var engine = engine();

        if (engine != null) {
            engine.eval(code, __.context);
        } else {
            System.err.format("No %s REPL available\n", getMagicNames()[0]);
        }
    }
}
