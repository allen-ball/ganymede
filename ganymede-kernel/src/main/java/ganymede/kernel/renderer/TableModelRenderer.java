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
import ganymede.notebook.ForClass;
import ganymede.notebook.Renderer;
import java.util.Map;
import javax.swing.table.TableModel;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * {@link TableModel} {@link Renderer} service provider.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@ServiceProviderFor({ Renderer.class })
@ForClass(TableModel.class)
@NoArgsConstructor @ToString
public class TableModelRenderer extends AbstractRenderer {
    @Override
    public void renderTo(ObjectNode bundle, Object object) {
        var resource = getClass().getSimpleName() + ".html";
        var map = Map.<String,Object>of("model", (TableModel) object);
        var output = ThymeleafRenderer.process(getClass(), resource, "html", map);

        renderers.renderTo(bundle, output);
    }
}
