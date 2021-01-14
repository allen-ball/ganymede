package galyleo.jupyter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Stream;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.zeromq.SocketType;
import org.zeromq.ZMQ;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.stream.Collectors.toList;

/**
 * Jupyter {@link Service}.
 *
 * {@bean.info}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@Log4j2
public abstract class Service extends ZMQ.Poller implements Runnable {
    private final ZMQ.Context context;
    private final SocketType type;
    private final BlockingDeque<Runnable> queue = new LinkedBlockingDeque<>();
    private final long timeout = 100;

    /**
     * Sole constructor.
     *
     * @param   context         The {@link ZMQ.Context}.
     * @param   type            The {@link SocketType} for created
     *                          {@link ZMQ.Socket}s.
     */
    protected Service(ZMQ.Context context, SocketType type) {
        super(context);

        this.context = Objects.requireNonNull(context);
        this.type = Objects.requireNonNull(type);
    }

    /**
     * Method to schedule creation of and connection to a {@link Socket} for
     * this {@link Service}.
     *
     * @param   address         The address of the {@link Socket} to be
     *                          created.
     */
    public void connect(String address) {
        Runnable runnable = () -> {
            var socket = new Socket(context, type, address);

            socket.connect();

            register(socket, ZMQ.Poller.POLLIN);
        };

        queue(runnable);
    }

    /**
     * Method to schedule a {@link Runnable} to be called within
     * {@link #dispatch()}.
     *
     * @param   runnable        The {@link Runnable}.
     *
     * @return  See {@link BlockingDeque#add(Object)}.
     */
    protected boolean queue(Runnable runnable) { return queue.add(runnable); }

    /**
     * Callback method to create and connect any outstanding
     * {@link Socket}s, poll for input, and call {@link #handle(Socket)}
     * where input is available.
     */
    protected void dispatch() {
        var list = new LinkedList<Runnable>();

        queue.drainTo(list);
        list.forEach(Runnable::run);

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
            if (queue.isEmpty()) {
                try {
                    var runnable = queue.poll(timeout, MICROSECONDS);

                    if (runnable != null) {
                        queue.putFirst(runnable);
                    }
                } catch (InterruptedException exception) {
                }
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

    /**
     * Method to queue a message for sending.
     *
     * @param   socket          The {@link Socket}.
     * @param   message         The message {@code byte}s.
     */
    public void send(Socket socket, byte[] message) {
        queue(() -> socket.send(message));
    }

    /**
     * Method to queue a multi-part message for sending.
     *
     * @param   socket          The {@link Socket}.
     * @param   blobs           The {@link List} of message parts.
     */
    public void send(Socket socket, List<byte[]> blobs) {
        Runnable runnable = () -> {
            var last = blobs.size() - 1;

            blobs.subList(0, last).forEach(socket::sendMore);
            socket.send(blobs.get(last));
        };

        queue(runnable);
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
     *
     * {@bean.info}
     */
    @Log4j2 @ToString
    public static class Heartbeat extends Service {

        /**
         * Sole constructor.
         *
         * @param  context      The {@link ZMQ.Context}.
         */
        public Heartbeat(ZMQ.Context context) {
            super(context, SocketType.REP);
        }

        @Override
        protected void handle(Socket socket) {
            var request = socket.recv();

            if (request != null) {
                socket.send(request);
            }
        }
    }

    /**
     * Jupyter {@link Service} abstract base class.
     *
     * {@bean.info}
     */
    @Log4j2 @ToString
    public static abstract class Jupyter extends Service {
        private final ObjectMapper mapper;
        private final ConcurrentSkipListMap<String,HMACDigester> map =
            new ConcurrentSkipListMap<>();

        /**
         * Sole constructor.
         *
         * @param  context      The {@link ZMQ.Context}.
         * @param  type         The {@link SocketType} for created
         *                      {@link ZMQ.Socket}s.
         * @param  mapper       The {@link ObjectMapper}.
         */
        public Jupyter(ZMQ.Context context, SocketType type, ObjectMapper mapper) {
            super(context, type);

            this.mapper = Objects.requireNonNull(mapper);
        }

        /**
         * Method to schedule creation of and connection to a {@link Socket}
         * for this {@link Service}.
         *
         * @param  address      The address of the {@link Socket} to be
         *                      created.
         * @param  digester     The {@link HMACDigester} for this
         *                      {@link Socket}.
         */
        public void connect(String address, HMACDigester digester) {
            map.put(address, digester);
            connect(address);
        }

        /**
         * Callback method to handle a {@link Message request}.
         *
         * @param  socket       The {@link Socket}.
         * @param  request      The request {@link Message}.
         */
        protected abstract void handle(Socket socket, Message request);

        @Override
        protected void handle(Socket socket) {
            try {
                var first = socket.recv();

                if (first != null) {
                    var blobs = new LinkedList<byte[]>();

                    blobs.add(first);

                    while (socket.hasReceiveMore()) {
                        blobs.add(socket.recv());
                    }

                    var request = deserialize(blobs, map.get(socket.getAddress()));

                    if (request != null) {
                        handle(socket, request);
                    }
                }
            } catch (Exception exception) {
                log.warn("{}", exception);
            }
        }

        private Message deserialize(List<byte[]> blobs, HMACDigester digester) {
            Iterator<byte[]> iterator = blobs.iterator();
            var identities = new LinkedList<byte[]>();

            while (iterator.hasNext()) {
                byte[] blob = iterator.next();

                if (! Arrays.equals(blob, Message.DELIMITER)) {
                    identities.add(blob);
                } else {
                    break;
                }
            }

            var signature = new String(iterator.next(), US_ASCII);
            var header = iterator.next();
            var parentHeader = iterator.next();
            var metadata = iterator.next();
            var content = iterator.next();
            var buffers = new LinkedList<byte[]>();

            while (iterator.hasNext()) {
                buffers.add(iterator.next());
            }

            if (digester != null) {
                if (! digester.verify(signature, header, parentHeader, metadata, content)) {
                    throw new SecurityException("Invalid signature");
                }
            }

            var message = new Message();

            message.setIdentities(identities);
            message.setHeader(deserialize(header, Message.Header.class));
            message.setParentHeader(deserialize(parentHeader, Message.Header.class));
            message.getMetadata().putAll(deserialize(metadata));
            message.getContent().putAll(deserialize(content));
            message.setBuffers(buffers);

            return message;
        }

        private <T> T deserialize(byte[] bytes, Class<T> type) {
            T value = null;

            try {
                var string = new String(bytes, UTF_8);
                var node = mapper.readTree(string);

                if (! node.isEmpty()) {
                    value = mapper.readValue(string, type);
                }
            } catch (Exception exception) {
                log.warn("{}", exception);
            }

            return value;
        }

        private Map<String,Object> deserialize(byte[] bytes) {
            Map<String,Object> value = null;

            try {
                value =
                    mapper.readValue(new String(bytes, UTF_8),
                                     new TypeReference<Map<String,Object>>() { });
            } catch (Exception exception) {
                log.warn("{}", exception);
            }

            return value;
        }

        private List<byte[]> serialize(Message message, HMACDigester digester) {
            var blobs = message.getIdentities().stream().collect(toList());

            blobs.add(Message.DELIMITER);

            var header = serialize(message.getHeader());
            var parentHeader = serialize(message.getParentHeader());
            var metadata = serialize(message.getMetadata());
            var content = serialize(message.getContent());

            var digest = "";

            if (digester != null) {
                digest =
                    digester.digest(header, parentHeader, metadata, content);
            }

            blobs.add(digest.getBytes(US_ASCII));

            Collections.addAll(blobs, header, parentHeader, metadata, content);

            return blobs;
        }

        private byte[] serialize(Object object) {
            var string = "{}";

            if (object == null) {
                object = Collections.emptyMap();
            }

            try {
                string = mapper.writeValueAsString(object);
            } catch (Exception exception) {
                log.warn("{}", exception);
            }

            return string.getBytes(UTF_8);
        }
    }
}
