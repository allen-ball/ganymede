package galyleo.server;

import ball.annotation.CompileTimeCheck;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import galyleo.io.PrintStreamBuffer;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.zeromq.ZMQ;

import static lombok.AccessLevel.PROTECTED;

/**
 * Jupyter Notebook {@link Server} (base class for kernel implementations).
 *
 * {@bean.info}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@Getter @Setter(PROTECTED) @Log4j2
public abstract class Server extends ScheduledThreadPoolExecutor {

    /**
     * The
     * {@link.uri https://jupyter-client.readthedocs.io/en/latest/messaging.html#versioning target=newtab Jupyter message specification version}.
     */
    protected static final String PROTOCOL_VERSION = "5.3";

    /**
     * Common {@link Server} static {@link ObjectMapper} instance.
     */
    public static final ObjectMapper OBJECT_MAPPER =
        new ObjectMapper()
        .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_VALUES)
        .enable(SerializationFeature.INDENT_OUTPUT);

    @CompileTimeCheck
    private static final Pattern CONNECTION_FILE_NAME_PATTERN =
        Pattern.compile("(?i)^(kernel-|)(?<id>[^.]+)[.]json$");

    private final ZMQ.Context context = ZMQ.context(8);
    private final ConcurrentSkipListMap<String,Connection> connections =
        new ConcurrentSkipListMap<>();
    private final Channel.Heartbeat heartbeat = new Channel.Heartbeat(this);
    private final Channel.Control control = new Control();
    private final Channel.IOPub iopub = new Channel.IOPub(this);
    private final Channel.Stdin stdin = new Stdin();
    private final Channel.Shell shell = new Shell();
    private final AtomicInteger execution_count = new AtomicInteger(0);
    private InputStream in = null;
    private PrintStreamBuffer out = null;
    private PrintStreamBuffer err = null;
    private String session = null;

    /**
     * Sole constructor.
     */
    protected Server() { super(16); }

    /**
     * Add a connection specified by a {@link Connection} {@link File}.
     *
     * @param   path            The path to the {@link Connection}
     *                          {@link File}.
     *
     * @throws  IOException     If the {@link File} cannot be opened or
     *                          parsed.
     */
    public void bind(String path) throws IOException {
        var file = new File(path);
        var id =
            Optional.of(file.getName())
            .map(CONNECTION_FILE_NAME_PATTERN::matcher)
            .filter(t -> t.matches())
            .map(t -> t.group("id"))
            .orElse(null);
        var node = OBJECT_MAPPER.readTree(file);
        var connection = new Connection(node);

        connections.put(id, connection);

        connection.connect(shell, control, iopub, stdin, heartbeat);

        log.info("Connected to {} {}",
                 id, connection.getNode().toPrettyString());
    }

    /**
     * Method to restart the {@link Server}.  Subclass implementations
     * should chain to the {@code super}-method.
     */
    protected void restart() throws Exception {
        in = new ByteArrayInputStream(new byte[] { });
        out = new PrintStreamBuffer();
        err = new PrintStreamBuffer();
    }

    /**
     * Method to execute code (cell's contents).
     *
     * @param   code            The cell code to execute.
     */
    protected abstract void execute(String code) throws Exception;

    /**
     * Method to get an {@link ArrayNode} of accumulated events from the
     * {@link Shell} (and {@link jdk.jshell.JShell}) from last
     * {@link #execute(String)} call.
     *
     * @return  The {@link ArrayNode}.
     */
    protected abstract ArrayNode getExecutionEvents();

    /**
     * Method to evaluate an expression.
     *
     * @param   expression      The expression.
     *
     * @return  The result of the evaluation.
     */
    protected abstract String evaluate(String expression) throws Exception;

    /**
     * Method to interrupt a kernel.
     */
    protected abstract void interrupt();

    /**
     * Method to stamp and outgoing {@link Message}.  Adds
     * {@link #PROTOCOL_VERSION}, session, and
     * {@link Message#timestamp() timestamp} if not already specified.
     *
     * @param   message         The {@link Message} to stamp.
     *
     * @return  The {@link Message}.
     */
    protected Message stamp(Message message) {
        if (message.version() == null) {
            message.version(PROTOCOL_VERSION);
        }

        if (message.session() == null) {
            message.session(getSession());
        }

        return message.timestamp();
    }

    @ToString
    private class Control extends Channel.Control {
        public Control() { super(Server.this); }

        @Override
        protected void dispatch(Dispatcher dispatcher, ZMQ.Socket socket, Message message) {
            if (message.isRequest()) {
                super.dispatch(dispatcher, socket, message);
            } else {
                log.warn("Ignoring non-request {}", message.msg_type());
            }
        }

        private void shutdown(Dispatcher dispatcher, Message request, Message reply) throws Exception {
            var restart = request.content().at("/restart").asBoolean();

            reply.content().put("restart", restart);

            if (restart) {
                Server.this.restart();
            } else {
                submit(() -> getServer().shutdown());
            }
        }

        private void interrupt(Dispatcher dispatcher, Message request, Message reply) throws Exception {
            Server.this.interrupt();
        }

        private void debug(Dispatcher dispatcher, Message request, Message reply) throws Exception {
            throw new UnsupportedOperationException();
        }
    }

    @ToString
    private class Stdin extends Channel.Stdin {
        public Stdin() { super(Server.this); }

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
            super(Server.this, Server.this.iopub, Server.this.stdin);
        }

        @Override
        protected void dispatch(Dispatcher dispatcher, ZMQ.Socket socket, Message message) {
            if (message.isRequest()) {
                super.dispatch(dispatcher, socket, message);
            } else {
                log.warn("Ignoring non-request {}", message.msg_type());
            }
        }

        private void kernel_info(Dispatcher dispatcher, Message request, Message reply) throws Exception {
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

        private void execute(Dispatcher dispatcher, Message request, Message reply) throws Exception {
            var code = request.content().at("/code").asText();
            var silent = request.content().at("/silent").asBoolean();
            var store_history = request.content().at("/store_history").asBoolean();
            var user_expressions = request.content().at("/user_expressions");
            var allow_stdin = request.content().at("/allow_stdin").asBoolean();
            var stop_on_error = request.content().at("/stop_on_error").asBoolean();

            try {
                if (! code.isEmpty()) {
                    if (! silent) {
                        if (store_history) {
                            execution_count.incrementAndGet();

                            iopub.pub(request.execute_input(code, execution_count.intValue()));
                        }
                    }

                    Server.this.execute(code);
                }
            } catch (Throwable throwable) {
                reply.status(throwable, code);
            } finally {
                reply.content().put("execution_count", execution_count.intValue());

                if (reply.content().get("status").asText().equals("ok")) {
                    reply.content().withArray("payload");

                    var in = request.content().at("/user_expressions");
                    var iterator = in.fields();
                    var out = reply.content().with("user_expressions");

                    while (iterator.hasNext()) {
                        var entry = iterator.next();
                        var name = entry.getKey();
                        var expression = entry.getValue().asText();

                        try {
                            out.put(name, String.valueOf(Server.this.evaluate(expression)));
                        } catch (Throwable throwable) {
                            out.set(name, Message.content(throwable, expression));
                        }
                    }
                }

                var events = getExecutionEvents();
                var stdout = out.toString();
                var stderr = err.toString();

                out.reset();
                err.reset();

                if (! silent) {
                    var iterator = events.iterator();

                    while (iterator.hasNext()) {
                        try {
                            var execute_result =
                                request.execute_result(execution_count.intValue(),
                                                       (ObjectNode) iterator.next());

                            iopub.pub(execute_result);
                        } catch (Exception exception) {
                            log.warn("{}", exception);
                        }
                    }

                    if (! stdout.isEmpty()) {
                        iopub.pub(request.stream(Message.stream.stdout, stdout));
                    }

                    if (! stderr.isEmpty()) {
                        iopub.pub(request.stream(Message.stream.stderr, stderr));
                    }
                }
            }
        }

        private void inspect(Dispatcher dispatcher, Message request, Message reply) throws Exception {
            var code = request.content().at("/code").asText();
            var cursor_pos = request.content().at("/cursor_pos").asInt();
            var detail_level = request.content().at("/detail_level").asInt();

            throw new UnsupportedOperationException();
        }

        private void complete(Dispatcher dispatcher, Message request, Message reply) throws Exception {
            var code = request.content().at("/code").asText();
            var cursor_pos = request.content().at("/cursor_pos").asInt();

            throw new UnsupportedOperationException();
        }

        private void history(Dispatcher dispatcher, Message request, Message reply) throws Exception {
            var output = request.content().at("/output").asBoolean();
            var raw = request.content().at("/raw").asBoolean();
            var hist_access_type = request.content().at("/hist_access_type").asText();
            var session = request.content().at("/session").asInt();
            var start = request.content().at("/start").asInt();
            var stop = request.content().at("/stop").asInt();
            var n = request.content().at("/n").asInt();
            var pattern = request.content().at("/pattern").asText();
            var unique = request.content().at("/unique").asBoolean();

            throw new UnsupportedOperationException();
        }

        private void is_complete(Dispatcher dispatcher, Message request, Message reply) throws Exception {
            var code = request.content().at("/code");

            reply.content().put("status", "unknown");
        }

        private void connect(Dispatcher dispatcher, Message request, Message reply) throws Exception {
            throw new UnsupportedOperationException();
        }

        private void comm_info(Dispatcher dispatcher, Message request, Message reply) throws Exception {
            /*
             * Empty reply
             */
            reply.content().with("comms");
            /*
             * Returned dictionary can be narrowed to target_name.
             */
            if (request.content().hasNonNull("target_name")) {
                var target_name =
                    request.content().at("/target_name").asText();
            }
        }
    }
}
