package ganymede.server.renderer;

import ball.annotation.ServiceProviderFor;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ganymede.io.PrintStreamBuffer;
import ganymede.server.Renderer;
import java.io.IOException;
import java.util.UUID;
import lombok.NoArgsConstructor;
import lombok.ToString;
import tech.tablesaw.plotly.components.Figure;

import static org.springframework.util.MimeTypeUtils.TEXT_HTML_VALUE;

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
public class TablesawFigureRenderer extends StringRenderer {
    @Override
    public void renderTo(ObjectNode bundle, Object object) {
        if (! bundle.with(DATA).has(TEXT_HTML_VALUE)) {
            var buffer = new PrintStreamBuffer();
            var figure = (Figure) object;
            var context = figure.getContext();
            var id = UUID.randomUUID().toString().replaceAll("-", "");
            /*
             * https://github.com/plotly/plotly.js
             * https://stackoverflow.com/questions/54654434/how-to-embed-tablesaw-graph-in-jupyter-notebook-with-ijava-kernel

%%thymeleaf html
[(${figure.asJavascript(id)})]
<div th:id="${id}"></div>
<script th:inline="javascript">
  require(['https://cdn.plot.ly/plotly-latest.min.js'], Plotly => {
    var target_[(${id})] = document.getElementById([[${id}]]);
    [(${figure.context.get('figure')})]
    [(${figure.context.get('plotFunction')})]
  })
</script>
             */
            buffer.println(figure.asJavascript(id));
            buffer.format("<div id=\"%s\"></div>\n", id);
            buffer.format("<script>require(['https://cdn.plot.ly/plotly-latest.min.js'], Plotly => {\n");
            buffer.format("    var target_%s = document.getElementById('%s');\n", id, id);
            buffer.format("    %s\n", context.get("figure"));
            buffer.format("    %s\n", context.get("plotFunction"));
            buffer.format("})</script>\n");

            bundle.with(DATA).put(TEXT_HTML_VALUE, buffer.toString());
        }
    }
}
