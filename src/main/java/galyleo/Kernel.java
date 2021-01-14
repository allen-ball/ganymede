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
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@Named @Singleton
@ToString @Log4j2
public class Kernel extends ScheduledThreadPoolExecutor {
    private final PrintStreamBuffer out = new PrintStreamBuffer();
    private final PrintStreamBuffer err = new PrintStreamBuffer();
    private final JShell java =
        JShell.builder()
        .in(InputStream.nullInputStream())
        .out(out)
        .err(err)
        .build();
    private final ZMQ.Context context = ZMQ.context(1);
    private final ObjectMapper mapper = new ObjectMapper();
    private final Service.Jupyter shell = new Shell();
    private final Service.Jupyter control = new Control();
    private final Service.Jupyter iopub = new IOPub();
    private final Service.Jupyter stdin = new Stdin();
    private final Service.Heartbeat heartbeat = new Service.Heartbeat(context);

    /**
     * Sole constructor.
     */
    public Kernel() {
        super(8);

        mapper.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_VALUES);

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
        protected void handle(Socket socket, Message request) {
            try {
                var type = request.getHeader().getMessageType();
                var substrings = type.split("_", 2);

                dispatch(substrings[0], socket, request);
            } catch (Exception exception) {
                log.warn("Could not dispatch {}", request.getHeader(), exception);
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
        private AtomicLong execution_count = new AtomicLong(0);

        public Shell() { super(SocketType.ROUTER); }
    }

    @ToString
    private class Control extends ServiceImpl {
        public Control() { super(SocketType.ROUTER); }
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
    }
}
