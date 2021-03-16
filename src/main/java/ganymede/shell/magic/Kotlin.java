package ganymede.shell.magic;

import ball.annotation.ServiceProviderFor;
import ganymede.shell.Magic;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

/**
 * {@link Kotlin} {@link ganymede.shell.Magic}.
 *
 * @see org.jetbrains.kotlin.script.jsr223.KotlinJsr223JvmLocalScriptEngineFactory
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@ServiceProviderFor({ Magic.class })
@Description("Execute code in kotlin REPL")
@NoArgsConstructor @ToString @Log4j2
public class Kotlin extends AbstractScriptEngineMagic {
    @Override
    public String getExtension() { return "kts"; }
}
