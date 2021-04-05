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
import java.util.Map;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.util.MimeTypeUtils.TEXT_HTML_VALUE;
import static org.springframework.util.MimeTypeUtils.TEXT_PLAIN_VALUE;
import static org.springframework.util.MimeTypeUtils.TEXT_XML_VALUE;

/**
 * Thymeleaf template {@link Renderer}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@ServiceProviderFor({ Renderer.class })
@ForClass(ThymeleafTemplateRenderer.Output.class)
@NoArgsConstructor @ToString
public class ThymeleafTemplateRenderer implements Renderer {
    private final TemplateEngine engine = new TemplateEngine();
    private final StringTemplateResolver resolver = new StringTemplateResolver();

    { engine.setTemplateResolver(resolver); }

    @Override
    public void renderTo(ObjectNode bundle, Object object) {
        var output = (Output) object;
        var string = output.process(engine, resolver);
        var mimeType = TEXT_PLAIN_VALUE;

        switch (resolver.getTemplateMode()) {
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
            bundle.with(DATA).put(mimeType, string);
        }
    }

    /**
     * Customized {@link Output Output} for
     * {@link ThymeleafTemplateRenderer}.
     *
     * {@bean.info}
     */
    @ToString
    public static class Output {
        private final String template;
        private final String mode;
        private final Context context;

        /**
         * Constructor to load a template from a {@link String}.
         *
         * @param template      The template {@link String}.
         * @param mode          The template resolver mode.
         * @param map           The {@link Context} name-value pairs.
         */
        public Output(String template, String mode, Map<String,Object> map) {
            this.template = template;
            this.mode = mode;
            this.context = new Context(null, map);
        }

        /**
         * Constructor to load a template from a resource.
         *
         * @param type          The {@link Class} to search relative for the
         *                      resource.
         * @param name          The template resource name.
         * @param mode          The template resolver mode.
         * @param map           The {@link Context} name-value pairs.
         */
        public Output(Class<?> type, String name, String mode, Map<String,Object> map) {
            this(getResourceAsString(type, name), mode, map);
        }

        private String process(TemplateEngine engine, StringTemplateResolver resolver) {
            resolver.setTemplateMode(mode);

            return engine.process(template, context);
        }

        private static String getResourceAsString(Class<?> type, String name) {
            try (var in = new ClassPathResource(name, type).getInputStream()) {
                return StreamUtils.copyToString(in, UTF_8);
            } catch (Exception exception) {
                throw new ExceptionInInitializerError(exception);
            }
        }
    }
}
