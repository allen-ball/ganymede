package galyleo.jupyter;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.zeromq.SocketType;
import org.zeromq.ZMQ;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Jupyter {@link ZMQ.Socket} {@link Dispatcher}.  All {@link ZMQ.Socket}
 * creation and manipulation calls happens in the {@link #run()} method.
 *
 * {@bean.info}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@Data @Log4j2
public class Dispatcher implements Runnable {
    @NonNull private final Service service;
    @NonNull private final String address;
    private final HMACDigester digester;
    private final BlockingDeque<Message> outgoing = new LinkedBlockingDeque<>();

    /**
     * Callback method to dispatch a received message.  Default
     * implementation calls
     * {@link Service#dispatch(Dispatcher,ZMQ.Socket,byte[])}.
     *
     * @param   socket          The {@link ZMQ.Socket}.
     * @param   message         The message.
     */
    protected void dispatch(ZMQ.Socket socket, byte[] message) {
        getService().dispatch(this, socket, message);
    }

    /**
     * Callback method to dispatch a {@link Message}.  Default
     * implementation calls
     * {@link Service.Jupyter#dispatch(Dispatcher,ZMQ.Socket,Message)}.
     *
     * @param   socket          The {@link ZMQ.Socket}.
     * @param   message         The message.
     */
    protected void dispatch(ZMQ.Socket socket, Message message) {
        ((Service.Jupyter) getService()).dispatch(this, socket, message);
    }

    /**
     * Method to schedule a message for publishing.
     *
     * @param   message         The message to send.
     */
    public void pub(Message message) {
        var type = getService().getSocketType();

        switch (type) {
        case PUB:
            outgoing.offer(message);
            break;

        default:
            throw new IllegalStateException("Unsupported SocketType: " + type);
        }
    }

    @Override
    public void run() {
        var server = getService().getServer();
        var context = server.getContext();
        var mapper = server.getObjectMapper();
        var digester = getDigester();
        var type = getService().getSocketType();

        while (! server.isTerminating()) {
            try (ZMQ.Socket socket = context.socket(type)) {
                if (socket.bind(getAddress())) {
                    log.info("Bound {} {}", type, address);
                } else {
                    log.warn("Could not bind to {}", address);
                }

                switch (type) {
                case REP:
                case ROUTER:
                    try (ZMQ.Poller poller = context.poller(1)) {
                        poller.register(socket, ZMQ.Poller.POLLIN);

                        while (! server.isTerminating()) {
                            int events = poller.poll(100);

                            if (events > 0 && poller.pollin(0)) {
                                var message = socket.recv();

                                if (message != null) {
                                    dispatch(socket, message);
                                }
                            }
                        }
                    }
                    break;

                case PUB:
                    while (! server.isTerminating()) {
                        Message message = outgoing.poll(100, MILLISECONDS);

                        if (message != null) {
                            dispatch(socket, message);
                        }
                    }
                    break;

                default:
                    throw new IllegalStateException("Unsupported SocketType: " + type);
                }
            } catch (Exception exception) {
                log.warn("{}", exception);
            }
        }
    }
}
