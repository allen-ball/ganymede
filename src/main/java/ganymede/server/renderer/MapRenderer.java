package ganymede.server.renderer;

import ball.annotation.ServiceProviderFor;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ganymede.server.Renderer;
import java.util.Map;
import lombok.NoArgsConstructor;
import lombok.ToString;

import static ganymede.server.Server.OBJECT_MAPPER;

/**
 * {@link Map} {@link Renderer} service provider.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@ServiceProviderFor({ Renderer.class })
@ForType(Map.class)
@NoArgsConstructor @ToString
public class MapRenderer extends JsonNodeRenderer {
    @Override
    public void renderTo(ObjectNode bundle, Object object) {
        super.renderTo(bundle, OBJECT_MAPPER.valueToTree(object));
    }
}
