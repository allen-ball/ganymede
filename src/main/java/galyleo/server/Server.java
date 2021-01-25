package galyleo.server;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.util.concurrent.ScheduledThreadPoolExecutor;
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
    private final ZMQ.Context context = ZMQ.context(8);
    private final ObjectMapper objectMapper =
        new ObjectMapper()
        .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_VALUES)
        .enable(SerializationFeature.INDENT_OUTPUT);
    private String session = null;

    /**
     * Sole constructor.
     */
    protected Server() { super(16); }
}
