package galyleo.shell;

import galyleo.dependency.Analyzer;
import galyleo.server.Message;
import galyleo.shell.jshell.RemoteRuntime;
import galyleo.shell.magic.AnnotatedMagic;
import galyleo.shell.magic.MagicNames;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import jdk.jshell.JShell;
import jdk.jshell.JShellException;
import jdk.jshell.SourceCodeAnalysis;
import lombok.NoArgsConstructor;
import lombok.Synchronized;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.io.IoBuilder;
import org.apache.maven.artifact.Artifact;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static jdk.jshell.Snippet.Status.REJECTED;
import static org.apache.logging.log4j.Level.WARN;

/**
 * Galyleo {@link Shell}.
 *
 * @see JShell
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@NoArgsConstructor @ToString @Log4j2
@MagicNames({ "java" })
public class Shell implements AnnotatedMagic, AutoCloseable {
    private static final String[] VMOPTIONS =
        Stream.of("--add-opens", "java.base/jdk.internal.misc=ALL-UNNAMED",
                  "-Dio.netty.tryReflectionSetAccessible=true")
        .toArray(String[]::new);
    private static final File KERNEL_JAR =
        new ApplicationHome(Shell.class).getSource();

    private Locale locale = null;       /* TBD: Query Notebook server */
    private final AtomicInteger restarts = new AtomicInteger(0);
    private final MagicMap magic = new MagicMap();
    private JShell jshell = null;
    private InputStream in = null;
    private PrintStream out = null;
    private PrintStream err = null;
    private final Analyzer analyzer = new Analyzer();
    private final Map<File,Set<Artifact>> classpath = new LinkedHashMap<>();

    /**
     * Accessor to the {@link JShell} instance (created and initialized on
     * first call).
     *
     * @return  The {@link JShell} instance.
     */
    @Synchronized
    public JShell jshell() {
        if (jshell == null) {
            jshell =
                JShell.builder()
                .remoteVMOptions(VMOPTIONS)
                .in(in).out(out).err(err).build();

            try {
                var logIn =
                    IoBuilder.forLogger(log)
                    .setLevel(WARN)
                    .filter(InputStream.nullInputStream())
                    .buildInputStream();
                var logOut =
                    IoBuilder.forLogger(log)
                    .setLevel(WARN)
                    .buildPrintStream();
                var bootstrap =
                    String.format(getResourceAsString("bootstrap.jsh"),
                                  KERNEL_JAR.toURI().toURL(),
                                  RemoteRuntime.class.getName());

                execute(jshell, logIn, logOut, logOut, null, bootstrap);
            } catch (Exception exception) {
                log.warn("{}", exception, exception);
            }

            classpath.keySet()
                .forEach(t -> jshell.addToClasspath(t.toString()));
        }

        return jshell;
    }

    private String getResourceAsString(String name) throws Exception {
        String string = null;
        var resource = new ClassPathResource(name);

        try (var in = resource.getInputStream()) {
            string = StreamUtils.copyToString(in, UTF_8);
        }

        return string;
    }

    /**
     * Method to start a {@link Shell}.
     *
     * @param   in              The {@code in} {@link InputStream}.
     * @param   out             The {@code out} {@link PrintStream}.
     * @param   err             The {@code err} {@link PrintStream}.
     */
    @Synchronized
    public void start(InputStream in, PrintStream out, PrintStream err) {
        this.in = in;
        this.out = out;
        this.err = err;

        magic.clear();
        magic.reload();

        Stream.of(getMagicNames()).forEach(t -> magic.put(t, this));
    }

    /**
     * Method to restart a {@link Shell}.
     *
     * @param   in              The {@code in} {@link InputStream}.
     * @param   out             The {@code out} {@link PrintStream}.
     * @param   err             The {@code err} {@link PrintStream}.
     */
    @Synchronized
    public void restart(InputStream in, PrintStream out, PrintStream err) {
        stop();
        close();
        start(in, out, err);
    }

    /**
     * Method to close (terminate) a {@link Shell}.
     */
    @Override
    @Synchronized
    public void close() {
        Stream.of(getMagicNames()).forEach(t -> magic.remove(t));

        try (var jshell = this.jshell) {
            this.jshell = null;

            if (jshell != null) {
                restarts.incrementAndGet();
            }
        }
    }

    /**
     * Method to search for and add jars to the {@link JShell} instance
     * {@code classpath}.  See {@link #addToClasspath(String...)}.
     *
     * @param   files           The directories ({@link File}s) to search.
     */
    public void addJarsToClasspath(File... files) throws IOException {
        addJarsToClasspath(Stream.of(files).map(File::toPath).toArray(Path[]::new));
    }

    /**
     * Method to search for and add jars to the {@link JShell} instance
     * {@code classpath}.  See {@link #addToClasspath(String...)}.
     *
     * @param   paths           The directory path(s) to search.
     */
    public void addJarsToClasspath(Path... paths) throws IOException {
        for (var path : paths) {
            path = path.toAbsolutePath();

            try (var stream = Files.newDirectoryStream(path, "*.jar")) {
                for (var entry : stream) {
                    addToClasspath(entry.toFile());
                }
            } catch (DirectoryIteratorException exception) {
                throw exception.getCause();
            }
        }
    }

    /**
     * Method to search for and add jars to the {@link JShell} instance
     * {@code classpath}.  See {@link #addToClasspath(String...)}.
     *
     * @param   paths           The directory path(s) to search.
     */
    public void addJarsToClasspath(String... paths) throws IOException {
        addJarsToClasspath(Stream.of(paths).map(Paths::get).toArray(Path[]::new));
    }

    private void addToClasspath(File... files) {
        for (var file : files) {
            file = file.getAbsoluteFile();

            if (! classpath.containsKey(file)) {
                addToClasspath(file, analyzer.getJarArtifacts(file));
            }
        }
    }

    /**
     * Method to manage and add path(s) to the {@link JShell} instance.  See
     * {@link JShell#addToClasspath(String)}.
     *
     * @param   paths           The path(s) to add.
     */
    public void addToClasspath(Path... paths) {
        addToClasspath(Stream.of(paths).map(Path::toFile).toArray(File[]::new));
    }

    /**
     * Method to manage and add path(s) to the {@link JShell} instance.  See
     * {@link JShell#addToClasspath(String)}.
     *
     * @param   paths           The path(s) to add.
     */
    public void addToClasspath(String... paths) {
        addToClasspath(Stream.of(paths).map(File::new).toArray(File[]::new));
    }

    /**
     * Method to add resolved a path to the {@link JShell} instance.  See
     * {@link JShell#addToClasspath(String)}.
     *
     * @param   file            The {@link File} to add.
     * @param   collection      The {@link Collection} of {@link Artifact}s
     *                          this {@link File} represents.
     */
    @Synchronized
    protected void addToClasspath(File file, Collection<Artifact> collection) {
        if (! classpath.containsKey(file)) {
            if (classpath.put(file, collection.stream().collect(toSet())) == null) {
                var jshell = this.jshell;

                if (jshell != null) {
                    jshell.addToClasspath(file.toString());
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
                var lines = code.split("\\R", 2);
                var line0 = lines[0];

                code = (lines.length > 1) ? lines[1] : "";

                var argv = Magic.getCellMagicCommand(line0);

                if (argv.length > 0 && magic.containsKey(argv[0])) {
                    magic.get(argv[0])
                        .execute(this, in, out, err, line0, code);
                } else {
                    throw new IllegalArgumentException(line0);
                }
            } else {
                execute(this, in, out, err, null, code);
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
    @Synchronized
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
            try (var reader = new StringReader(literal)) {
                var tokenizer = new StreamTokenizer(reader);

                tokenizer.nextToken();

                if (tokenizer.ttype == '"') {
                    string = tokenizer.sval;
                }
            } catch (IOException exception) {
            }
        }

        return string;
    }

    /**
     * Method to determine code's {@link Message.completeness completeness}.
     *
     * @param   code            The code to execute.
     *
     * @return  The code's {@link Message.completeness completeness}.
     */
    public Message.completeness isComplete(String code) {
        return Message.completeness.unknown;
    }

    /**
     * Method to stop (interrupt) a {@link Shell}.
     */
    @Synchronized
    public void stop() {
        var jshell = this.jshell;

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

    @Override
    @Synchronized
    public void execute(Shell shell,
                        InputStream in, PrintStream out, PrintStream err,
                        String magic, String code) throws Exception {
        execute(shell.jshell(), in, out, err, magic, code);
    }

    private void execute(JShell jshell,
                         InputStream in, PrintStream out, PrintStream err,
                         String magic, String code) throws Exception {
        try {
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
                    exception.printStackTrace(err);
                    break;
                }

                var reason =
                    events.stream()
                    .filter(t -> t.status().equals(REJECTED))
                    .map(t -> jshell.diagnostics(t.snippet())
                              .map(u -> u.getMessage(locale))
                              .collect(joining("\n",
                                               String.format("%s %s\n%s\n",
                                                             t.status(),
                                                             t.snippet().kind(),
                                                             t.snippet().source()),
                                               "")))
                    .findFirst().orElse(null);

                if (reason != null) {
                    err.println(reason);
                    break;
                }

                if (! iterator.hasNext()) {
                    if (! events.isEmpty()) {
                        var event = events.get(events.size() - 1);

                        switch (event.snippet().kind()) {
                        case EXPRESSION:
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
        throw new IllegalStateException();
    }
}
