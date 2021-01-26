package galyleo.shell;

import java.io.InputStream;
import java.io.PrintStream;
import jdk.jshell.JShell;
import jdk.jshell.SourceCodeAnalysis;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.system.ApplicationHome;

/**
 * Galyleo {@link Java} {@link Shell}.
 *
 * @see JShell
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@NoArgsConstructor @ToString @Log4j2
public class Java extends Shell {
    private JShell java = null;
    private int restarts = 0;

    @Override
    public void start(InputStream in, PrintStream out, PrintStream err) {
        synchronized (this) {
            java = JShell.builder().in(in).out(out).err(err).build();
            java.addToClasspath(new ApplicationHome().getSource().toString());
        }
    }

    @Override
    public void restart(InputStream in, PrintStream out, PrintStream err) {
        synchronized (this) {
            stop();
            close();
            start(in, out, err);
        }
    }

    @Override
    public void close() {
        synchronized (this) {
            try (var java = this.java) {
                this.java = null;

                if (java != null) {
                    restarts += 1;
                }
            }
        }
    }

    @Override
    public void execute(String code) throws Exception {
/*
        while (! isTerminating()) {
            var info = java.sourceCodeAnalysis().analyzeCompletion(code);

            switch (info.completeness()) {
            case COMPLETE:
                var events = java.eval(info.source());
                var exception =
                    events.stream()
                    .map(t -> t.exception())
                    .filter(Objects::nonNull)
                    .findFirst().orElse(null);

                if (exception != null) {
                    throw exception;
                }
                break;

            case EMPTY:
                break;

            case COMPLETE_WITH_SEMI:
            case CONSIDERED_INCOMPLETE:
            case DEFINITELY_INCOMPLETE:
            case UNKNOWN:
            default:
                throw new IllegalArgumentException(code);
                // break;
            }

            code = info.remaining();

            if (code.isEmpty()) {
                break;
            }
        }
*/
        java.eval(code);
    }

    /**
     * Method to evaluate an expression.
     *
     * @param   code            The code to execute.
     *
     * @return  The result of evaluating the expression.
     */
    @Override
    public Object evaluate(String code) throws Exception {
        var info = java.sourceCodeAnalysis().analyzeCompletion(code);

        if (info.completeness().equals(SourceCodeAnalysis.Completeness.COMPLETE_WITH_SEMI)) {
            throw new IllegalArgumentException("Code is not an expression: "
                                               + info.source());
        }

        var events = java.eval(info.source());

        return events.get(events.size() - 1).value();
    }

    @Override
    public void stop() {
        if (java != null) {
            java.stop();
        }
    }

    @Override
    public int restarts() { return restarts; }
}
