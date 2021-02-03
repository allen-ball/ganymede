package galyleo.shell;

import galyleo.shell.magic.Magic;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import jdk.jshell.JShell;
import jdk.jshell.JShellException;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

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
    private final List<String> classpath = new ArrayList<>();

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
            classpath.forEach(t -> jshell.addToClasspath(t));
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
            try (var jshell = jshell()) {
                this.jshell = null;

                if (jshell != null) {
                    restarts.incrementAndGet();
                }
            }
        }
    }

    /**
     * Method to search for and add jars to the {@link JShell} instance
     * {@code classpath}.  See {@link #addToClasspath(String...)}.
     *
     * @param   paths           The diretcory path(s) to search.
     */
    public void addJarsToClasspath(String... paths) throws IOException {
        for (var path : paths) {
            try (var stream =
                     Files.newDirectoryStream(Paths.get(path), "*.jar")) {
                for (var entry : stream) {
                    addToClasspath(entry.toString());
                }
            } catch (DirectoryIteratorException exception) {
                throw exception.getCause();
            }
        }
    }

    /**
     * Accessor to the active {@link JShell} instance.
     *
     * @return  The {@link JShell} instance.
     */
    public JShell jshell() { return jshell; }

    /**
     * Method to manage and add path(s) to the {@link JShell} instance.  See
     * {@link JShell#addToClasspath(String)}.
     *
     * @param   paths           The path(s) to add.
     */
    public void addToClasspath(String... paths) {
        for (var path : paths) {
            if (! classpath.contains(path)) {
                classpath.add(path);

                var jshell = jshell();

                if (jshell != null) {
                    jshell.addToClasspath(path);
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
                    Magic.MAP.get(name)
                        .execute(this, in, out, err, magic, code);
                } else {
                    throw new IllegalArgumentException(magic);
                }
            } else {
                Magic.MAP.get("java")
                    .execute(this, in, out, err, null, code);
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
        var jshell = jshell();
        var analyzer = jshell.sourceCodeAnalysis();
        var info = analyzer.analyzeCompletion(expression);

        if (! info.completeness().isComplete()) {
            throw new IllegalArgumentException(expression);
        }

        return unescape(jshell.eval(info.source()).get(0).value());
    }

    /**
     * https://stackoverflow.com/questions/3537706/how-to-unescape-a-java-string-literal-in-java
     */
    private String unescape(String literal) {
        var string = literal;

        if (literal != null) {
            try {
                var parser = new StreamTokenizer(new StringReader(literal));

                parser.nextToken();

                if (parser.ttype == '"') {
                    string = parser.sval;
                }
            } catch (IOException exception) {
            }
        }

        return string;
    }

    /**
     * Method to stop (interrupt) a {@link Shell}.
     */
    public void stop() {
        var jshell = jshell();

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
