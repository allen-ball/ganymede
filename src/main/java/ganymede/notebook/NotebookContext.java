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
import jdk.jshell.JShell;
import lombok.NoArgsConstructor;
import lombok.ToString;

import static java.util.stream.Collectors.toSet;
import static javax.script.ScriptContext.ENGINE_SCOPE;
import static javax.script.ScriptContext.GLOBAL_SCOPE;
import static jdk.jshell.Snippet.SubKind.TEMP_VAR_EXPRESSION_SUBKIND;

/**
 * {@link NotebookContext} for {@link Notebook} {@link ganymede.shell.Shell}
 * {@link JShell} instance.  Bound to {@code __} in the {@link JShell}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@NoArgsConstructor @ToString
public class NotebookContext {

    /**
     * Static method used by the {@link ganymede.shell.Shell} to update the
     * {@link NotebookContext} members.
     *
     * @param   jshell          The {@link JShell}.
     */
    public static void update(JShell jshell) {
        var analyzer = jshell.sourceCodeAnalysis();
        var variables =
            jshell.variables()
            .filter(t -> (! t.subKind().equals(TEMP_VAR_EXPRESSION_SUBKIND)))
            .filter(t -> (! t.name().equals("__")))
            .map(t -> t.name())
            .collect(toSet());

        for (var variable : variables) {
            var expression =
                String.format("__.context.getBindings(%1d).put(\"%2$s\", %2$s)",
                              GLOBAL_SCOPE, variable);
            var info = analyzer.analyzeCompletion(expression);

            jshell.eval(info.source());
        }
    }

    /**
     * Common {@link ScriptContext} supplied to
     * {@link ganymede.shell.Magic#execute(ScriptContext,String,String)}.
     */
    public final ScriptContext context = new SimpleScriptContext();

    {
        context.setBindings(new SimpleBindings(new ConcurrentSkipListMap<>()), GLOBAL_SCOPE);
        context.setBindings(new SimpleBindings(new ConcurrentSkipListMap<>()), ENGINE_SCOPE);
    }

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
     * See {@link NotebookMethods#print(Object)}.
     */
    public void print(Object object) { NotebookMethods.print(object); }
}
