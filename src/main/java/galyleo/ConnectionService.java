package galyleo;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.zeromq.ZMQ;

/**
 * {@link Connection} {@link Service}.
 *
 * {@injected.fields}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@Service
@NoArgsConstructor @ToString @Log4j2
public class ConnectionService {
    private final ZMQ.Context context = ZMQ.context(1);
    @Autowired
    private ObjectMapper mapper = null;

    /**
     * Create a new connection from a {@link Connection} {@link File}.
     *
     * @param   path            The path to the {@link Connection}
     *                          {@link File}.
     *
     * @return  A {@link Connection}.
     */
    public Connection newConnection(String path) throws IOException {
        var properties =
            mapper.readValue(new File(path), Connection.Properties.class);
        var connection = new Connection(context, properties);

        log.info("{}", mapper.writeValueAsString(properties));

        return connection;
    }
}
