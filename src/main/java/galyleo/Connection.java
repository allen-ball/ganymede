package galyleo;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.zeromq.SocketType;
import org.zeromq.ZMQ;

/**
 * Jupyter {@link Connection}.
 *
 * {@bean.info}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@Data @Log4j2
public class Connection {
    private final ZMQ.Context context;
    private final Properties properties;
    private final HMACDigester digester;
    private final ZMQ.Socket control;
    private final ZMQ.Socket shell;
    private final ZMQ.Socket stdin;
    private final ZMQ.Socket heartbeat;
    private final ZMQ.Socket iopub;

    /**
     * Sole constructor.
     *
     * @param   context         The {@link ZMQ.Context}.
     * @param   properties      The {@link Properties}.
     */
    protected Connection(ZMQ.Context context, Properties properties) {
        this.context = Objects.requireNonNull(context);
        this.properties = Objects.requireNonNull(properties);

        digester = new HMACDigesterImpl();

        control = socket(SocketType.ROUTER, properties.getControlPort());
        shell = socket(SocketType.ROUTER, properties.getShellPort());
        stdin = socket(SocketType.ROUTER, properties.getStdinPort());
        heartbeat = socket(SocketType.REP, properties.getHeartbeatPort());
        iopub = socket(SocketType.PUB, properties.getIopubPort());
    }

    private ZMQ.Socket socket(SocketType type, int port) {
        var socket = context.socket(type);

        socket.connect(String.format("%s://%s:%d",
                                     properties.getTransport(),
                                     properties.getIp(), port));

        return socket;
    }

    private class HMACDigesterImpl extends HMACDigester {
        public HMACDigesterImpl() {
            super(properties.getSignatureScheme(), properties.getKey());
        }
    }

    /**
     * See
     * "{@link.uri https://jupyter-client.readthedocs.io/en/stable/kernels.html#connection-files target=newtab Connection files}".
     *
     * {@bean.info}
     */
    @Data
    public static class Properties {
        @JsonProperty("control_port")       private int controlPort = -1;
        @JsonProperty("shell_port")         private int shellPort = -1;
        @JsonProperty("transport")          private String transport = null;
        @JsonProperty("signature_scheme")   private String signatureScheme = null;
        @JsonProperty("stdin_port")         private int stdinPort = -1;
        @JsonProperty("hb_port")            private int heartbeatPort = -1;
        @JsonProperty("ip")                 private String ip = null;
        @JsonProperty("iopub_port")         private int iopubPort = -1;
        @JsonProperty("key")                private String key = null;
    }
}
