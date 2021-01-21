package galyleo.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;

/**
 * Jupyter {@link Connection}.  See
 * "{@link.uri https://jupyter-client.readthedocs.io/en/stable/kernels.html#connection-files target=newtab Connection files}".
 *
 * {@bean.info}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@Data @Log4j2
public class Connection {
    @NonNull private final Properties properties;
    @NonNull private final HMACDigester digester;

    /**
     * Sole constructor.
     *
     * @param   properties      The {@link Properties}.
     */
    public Connection(Properties properties) {
        this.properties = properties;
        this.digester = new HMACDigesterImpl();
    }

    /**
     * Method to get a socket address for {@link.this} {@link Connection} on
     * the specified port.
     *
     * @param   port            The port number.
     *
     * @return  The address (as a {@link String}).
     */
    public String address(int port) {
        String address =
            String.format("%s://%s:%d",
                          properties.getTransport(), properties.getIp(), port);

        return address;
    }

    /**
     * Method to connect a kernel's {@link Channel}s.
     *
     * @param   shell           The shell {@link Channel}.
     * @param   control         The control {@link Channel}.
     * @param   iopub           The iopub {@link Channel}.
     * @param   stdin           The stdin {@link Channel}.
     * @param   heartbeat       The heartbeat {@link Channel}.
     */
    public void connect(Channel.Jupyter shell, Channel.Jupyter control,
                        Channel.Jupyter iopub, Channel.Jupyter stdin,
                        Channel.Heartbeat heartbeat) {
        var properties = getProperties();
        var digester = getDigester();

        shell.connect(address(properties.getShellPort()), digester);
        control.connect(address(properties.getControlPort()), digester);
        iopub.connect(address(properties.getIopubPort()), digester);
        stdin.connect(address(properties.getStdinPort()), digester);
        heartbeat.connect(address(properties.getHeartbeatPort()));
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
        @JsonProperty("kernel_name")            private String kernelName = null;
        @JsonProperty("control_port")           private int controlPort = -1;
        @JsonProperty("shell_port")             private int shellPort = -1;
        @JsonProperty("transport")              private String transport = null;
        @JsonProperty("signature_scheme")       private String signatureScheme = null;
        @JsonProperty("stdin_port")             private int stdinPort = -1;
        @JsonProperty("hb_port")                private int heartbeatPort = -1;
        @JsonProperty("ip")                     private String ip = null;
        @JsonProperty("iopub_port")             private int iopubPort = -1;
        @JsonProperty("key")                    private String key = null;
    }
}
