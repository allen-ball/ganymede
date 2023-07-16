package ganymede.jupyterlab;
/*-
 * ##########################################################################
 * Ganymede
 * %%
 * Copyright (C) 2023 Allen D. Ball
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
import com.fasterxml.jackson.databind.JsonNode;
import ganymede.jupyterlab.client.ApiClient;
import ganymede.jupyterlab.client.ApiException;
import ganymede.jupyterlab.client.JSON;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

/**
 * Ganymede Jupyter Lab REST client.  See
 * {@link.uri https://github.com/jupyter/jupyter/wiki/Jupyter-Notebook-Server-API target=newtab Jupyter Notebook Server API}.
 *
 * {@bean.info}
 *
 * @see ApiClient
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@ToString @Log4j2
public class JupyterLabServicesClient extends ApiClient {
    private static final String JUPYTER_RUNTIME_DIR = "JUPYTER_RUNTIME_DIR";
    private static final String JPY_PARENT_PID = "JPY_PARENT_PID";
    private static final String GLOB_FORMAT = "{nb,jp}server-%d.json";

    private static File getFileFromEnv() throws IOException {
        var directory = Paths.get(System.getenv().get("JUPYTER_RUNTIME_DIR"));
        var pid = Long.parseLong(System.getenv().get("JPY_PARENT_PID"));
        var glob = String.format(GLOB_FORMAT, pid);

        try (var stream = Files.newDirectoryStream(directory, glob)) {
            var path = stream.iterator().next();

            return path.toFile();
        } catch (NoSuchElementException exception) {
            throw new FileNotFoundException(glob);
        }
    }

    /**
     * No-argument constructor.  {@link File} is specified by the
     * {@code JUPYTER_RUNTIME_DIR} and {@code JPY_PARENT_PID} environment
     * variables.
     *
     * @throws  IOException     If the {@link File} cannot be opened or
     *                          parsed.
     */
    public JupyterLabServicesClient() throws IOException {
        this(getFileFromEnv());
    }

    /**
     * {@link File} constructor.
     *
     * @param   file            The {@link File} containing the server
     *                          parameters.
     *
     * @throws  IOException     If the {@link File} cannot be opened or
     *                          parsed.
     */
    public JupyterLabServicesClient(File file) throws IOException {
        this(JSON.getDefault().getMapper().readTree(file));
    }

    /**
     * {@link JsonNode} constructor.
     *
     * @param   node            The {@link JsonNode} describing the server
     *                          parameters.
     */
    public JupyterLabServicesClient(JsonNode node) {
        super();

        if (node.has("url")) {
            var url = URI.create(node.get("url").asText());

            setScheme(url.getScheme());
            setHost(url.getHost());
            setPort(url.getPort());
        }

        if (node.has("token")) {
            setRequestInterceptor(t -> t.header("Authorization", "Token " + node.get("token").asText()));
        }
    }
}
