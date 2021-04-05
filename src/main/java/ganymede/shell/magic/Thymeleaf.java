package ganymede.shell.magic;
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
import ganymede.jsr223.ThymeleafScriptEngine;
import ganymede.server.Renderer;
import ganymede.server.renderer.ForClass;
import ganymede.shell.Magic;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.thymeleaf.templatemode.TemplateMode;

import static ganymede.notebook.NotebookMethods.print;
import static org.springframework.util.MimeTypeUtils.TEXT_HTML_VALUE;
import static org.springframework.util.MimeTypeUtils.TEXT_PLAIN_VALUE;
import static org.springframework.util.MimeTypeUtils.TEXT_XML_VALUE;

/**
 * {@link Thymeleaf} template {@link Magic}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@ServiceProviderFor({ Magic.class })
@Description("Thymeleaf template evaluator")
@ScriptEngineName("thymeleaf")
@NoArgsConstructor @ToString @Log4j2
public class Thymeleaf extends AbstractScriptEngineMagic {
    @Override
    protected void render(Object object) {
        var engine = (ThymeleafScriptEngine) engine();
        var resolver = engine.getResolver();

        print(new Output(resolver.getTemplateMode(), String.valueOf(object)));
    }

    /**
     * Customized {@link Output Output} for {@link Thymeleaf}
     * {@link RendererImpl Renderer}.
     *
     * {@bean.info}
     */
    @Data
    public static class Output {
        private final TemplateMode templateMode;
        private final String output;
    }

    /**
     * Customized {@link Renderer} for {@link Thymeleaf}
     * {@link Output Output}.
     */
    @ServiceProviderFor({ Renderer.class })
    @ForClass(Output.class)
    @NoArgsConstructor @ToString
    public static class RendererImpl implements Renderer {
        @Override
        public void renderTo(ObjectNode bundle, Object object) {
            var output = (Output) object;
            var mimeType = TEXT_PLAIN_VALUE;

            switch (output.getTemplateMode()) {
            case CSS:
                mimeType = "text/css";
                break;

            case HTML:
            case HTML5:
            case LEGACYHTML5:
            case VALIDXHTML:
            case XHTML:
                mimeType = TEXT_HTML_VALUE;
                break;

            case JAVASCRIPT:
                mimeType = "text/javascript";
                break;

            case VALIDXML:
            case XML:
                mimeType = TEXT_XML_VALUE;
                break;

            default:
                break;
            }

            if (! bundle.with(DATA).has(mimeType)) {
                bundle.with(DATA)
                    .put(mimeType, output.getOutput());
            }

            if (! bundle.with(DATA).has(TEXT_PLAIN_VALUE)) {
                bundle.with(DATA)
                    .put(TEXT_PLAIN_VALUE, String.format("[%s]", mimeType));
            }
        }
    }
}
