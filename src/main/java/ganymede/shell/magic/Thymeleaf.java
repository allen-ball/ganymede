package ganymede.shell.magic;

import ball.annotation.ServiceProviderFor;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ganymede.server.Message;
import ganymede.server.Renderer;
import ganymede.server.renderer.AnnotatedRenderer;
import ganymede.server.renderer.ForType;
import ganymede.shell.Magic;
import java.util.stream.Stream;
import javax.script.Bindings;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import static ganymede.shell.jshell.CellMethods.print;
import static org.springframework.util.MimeTypeUtils.TEXT_HTML_VALUE;
import static org.springframework.util.MimeTypeUtils.TEXT_PLAIN_VALUE;
import static org.springframework.util.MimeTypeUtils.TEXT_XML_VALUE;

/**
 * Thymeleaf template {@link ganymede.shell.Magic}.
 *
 * @see TemplateEngine
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@ServiceProviderFor({ Magic.class })
@Description("Thymeleaf template evaluator")
@NoArgsConstructor @ToString @Log4j2
public class Thymeleaf extends AbstractMagic {
    @Override
    public void execute(Bindings bindings,
                        String line0, String code) throws Exception {
        try {
            var resolver = new StringTemplateResolver();
            var argv = Magic.getCellMagicCommand(line0);
            var mode = StringTemplateResolver.DEFAULT_TEMPLATE_MODE;

            if (argv.length > 1) {
                mode = TemplateMode.valueOf(argv[1].toUpperCase());
            }

            resolver.setTemplateMode(mode);

            var engine = new TemplateEngine();

            engine.setTemplateResolver(resolver);

            var context = new Context(null, bindings);
            var output = engine.process(code, context);

            print(new Output(resolver.getTemplateMode(), output));
        } catch (Exception exception) {
            exception.printStackTrace(System.err);
        }
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
    @ForType(Output.class)
    @NoArgsConstructor @ToString
    public static class RendererImpl implements AnnotatedRenderer {
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
