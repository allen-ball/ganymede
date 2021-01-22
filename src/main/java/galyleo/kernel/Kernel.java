package galyleo.kernel;

import galyleo.io.PrintStreamBuffer;
import galyleo.server.Channel;
import galyleo.server.Connection;
import galyleo.server.Dispatcher;
import galyleo.server.Message;
import galyleo.server.Server;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Named;
import javax.inject.Singleton;
import jdk.jshell.JShell;
import jdk.jshell.SourceCodeAnalysis;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.zeromq.SocketType;
import org.zeromq.ZMQ;

/**
 * Galyleo Jupyter {@link Kernel}.
 *
 * @see JShell
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@Named @Singleton
@SpringBootApplication
@NoArgsConstructor @ToString @Log4j2
public class Kernel extends Server implements ApplicationRunner {
    private final Channel.Heartbeat heartbeat = new Channel.Heartbeat(this);
    private final Channel.Control control = new Control();
    private final Channel.IOPub iopub = new Channel.IOPub(this);
    private final Channel.Stdin stdin = new Stdin();
    private final Channel.Shell shell = new Shell();
    private final AtomicInteger execution_count = new AtomicInteger(0);
    private final PrintStreamBuffer out = new PrintStreamBuffer();
    private final PrintStreamBuffer err = new PrintStreamBuffer();
    private JShell java = null;
    private int restarts = 0;

    @PostConstruct
    public void init() {
        out.reset();
        err.reset();
        java =
            JShell.builder()
            .in(new ByteArrayInputStream(new byte[] { }))
            .out(out).err(err)
            .build();

        setSession(String.join("-",
                               Kernel.class.getCanonicalName(),
                               String.valueOf(ProcessHandle.current().pid()),
                               String.valueOf(restarts)));
    }

    @PreDestroy
    public void destroy() { shutdown(); }

    /**
     * Add a connection specified by a {@link Connection} {@link File}.
     *
     * @param   path            The path to the {@link Connection}
     *                          {@link File}.
     *
     * @throws  IOException     If the {@link File} cannot be opened or
     *                          parsed.
     */
    public void listen(String path) throws IOException {
        var mapper = getObjectMapper();
        var node = mapper.readTree(new File(path));
        var properties = mapper.treeToValue(node, Connection.Properties.class);
        var connection = new Connection(properties);

        connection.connect(shell, control, iopub, stdin, heartbeat);

        log.info("Listening to {}", connection);
    }

    @Override
    public void run(ApplicationArguments arguments) throws Exception {
        var paths = arguments.getOptionValues("connection-file");

        if (! paths.isEmpty()) {
            for (var path : paths) {
                try {
                    listen(path);
                } catch (Exception exception) {
                    log.warn("{}", exception);
                }
            }
        } else {
            throw new IllegalArgumentException("No connection file specified");
        }
    }

    /**
     * Method to execute code (typically a cell's contents).
     *
     * @param   code            The code to execute.
     */
    protected void execute(String code) throws Exception {
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
    protected Object evaluate(String code) throws Exception {
        var info = java.sourceCodeAnalysis().analyzeCompletion(code);

        if (info.completeness().equals(SourceCodeAnalysis.Completeness.COMPLETE_WITH_SEMI)) {
            throw new IllegalArgumentException("Code is not an expression: "
                                               + info.source());
        }

        var events = java.eval(info.source());

        return events.get(events.size() - 1).value();
    }

    @ToString
    private class Control extends Channel.Control {
        public Control() { super(Kernel.this); }

        @Override
        protected void dispatch(Dispatcher dispatcher, ZMQ.Socket socket, Message message) {
            if (message.isRequest()) {
                super.dispatch(dispatcher, socket, message);
            } else {
                log.warn("Ignoring non-request {}", message.msg_type());
            }
        }

        private void shutdown(Dispatcher dispatcher, ZMQ.Socket socket,
                              Message request, Message reply) throws Exception {
            var restart = request.content().at("/restart").asBoolean();
            var old = java;

            java = null;

            if (old != null) {
                submit(() -> { old.stop(); old.close(); });
            }

            if (restart) {
                java =
                    JShell.builder()
                    .in(new ByteArrayInputStream(new byte[] { }))
                    .out(out).err(err)
                    .build();
                out.reset();
                err.reset();
            }

            restarts += 1;

            setSession(String.join("-",
                                   Kernel.class.getCanonicalName(),
                                   String.valueOf(ProcessHandle.current().pid()),
                                   String.valueOf(restarts)));

            if (! restart) {
                submit(() -> getServer().shutdown());
            }
        }

        private void interrupt(Dispatcher dispatcher, ZMQ.Socket socket,
                               Message request, Message reply) throws Exception {
            java.stop();
        }

        private void debug(Dispatcher dispatcher, ZMQ.Socket socket,
                           Message request, Message reply) throws Exception {
            throw new UnsupportedOperationException();
        }
    }

    @ToString
    private class Stdin extends Channel.Stdin {
        public Stdin() { super(Kernel.this); }

        @Override
        protected void dispatch(Dispatcher dispatcher, ZMQ.Socket socket, Message message) {
            if (message.isReply()) {
                log.warn("Ignoring {}", message.msg_type());
            } else {
                log.warn("Ignoring non-reply {}", message.msg_type());
            }
        }
    }

    @ToString
    private class Shell extends Channel.Shell {
        public Shell() {
            super(Kernel.this, Kernel.this.iopub, Kernel.this.stdin);
        }

        @Override
        protected void dispatch(Dispatcher dispatcher, ZMQ.Socket socket, Message message) {
            if (message.isRequest()) {
                super.dispatch(dispatcher, socket, message);
            } else {
                log.warn("Ignoring non-request {}", message.msg_type());
            }
        }

        private void kernel_info(Dispatcher dispatcher, ZMQ.Socket socket,
                                 Message request, Message reply) throws Exception {
            var content = reply.content();

            content.put("protocol_version", PROTOCOL_VERSION);
            content.put("implementation", "galyleo");
            content.put("implementation_version", "1.0.0");

            var language_info = content.with("language_info");

            language_info.put("name", "java");
            language_info.put("version", System.getProperty("java.version"));
            language_info.put("mimetype", "text/plain");
            language_info.put("file_extension", ".java");

            content.set("language_info", language_info);

            var help_links = content.with("help_links");
        }

        private void execute(Dispatcher dispatcher, ZMQ.Socket socket,
                             Message request, Message reply) throws Exception {
            var code = request.content().at("/code").asText();
            var silent = request.content().at("/silent").asBoolean();
            var store_history = request.content().at("/store_history").asBoolean();
            var user_expressions = request.content().at("/user_expressions");
            var allow_stdin = request.content().at("/allow_stdin").asBoolean();
            var stop_on_error = request.content().at("/stop_on_error").asBoolean();

            try {
                try {
                    if (! code.isEmpty()) {
                        Kernel.this.execute(code);
                    }
                    /*
                     * Tacky !!!
                     */
                    reply.content().withArray("payload");

                    var in = request.content().at("/user_expressions");
                    var iterator = in.fields();
                    var out = reply.content().with("user_expressions");

                    while (iterator.hasNext()) {
                        var entry = iterator.next();
                        var name = entry.getKey();
                        var expression = entry.getValue().asText();

                        try {
                            out.put(name, String.valueOf(Kernel.this.evaluate(expression)));
                        } catch (Throwable throwable) {
                            var error = out.with(name);

                            error.put("status", "error");
                            error.put("ename", throwable.getClass().getCanonicalName());
                            error.put("evalue", expression);

                            var array = error.putArray("traceback");
                            var buffer = new PrintStreamBuffer();

                            throwable.printStackTrace(buffer);

                            Stream.of(buffer.toString().split("\\R")).forEach(t -> array.add(t));
                        }
                    }
                } finally {
                    if (! (code.isEmpty() && silent)) {
                        if (store_history) {
                            execution_count.incrementAndGet();
                        }
                    }

                    reply.content().put("execution_count", execution_count.intValue());
                }
            } finally {
                if (! silent) {
                    var text = out.toString();

                    if (! text.isEmpty()) {
                        iopub.pub(Channel.IOPub.Stream.stdout, request, text);
                    }

                    text = err.toString();

                    if (! text.isEmpty()) {
                        iopub.pub(Channel.IOPub.Stream.stderr, request, text);
                    }
                }

                out.reset();
                err.reset();
            }
        }

        private void inspect(Dispatcher dispatcher, ZMQ.Socket socket,
                             Message request, Message reply) throws Exception {
            throw new UnsupportedOperationException();
        }

        private void complete(Dispatcher dispatcher, ZMQ.Socket socket,
                              Message request, Message reply) throws Exception {
            throw new UnsupportedOperationException();
        }

        private void history(Dispatcher dispatcher, ZMQ.Socket socket,
                             Message request, Message reply) throws Exception {
            throw new UnsupportedOperationException();
        }

        private void is_complete(Dispatcher dispatcher, ZMQ.Socket socket,
                                 Message request, Message reply) throws Exception {
            var code = request.content().at("/code");

            reply.content().put("status", "unknown");
        }

        private void connect(Dispatcher dispatcher, ZMQ.Socket socket,
                             Message request, Message reply) throws Exception {
            throw new UnsupportedOperationException();
        }

        private void comm_info(Dispatcher dispatcher, ZMQ.Socket socket,
                               Message request, Message reply) throws Exception {
            throw new UnsupportedOperationException();
        }
    }
}
