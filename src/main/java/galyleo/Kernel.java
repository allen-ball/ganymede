package galyleo;

import galyleo.jupyter.Connection;
import galyleo.jupyter.Dispatcher;
import galyleo.jupyter.Message;
import galyleo.jupyter.Server;
import galyleo.jupyter.Service;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Named;
import javax.inject.Singleton;
import jdk.jshell.JShell;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
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

    /**
     * Standard {@link SpringApplication} {@code main(String[])}
     * entry point.
     *
     * @param   argv            The command line argument vector.
     *
     * @throws  Exception       If the function does not catch
     *                          {@link Exception}.
     */
    public static void main(String[] argv) throws Exception {
        var application = new SpringApplication(Kernel.class);

        application.run(argv);
    }

    private final Shell shell = new Shell();
    private final Control control = new Control();
    private final Service.IOPub iopub = new Service.IOPub(this);
    private final Stdin stdin = new Stdin();
    private final Service.Heartbeat heartbeat = new Service.Heartbeat(this);
    private final PrintStreamBuffer out = new PrintStreamBuffer();
    private final PrintStreamBuffer err = new PrintStreamBuffer();
    private final JShell.Builder builder =
        JShell.builder()
        .in(InputStream.nullInputStream())
        .out(out)
        .err(err);
    private JShell java = null;
    private int restarts = 0;

    @PostConstruct
    public void init() {
        out.reset();
        err.reset();
        java = builder.build();
    }

    @PreDestroy
    public void destroy() { shutdown(); }

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

            iopub.pub(Service.IOPub.Status.starting);
            iopub.pub(Service.IOPub.Status.idle);
        } else {
            throw new IllegalArgumentException("No connection file specified");
        }
    }

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
    protected String getSession() {
        return String.join("-",
                           Kernel.class.getCanonicalName(),
                           String.valueOf(ProcessHandle.current().pid()),
                           String.valueOf(restarts));
    }

    @ToString
    private class Shell extends Service.Jupyter {
        private AtomicLong execution_count = new AtomicLong(1);

        public Shell() { super(Kernel.this, SocketType.ROUTER); }

        @Override
        protected void dispatch(Dispatcher dispatcher, ZMQ.Socket socket, Message message) {
            if (message.isRequest()) {
                super.dispatch(dispatcher, socket, message);
            } else {
                log.warn("Ignoring non-request {}", message.msg_type());
            }
        }

        private void kernel_info(Dispatcher dispatcher, ZMQ.Socket socket, Message request) {
            var reply = request.reply(getSession());

            try {
                iopub.pub(Service.IOPub.Status.busy);

                reply.content().put("protocol_version", PROTOCOL_VERSION);
                reply.content().put("implementation", "galyleo");
                reply.content().put("implementation_version", "1.0.0");

                var language_info = reply.content().with("language_info");

                language_info.put("name", "java");
                language_info.put("version", System.getProperty("java.version"));
                language_info.put("mimetype", "text/plain");
                language_info.put("file_extension", ".java");

                reply.content().set("language_info", language_info);

                var help_links = reply.content().with("help_links");
            } catch (Exception exception) {
                reply.setStatus(exception);
            } finally {
                iopub.pub(Service.IOPub.Status.idle);

                send(dispatcher, socket, reply);
            }
        }

        private void execute(Dispatcher dispatcher, ZMQ.Socket socket, Message request) {
            var reply = request.reply(getSession());

            try {
                var code = request.content().get("code");
                var silent = request.content().get("silent");
                var store_history = request.content().get("store_history");
                var user_expressions = request.content().get("user_expressions");
                var allow_stdin = request.content().get("allow_stdin");
                var stop_on_error = request.content().get("stop_on_error");

                reply.content().put("execution_count", String.valueOf(execution_count));
            } catch (Exception exception) {
                reply.setStatus(exception);
            } finally {
                send(dispatcher, socket, reply);
            }

            out.reset();
            err.reset();
        }

        private void inspect(Dispatcher dispatcher, ZMQ.Socket socket, Message request) {
            var reply = request.reply(getSession());

            try {
                throw new UnsupportedOperationException();
            } catch (Exception exception) {
                reply.setStatus(exception);
            } finally {
                send(dispatcher, socket, reply);
            }
        }

        private void complete(Dispatcher dispatcher, ZMQ.Socket socket, Message request) {
            var reply = request.reply(getSession());

            try {
                throw new UnsupportedOperationException();
            } catch (Exception exception) {
                reply.setStatus(exception);
            } finally {
                send(dispatcher, socket, reply);
            }
        }

        private void history(Dispatcher dispatcher, ZMQ.Socket socket, Message request) {
            var reply = request.reply(getSession());

            try {
                throw new UnsupportedOperationException();
            } catch (Exception exception) {
                reply.setStatus(exception);
            } finally {
                send(dispatcher, socket, reply);
            }
        }

        private void is_complete(Dispatcher dispatcher, ZMQ.Socket socket, Message request) {
            var reply = request.reply(getSession());

            try {
                var code = request.content().get("code");

                reply.content().put("status", "unknown");
            } catch (Exception exception) {
                reply.setStatus(exception);
            } finally {
                send(dispatcher, socket, reply);
            }
        }

        private void connect(Dispatcher dispatcher, ZMQ.Socket socket, Message request) {
            var reply = request.reply(getSession());

            try {
                throw new UnsupportedOperationException();
            } catch (Exception exception) {
                reply.setStatus(exception);
            } finally {
                send(dispatcher, socket, reply);
            }
        }

        private void comm_info(Dispatcher dispatcher, ZMQ.Socket socket, Message request) {
            var reply = request.reply(getSession());

            try {
                throw new UnsupportedOperationException();
            } catch (Exception exception) {
                reply.setStatus(exception);
            } finally {
                send(dispatcher, socket, reply);
            }
        }
    }

    @ToString
    private class Control extends Service.Jupyter {
        public Control() { super(Kernel.this, SocketType.ROUTER); }

        @Override
        protected void dispatch(Dispatcher dispatcher, ZMQ.Socket socket, Message message) {
            if (message.isRequest()) {
                super.dispatch(dispatcher, socket, message);
            } else {
                log.warn("Ignoring non-request {}", message.msg_type());
            }
        }

        private void shutdown(Dispatcher dispatcher, ZMQ.Socket socket, Message request) {
            var reply = request.reply(getSession());
            var restart = false /* request.content().get("restart") */;

            try {
                JShell old = java;

                java = null;

                if (old != null) {
                    submit(() -> { old.stop(); old.close(); });
                }

                if (restart) {
                    java = builder.build();
                    restarts += 1;
                    out.reset();
                    err.reset();
                }
            } catch (Exception exception) {
                reply.setStatus(exception);
            } finally {
                send(dispatcher, socket, reply);
            }

            if (! restart) {
                getServer().shutdown();
            }
        }

        private void interrupt(Dispatcher dispatcher, ZMQ.Socket socket, Message request) {
            var reply = request.reply(getSession());

            try {
                java.stop();
            } catch (Exception exception) {
                reply.setStatus(exception);
            } finally {
                send(dispatcher, socket, reply);
            }
        }

        private void debug(Dispatcher dispatcher, ZMQ.Socket socket, Message request) {
            var reply = request.reply(getSession());

            try {
                throw new UnsupportedOperationException();
            } catch (Exception exception) {
                reply.setStatus(exception);
            } finally {
                send(dispatcher, socket, reply);
            }
        }
    }

    @ToString
    private class Stdin extends Service.Jupyter {
        public Stdin() { super(Kernel.this, SocketType.ROUTER); }

        @Override
        protected void dispatch(Dispatcher dispatcher, ZMQ.Socket socket, Message message) {
            if (message.isReply()) {
                super.dispatch(dispatcher, socket, message);
            } else {
                log.warn("Ignoring non-reply {}", message.msg_type());
            }
        }

        private void input(Dispatcher dispatcher, ZMQ.Socket socket, Message reply) {
            log.warn("Ignoring {}", reply.msg_type());
        }
    }
}
