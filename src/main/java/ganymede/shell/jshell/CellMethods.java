package ganymede.shell.jshell;

import ganymede.kernel.Client;
import ganymede.server.Message;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PRIVATE;

/**
 * Methods available in Notebook cells.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@NoArgsConstructor(access = PRIVATE)
public abstract class CellMethods {

    /**
     * Method to print from a Notebook cell.
     *
     * @param   object          The {@link Object} to print.
     */
    public static void print(Object object) {
        try {
            new Client().print(Message.mime_bundle(object));
        } catch (Exception exception) {
            System.out.println(object);
            exception.printStackTrace(System.err);
        }
    }
}
