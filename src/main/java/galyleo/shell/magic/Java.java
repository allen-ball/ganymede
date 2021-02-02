package galyleo.shell.magic;

import ball.annotation.ServiceProviderFor;
import galyleo.shell.Shell;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import jdk.jshell.JShell;
import jdk.jshell.SourceCodeAnalysis;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

import static jdk.jshell.Snippet.Status.REJECTED;

/**
 * {@link Java} {@link Magic}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@ServiceProviderFor({ Magic.class })
@NoArgsConstructor @ToString @Log4j2
public class Java implements AnnotatedMagic {
    @Override
    public void execute(Shell shell,
                        InputStream in, PrintStream out, PrintStream err,
                        String magic, String code) throws Exception {
        try {
            var jshell = shell.jshell();
            var iterator = parse(jshell, code).entrySet().iterator();

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
        } finally {
            out.flush();
            err.flush();
        }
    }

    private Map<Integer,SourceCodeAnalysis.CompletionInfo> parse(JShell jshell, String code) {
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
    public void execute(String magic, String code) throws Exception {
        throw new IllegalArgumentException(magic);
    }
}
