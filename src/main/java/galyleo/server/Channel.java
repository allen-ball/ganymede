package galyleo.server;

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
     * Method to schedule creation of and binding to a {@link ZMQ.Socket}
     * for this {@link Channel}.
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
        protected static final String PROTOCOL_VERSION = "5.3";

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
     * {@link Control Control} {@link Channel} abstract base class.  The
     * default implementation of
     * {@link #dispatch(Dispatcher,ZMQ.Socket,Message)} constructs a
     * {@link Message reply} skeleton, executes a declared method of the
     * form {@code action(Dispatcher,Message,Message) throws Exception},
     * catches any {@link Exception} and updates the reply as necessary, and
     * send the reply.
     *
     * {@bean.info}
     */
    @ToString @Log4j2
    public static abstract class Control extends Protocol {
        private static abstract class PROTOTYPE {
            private void action(Dispatcher dispatcher, Message request, Message reply) throws Exception {
            }
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
                var reply = message.reply(getServer().getSession());

                try {
                    var method = getClass().getDeclaredMethod(action, PROTOTYPE.getParameterTypes());

                    method.setAccessible(true);
                    method.invoke(this, dispatcher, message, reply);
                } catch (Exception exception) {
                    reply.status(exception);
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

        /**
         * Parameter to {@link #pub(Stream,Message,String)}.
         */
        public enum Stream { stderr, stdout };

        /**
         * Method to compose and schedule a {@link Stream Stream}
         * {@link Message} for publishing.
         *
         * @param   stream      The {@link Message} {@link Stream Stream}.
         * @param   referent    The subject {@link Message}.
         * @param   text        The text {@link String}.
         */
        public void pub(Stream stream, Message referent, String text) {
            Message message = new Message();

            message.msg_type("stream");

            if (referent != null) {
                message.parentHeader().setAll(referent.header());
            }

            message.content().put("name", stream.name());
            message.content().put("text", text);

            pub(message);
        }

        /**
         * See {@link #pub(Stream,Message,String)}.
         *
         * @param   referent    The subject {@link Message}.
         * @param   text        The text {@link String}.
         */
        public void stdout(Message referent, String text) {
            pub(Stream.stdout, referent, text);
        }

        /**
         * See {@link #pub(Stream,Message,String)}.
         *
         * @param   referent    The subject {@link Message}.
         * @param   text        The text {@link String}.
         */
        public void stderr(Message referent, String text) {
            pub(Stream.stderr, referent, text);
        }

        /**
         * Parameter to {@link #pub(Status,Message)}.
         */
        public enum Status { starting, busy, idle };

        /**
         * Method to compose and schedule a {@link Status Status}
         * {@link Message} for publishing.
         *
         * @param   status      The {@link Message} {@link Status Status}.
         * @param   referent    The subject {@link Message}.
         */
        public void pub(Status status, Message referent) {
            Message message = new Message();

            message.msg_type("status");

            if (referent != null) {
                message.parentHeader().setAll(referent.header());
            }

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
        public void connect(String address, HMACDigester digester) {
            boolean isStarting = getDispatcherQueue().isEmpty();

            super.connect(address, digester);

            if (isStarting) {
                iopub.pub(Channel.IOPub.Status.starting, null);
                iopub.pub(Channel.IOPub.Status.idle, null);
            }
        }

        @Override
        protected void dispatch(Dispatcher dispatcher, ZMQ.Socket socket, Message message) {
            try {
                iopub.pub(Channel.IOPub.Status.busy, message);

                super.dispatch(dispatcher, socket, message);
            } finally {
                iopub.pub(Channel.IOPub.Status.idle, message);
            }
        }
    }
}
