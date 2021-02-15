package galyleo.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import java.util.stream.Stream;
import lombok.NoArgsConstructor;
import lombok.ToString;

import static galyleo.server.Server.OBJECT_MAPPER;
import static lombok.AccessLevel.PRIVATE;

/**
 * {@link Message#mime_bundle(Object)} output {@link Renderer}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@NoArgsConstructor(access = PRIVATE)
public abstract class Renderer {
    private static final String DATA = "data";
    private static final String METADATA = "metadata";

    /**
     * Method to render an {@link Object} to
     * {@link Message#execute_result(int,ObjectNode)}.
     *
     * @param   object          The {@link Object} to encode.
     *
     * @return  The {@link Message} {@code mime-bundle}.
     */
    public static ObjectNode render(Object object) {
        ObjectNode bundle = OBJECT_MAPPER.createObjectNode();

        if (object instanceof Map) {
            render(OBJECT_MAPPER.valueToTree(object), bundle);
        }

        if (object instanceof JsonNode) {
            render((JsonNode) object, bundle);
        }

        if (object instanceof String) {
            render((String) object, bundle);
        }

        render(String.valueOf(object), bundle);

        return bundle;
    }

    private static void render(JsonNode node, ObjectNode bundle) {
        var type = "application/json";

        if (! bundle.with(DATA).has(type)) {
            bundle.with(DATA).set(type, node);
            bundle.with(METADATA).with(type)
                .put("expanded", true);
        }
    }

    private static void render(String string, ObjectNode bundle) {
        var type = "text/plain";

        if (! bundle.with(DATA).has(type)) {
            bundle.with(DATA).put(type, string);
        }
    }
}
