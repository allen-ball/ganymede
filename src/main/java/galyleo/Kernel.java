package galyleo;

import galyleo.jupyter.Connection;
import galyleo.jupyter.Dispatcher;
import galyleo.jupyter.Message;
import galyleo.jupyter.Server;
import galyleo.jupyter.Service;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
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

    private final Service.Jupyter shell = new Shell();
    private final Service.Jupyter control = new Control();
    private final Service.Jupyter iopub = new IOPub();
    private final Service.Jupyter stdin = new Stdin();
    private final Service.Heartbeat heartbeat = new Service.Heartbeat(this);
    private final PrintStreamBuffer out = new PrintStreamBuffer();
    private final PrintStreamBuffer err = new PrintStreamBuffer();
    private final JShell.Builder builder =
        JShell.builder()
        .in(InputStream.nullInputStream())
        .out(out)
        .err(err);
    private JShell java = null;

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
        var properties =
            getObjectMapper()
            .readValue(new File(path), Connection.Properties.class);
        var connection = new Connection(properties);

        connection.connect(shell, control, iopub, stdin, heartbeat);

        log.info("Listening to {}", connection);
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
                log.warn("Ignoring non-request {}", message.getMessageType());
            }
        }

        private void execute(Dispatcher dispatcher, ZMQ.Socket socket, Message request) {
            var reply = request.reply();

            try {
                throw new UnsupportedOperationException();
/*
                var code = (String) request.getContent("code");
                var silent = (Boolean) request.getContent("silent");
                var store_history = (Boolean) request.getContent("store_history");
                var user_expressions = (Map<?,?>) request.getContent("user_expressions");
                var allow_stdin = (Boolean) request.getContent("allow_stdin");
                var stop_on_error = (Boolean) request.getContent("stop_on_error");

                reply.setContent("execution_count", execution_count);
*/
            } catch (Exception exception) {
                reply.setStatus(exception);
            } finally {
                reply.send(socket, getObjectMapper(), dispatcher.getDigester());
            }

            out.reset();
            err.reset();
        }

        private void inspect(Dispatcher dispatcher, ZMQ.Socket socket, Message request) {
            var reply = request.reply();

            try {
                throw new UnsupportedOperationException();
            } catch (Exception exception) {
                reply.setStatus(exception);
            } finally {
                reply.send(socket, getObjectMapper(), dispatcher.getDigester());
            }
        }

        private void complete(Dispatcher dispatcher, ZMQ.Socket socket, Message request) {
            var reply = request.reply();

            try {
                throw new UnsupportedOperationException();
            } catch (Exception exception) {
                reply.setStatus(exception);
            } finally {
                reply.send(socket, getObjectMapper(), dispatcher.getDigester());
            }
        }

        private void history(Dispatcher dispatcher, ZMQ.Socket socket, Message request) {
            var reply = request.reply();

            try {
                throw new UnsupportedOperationException();
            } catch (Exception exception) {
                reply.setStatus(exception);
            } finally {
                reply.send(socket, getObjectMapper(), dispatcher.getDigester());
            }
        }

        private void is_complete(Dispatcher dispatcher, ZMQ.Socket socket, Message request) {
            var reply = request.reply();

            try {
                var code = request.getContent("code");

                reply.setContent("status", "unknown");
            } catch (Exception exception) {
                reply.setStatus(exception);
            } finally {
                reply.send(socket, getObjectMapper(), dispatcher.getDigester());
            }
        }

        private void connect(Dispatcher dispatcher, ZMQ.Socket socket, Message request) {
            var reply = request.reply();

            try {
                throw new UnsupportedOperationException();
            } catch (Exception exception) {
                reply.setStatus(exception);
            } finally {
                reply.send(socket, getObjectMapper(), dispatcher.getDigester());
            }
        }

        private void comm_info(Dispatcher dispatcher, ZMQ.Socket socket, Message request) {
            var reply = request.reply();

            try {
                throw new UnsupportedOperationException();
            } catch (Exception exception) {
                reply.setStatus(exception);
            } finally {
                reply.send(socket, getObjectMapper(), dispatcher.getDigester());
            }
        }

        private void kernel_info(Dispatcher dispatcher, ZMQ.Socket socket, Message request) {
            var reply = request.reply();

            try {
                reply.setContent("protocol_version", PROTOCOL_VERSION);
                reply.setContent("implementation", "galyle");
                reply.setContent("implementation_version", "1.0.0");

                var language_info = new LinkedHashMap<String,Object>();

                language_info.put("name", "java");
                language_info.put("version", System.getProperty("java.vm.specification.version"));
                language_info.put("mimetype", "text/plain");

                reply.setContent("language_info", language_info);
            } catch (Exception exception) {
                reply.setStatus(exception);
            } finally {
                reply.send(socket, getObjectMapper(), dispatcher.getDigester());
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
                log.warn("Ignoring non-request {}", message.getMessageType());
            }
        }

        private void shutdown(Dispatcher dispatcher, ZMQ.Socket socket, Message request) {
            var reply = request.reply();
            var restart = (Boolean) request.getContent("restart");

            try {
                JShell old = java;

                java = null;

                if (old != null) {
                    submit(() -> { old.stop(); old.close(); });
                }

                if (restart) {
                    java = builder.build();
                    out.reset();
                    err.reset();
                }
            } catch (Exception exception) {
                reply.setStatus(exception);
            } finally {
                reply.send(socket, getObjectMapper(), dispatcher.getDigester());
            }

            if (! restart) {
                getServer().shutdown();
            }
        }

        private void interrupt(Dispatcher dispatcher, ZMQ.Socket socket, Message request) {
            var reply = request.reply();

            try {
                java.stop();
            } catch (Exception exception) {
                reply.setStatus(exception);
            } finally {
                reply.send(socket, getObjectMapper(), dispatcher.getDigester());
            }
        }

        private void debug(Dispatcher dispatcher, ZMQ.Socket socket, Message request) {
            var reply = request.reply();

            try {
                throw new UnsupportedOperationException();
            } catch (Exception exception) {
                reply.setStatus(exception);
            } finally {
                reply.send(socket, getObjectMapper(), dispatcher.getDigester());
            }
        }
    }

    @ToString
    private class IOPub extends Service.Jupyter {
        public IOPub() { super(Kernel.this, SocketType.PUB); }

        @Override
        protected void dispatch(Dispatcher dispatcher, ZMQ.Socket socket, byte[] message) {
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
                log.warn("Ignoring non-reply {}", message.getMessageType());
            }
        }

        private void input(Dispatcher dispatcher, ZMQ.Socket socket, Message reply) {
            log.warn("Ignoring {}", reply.getMessageType());
        }
    }
}
