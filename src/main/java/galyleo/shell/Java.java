package galyleo.shell;

import galyleo.shell.java.Imports;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import jdk.jshell.JShell;
import jdk.jshell.SourceCodeAnalysis;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.system.ApplicationHome;

import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;
import static java.util.stream.Collectors.toSet;
import static jdk.jshell.Snippet.Status.REJECTED;

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
    private final AtomicInteger restarts = new AtomicInteger(0);
    private JShell java = null;
    private InputStream in = null;
    private PrintStream out = null;
    private PrintStream err = null;

    @Override
    public void start(InputStream in, PrintStream out, PrintStream err) {
        synchronized (this) {
            this.in = in;
            this.out = out;
            this.err = err;

            java = JShell.builder().in(in).out(out).err(err).build();
            java.addToClasspath(new ApplicationHome(getClass()).getSource().toString());

            var analyzer = java.sourceCodeAnalysis();
            var imports =
                Stream.of(Imports.class.getDeclaredMethods())
                .filter(t -> isPublic(t.getModifiers()) && isStatic(t.getModifiers()))
                .map(t -> String.format("import static %s.%s;",
                                        t.getDeclaringClass().getName(), t.getName()))
                .map(t -> analyzer.analyzeCompletion(t))
                .collect(toSet());

            for (var snippet : imports) {
                java.eval(snippet.source())
                    .stream()
                    .filter(t -> t.status().equals(REJECTED))
                    .forEach(t -> log.warn("{}: {}",
                                           t.status(),
                                           t.snippet().source().trim()));
            }
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
                    restarts.incrementAndGet();
                }
            }
        }
    }

    @Override
    public void execute(String code) throws Exception {
        try {
            var iterator = parse(code).entrySet().iterator();

            while (iterator.hasNext()) {
                var entry = iterator.next();
                var info = entry.getValue();
                var events = java.eval(info.source());
                var exception =
                    events.stream()
                    .map(t -> t.exception())
                    .filter(Objects::nonNull)
                    .findFirst().orElse(null);

                if (exception != null) {
                    throw exception;
                }

                var rejected =
                    events.stream()
                    .filter(t -> t.status().equals(REJECTED))
                    .findFirst().orElse(null);

                if (rejected != null) {
                    String message =
                        String.format("%s: %s",
                                      rejected.status(),
                                      rejected.snippet().source().trim());

                    throw new Exception(message);
                }

                if (! iterator.hasNext()) {
                    if (! events.isEmpty()) {
                        var event = events.get(events.size() - 1);

                        switch (event.snippet().kind()) {
                        case EXPRESSION:
                        case VAR:
                            out.println(String.valueOf(event.value()));
                            break;

                        default:
                            break;
                        }
                    }
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace(err);
            throw exception;
        } finally {
            out.flush();
            err.flush();
        }
    }

    private Map<Integer,SourceCodeAnalysis.CompletionInfo> parse(String code) {
        var map = new TreeMap<Integer,SourceCodeAnalysis.CompletionInfo>();
        var analyzer = java.sourceCodeAnalysis();
        var offset = 0;
        var remaining = code;

        while (! remaining.isEmpty()) {
            var value = analyzer.analyzeCompletion(remaining);

            map.put(offset, value);

            offset += value.source().length();
            remaining = remaining.substring(value.source().length());
        }

        return map;
    }

    /**
     * Method to evaluate an expression.
     *
     * @param   code            The code to execute.
     *
     * @return  The result of evaluating the expression.
     */
    @Override
    public String evaluate(String code) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public void stop() {
        if (java != null) {
            java.stop();
        }
    }

    @Override
    public int restarts() { return restarts.intValue(); }
}
