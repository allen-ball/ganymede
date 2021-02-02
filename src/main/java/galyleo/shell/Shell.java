package galyleo.shell;

import galyleo.shell.java.Imports;
import galyleo.shell.magic.Magic;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import jdk.jshell.JShell;
import jdk.jshell.JShellException;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.system.ApplicationHome;

import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;
import static java.util.stream.Collectors.toSet;
import static jdk.jshell.Snippet.Status.REJECTED;

/**
 * Galyleo {@link Shell}.
 *
 * @see JShell
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@NoArgsConstructor @ToString @Log4j2
public class Shell implements AutoCloseable {
    private final AtomicInteger restarts = new AtomicInteger(0);
    private JShell jshell = null;
    private InputStream in = null;
    private PrintStream out = null;
    private PrintStream err = null;

    /**
     * Method to start a {@link Shell}.
     *
     * @param   in              The {@code in} {@link InputStream}.
     * @param   out             The {@code out} {@link PrintStream}.
     * @param   err             The {@code err} {@link PrintStream}.
     */
    public void start(InputStream in, PrintStream out, PrintStream err) {
        synchronized (this) {
            this.in = in;
            this.out = out;
            this.err = err;

            jshell = JShell.builder().in(in).out(out).err(err).build();
            jshell.addToClasspath(new ApplicationHome(getClass()).getSource().toString());

            var analyzer = jshell.sourceCodeAnalysis();
            var imports =
                Stream.of(Imports.class.getDeclaredMethods())
                .filter(t -> isPublic(t.getModifiers()) && isStatic(t.getModifiers()))
                .map(t -> String.format("import static %s.%s;",
                                        t.getDeclaringClass().getName(), t.getName()))
                .map(t -> analyzer.analyzeCompletion(t))
                .collect(toSet());

            for (var analysis : imports) {
                jshell.eval(analysis.source())
                    .stream()
                    .filter(t -> t.status().equals(REJECTED))
                    .forEach(t -> log.warn("{}: {}",
                                           t.status(),
                                           t.snippet().source().trim()));
            }
        }
    }

    /**
     * Method to restart a {@link Shell}.
     *
     * @param   in              The {@code in} {@link InputStream}.
     * @param   out             The {@code out} {@link PrintStream}.
     * @param   err             The {@code err} {@link PrintStream}.
     */
    public void restart(InputStream in, PrintStream out, PrintStream err) {
        synchronized (this) {
            stop();
            close();
            start(in, out, err);
        }
    }

    /**
     * Method to close (terminate) a {@link Shell}.
     */
    @Override
    public void close() {
        synchronized (this) {
            try (var jshell = this.jshell) {
                this.jshell = null;

                if (jshell != null) {
                    restarts.incrementAndGet();
                }
            }
        }
    }

    /**
     * Method to execute code (typically a cell's contents).
     *
     * @param   code            The code to execute.
     */
    public void execute(String code) throws Exception {
        try {
            if (Magic.isCellMagic(code)) {
                var pair = code.split("\\R", 2);
                var magic = pair[0];

                code = (pair.length > 1 && pair[1] != null) ? pair[1] : "";

                var name = magic.substring(Magic.CELL.length()).split("\\s+")[0];

                if (Magic.MAP.containsKey(name)) {
                    Magic.MAP.get(name).execute(jshell, in, out, err, magic, code);
                } else {
                    throw new IllegalArgumentException(magic);
                }
            } else {
                Magic.MAP.get("java").execute(jshell, in, out, err, null, code);
            }
        } catch (JShellException exception) {
            exception.printStackTrace(err);
            throw exception;
        } catch (Exception exception) {
            err.println(exception);
            throw exception;
        }
    }

    /**
     * Method to evaluate an expression.
     *
     * @param   expression      The code to evaluate.
     *
     * @return  The result of evaluating the expression.
     */
    public String evaluate(String expression) throws Exception {
        return Magic.evaluate(jshell, expression);
    }

    /**
     * Method to stop (interrupt) a {@link Shell}.
     */
    public void stop() {
        if (jshell != null) {
            jshell.stop();
        }
    }

    /**
     * Method to get the number of times {@link.this} {@link Shell} has been
     * restarted.
     *
     * @return  The restart count.
     */
    public int restarts() { return restarts.intValue(); }
}
