package ganymede.shell.magic;

import ball.annotation.ServiceProviderFor;
import ganymede.shell.Magic;
import javax.script.ScriptContext;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

/**
 * {@link SysProperties} {@link Magic}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@ServiceProviderFor({ Magic.class })
@Description("Add/Update or print System properties")
@NoArgsConstructor @ToString @Log4j2
public class SysProperties extends AbstractPropertiesMagic {
    @Override
    public void execute(ScriptContext context,
                        String line0, String code) throws Exception {
        if (! code.isBlank()) {
            var properties = compile(code);

            System.getProperties().putAll(properties);
        } else {
            System.getProperties().store(System.out, null);
        }
    }
}
