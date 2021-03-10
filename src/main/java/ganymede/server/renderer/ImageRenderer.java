package ganymede.server.renderer;

/* import ball.annotation.ServiceProviderFor; */
import com.fasterxml.jackson.databind.node.ObjectNode;
import ganymede.server.Renderer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Abstract image {@link Renderer} base class.  See {@link ImageIO}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
/* @ServiceProviderFor({ Renderer.class }) */
@ForType(byte[].class)
@NoArgsConstructor @ToString
public abstract class ImageRenderer extends ObjectRenderer {
    @Override
    public void renderTo(ObjectNode bundle, Object object) {
        var bytes = (byte[]) object;

        try (var in = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var reader = ImageIO.getImageReaders(in).next();

            reader.setInput(in);

            var mimeType = reader.getOriginatingProvider().getMIMETypes()[0];

            if (! bundle.with(DATA).has(mimeType)) {
                bundle.with(DATA)
                    .put(mimeType, BASE64_ENCODER.encodeToString(bytes));

                var metadata = bundle.with(METADATA).with(mimeType);

                metadata.put("height", reader.getHeight(0));
                metadata.put("width", reader.getWidth(0));
            }
        } catch (IOException exception) {
            exception.printStackTrace(System.err);
        }
    }
}
