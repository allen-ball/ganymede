package ganymede.server.renderer;
/*-
 * ##########################################################################
 * Ganymede
 * $Id$
 * $HeadURL$
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
