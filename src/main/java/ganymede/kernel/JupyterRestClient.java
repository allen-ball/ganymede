package ganymede.kernel;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

import static ganymede.server.Server.OBJECT_MAPPER;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Ganymede Jupyter Notebook/Lab REST client.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@Log4j2
public class JupyterRestClient {
    private final JsonNode json;
    private final URI uri;
    private final String token;
    private final HttpClient client = HttpClient.newHttpClient();

    /**
     * Sole constructor.
     *
     * @param   json            The {@link JsonNode} describing the server
     *                          parameters.
     */
    public JupyterRestClient(JsonNode json) {
        this.json = json;
        this.uri = URI.create(json.get("url").asText());
        this.token = json.get("token").asText();
    }

    /**
     * {@code GET /api/status}
     */
    public JsonNode getStatus() throws Exception {
        var request = builder().uri(uri.resolve("api/status")).GET().build();
        var response =
            client.send(request, HttpResponse.BodyHandlers.ofString());

        return OBJECT_MAPPER.readTree(response.body());
    }

    /**
     * {@code GET /api/kernels}
     */
    public JsonNode getKernels() throws Exception {
        var request =
            builder()
            .uri(uri.resolve("api/kernels"))
            .GET()
            .build();
        var response =
            client.send(request, HttpResponse.BodyHandlers.ofString());

        return OBJECT_MAPPER.readTree(response.body());
    }

    /**
     * {@code GET /api/kernels/(id)}
     */
    public JsonNode getKernel(String id) throws Exception {
        var request =
            builder()
            .uri(uri.resolve("api/kernels/").resolve(id))
            .GET()
            .build();
        var response =
            client.send(request, HttpResponse.BodyHandlers.ofString());

        return OBJECT_MAPPER.readTree(response.body());
    }

    /**
     * {@code GET /api/sessions}
     */
    public JsonNode getSessions() throws Exception {
        var request =
            builder()
            .uri(uri.resolve("api/sessions"))
            .GET()
            .build();
        var response =
            client.send(request, HttpResponse.BodyHandlers.ofString());

        return OBJECT_MAPPER.readTree(response.body());
    }

    /**
     * {@code GET /api/sessions/(id)}
     */
    public JsonNode getSession(String id) throws Exception {
        var request =
            builder()
            .uri(uri.resolve("api/sessions/").resolve(id))
            .GET()
            .build();
        var response =
            client.send(request, HttpResponse.BodyHandlers.ofString());

        return OBJECT_MAPPER.readTree(response.body());
    }

    /**
     * {@code GET /api/terminals}
     */
    public JsonNode getTerminals() throws Exception {
        var request =
            builder()
            .uri(uri.resolve("api/terminals"))
            .GET()
            .build();
        var response =
            client.send(request, HttpResponse.BodyHandlers.ofString());

        return OBJECT_MAPPER.readTree(response.body());
    }

    /**
     * {@code GET /api/terminals/(id)}
     */
    public JsonNode getTerminal(String id) throws Exception {
        var request =
            builder()
            .uri(uri.resolve("api/terminals/").resolve(id))
            .GET()
            .build();
        var response =
            client.send(request, HttpResponse.BodyHandlers.ofString());

        return OBJECT_MAPPER.readTree(response.body());
    }

    private HttpRequest.Builder builder() {
        var builder =
            HttpRequest.newBuilder()
            .header("Authorization", "Token " + token);

        return builder;
    }

    @Override
    public String toString() { return json.toPrettyString(); }
}
