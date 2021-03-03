package ganymede.kernel;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Ganymede {@link Kernel} REST client.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@NoArgsConstructor @ToString @Log4j2
public class Client {
    private static final String TEMPLATE = "http://localhost:%d/";

    private final int port = Integer.decode(System.getProperty(Kernel.PORT_PROPERTY));
    private final URI uri = URI.create(String.format(TEMPLATE, port));

    /**
     * See {@link Kernel#print(ObjectNode)}.
     *
     * @param   bundle          The MIME bundle {@link JsonNode}.
     */
    public void print(JsonNode bundle) throws Exception {
        var client = HttpClient.newHttpClient();
        var body = bundle.toPrettyString();
        var request =
            HttpRequest.newBuilder()
            .uri(uri.resolve("kernel/print"))
            .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
        var response =
            client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
