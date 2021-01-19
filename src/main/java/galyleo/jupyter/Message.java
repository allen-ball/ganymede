package galyleo.jupyter;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import galyleo.PrintStreamBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.zeromq.ZMQ;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static java.util.stream.Collectors.toList;

/**
 * Jupyter {@link Message}.  See
 * "{@link.uri https://jupyter-client.readthedocs.io/en/latest/messaging.html#general-message-format target=newtab General Message Format}."
 *
 * {@bean.info}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@Data @Log4j2
public class Message {
    private static final String DELIMITER_STRING = "<IDS|MSG>";
    private static final byte[] DELIMITER = DELIMITER_STRING.getBytes(US_ASCII);

    private List<byte[]> identities = new LinkedList<>();
    private Header header = new Header();
    private Header parentHeader = null;
    private Map<String,Object> metadata = new LinkedHashMap<>();
    private Map<String,Object> content = new LinkedHashMap<>();
    private List<byte[]> buffers = new LinkedList<>();

    /**
     * Convenience for {@code getHeader().getMessageType()}.
     *
     * @return  The {@link Message} type.
     */
    public String getMessageType() { return getHeader().getMessageType(); }

    /**
     * Convenience for {@code getHeader().setMessageType(type)}.
     *
     * @param   type            The {@link Message} type.
     */
    public void setMessageType(String type) { getHeader().setMessageType(type); }

    /**
     * Parses a {@link Message} type for an "action".
     *
     * @return  The parsed action.
     */
    public String getMessageTypeAction() {
        var action = getMessageType().toLowerCase();
        var index = action.lastIndexOf("_");

        if (index != -1) {
            action = action.substring(0, index);
        }

        return action;
    }

    /**
     * Method to determine if this message is a "reply".
     *
     * @return  {@code true} if it is a "reply"; {@code false} otherwise.
     */
    public boolean isReply() {
        var type = getMessageType();

        return type != null && type.toLowerCase().endsWith("_reply");
    }

    /**
     * Method to determine if this message is a "request".
     *
     * @return  {@code true} if it is a "request"; {@code false} otherwise.
     */
    public boolean isRequest() {
        var type = getMessageType();

        return type != null && type.toLowerCase().endsWith("_request");
    }

    /**
     * Convenience for {@code getHeader().getDate()}.
     *
     * @return  The {@link Message} date.
     */
    public String getDate() { return getHeader().getDate(); }

    /**
     * Convenience for {@code getHeader().setDate(date)}.
     *
     * @param   date            The {@link Message} date.
     */
    public void setDate(String date) { getHeader().setDate(date); }

    /**
     * Convenience for {@code getMetadata().get(key)}.
     *
     * @param   key             The key into the metadata map.
     *
     * @return  The corresponding value (if any).
     */
    public Object getMetadata(Object key) { return getMetadata().get(key); }

    /**
     * Convenience for {@code getMetadata().put(key, value)}.
     *
     * @param   key             The key into the metadata map.
     * @param   value           The value.
     */
    public void setMetadata(String key, Object value) {
        getMetadata().put(key, value);
    }

    /**
     * Convenience for {@code getContent().get(key)}.
     *
     * @param   key             The key into the content map.
     *
     * @return  The corresponding value (if any).
     */
    public Object getContent(Object key) { return getContent().get(key); }

    /**
     * Convenience for {@code getContent().put(key, value)}.
     *
     * @param   key             The key into the metadata map.
     * @param   value           The value.
     */
    public void setContent(String key, Object value) {
        getContent().put(key, value);
    }

    /**
     * Convenience for {@code setContent("status", value)}.
     *
     * @param   value           The value.
     */
    public void setStatus(String value) { setContent("status", value); }

    /**
     * Method to set status for a {@link Throwable}.
     *
     * @param   throwable       The {@link Throwable}.
     */
    public void setStatus(Throwable throwable) {
        setStatus("error");
        setContent("ename", throwable.getClass().getCanonicalName());
        setContent("evalue", throwable.getMessage());

        PrintStreamBuffer buffer = new PrintStreamBuffer();

        throwable.printStackTrace(buffer);

        setContent("traceback", buffer.toString().split("\\R"));
    }

    /**
     * Create a suitable reply {@link Message} for {@link.this}
     * {@link Message}.  Initializes message status to "OK".
     *
     * @return  The reply {@link Message}.
     */
    public Message reply() {
        if (! isRequest()) {
            throw new IllegalStateException("Source message is not a request");
        }

        return reply(getMessageTypeAction() + "_reply");
    }

    /**
     * Create a suitable reply {@link Message} for {@link.this}
     * {@link Message}.  Initializes message status to "OK".
     *
     * @param   type            The reply {@link Message} type.
     *
     * @return  The reply {@link Message}.
     */
    public Message reply(String type) {
        var reply = new Message();

        reply.getIdentities().addAll(getIdentities());
        reply.setMessageType(type);
        reply.getHeader().setMessageId(UUID.randomUUID().toString());
        reply.getHeader().setSession(getHeader().getSession());
        reply.getHeader().setUsername(getHeader().getUsername());
        reply.setParentHeader(getHeader());
        reply.setStatus("ok");
        reply.getBuffers().addAll(getBuffers());

        return reply;
    }

    /**
     * Set the {@link Header} date value if not already set.
     */
    public Message timestamp() {
        if (getDate() == null) {
            setDate(now(UTC).format(ISO_INSTANT));
        }

        return this;
    }

    /**
     * Method to serialize a {@link Message}.
     *
     * @param   mapper          The {@link ObjectMapper}.
     * @param   digester        The {@link HMACDigester} (may be
     *                          {@code null}).
     */
    public List<byte[]> serialize(ObjectMapper mapper, HMACDigester digester) {
        var blobs = getIdentities().stream().collect(toList());

        blobs.add(DELIMITER);

        var header = serialize(mapper, getHeader());
        var parentHeader = serialize(mapper, getParentHeader());
        var metadata = serialize(mapper, getMetadata());
        var content = serialize(mapper, getContent());

        var digest = "";

        if (digester != null) {
            digest = digester.digest(header, parentHeader, metadata, content);
        }

        Collections.addAll(blobs,
                           digest.getBytes(US_ASCII),
                           header, parentHeader, metadata, content);

        blobs.addAll(getBuffers());

        return blobs;
    }

    private byte[] serialize(ObjectMapper mapper, Object object) {
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
     * Method to receive a {@link Message} on a {@link ZMQ.Socket}.
     *
     * @param   socket          The {@link ZMQ.Socket}.
     * @param   mapper          The {@link ObjectMapper}.
     * @param   digester        The {@link HMACDigester} (may be
     *                          {@code null}).
     */
    public static Message receive(ZMQ.Socket socket, byte[] blob,
                                  ObjectMapper mapper, HMACDigester digester) {
        var identities = new LinkedList<byte[]>();
        var identity = blob;

        while (! Arrays.equals(identity, DELIMITER)) {
            identities.add(identity);

            identity = socket.recv();
        }

        var signature = socket.recvStr();
        var header = socket.recv();
        var parentHeader = socket.recv();
        var metadata = socket.recv();
        var content = socket.recv();
        var buffers = new LinkedList<byte[]>();

        while (socket.hasReceiveMore()) {
            buffers.add(socket.recv());
        }

        if (digester != null) {
            if (! digester.verify(signature, header, parentHeader, metadata, content)) {
                throw new SecurityException("Invalid signature");
            }
        }

        var message = new Message();

        message.setIdentities(identities);
        message.setHeader(deserialize(mapper, Message.Header.class, header));
        message.setParentHeader(deserialize(mapper, Message.Header.class, parentHeader));
        message.getMetadata().putAll(deserialize(mapper, metadata));
        message.getContent().putAll(deserialize(mapper, content));
        message.setBuffers(buffers);

        return message;
    }

    private static <T> T deserialize(ObjectMapper mapper, Class<T> type, byte[] bytes) {
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

    private static Map<String,Object> deserialize(ObjectMapper mapper, byte[] bytes) {
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
     * Method to send a {@link Message}.
     *
     * @param   socket          The {@link ZMQ.Socket}.
     * @param   mapper          The {@link ObjectMapper}.
     * @param   digester        The {@link HMACDigester} (may be
     *                          {@code null}).
     */
    public void send(ZMQ.Socket socket, ObjectMapper mapper, HMACDigester digester) {
        if (getHeader().getVersion() == null) {
            getHeader().setVersion(/* getService().PROTOCOL_VERSION */ "5.3");
        }

        timestamp();

        var blobs = serialize(mapper, digester);
        var last = blobs.size() - 1;

        blobs.subList(0, last).forEach(socket::sendMore);
        socket.send(blobs.get(last));
    }

    /**
     * See
     * "{@link.uri https://jupyter-client.readthedocs.io/en/latest/messaging.html#message-header target=newtab Message Header}".
     *
     * {@bean.info}
     */
    @Data
    public static class Header {
        @JsonProperty("msg_id")         private String messageId = null;
        @JsonProperty("msg_type")       private String messageType = null;
        @JsonProperty("session")        private String session = null;
        @JsonProperty("username")       private String username = null;
        @JsonProperty("date")           private String date = null;
        @JsonProperty("version")        private String version = null;
    }
}
