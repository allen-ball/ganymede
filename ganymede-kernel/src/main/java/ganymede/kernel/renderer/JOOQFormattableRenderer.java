package ganymede.kernel.renderer;
/*-
 * ##########################################################################
 * Ganymede
 * %%
 * Copyright (C) 2021 - 2023 Allen D. Ball
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
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.jooq.Formattable;

import static org.springframework.util.MimeTypeUtils.TEXT_HTML_VALUE;

/**
 * jOOQ {@link Formattable} {@link Renderer} service provider.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@ServiceProviderFor({ Renderer.class })
@ForClass(Formattable.class)
@NoArgsConstructor @ToString
public class JOOQFormattableRenderer extends AbstractRenderer {
    @Override
    public void renderTo(ObjectNode bundle, Object object) {
        var formattable = (Formattable) object;

        if (! bundle.withObject(DATA).has(TEXT_HTML_VALUE)) {
            bundle.withObject(DATA).put(TEXT_HTML_VALUE, formattable.formatHTML());
        }

        renderers.renderTo(bundle, formattable.format());
    }
}
