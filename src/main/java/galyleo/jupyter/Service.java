package galyleo.jupyter;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import lombok.Data;
import lombok.NonNull;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.zeromq.SocketType;
import org.zeromq.ZMQ;

/**
 * Jupyter {@link Service}.
 *
 * {@bean.info}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@Data @Log4j2
public abstract class Service {
    @NonNull private final Server server;
    @NonNull private final SocketType socketType;
    private final Queue<Dispatcher> dispatcherQueue = new ConcurrentLinkedQueue<>();

    /**
     * Method to schedule creation of and binding to a {@link ZMQ.Socket}
     * for this address.
     *
     * @param   address         The address of the {@link ZMQ.Socket} to be
     *                          created.
     * @param   digester        The {@link HMACDigester} for this address.
     */
    public void connect(String address, HMACDigester digester) {
        Dispatcher dispatcher = new Dispatcher(this, address, digester);

        getDispatcherQueue().add(dispatcher);
        getServer().submit(dispatcher);

        getServer()
            .setCorePoolSize(Math.max(getServer().getActiveCount() + 4,
                                      getServer().getCorePoolSize()));
    }

    /**
     * Method to schedule creation of and binding to a {@link ZMQ.Socket} for this {@link Service}.
     *
     * @param   address         The address of the {@link ZMQ.Socket} to be
     *                          created.
     */
    public void connect(String address) { connect(address, null); }

    /**
     * Callback method to receive and dispatch a {@link Message}.  This
     * method is called on the same thread that the {@link ZMQ.Socket} was
     * created on and the implementation may call {@link ZMQ.Socket} methods
     * (including {@code send()}).
     *
     * @param   dispatcher      The {@link Dispatcher}.
     * @param   socket          The {@link ZMQ.Socket}.
     * @param   frame           The first message frame.
     */
    protected abstract void dispatch(Dispatcher dispatcher, ZMQ.Socket socket, byte[] frame);

    /**
     * Callback method to dispatch a {@link Message}.  This method is called
     * on the same thread that the {@link ZMQ.Socket} was created on and the
     * implementation may call {@link ZMQ.Socket} methods
     * (including {@code send()}).
     *
     * @param   dispatcher      The {@link Dispatcher}.
     * @param   socket          The {@link ZMQ.Socket}.
     * @param   message         The {@link Message}.
     */
    protected void send(Dispatcher dispatcher, ZMQ.Socket socket, Message message) {
        message.send(socket, getServer().getObjectMapper(), dispatcher.getDigester());
    }

    /**
     * Standard
     * {@link.uri https://jupyter-client.readthedocs.io/en/latest/messaging.html#heartbeat-for-kernels Heartbeat}
     * {@link Service}.
     *
     * {@bean.info}
     */
    @ToString @Log4j2
    public static class Heartbeat extends Service {

        /**
         * Sole constructor.
         *
         * @param  server       The {@link Server}.
         */
        public Heartbeat(Server server) { super(server, SocketType.REP); }

        @Override
        protected void dispatch(Dispatcher dispatcher, ZMQ.Socket socket, byte[] frame) {
            socket.send(frame);
        }
    }

    /**
     * Jupyter {@link Service} abstract base class.
     *
     * {@bean.info}
     */
    @ToString @Log4j2
    public static abstract class Jupyter extends Service {
        private static final Class<?>[] PARAMETERS = {
            Dispatcher.class, ZMQ.Socket.class, Message.class
        };

        protected static final String PROTOCOL_VERSION = "5.3";

        /**
         * Sole constructor.
         *
         * @param  server       The {@link Server}.
         * @param  type         The {@link SocketType} for created
         *                      {@link ZMQ.Socket}s.
         */
        public Jupyter(Server server, SocketType type) { super(server, type); }

        /**
         * Callback method to dispatch a {@link Message}.  Default
         * implementation looks for a declared method
         * {@code action(Dispatcher,ZMQ.Socket,Message)}.
         *
         * @param  dispatcher   The {@link Dispatcher}.
         * @param  socket       The {@link ZMQ.Socket}.
         * @param  message      The {@link Message}.
         */
        protected void dispatch(Dispatcher dispatcher, ZMQ.Socket socket, Message message) {
            var action = message.getMessageTypeAction();

            if (action != null) {
                try {
                    var method = getClass().getDeclaredMethod(action, PARAMETERS);

                    method.setAccessible(true);
                    method.invoke(this, dispatcher, socket, message);
                } catch (IllegalAccessException | NoSuchMethodException exception) {
                    log.warn("Could not dispatch '{}'", action, exception);
                } catch (Exception exception) {
                    log.warn("Exception invoking '{}' handler",
                             action, exception);
                }
            } else {
                log.warn("Could not determine action from {}", message.header());
            }
        }

        @Override
        protected void dispatch(Dispatcher dispatcher, ZMQ.Socket socket, byte[] frame) {
            try {
                var message =
                    Message.receive(socket, frame,
                                    getServer().getObjectMapper(),
                                    dispatcher.getDigester());

                dispatch(dispatcher, socket, message);
            } catch (Exception exception) {
                log.warn("{}", exception);
            }
        }
    }

    /**
     * {@link Jupyter Jupyter} {@link IOPub IOPub} {@link Service}.
     *
     * {@bean.info}
     */
    @ToString @Log4j2
    public static class IOPub extends Jupyter {

        /**
         * Sole constructor.
         *
         * @param  server       The {@link Server}.
         */
        public IOPub(Server server) { super(server, SocketType.PUB); }

        /**
         * Method to schedule a {@link Message} for publishing.
         *
         * @param   message     The {@link Message} to send.
         */
        public void pub(Message message) {
            getDispatcherQueue().forEach(t -> t.pub(message));
        }

        /**
         * Parameter to {@link #pub(Status)}.
         */
        public enum Status { starting, busy, idle };

        /**
         * Method to compose and schedule a {@link Status Status}
         * {@link Message} for publishing.
         *
         * @param   status      The {@link Message} {@link Status Status}.
         */
        public void pub(Status status) {
            Message message = new Message();

            message.msg_type("status");
            message.content().put("execution_state", status.name());

            pub(message);
        }

        @Override
        protected void dispatch(Dispatcher dispatcher, ZMQ.Socket socket, byte[] frame) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void dispatch(Dispatcher dispatcher, ZMQ.Socket socket, Message message) {
            send(dispatcher, socket, message);
        }
    }
}
