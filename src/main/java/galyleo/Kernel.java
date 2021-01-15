package galyleo;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import galyleo.jupyter.Connection;
import galyleo.jupyter.Message;
import galyleo.jupyter.Service;
import galyleo.jupyter.Socket;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;
import javax.inject.Named;
import javax.inject.Singleton;
import jdk.jshell.JShell;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
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
@ToString @Log4j2
public class Kernel extends ScheduledThreadPoolExecutor {
    private final ZMQ.Context context = ZMQ.context(1);
    private final ObjectMapper mapper = new ObjectMapper();
    private final Service.Jupyter shell = new Shell();
    private final Service.Jupyter control = new Control();
    private final Service.Jupyter iopub = new IOPub();
    private final Service.Jupyter stdin = new Stdin();
    private final Service.Heartbeat heartbeat = new Service.Heartbeat(context);
    private final PrintStreamBuffer out = new PrintStreamBuffer();
    private final PrintStreamBuffer err = new PrintStreamBuffer();
    private final JShell.Builder builder =
        JShell.builder()
        .in(InputStream.nullInputStream())
        .out(out)
        .err(err);
    private JShell java = null;

    /**
     * Sole constructor.
     */
    public Kernel() {
        super(8);

        mapper.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_VALUES);

        java = builder.build();

        submit(shell);
        submit(control);
        submit(iopub);
        submit(stdin);
        submit(heartbeat);
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
            mapper.readValue(new File(path), Connection.Properties.class);
        var connection = new Connection(properties);

        connection.connect(shell, control, iopub, stdin, heartbeat);

        log.info("Listening to {}", connection);
    }

    @ToString
    private abstract class ServiceImpl extends Service.Jupyter {
        protected ServiceImpl(SocketType type) {
            super(Kernel.this.context, type, mapper);
        }

        @Override
        protected void handle(Socket socket, Message message) {
            try {
                var action = message.getMessageTypeAction();

                dispatch(action, socket, message);
            } catch (Exception exception) {
                log.warn("Could not dispatch {}", message.getHeader(), exception);
            }
        }

        private void dispatch(String action, Socket socket, Message request) {
            try {
                getClass()
                    .getDeclaredMethod(action, Socket.class, Message.class)
                    .invoke(this, socket, request);
            } catch (IllegalAccessException | NoSuchMethodException exception) {
                log.debug("{}", exception);
            } catch (Exception exception) {
                log.warn("Exception invoking {} handler", action, exception);
            }
        }
    }

    @ToString
    private class Shell extends ServiceImpl {
        private AtomicLong execution_count = new AtomicLong(1);

        public Shell() { super(SocketType.ROUTER); }

        @Override
        protected void handle(Socket socket, Message message) {
            if (message.isRequest()) {
                super.handle(socket, message);
            } else {
                log.warn("Ignoring non-request {}", message.getMessageType());
            }
        }

        private void execute(Socket socket, Message request) {
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
                send(socket, reply);
            }

            out.reset();
            err.reset();
        }

        private void inspect(Socket socket, Message request) {
            var reply = request.reply();

            try {
                throw new UnsupportedOperationException();
            } catch (Exception exception) {
                reply.setStatus(exception);
            } finally {
                send(socket, reply);
            }
        }

        private void complete(Socket socket, Message request) {
            var reply = request.reply();

            try {
                throw new UnsupportedOperationException();
            } catch (Exception exception) {
                reply.setStatus(exception);
            } finally {
                send(socket, reply);
            }
        }

        private void history(Socket socket, Message request) {
            var reply = request.reply();

            try {
                throw new UnsupportedOperationException();
            } catch (Exception exception) {
                reply.setStatus(exception);
            } finally {
                send(socket, reply);
            }
        }

        private void is_complete(Socket socket, Message request) {
            var reply = request.reply();

            try {
                var code = request.getContent("code");

                reply.setContent("status", "unknown");
            } catch (Exception exception) {
                reply.setStatus(exception);
            } finally {
                send(socket, reply);
            }
        }

        private void connect(Socket socket, Message request) {
            var reply = request.reply();

            try {
                throw new UnsupportedOperationException();
            } catch (Exception exception) {
                reply.setStatus(exception);
            } finally {
                send(socket, reply);
            }
        }

        private void comm_info(Socket socket, Message request) {
            var reply = request.reply();

            try {
                throw new UnsupportedOperationException();
            } catch (Exception exception) {
                reply.setStatus(exception);
            } finally {
                send(socket, reply);
            }
        }

        private void kernel_info(Socket socket, Message request) {
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
                send(socket, reply);
            }
        }
    }

    @ToString
    private class Control extends ServiceImpl {
        public Control() { super(SocketType.ROUTER); }

        @Override
        protected void handle(Socket socket, Message message) {
            if (message.isRequest()) {
                super.handle(socket, message);
            } else {
                log.warn("Ignoring non-request {}", message.getMessageType());
            }
        }

        private void shutdown(Socket socket, Message request) {
            var reply = request.reply();
            var restart = (Boolean) request.getContent("restart");

            try {
                JShell old = java;

                java = null;

                submit(() -> { old.stop(); old.close(); });

                if (restart) {
                    java = builder.build();
                    out.reset();
                    err.reset();
                }
            } catch (Exception exception) {
                reply.setStatus(exception);
            } finally {
                send(socket, reply);
            }

            if (! restart) {
                queue(() -> shutdownNow());
            }
        }

        private void interrupt(Socket socket, Message request) {
            var reply = request.reply();

            try {
                java.stop();
            } catch (Exception exception) {
                reply.setStatus(exception);
            } finally {
                send(socket, reply);
            }
        }

        private void debug(Socket socket, Message request) {
            var reply = request.reply();

            try {
                throw new UnsupportedOperationException();
            } catch (Exception exception) {
                reply.setStatus(exception);
            } finally {
                send(socket, reply);
            }
        }
    }

    @ToString
    private class IOPub extends ServiceImpl {
        public IOPub() { super(SocketType.PUB); }

        @Override
        protected void handle(Socket socket) {
            socket.recv();

            while (socket.hasReceiveMore()) {
                socket.recv();
            }
        }
    }

    @ToString
    private class Stdin extends ServiceImpl {
        public Stdin() { super(SocketType.ROUTER); }

        @Override
        protected void handle(Socket socket, Message message) {
            if (message.isReply()) {
                super.handle(socket, message);
            } else {
                log.warn("Ignoring non-reply {}", message.getMessageType());
            }
        }

        private void input(Socket socket, Message reply) {
            log.warn("Ignoring {}", reply.getMessageType());
        }
    }
}
