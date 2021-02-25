package galyleo.server.renderer;

import ball.annotation.ServiceProviderFor;
import com.fasterxml.jackson.databind.node.ObjectNode;
import galyleo.server.Renderer;
import java.io.IOException;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;

/**
 * {@link.uri https://github.com/jfree/jfreechart target=newtab JFreeChart}
 * {@link Renderer} service provider.  See {@link ChartUtils}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@ServiceProviderFor({ Renderer.class })
@ForType(JFreeChart.class)
@NoArgsConstructor @ToString
public class JFreeChartRenderer extends ImageRenderer {
    @Override
    public void renderTo(ObjectNode bundle, Object object) {
        try {
            var chart = (JFreeChart) object;
            var width = 800;
            var height = 600;
            var bytes = ChartUtils.encodeAsPNG(chart.createBufferedImage(width, height));

            super.renderTo(bundle, bytes);
            /*
             * HTML
             *
             * ChartUtils.writeImageMap(PrintWriter writer,
             *                          String name,
             *                          ChartRenderingInfo info,
             *                          false);
             */
        } catch (IOException exception) {
            exception.printStackTrace(System.err);
        }
    }
}
