package ganymede.notebook;
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
import java.util.concurrent.ConcurrentSkipListMap;
import javax.script.ScriptContext;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;
import lombok.NoArgsConstructor;
import lombok.ToString;

import static javax.script.ScriptContext.ENGINE_SCOPE;
import static javax.script.ScriptContext.GLOBAL_SCOPE;

/**
 * {@link NotebookContext} for {@link Notebook} {@link ganymede.shell.Shell}
 * {@link jdk.jshell.JShell} instance.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@NoArgsConstructor @ToString
public class NotebookContext {
    public final ScriptContext context = new SimpleScriptContext();

    {
        context.setBindings(new SimpleBindings(new ConcurrentSkipListMap<>()), GLOBAL_SCOPE);
        context.setBindings(new SimpleBindings(new ConcurrentSkipListMap<>()), ENGINE_SCOPE);
    }

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

    public Object invokeStaticMethod(String type, String method) {
        return invokeStaticMethod(type, method,
                                  new Class<?>[] { }, new Object[] { });
    }

    public void print(Object object) { NotebookMethods.print(object); }
}
