package galyleo.server;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import galyleo.io.PrintStreamBuffer;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.zeromq.ZMQ;

import static lombok.AccessLevel.PROTECTED;

/**
 * Jupyter {@link Server} (base class for kernel implementations).
 *
 * {@bean.info}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@Getter(PROTECTED) @Setter(PROTECTED) @Log4j2
public abstract class Server extends ScheduledThreadPoolExecutor {

    /**
     * The
     * {@link.uri https://jupyter-client.readthedocs.io/en/latest/messaging.html#versioning target=newtab Jupyter message specification version}.
     */
    protected static final String PROTOCOL_VERSION = "5.3";

    /**
     * Common {@link Server} static {@link ObjectMapper} instance.
     */
    public static final ObjectMapper OBJECT_MAPPER =
        new ObjectMapper()
        .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_VALUES)
        .enable(SerializationFeature.INDENT_OUTPUT);

    private final ZMQ.Context context = ZMQ.context(8);
    private String session = null;

    /**
     * Sole constructor.
     */
    protected Server() { super(16); }

    /**
     * Method to stamp and outgoing {@link Message}.  Adds
     * {@link #PROTOCOL_VERSION}, session, and
     * {@link Message#timestamp() timestamp} if not already specified.
     *
     * @param   message         The {@link Message} to stamp.
     *
     * @return  The {@link Message}.
     */
    public Message stamp(Message message) {
        if (message.version() == null) {
            message.version(PROTOCOL_VERSION);
        }

        if (message.session() == null) {
            message.session(getSession());
        }

        return message.timestamp();
    }
}
