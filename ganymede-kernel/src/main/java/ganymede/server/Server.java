package ganymede.server;
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
import com.fasterxml.jackson.databind.node.ObjectNode;
import ganymede.io.PrintStreamBuffer;
import ganymede.util.ObjectMappers;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.zeromq.ZMQ;

import static lombok.AccessLevel.PROTECTED;

/**
 * Jupyter Notebook {@link Server} (base class for kernel implementations).
 *
 * {@bean.info}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@Getter @Setter(PROTECTED) @Log4j2
public abstract class Server extends ScheduledThreadPoolExecutor {

    /**
     * The
     * {@link.uri https://jupyter-client.readthedocs.io/en/latest/messaging.html#versioning target=newtab Jupyter message specification version}.
     */
    protected static final ComparableVersion PROTOCOL_VERSION = new ComparableVersion("5.3");

    private final ZMQ.Context context = ZMQ.context(1);
    private final Channel.Heartbeat heartbeat = new Channel.Heartbeat(this);
    private final Channel.Control control = new Control();
    private final Channel.IOPub iopub = new Channel.IOPub(this);
    private final Channel.Stdin stdin = new Stdin();
    private final Channel.Shell shell = new Shell();
    private InputStream in = null;
    private PrintStreamBuffer out = null;
    private PrintStreamBuffer err = null;
    private UUID kernelId = null;
    private UUID kernelSessionId = null;
    protected final AtomicInteger execution_count = new AtomicInteger(0);
    private transient Message request = null;

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
        bind(new File(path));
    }

    /**
     * Add a connection specified by a {@link Connection} {@link File}.
     *
     * @param   file            The {@link Connection} {@link File}.
     *
     * @throws  IOException     If the {@link File} cannot be opened or
     *                          parsed.
     */
    protected void bind(File file) throws IOException {
        var connection = Connection.parse(file);

        setKernelId(connection.getKernelId());

        log.info("Kernel {}", getKernelId());

        connection.connect(shell, control, iopub, stdin, heartbeat);

        log.info("Connected to {}", connection.getNode().toPrettyString());
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
     * Method to get {@link.this} {@link Server}'s {@code kernel_info_reply}
     * content.
     *
     * @return  The {@link ObjectNode} containing the {@link Message}
     *          content.
     */
    protected abstract ObjectNode getKernelInfo();

    /**
     * Method to execute code (cell's contents).
     *
     * @param   code            The cell code to execute.
     */
    protected abstract void execute(String code) throws Exception;

    /**
     * Method to evaluate an expression.
     *
     * @param   expression      The expression.
     *
     * @return  The result of the evaluation.
     */
    protected abstract String evaluate(String expression) throws Exception;

    /**
     * Method to determine code's
     * {@link Message.completeness completeness}.
     *
     * @param   code            The cell code to execute.
     *
     * @return  The {@link Message.completeness completeness}.
     */
    protected abstract Message.completeness isComplete(String code) throws Exception;

    /**
     * Method to interrupt a kernel.
     */
    protected abstract void interrupt();

    /**
     * Method to schedule a {@link Message} for publishing.  See
     * {@link Channel.IOPub#pub(Message)}.
     *
     * @param   message     The {@link Message} to send.
     */
    protected void pub(Message message) { iopub.pub(message); }

    /**
     * Method to stamp an outgoing {@link Message}.  Adds
     * {@link #PROTOCOL_VERSION}, session, and
     * {@link Message#timestamp() timestamp} if not already specified.
     *
     * @param   message         The {@link Message} to stamp.
     *
     * @return  The {@link Message}.
     */
    protected Message stamp(Message message) {
        if (message.version() == null) {
            message.version(PROTOCOL_VERSION.toString());
        }

        if (message.session() == null) {
            message.session(getKernelSessionId().toString());
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
                Server.this.shutdown();
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
                try {
                    Server.this.request = message;

                    super.dispatch(dispatcher, socket, Server.this.request);
                } finally {
                    Server.this.request = null;
                }
            } else {
                log.warn("Ignoring non-request {}", message.msg_type());
            }
        }

        private void kernel_info(Dispatcher dispatcher, Message request, Message reply) throws Exception {
            reply.content().setAll(getKernelInfo());
        }

        private void execute(Dispatcher dispatcher, Message request, Message reply) throws Exception {
            /*
             * jupyter lab populates execute_request metadata.  E.g.,
             *
             * metadata: {
             *   "deletedCells" : [ ],
             *   "recordTiming" : false,
             *   "cellId" : "4cf407d2"
             * }
             */
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

                var stdout = out.toString();
                var stderr = err.toString();

                out.reset();
                err.reset();

                if (! silent) {
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
            var code = request.content().at("/code").asText();

            reply.status(isComplete(code));
        }

        @Deprecated(since = "5.1")
        private void connect(Dispatcher dispatcher, Message request, Message reply) throws Exception {
            reply.content().setAll(dispatcher.getConnection().getNode());
        }

        private void comm_info(Dispatcher dispatcher, Message request, Message reply) throws Exception {
            var comms = reply.content().with("comms");
            /*
             * Currently unsupported so empty reply.  But remember to *copy*
             * internal state since output may be culled after satisfying
             * query.
             */
            if (request.content().hasNonNull("target_name")) {
                var target_name =
                    request.content().at("/target_name").asText();
                var iterator = comms.fields();

                while (iterator.hasNext()) {
                    var entry = iterator.next();
                    var dict = entry.getValue();

                    if (dict.isObject()) {
                        ((ObjectNode) dict).retain(target_name);

                        if (dict.isEmpty()) {
                            iterator.remove();
                        }
                    }
                }
            }
        }
    }
}
