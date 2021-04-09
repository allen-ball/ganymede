package ganymede.shell;
/*-
 * ##########################################################################
 * Ganymede
 * %%
 * Copyright (C) 2021 Allen D. Ball
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ##########################################################################
 */
import ganymede.dependency.POM;
import ganymede.dependency.Resolver;
import ganymede.kernel.Kernel;
import ganymede.notebook.Notebook;
import ganymede.notebook.NotebookContext;
import ganymede.server.Message;
import ganymede.shell.magic.AnnotatedMagic;
import ganymede.shell.magic.Description;
import ganymede.shell.magic.MagicNames;
import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
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

import static ganymede.notebook.NotebookContext.unescape;
import static java.util.stream.Collectors.joining;
import static jdk.jshell.Snippet.Status.REJECTED;
import static org.apache.logging.log4j.Level.WARN;

/**
 * Ganymede {@link Shell}.
 *
 * @see JShell
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@ToString @Log4j2
public class Shell implements AutoCloseable {
    private static final String[] VMOPTIONS =
        Stream.of("--add-opens", "java.base/jdk.internal.misc=ALL-UNNAMED",
                  "-Dio.netty.tryReflectionSetAccessible=true",
                  "-Djava.awt.headless=true")
        .toArray(String[]::new);

    private final Kernel kernel;
    private Locale locale = null;       /* TBD: Query Notebook server */
    private final AtomicInteger restarts = new AtomicInteger(0);
    private final MagicMap magics = new MagicMap();
    private final Java java = new Java();
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

        Stream.of(java.getMagicNames()).forEach(t -> magics.put(t, java));
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
        /* stop(); */
        close();

        if (! kernel().isTerminating()) {
            start(in, out, err);
        }
    }

    /**
     * Method to close (terminate) a {@link Shell}.
     */
    @Override
    @Synchronized
    public void close() {
        Stream.of(java.getMagicNames()).forEach(t -> magics.remove(t));

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
     * {@link Resolver#resolve(Shell,PrintStream,PrintStream,POM)}.
     *
     * @param   pom             The {@link POM} to merge.
     */
    public void resolve(POM pom) {
        for (var file : resolver().resolve(this, out, err, pom)) {
            var jshell = this.jshell;

            if (jshell != null) {
                jshell.addToClasspath(file.toString());
            } else {
                break;
            }
        }
    }

    /**
     * Method to add resolved paths to the {@link JShell} instance.  See
     * {@link JShell#addToClasspath(String)}.
     *
     * @param   files           The {@link File}s to add.
     */
    @Synchronized
    public void addToClasspath(File... files) {
        for (var file : resolver.addToClasspath(files)) {
            var jshell = this.jshell;

            if (jshell != null) {
                jshell.addToClasspath(file.toString());
            } else {
                break;
            }
        }
    }

    /**
     * Method to add known dependencies found within a parent directory to
     * the {@link Resolver#classpath()}.  See
     * {@link Resolver#addKnownDependenciesToClasspath(File)}.
     *
     * @param   parent          The parent {@link File} to analyze.
     */
    @Synchronized
    public void addKnownDependenciesToClasspath(File parent) {
        for (var file : resolver.addKnownDependenciesToClasspath(parent)) {
            var jshell = this.jshell;

            if (jshell != null) {
                jshell.addToClasspath(file.toString());
            } else {
                break;
            }
        }
    }

    /**
     * Method to get the current classpath.  See
     * {@link Resolver#classpath()}.
     *
     * @return  The {@link Set} of {@link File}s.
     */
    public Set<File> classpath() { return resolver.classpath(); }

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
            options.add("-D" + Map.entry(Kernel.PORT_PROPERTY, kernel.getPort()));

            if (! kernel.isTerminating()) {
                try {
                    jshell =
                        JShell.builder()
                        .remoteVMOptions(options.toArray(new String[] { }))
                        .in(in).out(out).err(err).build();

                    resolver().classpath()
                        .forEach(t -> jshell.addToClasspath(t.toString()));

                    var logIn =
                        IoBuilder.forLogger(log)
                        .setLevel(WARN)
                        .filter(InputStream.nullInputStream())
                        .buildInputStream();
                    var logOut =
                        IoBuilder.forLogger(log)
                        .setLevel(WARN)
                        .buildPrintStream();

                    java.execute(jshell, logIn, logOut, logOut, Notebook.bootstrap());
                } catch (Exception exception) {
                    log.warn("{}", exception, exception);
                }
            }
        }

        return jshell;
    }

    /**
     * Method to execute code (typically a cell's contents).
     *
     * @param   code            The code to execute.
     */
    public void execute(String code) throws Exception {
        try {
            var jshell = this.jshell;

            if (jshell != null) {
                NotebookContext.update(jshell);
            }

            var application = new Magic.Application(code);
            var name = application.getMagicName();

            if (name != null && (! magics.containsKey(name))) {
                throw new IllegalArgumentException(application.getLine0());
            }

            var magic = (name != null) ? magics.get(name) : java;

            application.apply(magic, this, in, out, err);
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
        var result = "";
        var jshell = jshell();

        if (jshell != null) {
            var analyzer = jshell.sourceCodeAnalysis();
            var info = analyzer.analyzeCompletion(expression);

            if (! info.completeness().isComplete()) {
                throw new IllegalArgumentException(expression);
            }

            result = unescape(jshell.eval(info.source()).get(0).value());
        }

        return result;
    }

    /**
     * Method to determine code's {@link Message.completeness completeness}.
     *
     * @param   code            The code to execute.
     *
     * @return  The code's {@link Message.completeness completeness}.
     */
    public Message.completeness isComplete(String code) {
        var completeness = Message.completeness.unknown;
        var application = new Magic.Application(code);
        var name = application.getMagicName();

        if (name == null || magics.containsKey(name)) {
            var magic = (name != null) ? magics.get(name) : java;

            completeness =
                magic.isComplete(application.getLine0(), application.getCode());
        } else {
            completeness = Message.completeness.invalid;
        }

        return completeness;
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

    private SortedMap<Integer,SourceCodeAnalysis.CompletionInfo> parse(JShell jshell, String code) {
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

    @MagicNames({ "java" })
    @Description("Execute code in Java REPL")
    @NoArgsConstructor @ToString
    private class Java implements AnnotatedMagic {
        @Override
        public void execute(Shell shell,
                            InputStream in, PrintStream out, PrintStream err,
                            String line0, String code) throws Exception {
            if (! code.isBlank()) {
                execute(shell.jshell(), in, out, err, code);
            } else {
                if (line0 != null) {
                    Magic.sendTo(shell, getMagicNames()[0], line0, code);
                }
            }
        }

        @Override
        public Message.completeness isComplete(String line0, String code) {
            var completeness = Message.completeness.unknown;

            if (! code.isBlank()) {
                var jshell = Shell.this.jshell;

                if (jshell != null) {
                    var map = parse(jshell, code);

                    if (! map.isEmpty()) {
                        var info = map.get(map.lastKey());

                        if (info.completeness().isComplete()) {
                            completeness = Message.completeness.complete;
                        } else {
                            switch (info.completeness()) {
                            case CONSIDERED_INCOMPLETE:
                            case DEFINITELY_INCOMPLETE:
                                completeness = Message.completeness.incomplete;
                                break;

                            case UNKNOWN:
                                completeness = Message.completeness.invalid;
                                break;

                            default:
                                break;
                            }
                        }
                    } else {
                        completeness = Message.completeness.invalid;
                    }
                }
            } else {
                completeness = Message.completeness.incomplete;
            }

            return completeness;
        }

        @Override
        public void execute(NotebookContext __, String line0, String code) throws Exception {
            throw new IllegalStateException();
        }

        protected void execute(JShell jshell,
                               InputStream in, PrintStream out, PrintStream err,
                               String code) throws Exception {
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
    }
}
