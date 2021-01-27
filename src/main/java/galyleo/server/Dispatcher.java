package galyleo.server;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.zeromq.ZMQ;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Jupyter {@link ZMQ.Socket} {@link Dispatcher}.  All {@link ZMQ.Socket}
 * creation and manipulation calls happens in the {@link #run()} method.
 * See {@link.uri https://zguide.zeromq.org/ target=newtab ØMQ - The Guide},
 * {@link.uri https://zguide.zeromq.org/docs/chapter3/ target=newtab Chapter 3}.
 *
 * {@bean.info}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@Data @Log4j2
public class Dispatcher implements Runnable {
    @NonNull private final Channel channel;
    @NonNull private final String address;
    private final HMACDigester digester;
    private final BlockingDeque<Message> outgoing = new LinkedBlockingDeque<>();

    /**
     * Callback method to dispatch a received message.  Default
     * implementation calls
     * {@link Channel#dispatch(Dispatcher,ZMQ.Socket,byte[])}.
     *
     * @param   socket          The {@link ZMQ.Socket}.
     * @param   frame           The first message frame.
     */
    protected void dispatch(ZMQ.Socket socket, byte[] frame) {
        getChannel().dispatch(this, socket, frame);
    }

    /**
     * Callback method to dispatch a {@link Message}.  Default
     * implementation calls
     * {@link Channel.Protocol#dispatch(Dispatcher,ZMQ.Socket,Message)}.
     *
     * @param   socket          The {@link ZMQ.Socket}.
     * @param   message         The {@link Message}.
     */
    protected void dispatch(ZMQ.Socket socket, Message message) {
        ((Channel.Protocol) getChannel()).dispatch(this, socket, message);
    }

    /**
     * Method to schedule a message for publishing.
     *
     * @param   message         The message to send.
     */
    public void pub(Message message) {
        var type = getChannel().getSocketType();

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
        var server = getChannel().getServer();
        var context = server.getContext();
        var digester = getDigester();
        var type = getChannel().getSocketType();

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
