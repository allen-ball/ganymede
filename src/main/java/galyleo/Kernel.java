package galyleo;

import com.fasterxml.jackson.databind.ObjectMapper;
import galyleo.jupyter.Channel;
import galyleo.jupyter.Connection;
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
    private final Service shell;
    private final Service control;
    private final Channel iopub;
    private final Service heartbeat;

    /**
     * Sole constructor.
     */
    public Kernel() {
        super(8);

        shell = new Shell();
        control = new Control();
        iopub = new IOPub();
        heartbeat = new Service.Heartbeat(context);

        submit(shell);
        submit(control);
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
        Connection.Properties properties =
            mapper.readValue(new File(path), Connection.Properties.class);
        var connection = new Connection(properties);

        log.info("Listening to {}", connection);
    }

    @ToString
    private abstract class ServiceImpl extends Service {
        protected ServiceImpl(SocketType type) {
            super(Kernel.this.context, type);
        }
    }

    @ToString
    private class Shell extends ServiceImpl {
        public Shell() { super(SocketType.ROUTER); }

        @Override
        protected void handle(Socket socket) {
            var message = socket.recv();

            if (message != null) {
                while (socket.hasReceiveMore()) {
                    socket.recv();
                }
            }
        }
    }

    @ToString
    private class Control extends ServiceImpl {
        public Control() { super(SocketType.ROUTER); }

        @Override
        protected void handle(Socket socket) {
            var message = socket.recv();

            if (message != null) {
                while (socket.hasReceiveMore()) {
                    socket.recv();
                }
            }
        }
    }

    @ToString
    private class IOPub extends ServiceImpl {
        public IOPub() { super(SocketType.PUB); }

        @Override
        protected void handle(Socket socket) {
            var message = socket.recv();

            if (message != null) {
                while (socket.hasReceiveMore()) {
                    socket.recv();
                }
            }
        }
    }
}
