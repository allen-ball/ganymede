package ganymede.server.renderer;
/*-
 * ##########################################################################
 * Ganymede
 * %%
 * Copyright (C) 2021 Allen D. Ball
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
import ganymede.server.Renderer;
import java.io.IOException;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;

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
public class JFreeChartRenderer implements Renderer {
    @Override
    public void renderTo(ObjectNode bundle, Object object) {
        try {
            var chart = (JFreeChart) object;
            var width = 800;
            var height = 600;
            var bytes = ChartUtils.encodeAsPNG(chart.createBufferedImage(width, height));

            MAP.renderTo(bundle, bytes);
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
