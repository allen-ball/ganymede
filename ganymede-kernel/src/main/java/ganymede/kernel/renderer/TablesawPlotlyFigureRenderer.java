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
import ganymede.notebook.ForClassName;
import ganymede.notebook.AbstractRenderer;
import ganymede.notebook.Renderer;
import java.util.Map;
import java.util.Optional;
import lombok.NoArgsConstructor;
import lombok.ToString;
import tech.tablesaw.plotly.components.Figure;

/**
 * {@link.uri https://github.com/jtablesaw/tablesaw target=newtab Tablesaw}
 * {@link.uri https://github.com/plotly target=newtab Plot.ly}
 * {@link Figure} {@link Renderer} service provider.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@ServiceProviderFor({ Renderer.class })
@ForClassName("tech.tablesaw.plotly.components.Figure")
@NoArgsConstructor @ToString
public class TablesawPlotlyFigureRenderer extends AbstractRenderer {
    @Override
    public Optional<TablesawPlotlyFigureRenderer> instance() {
        return Optional.ofNullable(getRenderType()).map(t -> new Impl());
    }

    @Override
    public void renderTo(ObjectNode bundle, Object object) {
        throw new IllegalStateException();
    }

    @NoArgsConstructor @ToString
    private class Impl extends TablesawPlotlyFigureRenderer {
        @Override
        public void renderTo(ObjectNode bundle, Object object) {
            var type = getClass().getEnclosingClass();
            var resource = type.getSimpleName() + ".html";
            var map = Map.<String,Object>of("figure", (Figure) object);
            var output = ThymeleafRenderer.process(type, resource, "html", map);

            renderers.renderTo(bundle, output);
        }
    }
}
