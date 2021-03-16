package ganymede.shell.magic;

import java.io.StringReader;
import java.util.Objects;
import java.util.Properties;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

import static lombok.AccessLevel.PROTECTED;

/**
 * Abstract {@link Properties} {@link ganymede.shell.Magic} base class.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@NoArgsConstructor(access = PROTECTED) @ToString @Log4j2
public abstract class AbstractPropertiesMagic extends AbstractMagic {

    /**
     * Method to convert the cell {@code code} to a {@link Properties}
     * instance.
     *
     * @param   code            The code to parse.
     *
     * @return  The {@link Properties}.
     *
     * @see #HELPER
     */
    protected Properties compile(String code) throws Exception {
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

            return out;
        }
    }
}
