package ganymede.notebook;

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
