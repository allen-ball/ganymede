package ganymede.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ganymede.io.PrintStreamBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.log4j.Log4j2;
import org.zeromq.ZMQ;

import static ganymede.server.Server.OBJECT_MAPPER;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static java.util.stream.Collectors.toList;
import static lombok.AccessLevel.PRIVATE;

/**
 * Jupyter {@link Message}.  See
 * "{@link.uri https://jupyter-client.readthedocs.io/en/latest/messaging.html#general-message-format target=newtab General Message Format}."
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@NoArgsConstructor(access = PRIVATE) @Data @Accessors(fluent = true) @Log4j2
public class Message {
    private static final String DELIMITER_STRING = "<IDS|MSG>";
    private static final byte[] DELIMITER = DELIMITER_STRING.getBytes(UTF_8);

    private static final StackWalker WALKER = StackWalker.getInstance();

    /* private enum Status { ok, error } */

    protected final List<byte[]> envelope = new LinkedList<>();
    protected ObjectNode header = new ObjectNode(JsonNodeFactory.instance);
    protected ObjectNode parentHeader = new ObjectNode(JsonNodeFactory.instance);
    protected ObjectNode metadata = new ObjectNode(JsonNodeFactory.instance);
    protected ObjectNode content = new ObjectNode(JsonNodeFactory.instance);
    protected final List<byte[]> buffers = new LinkedList<>();

    { msg_id(UUID.randomUUID().toString()); }

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

    private String asText(JsonNode node) {
        return (node != null) ? node.asText() : null;
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
     * Method to set status for a {@link Throwable}.
     *
     * @param   throwable       The {@link Throwable}.
     * @param   evalue          The expression value.
     *
     * @return  {@link.this} {@link Message} for chaining.
     */
    public Message status(Throwable throwable, String evalue) {
        content().setAll(content(throwable, evalue));

        return this;
    }

    /**
     * Method to set status for a {@link Throwable}.
     *
     * @param   throwable       The {@link Throwable}.
     *
     * @return  {@link.this} {@link Message} for chaining.
     */
    public Message status(Throwable throwable) {
        return status(throwable, throwable.getMessage());
    }

    /**
     * Parameter to {@link Message#status(Message.completeness)}.
     */
    public enum completeness { complete, incomplete, invalid, unknown };

    /**
     * Method to set status for a code {@link completeness completeness}
     * test.
     *
     * @param   completeness    The {@link completeness}.
     *
     * @return  {@link.this} {@link Message} for chaining.
     */
    public Message status(completeness completeness) {
        content().put("status", completeness.name());

        return this;
    }

    /**
     * Create a suitable reply {@link Message} for {@link.this}
     * {@link Message}.  Copies {@link.this} {@link Message}'s envelope and
     * buffers and initializes the message status to "OK".
     *
     * @return  The reply {@link Message}.
     */
    public Message reply() { return new Reply(this); }

    /**
     * Parameter to {@link Message#stream(Message.stream,String)}.
     */
    public enum stream { stderr, stdout };

    /**
     * See
     * {@link.uri https://jupyter-client.readthedocs.io/en/latest/messaging.html#streams-stdout-stderr-etc stream}.
     */
    public Message stream(stream stream, String text) {
        var message = new Event(getCallingMethodName(1), this);

        message.content().put("name", stream.name());
        message.content().put("text", text);

        return message;
    }

    /**
     * See
     * {@link.uri https://jupyter-client.readthedocs.io/en/latest/messaging.html#code-inputs execute_input}.
     */
    public Message execute_input(String code, int execution_count) {
        var message = new Event(getCallingMethodName(1), this);

        message.content().put("code", code);
        message.content().put("execution_count", execution_count);

        return message;
    }

    /**
     * See
     * {@link.uri https://jupyter-client.readthedocs.io/en/latest/messaging.html#id6 execute_result}.
     */
    public Message execute_result(int execution_count, ObjectNode content) {
        var message = new Event(getCallingMethodName(1), this);

        message.content().put("execution_count", execution_count);
        message.content().setAll(content);

        return message;
    }

    /**
     * See
     * {@link.uri https://jupyter-client.readthedocs.io/en/latest/messaging.html#display-data display_data}.
     */
    public Message display_data(ObjectNode content) {
        var message = new Event(getCallingMethodName(1), this);

        message.content().setAll(content);

        return message;
    }

    /**
     * Parameter to {@link Message#status(Message.status)} and
     * {@link Message#status(Message.status,Message)}.
     */
    public enum status { starting, busy, idle };

    /**
     * See
     * {@link.uri https://jupyter-client.readthedocs.io/en/latest/messaging.html#kernel-status status}.
     */
    public static Message status(status status, Message request) {
        var message = new Event(getCallingMethodName(1), request);

        message.content().put("execution_state", status.name());

        return message;
    }

    /**
     * See
     * {@link.uri https://jupyter-client.readthedocs.io/en/latest/messaging.html#kernel-status status}.
     */
    public Message status(status status) { return status(status, this); }

    /**
     * Set the {@link date()} value if not already set.
     *
     * @return  {@link.this} {@link Message} for chaining.
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
     * @param   digester        The {@link HMACDigester} (may be
     *                          {@code null}).
     */
    public List<byte[]> serialize(HMACDigester digester) {
        var frames = envelope().stream().collect(toList());

        frames.add(DELIMITER);

        var header = serialize(header());
        var parentHeader = serialize(parentHeader());
        var metadata = serialize(metadata());
        var content = serialize(content());

        var digest = "";

        if (digester != null) {
            digest = digester.digest(header, parentHeader, metadata, content);
        }

        Collections.addAll(frames,
                           digest.getBytes(UTF_8),
                           header, parentHeader, metadata, content);

        frames.addAll(buffers());

        return frames;
    }

    private byte[] serialize(JsonNode node) {
        var string = "{}";

        try {
            string = OBJECT_MAPPER.writeValueAsString(node);
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
     * @param   digester        The {@link HMACDigester} (may be
     *                          {@code null}).
     */
    public static Message receive(ZMQ.Socket socket, byte[] frame, HMACDigester digester) {
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
            if (! digester.verify(new String(signature, UTF_8),
                                  header, parentHeader, metadata, content)) {
                throw new SecurityException("Invalid signature");
            }
        }

        var message = new Message();

        message.envelope().addAll(envelope);
        message.header().setAll(deserialize(header));
        message.parentHeader().setAll(deserialize(parentHeader));
        message.metadata().setAll(deserialize(metadata));
        message.content().setAll(deserialize(content));
        message.buffers().addAll(buffers);

        return message;
    }

    private static ObjectNode deserialize(byte[] bytes) {
        ObjectNode value = null;

        try {
            value = (ObjectNode) OBJECT_MAPPER.readTree(new String(bytes, UTF_8));
        } catch (Exception exception) {
            log.warn("{}", exception);
        }

        return value;
    }

    /**
     * Method to create a "standard" error node.  If both arguments are
     * {@code null} then an {@link ObjectNode} with {@code status = "ok"} is
     * created.
     *
     * @param   throwable       The {@link Throwable} source of the error.
     * @param   evalue          The expression value.
     *
     * @return  The corresponding {@link ObjectNode}.
     */
    public static ObjectNode content(Throwable throwable, String evalue) {
        var node = OBJECT_MAPPER.createObjectNode();

        if (throwable != null || evalue != null) {
            node.put("status", "error");
            node.put("ename", throwable.getClass().getCanonicalName());
            node.put("evalue", evalue);

            var array = node.putArray("traceback");

            if (throwable != null) {
                var buffer = new PrintStreamBuffer();

                throwable.printStackTrace(buffer);
                Stream.of(buffer.toString().split("\\R"))
                    .forEach(t -> array.add(t));
            }
        } else {
            node.put("status", "ok");
        }

        return node;
    }

    /**
     * Method to create {@link #execute_result(int,ObjectNode)}
     * {@link Message} {@code mime-bundle}.
     *
     * @param   object          The {@link Object} to encode.
     *
     * @return  The {@link Message} {@code mime-bundle}.
     */
    public static ObjectNode mime_bundle(Object object) {
        return Renderer.render(object);
    }

    private static String getCallingMethodName(int skip) {
        String name =
            WALKER.walk(t -> t.map(StackWalker.StackFrame::getMethodName)
                                  .skip(skip)
                                  .findFirst())
            .get();

        return name;
    }

    /**
     * Method to send a {@link Message}.
     *
     * @param   socket          The {@link ZMQ.Socket}.
     * @param   digester        The {@link HMACDigester} (may be
     *                          {@code null}).
     */
    public void send(ZMQ.Socket socket, HMACDigester digester) {
        var list = serialize(digester);
        var iterator = list.iterator();

        while (iterator.hasNext()) {
            socket.send(iterator.next(), iterator.hasNext() ? ZMQ.SNDMORE : 0);
        }
    }

    @Override
    public String toString() {
        var node = new ObjectNode(JsonNodeFactory.instance);

        node.set("header", header());
        node.set("parentHeader", parentHeader());
        node.set("metadata", metadata());
        node.set("content", content());

        return node.toPrettyString();
    }

    private static class Event extends Message {
        public Event(String msg_type, Message request) {
            super();

            if (msg_type != null) {
                msg_type(msg_type);
            }

            if (request != null) {
                parentHeader().setAll(request.header());
            }
        }
    }

    private static class Reply extends Event {
        public Reply(Message request) {
            super(request.getMessageTypeAction() + "_reply", request);

            if (request.isRequest()) {
                envelope().addAll(request.envelope());
                content().setAll(content(null, null));
                buffers().addAll(request.buffers());
            } else {
                throw new IllegalStateException("Source message is not a request");
            }
        }
    }
}
