package galyleo.server.renderer;

import ball.annotation.ServiceProviderFor;
import com.fasterxml.jackson.databind.node.ObjectNode;
import galyleo.server.Renderer;
import java.io.IOException;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.internal.chartpart.Chart;

import static org.knowm.xchart.BitmapEncoder.BitmapFormat.PNG;

/**
 * {@link.uri https://github.com/knowm/XChart target=newtab XChart}
 * {@link Renderer} service provider.  See {@link BitmapEncoder}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@ServiceProviderFor({ Renderer.class })
@ForType(Chart.class)
@NoArgsConstructor @ToString
public class XChartRenderer extends ImageRenderer {
    @Override
    public void renderTo(ObjectNode bundle, Object object) {
        try {
            var bytes = BitmapEncoder.getBitmapBytes((Chart) object, PNG);

            super.renderTo(bundle, bytes);
        } catch (IOException exception) {
            exception.printStackTrace(System.err);
        }
    }
}
