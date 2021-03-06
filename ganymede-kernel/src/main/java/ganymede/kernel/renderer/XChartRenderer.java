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
import java.io.IOException;
import java.util.Optional;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.internal.chartpart.Chart;

import static org.knowm.xchart.BitmapEncoder.BitmapFormat.PNG;

/**
 * {@link.uri https://github.com/knowm/XChart target=newtab XChart}
 * {@link Chart} {@link Renderer} service provider.  See
 * {@link BitmapEncoder}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@ServiceProviderFor({ Renderer.class })
@ForClassName("org.knowm.xchart.internal.chartpart.Chart")
@NoArgsConstructor @ToString
public class XChartRenderer extends AbstractRenderer {
    @Override
    public Optional<XChartRenderer> instance() {
        return Optional.ofNullable(getRenderType()).map(t -> new Impl());
    }

    @Override
    public void renderTo(ObjectNode bundle, Object object) {
        throw new IllegalStateException();
    }

    @NoArgsConstructor @ToString
    private class Impl extends XChartRenderer {
        @Override
        public void renderTo(ObjectNode bundle, Object object) {
            try {
                var bytes = BitmapEncoder.getBitmapBytes((Chart) object, PNG);

                renderers.renderTo(bundle, bytes);
            } catch (IOException exception) {
                exception.printStackTrace(System.err);
            }
        }
    }
}
