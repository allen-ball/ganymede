package ganymede.shell.magic;
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
import ganymede.kernel.KernelRestClient;
import ganymede.server.Message;
import ganymede.server.Renderer;
import ganymede.server.renderer.ForClass;
import ganymede.shell.Magic;
import java.util.ArrayList;
import java.util.List;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.commonmark.Extension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.renderer.text.TextContentRenderer;

import static org.springframework.util.MimeTypeUtils.TEXT_HTML_VALUE;

/**
 * {@link Markdown} {@link Magic}.
 *
 * @see Parser
 * @see HtmlRenderer
 * @see TextContentRenderer
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@ServiceProviderFor({ Magic.class })
@Description("Markdown template evaluator")
@ScriptEngineName("handlebars")
@NoArgsConstructor @ToString @Log4j2
public class Markdown extends Handlebars {
    private static final List<Extension> EXTENSIONS = new ArrayList<>();

    /**
     * Installed {@link Extension}s.
     *
     * {@include #TYPES}
     */
    public static final List<Class<? extends Extension>> TYPES =
        List.of(org.commonmark.ext.autolink.AutolinkExtension.class,
                org.commonmark.ext.gfm.strikethrough.StrikethroughExtension.class,
                org.commonmark.ext.gfm.tables.TablesExtension.class,
                org.commonmark.ext.heading.anchor.HeadingAnchorExtension.class,
                org.commonmark.ext.image.attributes.ImageAttributesExtension.class,
                org.commonmark.ext.ins.InsExtension.class,
                org.commonmark.ext.task.list.items.TaskListItemsExtension.class,
                org.commonmark.ext.front.matter.YamlFrontMatterExtension.class);
    static {
        try {
            for (Class<? extends Extension> type : TYPES) {
                var extension = (Extension) type.getMethod("create").invoke(null);

                EXTENSIONS.add(extension);
            }
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private final Parser parser = Parser.builder().extensions(EXTENSIONS).build();

    @Override
    protected void render(Object object) {
        try {
            var markdown = (String) object;
            var node = parser.parse(markdown);

            new KernelRestClient().print(Message.mime_bundle(node));
        } catch (Exception exception) {
            exception.printStackTrace(System.err);
        }
    }

    /**
     * CommonMark {@link Node} {@link Renderer} service provider.
     */
    @ServiceProviderFor({ Renderer.class })
    @ForClass(Node.class)
    @NoArgsConstructor @ToString
    public static class CommonMarkNodeRenderer implements Renderer {
        private HtmlRenderer html = HtmlRenderer.builder().extensions(EXTENSIONS).build();
        private TextContentRenderer text = TextContentRenderer.builder().extensions(EXTENSIONS).build();

        @Override
        public void renderTo(ObjectNode bundle, Object object) {
            var node = (Node) object;

            if (! bundle.with(DATA).has(TEXT_HTML_VALUE)) {
                bundle.with(DATA).put(TEXT_HTML_VALUE, html.render(node));
            }

            MAP.renderTo(bundle, text.render(node));
        }
    }
}
