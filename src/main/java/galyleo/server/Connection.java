package galyleo.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    @NonNull private final JsonNode node;
    @NonNull private final HMACDigester digester;

    /**
     * Sole constructor.
     *
     * @param   node            The {@link JsonNode} describing the
     *                          {@link Connection}.
     */
    public Connection(JsonNode node) {
        this.node = node;
        this.digester = new HMACDigesterImpl();
    }

    /**
     * Method to connect a kernel's {@link Channel}s.
     *
     * @param   shell           The {@link Channel.Shell Shell}
     *                          {@link Channel}.
     * @param   control         The {@link Channel.Control Control}
     *                          {@link Channel}.
     * @param   iopub           The {@link Channel.IOPub IOPub}
     *                          {@link Channel}.
     * @param   stdin           The {@link Channel.Stdin Stdin}
     *                          {@link Channel}.
     * @param   heartbeat       The {@link Channel.Heartbeat Heartbeat}
     *                          {@link Channel}.
     */
    public void connect(Channel.Shell shell, Channel.Control control,
                        Channel.IOPub iopub, Channel.Stdin stdin,
                        Channel.Heartbeat heartbeat) {
        var digester = getDigester();

        shell.connect(getAddress("shell_port"), digester);
        control.connect(getAddress("control_port"), digester);
        iopub.connect(getAddress("iopub_port"), digester);
        stdin.connect(getAddress("stdin_port"), digester);
        heartbeat.connect(getAddress("hb_port"));
    }

    private String getAddress(String portName) {
        String address =
            String.format("%s://%s:%d",
                          node.at("/transport").asText(),
                          node.at("/ip").asText(),
                          node.at("/" + portName).asInt());

        return address;
    }

    private class HMACDigesterImpl extends HMACDigester {
        public HMACDigesterImpl() {
            super(node.at("/signature_scheme").asText(),
                  node.at("/key").asText());
        }
    }
}
