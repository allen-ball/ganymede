package galyleo.jupyter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.stream.Stream;
import lombok.extern.log4j.Log4j2;
import org.zeromq.SocketType;
import org.zeromq.ZMQ;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;

/**
 * Jupyter {@link Socket}.
 *
 * {@bean.info}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@Log4j2
public class Socket extends ZMQ.Socket {
    private final HMACDigester digester;
    private final ObjectMapper mapper;
    private final String address;

    /**
     * Sole constructor.
     *
     * @param   context         The {@link ZMQ.Context}.
     * @param   type            The {@link SocketType}.
     * @param   address         The {@link Socket}'s address.
     */
    public Socket(ZMQ.Context context, SocketType type, String address) {
        super(context, type);

        this.digester = new HMACDigester(null, null);   /* Tacky !!! */
        this.mapper = new ObjectMapper();               /* Tacky !!! */
        this.address = address;
    }

    /**
     * See {@link ZMQ.Socket#connect(String)}.
     *
     * @return  {@code true} if the {@link Socket} was connected;
     *          {@code false} otherwise.
     */
    public boolean connect() { return connect(address); }

    /**
     * Method to receive and de-serialize and {@link Message} on a
     * {@link ZMQ.Socket}.
     *
     * @return  The {@link Message}.
     */
    public Message receive() {
        var identities =
            Stream.generate(() -> recv())
            .takeWhile(t -> (! Arrays.equals(t, Message.DELIMITER)))
            .collect(toList());

        var signature = recvStr();
        var header = recv();
        var parentHeader = recv();
        var metadata = recv();
        var content = recv();
        var buffers = new LinkedList<byte[]>();

        while (hasReceiveMore()) {
            buffers.add(recv());
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
     * Method to serialize and send a {@link Message} on {@link.this}
     * {@link Socket}.
     *
     * @param   message         The {@link Message}.
     */
    public void send(Message message) {
        var packets = message.getIdentities().stream().collect(toList());

        packets.add(Message.DELIMITER);

        var header = serialize(message.getHeader());
        var parentHeader = serialize(message.getParentHeader());
        var metadata = serialize(message.getMetadata());
        var content = serialize(message.getContent());

        packets.add(digester.digest(header, parentHeader, metadata, content)
                    .getBytes(US_ASCII));

        Collections.addAll(packets, header, parentHeader, metadata, content);

        var last = packets.size() - 1;

        packets.subList(0, last).forEach(this::sendMore);
        send(packets.get(last));
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
