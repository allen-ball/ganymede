package galyleo.server.renderer;

import ball.annotation.ServiceProviderFor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import galyleo.server.Renderer;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * {@link JsonNode} {@link Renderer} service provider.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@ServiceProviderFor({ Renderer.class })
@MimeType("application/json") @ForType(JsonNode.class)
@NoArgsConstructor @ToString
public class JsonNodeRenderer implements AnnotatedRenderer {
    @Override
    public void renderTo(ObjectNode bundle, Object object) {
        if (! bundle.with(DATA).has(getMimeType())) {
            bundle.with(DATA).set(getMimeType(), (JsonNode) object);
            bundle.with(METADATA).with(getMimeType())
                .put("expanded", true);
        }
    }
}
