package ganymede.kernel.renderer;
/*-
 * ##########################################################################
 * Ganymede
 * %%
 * Copyright (C) 2021, 2022 Allen D. Ball
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ##########################################################################
 */
import ball.annotation.ServiceProviderFor;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ganymede.notebook.AbstractRenderer;
import ganymede.notebook.ForClassName;
import ganymede.notebook.Renderer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;
import java.util.UUID;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.entity.StandardEntityCollection;

import static org.springframework.util.MimeTypeUtils.TEXT_HTML_VALUE;

/**
 * {@link.uri https://github.com/jfree/jfreechart target=newtab JFreeChart}
 * {@link Renderer} service provider.
 *
 * @see JFreeChart
 * @see ChartUtils
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@ServiceProviderFor({ Renderer.class })
@ForClassName("org.jfree.chart.JFreeChart")
@NoArgsConstructor @ToString
public class JFreeChartRenderer extends AbstractRenderer {
    @Override
    public Optional<JFreeChartRenderer> instance() {
        return Optional.ofNullable(getRenderType()).map(t -> new Impl());
    }

    @Override
    public void renderTo(ObjectNode bundle, Object object) {
        throw new IllegalStateException();
    }

    @NoArgsConstructor @ToString
    private class Impl extends JFreeChartRenderer {
        @Override
        public Optional<JFreeChartRenderer> instance() {
            return Optional.of(this);
        }

        @Override
        public void renderTo(ObjectNode bundle, Object object) {
            var chart = (JFreeChart) object;
            var width = 800;
            var height = 600;

            try (var out = new ByteArrayOutputStream()) {
                var info = new ChartRenderingInfo(new StandardEntityCollection());

                ChartUtils.writeChartAsPNG(out, chart, width, height, info);

                var image = out.toByteArray();

                renderers.renderTo(bundle, image);

                if (! bundle.with(DATA).has(TEXT_HTML_VALUE)) {
                    var html = new StringWriter();
                    var name = UUID.randomUUID().toString();
                    var mimeType = bundle.with(METADATA).fieldNames().next();
                    var base64 = bundle.with(DATA).get(mimeType).asText();

                    try (var writer = new PrintWriter(html)) {
                        writer.format("<img usemap=\"#%s\" src=\"data:%s;base64,%s\"/>\n", name, mimeType, base64);
                        ChartUtils.writeImageMap(writer, name, info, false);
                    }

                    bundle.with(DATA).put(TEXT_HTML_VALUE, html.toString());
                }
            } catch (Throwable throwable) {
                throwable.printStackTrace(System.err);
            }
        }
    }
}
