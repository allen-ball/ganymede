package galyleo.shell.magic;

import ball.annotation.ServiceProviderFor;
import galyleo.shell.Magic;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

/**
 * {@link Perl} {@link Script} {@link galyleo.shell.Magic}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@ServiceProviderFor({ Magic.class })
@Description("Execute script with 'perl' command")
@NoArgsConstructor @ToString @Log4j2
public class Perl extends Script {
    @Override
    public void execute(String magic, String code) throws Exception {
        try {
            super.execute(CELL + super.getMagicNames()[0]
                          + " " + magic.substring(CELL.length()),
                          code);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(magic);
        }
    }
}
