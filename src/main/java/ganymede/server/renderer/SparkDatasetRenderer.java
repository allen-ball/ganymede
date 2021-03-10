package ganymede.server.renderer;

import ball.annotation.ServiceProviderFor;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ganymede.server.Renderer;
import java.util.Map;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.util.MimeTypeUtils.TEXT_HTML_VALUE;

/**
 * Spark {@link Dataset} {@link Renderer} service provider.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@ServiceProviderFor({ Renderer.class })
@ForType(Dataset.class)
@NoArgsConstructor @ToString
public class SparkDatasetRenderer extends StringRenderer {
    private final TemplateEngine engine = new TemplateEngine();
    private final String template;

    {
        var resolver = new StringTemplateResolver();

        resolver.setTemplateMode(StringTemplateResolver.DEFAULT_TEMPLATE_MODE);

        engine.setTemplateResolver(resolver);

        var name = getClass().getSimpleName() + ".html";
        var resource = new ClassPathResource(name, getClass());

        try (var in = resource.getInputStream()) {
            template = StreamUtils.copyToString(in, UTF_8);
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    @Override
    public void renderTo(ObjectNode bundle, Object object) {
        if (! bundle.with(DATA).has(TEXT_HTML_VALUE)) {
            try {
                var map = Map.<String,Object>of("dataset", object, "view", 50);
                var html = engine.process(template, new Context(null, map));

                bundle.with(DATA).put(TEXT_HTML_VALUE, html);
            } catch (Exception exception) {
            }
        }

        super.renderTo(bundle, String.valueOf(object));
    }
}
