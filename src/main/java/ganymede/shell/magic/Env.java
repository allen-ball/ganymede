package ganymede.shell.magic;

import ball.annotation.ServiceProviderFor;
import ganymede.shell.Magic;
import java.util.Properties;
import javax.script.Bindings;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

/**
 * {@link Env} {@link Magic}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@ServiceProviderFor({ Magic.class })
@Description("Add/Update or print the environment")
@NoArgsConstructor @ToString @Log4j2
public class Env extends AbstractPropertiesMagic {
    @Override
    public void execute(Bindings bindings,
                        String line0, String code) throws Exception {
        if (! code.isBlank()) {
            var properties = compile(code);

            for (var entry : properties.entrySet()) {
                throw new UnsupportedOperationException(System.getProperty("os.name")
                                                        + ": Cannot set " + entry);
            }
        } else {
            var properties = new Properties();

            properties.putAll(System.getenv());
            properties.store(System.out, null);
        }
    }
}
