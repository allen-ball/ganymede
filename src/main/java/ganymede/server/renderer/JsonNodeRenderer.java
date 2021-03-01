package ganymede.server.renderer;

import ball.annotation.ServiceProviderFor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ganymede.server.Renderer;
import lombok.NoArgsConstructor;
import lombok.ToString;

import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;

/**
 * {@link JsonNode} {@link Renderer} service provider.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@ServiceProviderFor({ Renderer.class })
@ForType(JsonNode.class)
@NoArgsConstructor @ToString
public class JsonNodeRenderer implements AnnotatedRenderer {
    @Override
    public void renderTo(ObjectNode bundle, Object object) {
        if (! bundle.with(DATA).has(APPLICATION_JSON_VALUE)) {
            bundle.with(DATA)
                .set(APPLICATION_JSON_VALUE, (JsonNode) object);
            bundle.with(METADATA).with(APPLICATION_JSON_VALUE)
                .put("expanded", true);
        }
    }
}
