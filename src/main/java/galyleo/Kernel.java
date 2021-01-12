package galyleo;

import java.util.Objects;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.zeromq.ZMQ;

/**
 * Galyleo Jupyter {@link Kernel}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@ToString @Log4j2
public class Kernel extends ScheduledThreadPoolExecutor {
    private final Connection connection;

    /**
     * Sole constructor.
     *
     * @param   connection      The configured {@link Connection}.
     */
    public Kernel(Connection connection) {
        super(8);

        this.connection = Objects.requireNonNull(connection);
    }

    private void control(Connection connection) {
    }

    private void shell(Connection connection) {
    }

    private void stdin(Connection connection) {
    }

    private void heartbeat(Connection connection) {
        var socket = connection.getHeartbeatSocket();
        var poller = connection.getContext().poller(1);

        poller.register(socket, ZMQ.Poller.POLLIN);

        for (;;) {
            if (poller.poll() > 0) {
                var message = socket.recv();

                if (message != null) {
                    socket.send(message);
                }
            }
        }
    }
}
