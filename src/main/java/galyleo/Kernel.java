package galyleo;

import com.fasterxml.jackson.databind.ObjectMapper;
import galyleo.jupyter.Connection;
import galyleo.jupyter.Message;
import galyleo.jupyter.Service;
import galyleo.jupyter.Socket;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import javax.inject.Named;
import javax.inject.Singleton;
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
    }

    @ToString
    private class Shell extends ServiceImpl {
        public Shell() { super(SocketType.ROUTER); }

        @Override
        protected void handle(Socket socket, Message request) {
            switch (request.getHeader().getMessageType()) {
            default:
                log.warn("Unrecognized message on {}: {}",
                         socket.getAddress(), request.getHeader());
                break;
            }
        }
    }

    @ToString
    private class Control extends ServiceImpl {
        public Control() { super(SocketType.ROUTER); }

        @Override
        protected void handle(Socket socket, Message request) {
            switch (request.getHeader().getMessageType()) {
            default:
                log.warn("Unrecognized message on {}: {}",
                         socket.getAddress(), request.getHeader());
                break;
            }
        }
    }

    @ToString
    private class IOPub extends ServiceImpl {
        public IOPub() { super(SocketType.PUB); }

        @Override
        protected void handle(Socket socket, Message request) {
            switch (request.getHeader().getMessageType()) {
            default:
                log.warn("Unrecognized message on {}: {}",
                         socket.getAddress(), request.getHeader());
                break;
            }
        }
    }

    @ToString
    private class Stdin extends ServiceImpl {
        public Stdin() { super(SocketType.ROUTER); }

        @Override
        protected void handle(Socket socket, Message request) {
            switch (request.getHeader().getMessageType()) {
            default:
                log.warn("Unrecognized message on {}: {}",
                         socket.getAddress(), request.getHeader());
                break;
            }
        }
    }
}
