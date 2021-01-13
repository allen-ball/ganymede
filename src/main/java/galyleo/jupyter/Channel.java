package galyleo.jupyter;

import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import org.zeromq.SocketType;
import org.zeromq.ZMQ;

/**
 * Jupyter {@link Channel}.
 *
 * {@bean.info}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
public abstract class Channel extends ZMQ.Poller {
    private final ZMQ.Context context;
    private final SocketType type;
    private final LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();
    private final long timeout = 1000;

    /**
     * Sole constructor.
     *
     * @param   context         The {@link ZMQ.Context}.
     * @param   type            The {@link SocketType} for created
     *                          {@link ZMQ.Socket}s.
     */
    protected Channel(ZMQ.Context context, SocketType type) {
        super(context);

        this.context = Objects.requireNonNull(context);
        this.type = Objects.requireNonNull(type);
    }

    /**
     * Method to schedule creation of and connection to a {@link Socket} for
     * this {@link Channel}.
     *
     * @param   address         The address of the {@link Socket} to be
     *                          created.
     */
    public void connect(String address) { queue.offer(address); }

    private void connect() {
        var list = new LinkedList<String>();

        queue.drainTo(list);

        while (! list.isEmpty()) {
            var socket = new Socket(context, type, list.remove());

            socket.connect();

            register(socket, ZMQ.Poller.POLLIN);
        }
    }

    /**
     * Callback method to create and connect any outstanding
     * {@link Socket}s, poll for input, and call {@link #handle(Socket)}
     * where input is available.
     */
    protected void dispatch() {
        connect();

        if (getSize() > 0) {
            int count = poll(timeout);

            if (count > 0) {
                for (int i = 0, n = getSize(); i < n; i += 1) {
                    if (pollin(i)) {
                        var socket = getSocket(i);

                        if (socket != null) {
                            handle((Socket) socket);
                        }
                    }
                }
            }
        } else {
            try {
                Thread.sleep(timeout);
            } catch(Exception exception) {
            }
        }
    }

    /**
     * Callback method to handle a {@link Socket} that has been signalled
     * for input.
     *
     * @param   socket          The {@link Socket}.
     */
    protected abstract void handle(Socket socket);
}
