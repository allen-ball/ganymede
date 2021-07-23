package ganymede.server;
/*-
 * ##########################################################################
 * Ganymede
 * %%
 * Copyright (C) 2021 Allen D. Ball
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ##########################################################################
 */
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ganymede.io.PrintStreamBuffer;
import ganymede.notebook.Magic;
import ganymede.notebook.Renderer;
import ganymede.util.ObjectMappers;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.log4j.Log4j2;
import org.zeromq.ZMQ;
import org.zeromq.util.ZData;

import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.stream.Collectors.joining;
import static lombok.AccessLevel.PRIVATE;

/**
 * Jupyter {@link Message}.  See
 * "{@link.uri https://jupyter-client.readthedocs.io/en/latest/messaging.html#general-message-format target=newtab General Message Format}."
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@RequiredArgsConstructor(access = PRIVATE) @Data @Accessors(fluent = true) @Log4j2
public class Message {
    private static final String DELIMITER_STRING = "<IDS|MSG>";
    private static final byte[] DELIMITER_BYTES = DELIMITER_STRING.getBytes(ZMQ.CHARSET);
    private static final ZData DELIMITER_ZDATA = new ZData(DELIMITER_BYTES);

    private static final StackWalker WALKER = StackWalker.getInstance();

    /* private enum Status { ok, error } */

    private final Connection connection;
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
     * Method to set status for a code {@link Magic.completeness} test.
     *
     * @param   completeness    The {@link Magic.completeness}.
     *
     * @return  {@link.this} {@link Message} for chaining.
     */
    public Message status(Magic.completeness completeness) {
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
        var message = new Pub(getCallingMethodName(1), this);

        message.content().put("name", stream.name());
        message.content().put("text", text);

        return message;
    }

    /**
     * See
     * {@link.uri https://jupyter-client.readthedocs.io/en/latest/messaging.html#code-inputs execute_input}.
     */
    public Message execute_input(String code, int execution_count) {
        var message = new Pub(getCallingMethodName(1), this);

        message.content().put("code", code);
        message.content().put("execution_count", execution_count);

        return message;
    }

    /**
     * See
     * {@link.uri https://jupyter-client.readthedocs.io/en/latest/messaging.html#id6 execute_result}.
     */
    public Message execute_result(int execution_count, ObjectNode content) {
        var message = new Pub(getCallingMethodName(1), this);

        message.content().put("execution_count", execution_count);
        message.content().setAll(content);

        return message;
    }

    /**
     * See
     * {@link.uri https://jupyter-client.readthedocs.io/en/latest/messaging.html#id6 execute_result}.
     */
    public Message execute_result(int execution_count, String stdout) {
        return execute_result(execution_count, mime_bundle(stdout));
    }

    /**
     * See
     * {@link.uri https://jupyter-client.readthedocs.io/en/latest/messaging.html#display-data display_data}.
     */
    public Message display_data(ObjectNode content) {
        var message = new Pub(getCallingMethodName(1), this);

        message.content().setAll(content);
        message.content().with("transient");

        return message;
    }

    /**
     * See
     * {@link.uri https://jupyter-client.readthedocs.io/en/latest/messaging.html#display-data display_data}.
     */
    public Message display_data(String stdout) {
        return display_data(mime_bundle(stdout));
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
        var message = new Pub(getCallingMethodName(1), request);

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
            date(now(UTC).truncatedTo(MILLIS).format(ISO_INSTANT));
        }

        return this;
    }

    /**
     * Method to copy a {@link Message}.
     *
     * @return  A copy of {@link.this} {@link Message}.
     */
    public Message copy() { return new Copy(this); }

    /**
     * Method to serialize a {@link Message}.
     *
     * @param   digester        The {@link HMACDigester} (may be
     *                          {@code null}).
     *
     * @return  The {@link List} of serialized frames.
     */
    public List<byte[]> serialize(HMACDigester digester) {
        var frames = new LinkedList<byte[]>();

        frames.addAll(envelope());
        frames.add(DELIMITER_BYTES);

        var header = serialize(header());
        var parentHeader = serialize(parentHeader());
        var metadata = serialize(metadata());
        var content = serialize(content());

        var digest = "";

        if (digester != null) {
            digest = digester.digest(header, parentHeader, metadata, content);
        }

        Collections.addAll(frames,
                           digest.getBytes(ZMQ.CHARSET),
                           header, parentHeader, metadata, content);

        frames.addAll(buffers());

        return frames;
    }

    private byte[] serialize(JsonNode node) {
        var string = "{}";

        try {
            string = ObjectMappers.JSON.writeValueAsString(node);
        } catch (Exception exception) {
            log.warn("{}", exception);
        }

        return string.getBytes(ZMQ.CHARSET);
    }

    /**
     * Method to receive a {@link Message} on a {@link ZMQ.Socket}.
     *
     * param    connection      The {@link Connection}.
     * @param   socket          The {@link ZMQ.Socket}.
     * @param   frame           The first message frame.
     */
    public static Message receive(Connection connection, ZMQ.Socket socket, byte[] frame) {
        var envelope = new LinkedList<byte[]>();

        while (! DELIMITER_ZDATA.equals(frame)) {
            envelope.add(frame);

            frame = recv(socket);
        }

        var signature = recv(socket);
        var header = recv(socket);
        var parentHeader = recv(socket);
        var metadata = recv(socket);
        var content = recv(socket);
        var buffers = new LinkedList<byte[]>();

        while (socket.hasReceiveMore()) {
            buffers.add(recv(socket));
        }

        var digester = connection.getDigester();

        if (digester != null) {
            if (! digester.verify(new String(signature, ZMQ.CHARSET),
                                  header, parentHeader, metadata, content)) {
                throw new SecurityException("Invalid signature");
            }
        }

        var message = new Message(connection);

        message.envelope().addAll(envelope);
        message.header().setAll(deserialize(header));
        message.parentHeader().setAll(deserialize(parentHeader));
        message.metadata().setAll(deserialize(metadata));
        message.content().setAll(deserialize(content));
        message.buffers().addAll(buffers);

        return message;
    }

    private static byte[] recv(ZMQ.Socket socket) {
        return Objects.requireNonNull(socket.recv(ZMQ.DONTWAIT));
    }

    private static ObjectNode deserialize(byte[] bytes) {
        ObjectNode value = null;

        try {
            value = (ObjectNode) ObjectMappers.JSON.readTree(new String(bytes, ZMQ.CHARSET));
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
        var node = new ObjectNode(JsonNodeFactory.instance);

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
     * @param   alternates      Optional alternate representations.
     *
     * @return  The {@link Message} {@code mime-bundle}.
     */
    public static ObjectNode mime_bundle(Object object, Object... alternates) {
        return Renderer.MAP.render(object, alternates);
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
     * param    connection      The {@link Connection}.
     * @param   socket          The {@link ZMQ.Socket}.
     */
    public void send(Connection connection, ZMQ.Socket socket) {
        var list = serialize(connection.getDigester());
        var iterator = list.iterator();

        while (iterator.hasNext()) {
            var frame = iterator.next();

            socket.send(frame, iterator.hasNext() ? ZMQ.SNDMORE : 0);
        }
    }

    /**
     * Convenience method to return {@link #header}, {@link #parentHeader},
     * {@link metadata}, and {@link #content} wrapped in an
     * {@link ObjectNode}.
     *
     * @return The {@link ObjectNode}.
     */
    public ObjectNode asObjectNode() {
        var node = new ObjectNode(JsonNodeFactory.instance);

        node.set("header", header);
        node.set("parentHeader", parentHeader);
        node.set("metadata", metadata);
        node.set("content", content);

        return node;
    }

    @Override
    public String toString() {
        var string =
            Stream.of(envelope().stream().map(ZData::new),
                      Stream.of(DELIMITER_STRING, "DIGEST"),
                      Stream.of(header(), parentHeader(), metadata(), content())
                      .map(t -> t.toPrettyString()),
                      buffers().stream().map(ZData::new))
            .flatMap(t -> t)
            .map(Object::toString)
            .collect(joining("\n"));

        return string;
    }

    private static class Copy extends Message {
        public Copy(Message message) {
            super(null);

            envelope().addAll(message.envelope());
            header().setAll(message.header());
            parentHeader().setAll(message.parentHeader());
            metadata().setAll(message.metadata());
            content().setAll(message.content());
            buffers().addAll(message.buffers());
        }
    }

    private static abstract class Child extends Message {
        protected Child(String msg_type, Message request) {
            super(null);

            if (msg_type != null) {
                msg_type(msg_type);
            }

            if (request != null) {
                parentHeader().setAll(request.header());
            }
        }
    }

    private static class Reply extends Child {
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

    private static class Pub extends Child {
        public Pub(String msg_type, Message request) {
            super(msg_type, request);

            envelope().clear();

            var topic = msg_type;

            if (request != null && request.connection != null) {
                var kernelId = request.connection.getKernelId();

                if (kernelId != null) {
                    topic = String.format("kernel.%s.%s", kernelId, msg_type);
                }
            }

            if (topic != null) {
                envelope().add(topic.getBytes(ZMQ.CHARSET));
            }
        }
    }
}
