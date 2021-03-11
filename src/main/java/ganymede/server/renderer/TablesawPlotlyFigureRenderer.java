package ganymede.server.renderer;

import ball.annotation.ServiceProviderFor;
import ganymede.server.Renderer;
import java.util.Map;
import lombok.NoArgsConstructor;
import lombok.ToString;
import tech.tablesaw.plotly.components.Figure;

/**
 * {@link.uri https://github.com/jtablesaw/tablesaw target=newtab Tablesaw}
 * {@link.uri https://github.com/plotly target=newtab Plot.ly}
 * {@link Figure} {@link Renderer} service provider.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@ServiceProviderFor({ Renderer.class })
@ForType(Figure.class)
@NoArgsConstructor @ToString
public class TablesawPlotlyFigureRenderer extends AbstractThymeleafHTMLRenderer {
    @Override
    protected Map<String,Object> getMap(Object object) {
        return Map.<String,Object>of("figure", object);
    }
}
