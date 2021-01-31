package galyleo.shell.java;

import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PRIVATE;

/**
 * {@link galyleo.shell.Java} Notebook static imported methods.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@NoArgsConstructor(access = PRIVATE)
public abstract class Imports {

    /**
     * Method to print from a Notebook cell.
     *
     * @param   object          The {@link Object} to print.
     */
    public static void print(Object object) {
        try {
            System.out.println(object);
        } catch (Exception exception) {
            exception.printStackTrace(System.err);
        }
    }
}
