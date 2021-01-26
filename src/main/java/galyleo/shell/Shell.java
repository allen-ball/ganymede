package galyleo.shell;

import java.io.InputStream;
import java.io.PrintStream;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

/**
 * Galyleo abstract {@link Shell} base class.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@NoArgsConstructor @ToString @Log4j2
public abstract class Shell implements AutoCloseable {

    /**
     * Method to start a {@link Shell}.
     *
     * @param   in              The {@code in} {@link InputStream}.
     * @param   out             The {@code out} {@link PrintStream}.
     * @param   err             The {@code err} {@link PrintStream}.
     */
    public abstract void start(InputStream in, PrintStream out, PrintStream err);

    /**
     * Method to restart a {@link Shell}.
     *
     * @param   in              The {@code in} {@link InputStream}.
     * @param   out             The {@code out} {@link PrintStream}.
     * @param   err             The {@code err} {@link PrintStream}.
     */
    public abstract void restart(InputStream in, PrintStream out, PrintStream err);

    /**
     * Method to close (terminate) a {@link Shell}.
     */
    @Override
    public abstract void close();

    /**
     * Method to execute code (typically a cell's contents).
     *
     * @param   code            The code to execute.
     */
    public abstract void execute(String code) throws Exception;

    /**
     * Method to evaluate an expression.
     *
     * @param   code            The code to execute.
     *
     * @return  The result of evaluating the expression.
     */
    public abstract Object evaluate(String code) throws Exception;

    /**
     * Method to stop (interrupt) a {@link Shell}.
     */
    public abstract void stop();

    /**
     * Method to get the number of times {@link.this} {@link Shell} has been
     * restarted.
     *
     * @return  The restart count.
     */
    public abstract int restarts();
}
