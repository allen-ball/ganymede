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
import com.fasterxml.jackson.databind.node.ObjectNode;
import ganymede.server.Renderer;
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
public class XChartRenderer implements AnnotatedRenderer {
    @Override
    public void renderTo(ObjectNode bundle, Object object) {
        try {
            var bytes = BitmapEncoder.getBitmapBytes((Chart) object, PNG);

            new ImageRenderer().renderTo(bundle, bytes);
        } catch (IOException exception) {
            exception.printStackTrace(System.err);
        }
    }
}
