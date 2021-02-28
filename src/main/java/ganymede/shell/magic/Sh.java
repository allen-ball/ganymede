package ganymede.shell.magic;

import ball.annotation.ServiceProviderFor;
import ganymede.shell.Magic;
import javax.script.Bindings;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

/**
 * {@link Sh} {@link Script} {@link ganymede.shell.Magic}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@ServiceProviderFor({ Magic.class })
@Description("Execute script with 'sh' command")
@NoArgsConstructor @ToString @Log4j2
public class Sh extends Script {
    @Override
    public void execute(Bindings bindings,
                        String line0, String code) throws Exception {
        try {
            super.execute(bindings,
                          String.format("%s%s %s",
                                        CELL, super.getMagicNames()[0],
                                        line0.substring(CELL.length())),
                          code);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(line0);
        }
    }
}
