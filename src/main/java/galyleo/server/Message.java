package galyleo.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import galyleo.PrintStreamBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Data;
import lombok.experimental.Accessors;
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
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@Data @Accessors(fluent = true) @Log4j2
public class Message {
    private static final String DELIMITER_STRING = "<IDS|MSG>";
    private static final byte[] DELIMITER = DELIMITER_STRING.getBytes(US_ASCII);

    private static final StackWalker WALKER = StackWalker.getInstance();

    protected final List<byte[]> envelope = new LinkedList<>();
    protected ObjectNode header = new ObjectNode(JsonNodeFactory.instance);
    protected ObjectNode parentHeader = new ObjectNode(JsonNodeFactory.instance);
    protected ObjectNode metadata = new ObjectNode(JsonNodeFactory.instance);
    protected ObjectNode content = new ObjectNode(JsonNodeFactory.instance);
    protected final List<byte[]> buffers = new LinkedList<>();

    { msg_id(UUID.randomUUID().toString()); }

    private String getCallingMethodName(int skip) {
        String name =
            WALKER.walk(t -> t.map(StackWalker.StackFrame::getMethodName)
                                  .skip(1)
                                  .findFirst())
            .get();

        return name;
    }

    private String asText(JsonNode node) {
        return (node != null) ? node.asText() : null;
    }

    public String msg_id() {
        JsonNode node = header().get(getCallingMethodName(1));

        return asText(node);
    }

    public Message msg_id(String value) {
        header().put(getCallingMethodName(1), value);

        return this;
    }

    public String msg_type() {
        JsonNode node = header().get(getCallingMethodName(1));

        return asText(node);
    }

    public Message msg_type(String value) {
        header().put(getCallingMethodName(1), value);

        return this;
    }

    public String session() {
        JsonNode node = header().get(getCallingMethodName(1));

        return asText(node);
    }

    public Message session(String value) {
        header().put(getCallingMethodName(1), value);

        return this;
    }

    public String username() {
        JsonNode node = header().get(getCallingMethodName(1));

        return asText(node);
    }

    public Message username(String value) {
        header().put(getCallingMethodName(1), value);

        return this;
    }

    public String date() {
        JsonNode node = header().get(getCallingMethodName(1));

        return asText(node);
    }

    public Message date(String value) {
        header().put(getCallingMethodName(1), value);

        return this;
    }

    public String version() {
        JsonNode node = header().get(getCallingMethodName(1));

        return asText(node);
    }

    public Message version(String value) {
        header().put(getCallingMethodName(1), value);

        return this;
    }

    /**
     * Parses a {@link Message} type for an "action".
     *
     * @return  The parsed action.
     */
    public String getMessageTypeAction() {
        var action = msg_type().toLowerCase();
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
        var type = msg_type();

        return type != null && type.toLowerCase().endsWith("_reply");
    }

    /**
     * Method to determine if this message is a "request".
     *
     * @return  {@code true} if it is a "request"; {@code false} otherwise.
     */
    public boolean isRequest() {
        var type = msg_type();

        return type != null && type.toLowerCase().endsWith("_request");
    }

    /**
     * Convenience for {@code getContent().put("status", value)}.
     *
     * @param   value           The value.
     */
    public void setStatus(String value) { content().put("status", value); }

    /**
     * Method to set status for a {@link Throwable}.
     *
     * @param   throwable       The {@link Throwable}.
     */
    public void setStatus(Throwable throwable) {
        setStatus("error");
        content().put("ename", throwable.getClass().getCanonicalName());
        content().put("evalue", throwable.getMessage());

        PrintStreamBuffer buffer = new PrintStreamBuffer();

        throwable.printStackTrace(buffer);

        content().put("traceback", buffer.toString().split("\\R")[0]);
    }

    /**
     * Create a suitable reply {@link Message} for {@link.this}
     * {@link Message}.  Initializes message status to "OK".
     *
     * @param   session         The kernel session ID.
     *
     * @return  The reply {@link Message}.
     */
    public Message reply(String session) {
        if (! isRequest()) {
            throw new IllegalStateException("Source message is not a request");
        }

        return reply(session, getMessageTypeAction() + "_reply");
    }

    /**
     * Create a suitable reply {@link Message} for {@link.this}
     * {@link Message}.  Initializes message status to "OK".
     *
     * @param   session         The kernel session ID.
     * @param   type            The reply {@link Message} type.
     *
     * @return  The reply {@link Message}.
     */
    public Message reply(String session, String type) {
        var reply = new Message();

        reply.envelope().addAll(envelope());
        reply.msg_type(type);
        reply.session(session);
        reply.username(username());
        reply.parentHeader().setAll(header());
        reply.setStatus("ok");
        reply.buffers().addAll(buffers());

        return reply;
    }

    /**
     * Set the {@link date()} value if not already set.
     */
    public Message timestamp() {
        if (date() == null) {
            date(now(UTC).format(ISO_INSTANT));
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
        var frames = envelope().stream().collect(toList());

        frames.add(DELIMITER);

        var header = serialize(mapper, header());
        var parentHeader = serialize(mapper, parentHeader());
        var metadata = serialize(mapper, metadata());
        var content = serialize(mapper, content());

        var digest = "";

        if (digester != null) {
            digest = digester.digest(header, parentHeader, metadata, content);
        }

        Collections.addAll(frames,
                           digest.getBytes(US_ASCII),
                           header, parentHeader, metadata, content);

        frames.addAll(buffers());

        return frames;
    }

    private byte[] serialize(ObjectMapper mapper, JsonNode node) {
        var string = "{}";

        try {
            string = mapper.writeValueAsString(node);
        } catch (Exception exception) {
            log.warn("{}", exception);
        }

        return string.getBytes(UTF_8);
    }

    /**
     * Method to receive a {@link Message} on a {@link ZMQ.Socket}.
     *
     * @param   socket          The {@link ZMQ.Socket}.
     * @param   frame           The first message frame.
     * @param   mapper          The {@link ObjectMapper}.
     * @param   digester        The {@link HMACDigester} (may be
     *                          {@code null}).
     */
    public static Message receive(ZMQ.Socket socket, byte[] frame,
                                  ObjectMapper mapper, HMACDigester digester) {
        var envelope = new LinkedList<byte[]>();

        while (! Arrays.equals(frame, DELIMITER)) {
            envelope.add(frame);

            frame = socket.recv();
        }

        var signature = socket.recv();
        var header = socket.recv();
        var parentHeader = socket.recv();
        var metadata = socket.recv();
        var content = socket.recv();
        var buffers = new LinkedList<byte[]>();

        while (socket.hasReceiveMore()) {
            buffers.add(socket.recv());
        }

        if (digester != null) {
            if (! digester.verify(new String(signature, US_ASCII),
                                  header, parentHeader, metadata, content)) {
                throw new SecurityException("Invalid signature");
            }
        }

        var message = new Message();

        message.envelope().addAll(envelope);
        message.header().setAll(deserialize(mapper, header));
        message.parentHeader().setAll(deserialize(mapper, parentHeader));
        message.metadata().setAll(deserialize(mapper, metadata));
        message.content().setAll(deserialize(mapper, content));
        message.buffers().addAll(buffers);

        return message;
    }

    private static ObjectNode deserialize(ObjectMapper mapper, byte[] bytes) {
        ObjectNode value = null;

        try {
            value = (ObjectNode) mapper.readTree(new String(bytes, UTF_8));
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
        if (version() == null) {
            version(/* getService().PROTOCOL_VERSION */ "5.3");
        }

        timestamp();

        var list = serialize(mapper, digester);
        var iterator = list.iterator();

        while (iterator.hasNext()) {
            socket.send(iterator.next(), iterator.hasNext() ? ZMQ.SNDMORE : 0);
        }
    }
}
