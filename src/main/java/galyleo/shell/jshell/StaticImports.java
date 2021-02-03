package galyleo.shell.jshell;

import galyleo.server.Message;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PRIVATE;

/**
 * {@link galyleo.shell.Shell} Notebook statically imported methods.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@NoArgsConstructor(access = PRIVATE)
public abstract class StaticImports {

    /**
     * Method to print from a Notebook cell.
     *
     * @param   object          The {@link Object} to print.
     */
    public static void print(Object object) {
        try {
            ExecutionEvents.json.add(Message.content(object));
        } catch (Exception exception) {
            System.out.println(object);
            exception.printStackTrace(System.err);
        }
    }
}
