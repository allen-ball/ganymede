package ganymede.shell.magic;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import lombok.NoArgsConstructor;
import lombok.Synchronized;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

import static lombok.AccessLevel.PROTECTED;

/**
 * Abstract {@link ScriptEngine} {@link ganymede.shell.Magic} base class.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@NoArgsConstructor(access = PROTECTED) @ToString @Log4j2
public abstract class AbstractScriptEngineMagic extends AbstractMagic {
    private ScriptEngine engine = null;

    /**
     * Method to get the script extension.
     *
     * @return  The script extension.
     */
    public String getExtension() { return getMagicNames()[0]; }

    /**
     * Method to get the {@link ScriptEngine}.
     *
     * @return  The {@link ScriptEngine} if it can be instantiated;
     *          {@code null} otherwise.
     */
    @Synchronized
    protected ScriptEngine engine(Bindings bindings) {
        if (engine == null) {
            var manager = new ScriptEngineManager();

            manager.setBindings(bindings);

            engine = manager.getEngineByExtension(getExtension());
        }

        return engine;
    }

    @Override
    public void execute(Bindings bindings,
                        String magic, String code) throws Exception {
        var engine = engine(bindings);

        if (engine != null) {
            engine.eval(code);
        } else {
            System.err.format("No %s REPL available\n", getMagicNames()[0]);
        }
    }
}
