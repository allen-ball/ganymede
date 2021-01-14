package galyleo.jupyter;

import com.fasterxml.jackson.annotation.JsonProperty;
import galyleo.PrintStreamBuffer;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Data;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;

/**
 * Jupyter {@link Message}.  See
 * "{@link.uri https://jupyter-client.readthedocs.io/en/latest/messaging.html#general-message-format target=newtab General Message Format}."
 *
 * {@bean.info}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@Data
public class Message {
    private static final String DELIMITER_STRING = "<IDS|MSG>";
    public static final byte[] DELIMITER = DELIMITER_STRING.getBytes(US_ASCII);

    private static final String VERSION = "5.3";

    private List<byte[]> identities = new LinkedList<>();
    private Header header = null;
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
     * {@link Message}.
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
     * {@link Message}.
     *
     * @param   type            The reply {@link Message} type.
     *
     * @return  The reply {@link Message}.
     */
    public Message reply(String type) {
        var reply = new Message();

        /* reply.getIdentities().addAll(getIdentities()); */
        reply.setMessageType(type);
        reply.getHeader().setMessageId(UUID.randomUUID().toString());
        reply.getHeader().setSession(getHeader().getSession());
        reply.getHeader().setUsername(getHeader().getUsername());
        reply.getHeader().setVersion(VERSION);
        reply.setParentHeader(getHeader());
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
     * Jupyter {@link Message} types.
     */
    public enum Type {
        /*
         * Shell
         */
        execute_request, execute_reply,
        inspect_request, inspect_reply,
        complete_request, complete_reply,
        history_request, history_reply,
        is_complete_request, is_complete_reply,
        comm_info_request, comm_info_reply,
        kernel_info_request, kernel_info_reply,
        comm_open, comm_msg, comm_close,
        /*
         * Control
         */
        shutdown_request, shutdown_reply,
        interrupt_request, interrupt_reply,
        debug_request, debug_reply,
        /*
         * IOPub
         */
        stream,
        display_data,
        update_display_data,
        /* execute_request, execute_reply */
        error,
        status,
        clear_output,
        debug_event,
        /*
         * Stdin
         */
        input_request, input_reply;
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
