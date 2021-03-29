package ganymede.kernel;
/*-
 * ##########################################################################
 * Ganymede
 * $Id$
 * $HeadURL$
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
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

import static ganymede.server.Server.OBJECT_MAPPER;
import static java.util.stream.Collectors.toList;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Ganymede {@link Kernel} REST client.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@NoArgsConstructor @ToString @Log4j2
public class KernelRestClient {
    private static final String TEMPLATE = "http://localhost:%d/";

    private final int port = Integer.decode(System.getProperty(Kernel.PORT_PROPERTY));
    private final URI uri = URI.create(String.format(TEMPLATE, port));
    private final HttpClient client = HttpClient.newHttpClient();

    /**
     * See {@link Kernel#classpath()}.
     */
    public List<String> classpath() throws Exception {
        var request =
            HttpRequest.newBuilder()
            .uri(uri.resolve("kernel/classpath"))
            .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            .GET()
            .build();
        var response =
            client.send(request, HttpResponse.BodyHandlers.ofString());
        var list =
            asStream(OBJECT_MAPPER.readTree(response.body()))
            .map(JsonNode::asText)
            .collect(toList());

        return list;
    }

    private <T> Stream<T> asStream(Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    /**
     * See {@link Kernel#print(ObjectNode)}.
     *
     * @param   bundle          The MIME bundle {@link JsonNode}.
     */
    public void print(JsonNode bundle) throws Exception {
        var body = bundle.toPrettyString();
        var request =
            HttpRequest.newBuilder()
            .uri(uri.resolve("kernel/print"))
            .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            .PUT(HttpRequest.BodyPublishers.ofString(body))
            .build();
        var response =
            client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
