package galyleo;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    private List<byte[]> identities = new ArrayList<>();
    private Header header = null;
    private Header parentHeader = null;
    private Map<String,Object> metadata = new LinkedHashMap<>();
    private Map<String,Object> content = new LinkedHashMap<>();
    private List<byte[]> buffers = new ArrayList<>();

    /**
     * See
     * "{@link.uri https://jupyter-client.readthedocs.io/en/latest/messaging.html#message-header target=newtab Message Header}."
     */
    @Data
    public static class Header {
        @JsonProperty("msg_id")         private String msgId = null;
        @JsonProperty("msg_type")       private String msgType = null;
        @JsonProperty("session")        private String session = null;
        @JsonProperty("username")       private String username = null;
        @JsonProperty("date")           private String date = null;
        @JsonProperty("version")        private String version = null;
    }
}
