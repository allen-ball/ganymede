package ganymede.notebook;

import java.util.concurrent.ConcurrentSkipListMap;
import javax.script.Bindings;
import javax.script.SimpleBindings;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * {@link NotebookContext} for {@link Notebook} {@link ganymede.shell.Shell}
 * {@link jdk.jshell.JShell} instance.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@NoArgsConstructor @ToString
public class NotebookContext {
    public final Bindings bindings =
        new SimpleBindings(new ConcurrentSkipListMap<>());

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
