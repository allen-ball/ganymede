package ganymede.shell.magic;

import ball.annotation.ServiceProviderFor;
import ganymede.kernel.KernelRestClient;
import ganymede.shell.Magic;
import java.util.List;
import javax.script.Bindings;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import scala.collection.JavaConversions;
import scala.tools.nsc.Settings;
import scala.tools.nsc.interpreter.IMain;

/**
 * {@link Scala} {@link Magic}.
 *
 * @see IMain
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@ServiceProviderFor({ Magic.class })
@Description("Execute code in scala REPL")
@NoArgsConstructor @ToString @Log4j2
public class Scala extends AbstractMagic {
    /*
     * Can't simply extend AbstractScriptEngineMagic because:
     *
     *     new ScriptEngineManager().getEngineByName("scala");
     *
     * tickles https://github.com/scala/bug/issues/11754.
     */
    private IMain engine = null;

    private IMain engine() {
        if (engine == null) {
            try {
                var settings = new Settings();

                settings.usejavacp().value_$eq(true);

                new KernelRestClient().classpath().stream()
                    .forEach(t -> settings.classpath().append(t));

                engine = new IMain(settings);
                engine.initializeSynchronous();
            } catch (Exception exception) {
                exception.printStackTrace(System.err);
            }
        }

        return engine;
    }

    @Override
    public void execute(Bindings bindings,
                        String line0, String code) throws Exception {
        var engine = engine();
        var modifiers = JavaConversions.asScalaBuffer(List.<String>of()).toList();

        for (var entry : bindings.entrySet()) {
            var key = entry.getKey();
            var value = entry.getValue();
            var type = (value != null) ? value.getClass() : Object.class;

            engine.bind(key, type.getName(), value, modifiers);
        }

        engine.interpret(code);
    }
}
