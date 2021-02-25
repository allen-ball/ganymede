package ganymede.server;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import lombok.Data;
import lombok.NonNull;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.zeromq.SocketType;
import org.zeromq.ZMQ;

/**
 * {@link Server} {@link Channel}.
 *
 * {@bean.info}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@Data @Log4j2
public abstract class Channel {
    @NonNull private final Server server;
    @NonNull private final SocketType socketType;
    private final Queue<Dispatcher> dispatcherQueue = new ConcurrentLinkedQueue<>();

    /**
     * Method to schedule creation of and binding to a {@link ZMQ.Socket}
     * for this address.
     *
     * @param   connection      The kernel {@link Connection}.
     * @param   address         The address of the {@link ZMQ.Socket} to be
     *                          created.
     */
    public void connect(Connection connection, String address) {
        Dispatcher dispatcher = new Dispatcher(this, connection, address);

        getDispatcherQueue().add(dispatcher);
        getServer().submit(dispatcher);

        getServer()
            .setCorePoolSize(Math.max(getServer().getActiveCount() + 4,
                                      getServer().getCorePoolSize()));
    }

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
     * Callback method to {@link Server#stamp(Message) stamp} and dispatch a
     * {@link Message}.  This method is called on the same thread that the
     * {@link ZMQ.Socket} was created on and the implementation may call
     * {@link ZMQ.Socket} methods (including {@code send()}).
     *
     * @param   dispatcher      The {@link Dispatcher}.
     * @param   socket          The {@link ZMQ.Socket}.
     * @param   message         The {@link Message}.
     */
    protected void send(Dispatcher dispatcher, ZMQ.Socket socket, Message message) {
        getServer().stamp(message)
            .send(socket, dispatcher.getConnection().getDigester());
    }

    /**
     * Standard
     * {@link.uri https://jupyter-client.readthedocs.io/en/latest/messaging.html#heartbeat-for-kernels Heartbeat}
     * {@link Channel}.
     *
     * {@bean.info}
     */
    @ToString @Log4j2
    public static class Heartbeat extends Channel {

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
     * Jupyter protocol {@link Channel} abstract base class.
     *
     * {@bean.info}
     */
    @ToString @Log4j2
    public static abstract class Protocol extends Channel {

        /**
         * Sole constructor.
         *
         * @param  server       The {@link Server}.
         * @param  type         The {@link SocketType} for created
         *                      {@link ZMQ.Socket}s.
         */
        public Protocol(Server server, SocketType type) { super(server, type); }

        /**
         * Callback method to dispatch a {@link Message}.  Default
         * implementation looks for a declared method
         * {@code action(Dispatcher,ZMQ.Socket,Message)}.
         *
         * @param  dispatcher   The {@link Dispatcher}.
         * @param  socket       The {@link ZMQ.Socket}.
         * @param  message      The {@link Message}.
         */
        protected abstract void dispatch(Dispatcher dispatcher, ZMQ.Socket socket, Message message);

        @Override
        protected void dispatch(Dispatcher dispatcher, ZMQ.Socket socket, byte[] frame) {
            try {
                var message =
                    Message.receive(socket, frame, dispatcher.getConnection().getDigester());

                dispatch(dispatcher, socket, message);
            } catch (Exception exception) {
                log.warn("{}", exception);
            }
        }
    }

    /**
     * {@link Control Control} {@link Channel} abstract base class.  The
     * default implementation of
     * {@link #dispatch(Dispatcher,ZMQ.Socket,Message)} constructs a
     * {@link Message reply} skeleton, executes a declared method of the
     * form {@code action(Dispatcher,Message,Message) throws Exception},
     * catches any {@link Exception} and updates the reply as necessary, and
     * sends the reply.
     *
     * {@bean.info}
     */
    @ToString @Log4j2
    public static abstract class Control extends Protocol {
        private interface PROTOTYPE {
            public void action(Dispatcher dispatcher, Message request, Message reply) throws Exception;
        }

        private static final Method PROTOTYPE;

        static {
            PROTOTYPE = PROTOTYPE.class.getDeclaredMethods()[0];
            PROTOTYPE.setAccessible(true);
        }

        /**
         * Sole constructor.
         *
         * @param  server       The {@link Server}.
         */
        protected Control(Server server) { super(server, SocketType.ROUTER); }

        @Override
        protected void dispatch(Dispatcher dispatcher, ZMQ.Socket socket, Message message) {
            var action = message.getMessageTypeAction();

            if (action != null) {
                var reply = message.reply();

                try {
                    var method = getClass().getDeclaredMethod(action, PROTOTYPE.getParameterTypes());

                    method.setAccessible(true);
                    method.invoke(this, dispatcher, message, reply);
                } catch (Throwable throwable) {
                    if (throwable instanceof InvocationTargetException) {
                        if (throwable.getCause() != null) {
                            throwable = throwable.getCause();
                        }
                    }

                    reply.status(throwable);
                } finally {
                    send(dispatcher, socket, reply);
                }
            } else {
                log.warn("Could not determine action from {}", message.header());
            }
        }
    }

    /**
     * {@link IOPub IOPub} {@link Channel}.
     *
     * {@bean.info}
     */
    @ToString @Log4j2
    public static class IOPub extends Protocol {

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

        @Override
        protected void dispatch(Dispatcher dispatcher, ZMQ.Socket socket, byte[] frame) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void dispatch(Dispatcher dispatcher, ZMQ.Socket socket, Message message) {
            send(dispatcher, socket, message);
        }
    }

    /**
     * {@link Stdin Stdin} {@link Channel} abstract base class.
     *
     * {@bean.info}
     */
    @ToString @Log4j2
    public static abstract class Stdin extends Protocol {

        /**
         * Sole constructor.
         *
         * @param  server       The {@link Server}.
         */
        protected Stdin(Server server) { super(server, SocketType.ROUTER); }
    }

    /**
     * {@link Shell Shell} {@link Channel} abstract base class.
     *
     * {@bean.info}
     */
    @ToString @Log4j2
    public static abstract class Shell extends Control {
        private final IOPub iopub;
        private final Stdin stdin;

        /**
         * Sole constructor.
         *
         * @param  server       The {@link Server}.
         * @param  iopub        The associated {@link IOPub IOPub}
         *                      {@link Channel}.
         * @param  stdin        The associated {@link Stdin Stdin}
         *                      {@link Channel}.
         */
        protected Shell(Server server, IOPub iopub, Stdin stdin) {
            super(server);

            this.iopub = iopub;
            this.stdin = stdin;
        }

        @Override
        public void connect(Connection connection, String address) {
            boolean isStarting = getDispatcherQueue().isEmpty();

            super.connect(connection, address);

            if (isStarting) {
                iopub.pub(Message.status(Message.status.starting, null));
                iopub.pub(Message.status(Message.status.idle, null));
            }
        }

        @Override
        protected void dispatch(Dispatcher dispatcher, ZMQ.Socket socket, Message message) {
            try {
                iopub.pub(message.status(Message.status.busy));

                super.dispatch(dispatcher, socket, message);
            } finally {
                iopub.pub(message.status(Message.status.idle));
            }
        }
    }
}
