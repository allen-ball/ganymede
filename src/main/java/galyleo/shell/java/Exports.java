package galyleo.shell.java;

import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PRIVATE;

/**
 * {@link galyleo.shell.Shell} well-known static fields.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@NoArgsConstructor(access = PRIVATE)
public abstract class Exports {

    /**
     * Result of last {@link Imports#print(Object)} call.
     */
    public static String execute_result = null;
}
