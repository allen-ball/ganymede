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
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import static lombok.AccessLevel.PRIVATE;

/**
 * Jupyter {@link Connection}.  See
 * "{@link.uri https://jupyter-client.readthedocs.io/en/stable/kernels.html#connection-files target=newtab Connection files}".
 *
 * {@bean.info}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@RequiredArgsConstructor(access = PRIVATE) @Data @Log4j2
public class Connection {

    /**
     * {@link Connection} file name {@link Pattern}.
     * Provides named group "kernelId".
     */
    @CompileTimeCheck
    public static final Pattern FILE_NAME_PATTERN =
        Pattern.compile("(?i)^(kernel-|)(?<kernelId>[^.]+)[.]json$");

    /**
     * Static factory method.
     *
     * @param   file            The {@link File} describing the
     *                          {@link Connection}.
     *
     * @return  The parsed {@link Connection}.
     */
    public static Connection parse(File file) throws IOException {
        var kernelId = UUID.randomUUID();
        var matcher = FILE_NAME_PATTERN.matcher(file.getName());

        if (matcher.matches()) {
            try {
                kernelId = UUID.fromString(matcher.group("kernelId"));
            } catch (Exception exception) {
                log.warn("{}", exception.getMessage());
            }
        }

        var node = (ObjectNode) ObjectMappers.JSON.readTree(file);
        var digester =
            new HMACDigester(node.at("/signature_scheme").asText(),
                             node.at("/key").asText());

        return new Connection(kernelId, node, digester);
    }

    @NonNull private final UUID kernelId;
    @NonNull private final ObjectNode node;
    @NonNull private final HMACDigester digester;

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
}
