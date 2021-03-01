package ganymede.server.renderer;

import ball.annotation.ServiceProviderFor;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ganymede.server.Renderer;
import lombok.NoArgsConstructor;
import lombok.ToString;

import static org.springframework.util.MimeTypeUtils.TEXT_PLAIN_VALUE;

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
    @Override
    public void renderTo(ObjectNode bundle, Object object) {
        if (! bundle.with(DATA).has(TEXT_PLAIN_VALUE)) {
            bundle.with(DATA).put(TEXT_PLAIN_VALUE, (String) object);
        }
    }
}
