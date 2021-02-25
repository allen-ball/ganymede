package ganymede.server.renderer;

import ball.annotation.ServiceProviderFor;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ganymede.server.Renderer;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * {@link String} {@link Renderer} service provider.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@ServiceProviderFor({ Renderer.class })
@ForType(String.class)
@NoArgsConstructor @ToString
public class StringRenderer implements AnnotatedRenderer {
    private static final String MIME_TYPE = "text/plain";

    @Override
    public void renderTo(ObjectNode bundle, Object object) {
        if (! bundle.with(DATA).has(MIME_TYPE)) {
            bundle.with(DATA).put(MIME_TYPE, (String) object);
        }
    }
}
