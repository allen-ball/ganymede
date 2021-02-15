package galyleo.server.renderer;

import ball.annotation.ServiceProviderFor;
import com.fasterxml.jackson.databind.node.ObjectNode;
import galyleo.server.Renderer;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * {@link String} {@link Renderer} service provider.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@ServiceProviderFor({ Renderer.class })
@MimeType("text/plain") @ForType(String.class)
@NoArgsConstructor @ToString
public class StringRenderer implements AnnotatedRenderer {
    @Override
    public void renderTo(ObjectNode bundle, Object object) {
        if (! bundle.with(DATA).has(getMimeType())) {
            bundle.with(DATA).put(getMimeType(), (String) object);
        }
    }
}
