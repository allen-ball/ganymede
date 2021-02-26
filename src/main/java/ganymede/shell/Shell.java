package ganymede.shell;

import ganymede.dependency.POM;
import ganymede.dependency.Resolver;
import ganymede.kernel.Client;
import ganymede.kernel.Kernel;
import ganymede.server.Message;
import ganymede.shell.jshell.CellMethods;
import ganymede.shell.magic.AnnotatedMagic;
import ganymede.shell.magic.Description;
import ganymede.shell.magic.MagicNames;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import jdk.jshell.JShell;
import jdk.jshell.JShellException;
import jdk.jshell.SourceCodeAnalysis;
import lombok.Synchronized;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.io.IoBuilder;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;
import static jdk.jshell.Snippet.Status.REJECTED;
import static org.apache.logging.log4j.Level.WARN;

/**
 * Ganymede {@link Shell}.
 *
 * @see JShell
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@ToString @Log4j2
@MagicNames({ "java" }) @Description("Execute code in Java REPL")
public class Shell implements AnnotatedMagic, AutoCloseable {
    private static final String[] VMOPTIONS =
        Stream.of("--add-opens", "java.base/jdk.internal.misc=ALL-UNNAMED",
                  "-Dio.netty.tryReflectionSetAccessible=true",
                  "-Djava.awt.headless=true")
        .toArray(String[]::new);
    private static final File KERNEL_JAR =
        new ApplicationHome(Shell.class).getSource();

    private final Kernel kernel;
    private Locale locale = null;       /* TBD: Query Notebook server */
    private final AtomicInteger restarts = new AtomicInteger(0);
    private final MagicMap magics = new MagicMap();
    private JShell jshell = null;
    private InputStream in = null;
    private PrintStream out = null;
    private PrintStream err = null;
    private final Resolver resolver = new Resolver();

    /**
     * Sole constructor.
     *
     * @param   kernel          The {@link Kernel}.
     */
    public Shell(Kernel kernel) {
        this.kernel = Objects.requireNonNull(kernel);
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

        magics.clear();
        magics.reload();

        Stream.of(getMagicNames()).forEach(t -> magics.put(t, this));
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
        Stream.of(getMagicNames()).forEach(t -> magics.remove(t));

        try (var jshell = this.jshell) {
            this.jshell = null;

            if (jshell != null) {
                restarts.incrementAndGet();
            }
        }
    }

    /**
     * Method to get the {@link Kernel}.
     *
     * @return  The {@link Kernel}.
     */
    public Kernel kernel() { return kernel; }

    /**
     * Method to get the {@link Map} of configured {@link Magic}s.
     *
     * @return  The {@link Map} of configured {@link Magic}s.
     */
    public Map<String,Magic> magics() { return magics; }

    /**
     * Method to get the {@link Resolver}.
     *
     * @return  The {@link Resolver}.
     */
    public Resolver resolver() { return resolver; }

    /**
     * Method to call
     * {@link Resolver#resolve(Shell,POM,PrintStream,PrintStream)}.
     *
     * @param   pom             The {@link POM} to merge.
     */
    public void resolve(POM pom) {
        var list = resolver().resolve(this, pom, out, err);

        for (var file : list) {
            var jshell = this.jshell;

            if (jshell != null) {
                jshell.addToClasspath(file.toString());
            } else {
                break;
            }
        }
    }

    /**
     * Method to search for and add jars to the {@link JShell} instance
     * {@code classpath}.  See {@link #addToClasspath(File...)}.
     *
     * @param   files           The directories ({@link File}s) to search.
     */
    public void addJarsToClasspath(File... files) throws IOException {
        for (var file : files) {
            var path = file.toPath().toAbsolutePath();

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
     * Method to add resolved a paths to the {@link JShell} instance.  See
     * {@link JShell#addToClasspath(String)}.
     *
     * @param   files           The {@link File} to add.
     */
    @Synchronized
    public void addToClasspath(File... files) {
        for (var file : files) {
            file = file.getAbsoluteFile();

            if (! resolver().classpath().contains(file)) {
                resolver().addToClasspath(file);

                var jshell = this.jshell;

                if (jshell != null) {
                    jshell.addToClasspath(file.toString());
                }
            }
        }
    }

    /**
     * Accessor to the {@link JShell} instance (created and initialized on
     * first call).
     *
     * @return  The {@link JShell} instance.
     */
    @Synchronized
    public JShell jshell() {
        if (jshell == null) {
            var options = new ArrayList<String>();
            var definitions =
                Stream.of(ProcessHandle.current().info().arguments())
                .flatMap(Optional::stream)
                .flatMap(Stream::of)
                .takeWhile(t -> (! Objects.equals(t, "-jar")))
                .filter(t -> t.startsWith("-D"))
                .toArray(String[]::new);

            Collections.addAll(options, definitions);
            Collections.addAll(options, VMOPTIONS);
            options.add("-D" + Map.entry(Client.KERNEL_PORT_PROPERTY, kernel.getPort()));

            jshell =
                JShell.builder()
                .remoteVMOptions(options.toArray(new String[] { }))
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
                                  CellMethods.class.getName());

                execute(jshell, logIn, logOut, logOut, null, bootstrap);
            } catch (Exception exception) {
                log.warn("{}", exception, exception);
            }

            resolver().classpath()
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

                if (argv.length > 0 && magics.containsKey(argv[0])) {
                    magics.get(argv[0])
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
