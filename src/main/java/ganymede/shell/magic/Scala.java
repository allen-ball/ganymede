package ganymede.shell.magic;

import ball.annotation.ServiceProviderFor;
import ganymede.kernel.KernelRestClient;
import ganymede.shell.Magic;
import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import scala.tools.nsc.Settings;
import scala.tools.nsc.interpreter.Scripted;

import static javax.script.ScriptContext.GLOBAL_SCOPE;

/**
 * {@link Scala} {@link Magic}.
 *
 * @see Scripted
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@ServiceProviderFor({ Magic.class })
@Description("Execute code in scala REPL")
@NoArgsConstructor @ToString @Log4j2
public class Scala extends AbstractScriptEngineMagic {
    @Override
    protected ScriptEngine engine() {
        if (engine == null) {
            /*
             * Can't simply extend AbstractScriptEngineMagic because:
             *
             *     new ScriptEngineManager().getEngineByName("scala");
             *
             * tickles https://github.com/scala/bug/issues/11754.
             */
            try {
                var manager = new ScriptEngineManager(getClass().getClassLoader());
                var factory =
                    (Scripted.Factory)
                    manager.getEngineFactories().stream()
                    .filter(t -> t.getExtensions().contains(getExtension()))
                    .findFirst().orElse(null);
                var settings = new Settings();

                settings.usejavacp().value_$eq(true);

                new KernelRestClient().classpath().stream()
                    .forEach(t -> settings.classpath().append(t));

                engine = new Scripted(factory, settings, Scripted.apply$default$3());
            } catch (NoClassDefFoundError exception) {
            } catch (Exception exception) {
                exception.printStackTrace(System.err);
            }
        }

        return engine;
    }
}
