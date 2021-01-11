package galyleo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.zeromq.SocketType;
import org.zeromq.ZMQ;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;

/**
 * Jupyter {@link Connection}.
 *
 * {@bean.info}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@Data @Log4j2
public class Connection {
    private static final String DELIMITER_STRING = "<IDS|MSG>";
    private static final byte[] DELIMITER = DELIMITER_STRING.getBytes(US_ASCII);

    private final ZMQ.Context context;
    private final Properties properties;
    private final ObjectMapper mapper;
    private final HMACDigester digester;
    private final ZMQ.Socket control;
    private final ZMQ.Socket shell;
    private final ZMQ.Socket stdin;
    private final ZMQ.Socket heartbeat;
    private final ZMQ.Socket iopub;

    /**
     * Sole constructor.
     *
     * @param   context         The {@link ZMQ.Context}.
     * @param   properties      The {@link Properties}.
     * @param   mapper          The {@link ObjectMapper}.
     */
    protected Connection(ZMQ.Context context,
                         Properties properties, ObjectMapper mapper) {
        this.context = Objects.requireNonNull(context);
        this.properties = Objects.requireNonNull(properties);
        this.mapper = Objects.requireNonNull(mapper);

        digester = new HMACDigesterImpl();

        control = socket(SocketType.ROUTER, properties.getControlPort());
        shell = socket(SocketType.ROUTER, properties.getShellPort());
        stdin = socket(SocketType.ROUTER, properties.getStdinPort());
        heartbeat = socket(SocketType.REP, properties.getHeartbeatPort());
        iopub = socket(SocketType.PUB, properties.getIopubPort());
    }

    private ZMQ.Socket socket(SocketType type, int port) {
        var socket = context.socket(type);

        socket.connect(String.format("%s://%s:%d",
                                     properties.getTransport(),
                                     properties.getIp(), port));

        return socket;
    }

    private class HMACDigesterImpl extends HMACDigester {
        public HMACDigesterImpl() {
            super(properties.getSignatureScheme(), properties.getKey());
        }
    }

    /**
     * Method to receive and de-serialize and {@link Message} on a
     * {@link ZMQ.Socket}.
     *
     * @param   socket          The {@link ZMQ.Socket}.
     *
     * @return  The {@link Message}.
     */
    public Message receive(ZMQ.Socket socket) {
        var identities =
            Stream.generate(() -> socket.recv())
            .takeWhile(t -> (! Arrays.equals(t, DELIMITER)))
            .collect(toList());

        var signature = socket.recvStr();
        var header = socket.recv();
        var parentHeader = socket.recv();
        var metadata = socket.recv();
        var content = socket.recv();
        var buffers = new LinkedList<byte[]>();

        while (socket.hasReceiveMore()) {
            buffers.add(socket.recv());
        }

        if (! digester.verify(signature, header, parentHeader, metadata, content)) {
            throw new SecurityException("Invalid signature");
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

    /**
     * Method to serialize and send a {@link Message} on a
     * {@link ZMQ.Socket}.
     *
     * @param   socket          The {@link ZMQ.Socket}.
     * @param   message         The {@link Message}.
     */
    public void send(ZMQ.Socket socket, Message message) {
        var packets = message.getIdentities().stream().collect(toList());

        packets.add(DELIMITER);

        var header = serialize(message.getHeader());
        var parentHeader = serialize(message.getParentHeader());
        var metadata = serialize(message.getMetadata());
        var content = serialize(message.getContent());

        packets.add(digester.digest(header, parentHeader, metadata, content)
                    .getBytes(US_ASCII));

        Collections.addAll(packets, header, parentHeader, metadata, content);

        var last = packets.size() - 1;

        packets.subList(0, last).forEach(socket::sendMore);
        socket.send(packets.get(last));
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

    /**
     * See
     * "{@link.uri https://jupyter-client.readthedocs.io/en/stable/kernels.html#connection-files target=newtab Connection files}".
     *
     * {@bean.info}
     */
    @Data
    public static class Properties {
        @JsonProperty("control_port")           private int controlPort = -1;
        @JsonProperty("shell_port")             private int shellPort = -1;
        @JsonProperty("transport")              private String transport = null;
        @JsonProperty("signature_scheme")       private String signatureScheme = null;
        @JsonProperty("stdin_port")             private int stdinPort = -1;
        @JsonProperty("hb_port")                private int heartbeatPort = -1;
        @JsonProperty("ip")                     private String ip = null;
        @JsonProperty("iopub_port")             private int iopubPort = -1;
        @JsonProperty("key")                    private String key = null;
    }
}
