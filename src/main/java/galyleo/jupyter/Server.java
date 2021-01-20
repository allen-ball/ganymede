package galyleo.jupyter;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.zeromq.ZMQ;

/**
 * Jupyter {@link Server} (base class for kernel implementations).
 *
 * {@bean.info}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@Getter @Log4j2
public abstract class Server extends ScheduledThreadPoolExecutor {
    private final ZMQ.Context context = ZMQ.context(8);
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Sole constructor.
     */
    protected Server() { super(16); }

    { getObjectMapper().enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_VALUES); }

    /**
     * Method to get {@link.this} {@link Server}'s session ID.
     *
     * @return  The session ID.
     */
    protected abstract String getSession();
}
