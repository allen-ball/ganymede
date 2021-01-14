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
     *
     * @return  The previous value (if any).
     */
    public Object setMetadata(String key, Object value) {
        return getMetadata().put(key, value);
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
     *
     * @return  The previous value (if any).
     */
    public Object setContent(String key, Object value) {
        return getContent().put(key, value);
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

        reply.getIdentities().addAll(getIdentities());
        reply.getHeader().setMessageId(UUID.randomUUID().toString());
        reply.getHeader().setMessageType(type);
        reply.getHeader().setSession(getHeader().getSession());
        reply.getHeader().setUsername(getHeader().getUsername());
        reply.getHeader().setVersion(VERSION);
        reply.setParentHeader(getHeader());
        reply.getBuffers().addAll(getBuffers());

        return reply;
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
