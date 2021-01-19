package galyleo.jupyter;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedList;
import java.util.List;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.zeromq.SocketType;
import org.zeromq.ZMQ;

/**
 * Jupyter {@link Socket} {@link Dispatcher}.  All {@link Socket} creation
 * and manipulation calls happens in the {@link #run()} method.
 *
 * {@bean.info}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@Data @Log4j2
public abstract class Dispatcher implements Runnable {
    @NonNull private final Service service;
    @NonNull private final String address;
    private final HMACDigester digester;

    /**
     * Callback method to dispatch a received message.  Default
     * implementation calls
     * {@link Service#dispatch(Dispatcher,ZMQ.Socket,Message)}.
     *
     * @param   socket          The {@link ZMQ.Socket}.
     * @param   message         The message.
     */
    protected void dispatch(ZMQ.Socket socket, byte[] message) {
        getService().dispatch(this, socket, message);
    }

    @Override
    public void run() {
        var context = getService().getServer().getContext();
        var type = getService().getSocketType();

        for (;;) {
            try (ZMQ.Socket socket = context.socket(type)) {
                if (socket.bind(getAddress())) {
                    log.info("Bound {} {}", type, address);
                } else {
                    log.warn("Could not bind to {}", address);
                }

                try (ZMQ.Poller poller = context.poller(1)) {
                    poller.register(socket, ZMQ.Poller.POLLIN);

                    for (;;) {
                        int events = poller.poll(0);

                        if (events > 0 && poller.pollin(0)) {
                            var message = socket.recv();

                            if (message != null) {
                                dispatch(socket, message);
                            }
                        }
                    }
                }
            } catch (Exception exception) {
                log.warn("{}", exception);
            }
        }
    }
}
