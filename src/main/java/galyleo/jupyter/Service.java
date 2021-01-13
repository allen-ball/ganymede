package galyleo.jupyter;

import lombok.ToString;
import org.zeromq.SocketType;
import org.zeromq.ZMQ;

/**
 * Jupyter kernel {@link Service}.
 *
 * {@bean.info}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
public abstract class Service extends Channel implements Runnable {

    /**
     * Sole constructor.
     *
     * @param   context         The {@link ZMQ.Context}.
     * @param   type            The {@link SocketType} for created
     *                          {@link ZMQ.Socket}s.
     */
    protected Service(ZMQ.Context context, SocketType type) {
        super(context, type);
    }

    @Override
    public void run() {
        for (;;) {
            dispatch();
        }
    }

    /**
     * Standard
     * {@link.uri https://jupyter-client.readthedocs.io/en/latest/messaging.html#heartbeat-for-kernels Heartbeat}
     * {@link Service}.
     */
    @ToString
    public static class Heartbeat extends Service {
        public Heartbeat(ZMQ.Context context) {
            super(context, SocketType.REP);
        }

        @Override
        protected void handle(Socket socket) {
            var message = socket.recv();

            if (message != null) {
                socket.send(message);
            }
        }
    }
}
