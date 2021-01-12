package galyleo;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import javax.inject.Named;
import javax.inject.Singleton;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.zeromq.ZMQ;

/**
 * Galyleo Jupyter {@link Kernel}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@Named @Singleton
@ToString @Log4j2
public class Kernel extends ScheduledThreadPoolExecutor {
    private final ZMQ.Context context = ZMQ.context(1);
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Sole constructor.
     */
    public Kernel() { super(8); }

    /**
     * Add a connection specified by a {@link Connection} {@link File}.
     *
     * @param   path            The path to the {@link Connection}
     *                          {@link File}.
     *
     * @throws  IOException     If the {@link File} cannot be opened or
     *                          parsed.
     */
    public void listen(String path) throws IOException {
        Connection.Properties properties =
            mapper.readValue(new File(path), Connection.Properties.class);
        var connection = new Connection(properties, mapper);

        log.info("{}", mapper.writeValueAsString(properties));
    }
}
