package ganymede.shell.magic;

import ball.annotation.ServiceProviderFor;
import ganymede.shell.Magic;
import javax.script.Bindings;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import scala.tools.nsc.Settings;
import scala.tools.nsc.interpreter.IMain;
import scala.tools.nsc.settings.MutableSettings;

/**
 * {@link Scala} {@link ganymede.shell.Magic}.
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
    private IMain interpreter = null;

    private IMain interpreter(Bindings bindings) {
        if (interpreter == null) {
            var settings = new Settings();

            settings.usejavacp().value_$eq(true);

            interpreter = new IMain(settings);
        }

        return interpreter;
    }

    @Override
    public void execute(Bindings bindings,
                        String line0, String code) throws Exception {
        var interpreter = interpreter(bindings);

        interpreter.interpret(code);
    }
}
