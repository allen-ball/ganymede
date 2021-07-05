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
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.extras.java8time.dialect.Java8TimeDialect;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.util.MimeTypeUtils.TEXT_HTML_VALUE;
import static org.springframework.util.MimeTypeUtils.TEXT_PLAIN_VALUE;
import static org.springframework.util.MimeTypeUtils.TEXT_XML_VALUE;

/**
 * Thymeleaf template {@link Renderer}.
 *
 * @see TemplateEngine
 * @see StringTemplateResolver
 * @see Java8TimeDialect
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@ServiceProviderFor({ Renderer.class })
@ForClass(ThymeleafRenderer.Output.class)
@NoArgsConstructor @ToString
public class ThymeleafRenderer implements Renderer {
    private static final StringTemplateResolver RESOLVER;
    private static final TemplateEngine ENGINE;

    static {
        RESOLVER = new StringTemplateResolver();

        ENGINE = new TemplateEngine();
        ENGINE.setTemplateResolver(RESOLVER);
        ENGINE.addDialect(new Java8TimeDialect());
    }

    /**
     * Method to evaluate a template from a {@link String}.
     *
     * @param template      The template {@link String}.
     * @param mode          The template resolver mode.
     * @param map           The {@link Context} name-value pairs.
     *
     * @return  The {@link Output} to be renderered.
     */
    public static Output process(String template, String mode, Map<String,Object> map) {
        RESOLVER.setTemplateMode(mode);

        return new Output(RESOLVER.getTemplateMode(),
                          ENGINE.process(template, new Context(null, map)));
    }

    /**
     * Method to evaluate a template from a resource.
     *
     * @param type          The {@link Class} to search relative for the
     *                      resource.
     * @param name          The template resource name.
     * @param mode          The template resolver mode.
     * @param map           The {@link Context} name-value pairs.
     *
     * @return  The {@link Output} to be renderered.
     */
    public static Output process(Class<?> type, String name, String mode, Map<String,Object> map) {
        return process(getResourceAsString(type, name), mode, map);
    }

    @Override
    public void renderTo(ObjectNode bundle, Object object) {
        var output = (Output) object;
        var string = output.getOutput();
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
            bundle.with(DATA).put(mimeType, string);
        }
    }

    private static String getResourceAsString(Class<?> type, String name) {
        try (var in = new ClassPathResource(name, type).getInputStream()) {
            return StreamUtils.copyToString(in, UTF_8);
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    /**
     * Customized {@link Output Output} for {@link ThymeleafRenderer}.
     *
     * {@bean.info}
     */
    @Data
    public static class Output {
        private final TemplateMode templateMode;
        private final String output;
    }
}
