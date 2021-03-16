package ganymede.shell.magic;

import ball.annotation.ServiceProviderFor;
import ganymede.shell.Magic;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

/**
 * {@link Groovy} {@link ganymede.shell.Magic}.
 *
 * @see org.codehaus.groovy.jsr223.GroovyScriptEngineFactory
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@ServiceProviderFor({ Magic.class })
@Description("Execute code in groovy REPL")
@NoArgsConstructor @ToString @Log4j2
public class Groovy extends AbstractScriptEngineMagic {
}
