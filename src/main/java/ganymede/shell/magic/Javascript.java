package ganymede.shell.magic;

import ball.annotation.ServiceProviderFor;
import ganymede.shell.Magic;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

/**
 * {@link Javascript} {@link ganymede.shell.Magic}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@ServiceProviderFor({ Magic.class })
@MagicNames({ "javascript", "js" })
@Description("Execute code in javascript REPL")
@NoArgsConstructor @ToString @Log4j2
public class Javascript extends AbstractScriptEngineMagic {
    @Override
    public String getExtension() { return "js"; }
}
