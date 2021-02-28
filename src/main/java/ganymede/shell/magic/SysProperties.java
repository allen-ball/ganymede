package ganymede.shell.magic;

import ball.annotation.ServiceProviderFor;
import ganymede.shell.Magic;
import java.io.StringReader;
import java.util.Objects;
import java.util.Properties;
import javax.script.Bindings;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

/**
 * {@link SysProperties} {@link ganymede.shell.Magic}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@ServiceProviderFor({ Magic.class })
@Description("Add/Update or print System properties")
@NoArgsConstructor @ToString @Log4j2
public class SysProperties extends AbstractMagic {
    @Override
    public void execute(Bindings bindings,
                        String magic, String code) throws Exception {
        if (! code.isBlank()) {
            try (var reader = new StringReader(code)) {
                var in = new Properties(System.getProperties());

                in.load(reader);

                var out = new Properties(in);
                var changed = true;

                while (changed) {
                    changed = false;

                    for (var key : in.keySet()) {
                        if (key instanceof String) {
                            var string = (String) key;
                            var value = HELPER.replacePlaceholders(in.getProperty(string), out);

                            changed |= (! Objects.equals(value, out.put(string, value)));
                        }
                    }
                }

                System.getProperties().putAll(out);
            } catch (Exception exception) {
                exception.printStackTrace(System.err);
            }
        } else {
            System.getProperties().store(System.out, null);
        }
    }
}
