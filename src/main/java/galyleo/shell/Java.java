package galyleo.shell;

import galyleo.shell.java.Imports;
import galyleo.shell.magic.Magic;
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
    private JShell jshell = null;
    private InputStream in = null;
    private PrintStream out = null;
    private PrintStream err = null;

    @Override
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
            try (var jshell = this.jshell) {
                this.jshell = null;

                if (jshell != null) {
                    restarts.incrementAndGet();
                }
            }
        }
    }

    @Override
    public void execute(String code) throws Exception {
        if (Magic.isCellMagic(code)) {
            Magic.execute(this, code);
        } else {
            java(code);
        }
    }

    private void java(String code) throws Exception {
        try {
            var iterator = parse(code).entrySet().iterator();

            while (iterator.hasNext()) {
                var entry = iterator.next();
                var info = entry.getValue();
                var events = jshell.eval(info.source());
                var exception =
                    events.stream()
                    .map(t -> t.exception())
                    .filter(Objects::nonNull)
                    .findFirst().orElse(null);

                if (exception != null) {
                    throw exception;
                }

                String reason =
                    events.stream()
                    .filter(t -> t.status().equals(REJECTED))
                    .map(t -> String.format("%s: %s",
                                            t.status(),
                                            t.snippet().source().trim()))
                    .findFirst().orElse(null);

                if (reason != null) {
                    throw new Exception(reason);
                }

                if (! iterator.hasNext()) {
                    if (! events.isEmpty()) {
                        var event = events.get(events.size() - 1);

                        switch (event.snippet().kind()) {
                        case EXPRESSION:
                        case VAR:
                            out.println(event.value());
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
        var analyzer = jshell.sourceCodeAnalysis();
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

    @Override
    public String evaluate(String expression) throws Exception {
        var analyzer = jshell.sourceCodeAnalysis();
        var info = analyzer.analyzeCompletion(expression);

        if (! info.completeness().isComplete()) {
            throw new IllegalArgumentException(expression);
        }

        return jshell.eval(info.source()).get(0).value();
    }

    @Override
    public void stop() {
        if (jshell != null) {
            jshell.stop();
        }
    }

    @Override
    public int restarts() { return restarts.intValue(); }
}
