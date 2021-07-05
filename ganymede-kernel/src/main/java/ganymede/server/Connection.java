package ganymede.server;
/*-
 * ##########################################################################
 * Ganymede
 * %%
 * Copyright (C) 2021 Allen D. Ball
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ##########################################################################
 */
import ball.annotation.CompileTimeCheck;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ganymede.util.ObjectMappers;
import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;
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
 */
@Data @Log4j2
public class Connection {

    /**
     * {@link Connection} file name {@link Pattern}.
     * Provides named group "kernelId".
     */
    @CompileTimeCheck
    public static final Pattern FILE_NAME_PATTERN =
        Pattern.compile("(?i)^(kernel-|)(?<kernelId>[^.]+)[.]json$");

    @NonNull private final String kernelId;
    @NonNull private final ObjectNode node;
    @NonNull private final HMACDigester digester;

    /**
     * {@link File} constructor.
     *
     * @param   id              The kernel ID.
     * @param   file            The {@link File} describing the
     *                          {@link Connection}.
     */
    public Connection(String id, File file) throws IOException {
        this(id, (ObjectNode) ObjectMappers.JSON.readTree(file));
    }

    /**
     * {@link ObjectNode} constructor.
     *
     * @param   kernelId        The kernel ID.
     * @param   node            The {@link ObjectNode} describing the
     *                          {@link Connection}.
     */
    public Connection(String kernelId, ObjectNode node) {
        this.kernelId = kernelId;
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

        shell.connect(this, getAddress("shell_port"));
        control.connect(this, getAddress("control_port"));
        iopub.connect(this, getAddress("iopub_port"));
        stdin.connect(this, getAddress("stdin_port"));
        heartbeat.connect(this, getAddress("hb_port"));
    }

    /**
     * Method to get the address corresponding to port name (JSON field).
     *
     * @param   portName        The JSON field specifying the desired port.
     *
     * @return  The address (as a {@link String}).
     */
    public String getAddress(String portName) {
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
