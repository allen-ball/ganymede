package ganymede.shell.magic;

import java.util.concurrent.ConcurrentSkipListMap;
import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

import static javax.script.ScriptContext.ENGINE_SCOPE;
import static javax.script.ScriptContext.GLOBAL_SCOPE;
import static lombok.AccessLevel.PROTECTED;

/**
 * Abstract
 * {@link.uri https://www.jcp.org/en/jsr/detail?id=223 target=newtab JSR 223}
 * {@link ScriptEngine} {@link ganymede.shell.Magic} base class.
 *
 * @see ScriptEngineManager
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@NoArgsConstructor(access = PROTECTED) @ToString @Log4j2
public abstract class AbstractScriptEngineMagic extends AbstractMagic {
    protected static final SimpleScriptContext CONTEXT = new SimpleScriptContext();

    static {
        var bindings = new SimpleBindings(new ConcurrentSkipListMap<>());

        CONTEXT.setBindings(bindings, ENGINE_SCOPE);
    }

    protected ScriptEngine engine = null;

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
    protected ScriptEngine engine() {
        if (engine == null) {
            var manager = new ScriptEngineManager(getClass().getClassLoader());

            engine = manager.getEngineByExtension(getExtension());

            if (engine == null) {
                engine =
                    manager.getEngineFactories().stream()
                    .filter(t -> t.getExtensions().contains(getExtension()))
                    .map(t -> t.getScriptEngine())
                    .findFirst().orElse(null);
            }
        }

        return engine;
    }

    @Override
    public void execute(Bindings bindings,
                        String line0, String code) throws Exception {
        var engine = engine();

        if (engine != null) {
            CONTEXT.setBindings(bindings, GLOBAL_SCOPE);

            engine.eval(code, CONTEXT);
        } else {
            System.err.format("No %s REPL available\n", getMagicNames()[0]);
        }
    }
}
