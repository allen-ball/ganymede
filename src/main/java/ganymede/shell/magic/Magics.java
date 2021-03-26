package ganymede.shell.magic;

import ball.annotation.ServiceProviderFor;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ganymede.server.Message;
import ganymede.server.Renderer;
import ganymede.server.renderer.ForType;
import ganymede.server.renderer.StringRenderer;
import ganymede.shell.Magic;
import ganymede.shell.Shell;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import javax.script.ScriptContext;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

import static ganymede.server.Server.OBJECT_MAPPER;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import static org.springframework.util.MimeTypeUtils.TEXT_HTML_VALUE;

/**
 * {@link Magics} {@link ganymede.shell.Magic}: List configured
 * {@link Magic}s.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@ServiceProviderFor({ Magic.class })
@Description("Lists available cell magics")
@NoArgsConstructor @ToString @Log4j2
public class Magics extends AbstractMagic {
    @Override
    public void execute(Shell shell,
                        InputStream in, PrintStream out, PrintStream err,
                        String line0, String code) throws Exception {
        shell.kernel()
            .print(Message.mime_bundle(new Output(shell)));
    }

    @Override
    public void execute(ScriptContext context,
                        String line0, String code) throws Exception {
        throw new IllegalArgumentException(line0);
    }

    private static final Comparator<List<String>> COMPARATOR =
        Comparator.comparing(t -> t.toString());

    /**
     * Customized {@link Output Output} for {@link Magics}
     * {@link RendererImpl Renderer}.
     */
    public static class Output extends TreeMap<List<String>,String> {
        private static final long serialVersionUID = -2609872192564130449L;

        private Output(Shell shell) {
            super(COMPARATOR);

            var magics = shell.magics();

            magics.values().stream()
                .distinct()
                .forEach(t -> putIfAbsent(List.of(t.getMagicNames()),
                                          t.getDescription()));
        }
    }

    /**
     * Customized {@link Renderer} for {@link Magics} {@link Output Output}.
     */
    @ServiceProviderFor({ Renderer.class })
    @ForType(Output.class)
    @NoArgsConstructor @ToString
    public static class RendererImpl extends StringRenderer {
        @Override
        public void renderTo(ObjectNode bundle, Object object) {
            var output = (Output) object;

            if (! bundle.with(DATA).has(TEXT_HTML_VALUE)) {
                try (var writer = new StringWriter()) {
                    var out = new PrintWriter(writer);

                    out.println("<h4>Cell Magic</h4>");
                    out.println("<table>");
                    out.format("<tr><th>%s</th><th>%s</th></tr>\n",
                               "Name(s)", "Description");
                    output.entrySet().stream()
                        .forEach(t -> out.format("<tr><td>%s</td><td>%s</td></tr>\n",
                                                 String.join(", ", t.getKey()),
                                                 t.getValue()));
                    out.println("</table>");

                    bundle.with(DATA).put(TEXT_HTML_VALUE, writer.toString());
                } catch (Exception exception) {
                    throw new IllegalStateException(exception);
                }
            }

            try (var writer = new StringWriter()) {
                var out = new PrintWriter(writer);

                output.entrySet().stream()
                    .forEach(t -> out.format("%s\t%s\n",
                                             String.join(", ", t.getKey()),
                                             t.getValue()));

                super.renderTo(bundle, writer.toString());
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
        }
    }
}
