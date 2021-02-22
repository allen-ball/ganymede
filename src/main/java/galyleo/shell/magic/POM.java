package galyleo.shell.magic;

import ball.annotation.ServiceProviderFor;
import com.fasterxml.jackson.core.JsonProcessingException;
import galyleo.shell.Magic;
import galyleo.shell.Shell;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.stream.Stream;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

/**
 * {@link POM} {@link galyleo.shell.Magic}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@ServiceProviderFor({ Magic.class })
@Description("Define the Notebook's Project Object Model")
@NoArgsConstructor @ToString @Log4j2
public class POM extends JShell {
    @Override
    public void execute(Shell shell,
                        InputStream in, PrintStream out, PrintStream err,
                        String magic, String code) throws Exception {
        try {
            shell.resolve(galyleo.dependency.POM.parse(code));
        } catch (JsonProcessingException exception) {
            err.println(exception.getMessage());
        } catch (Exception exception) {
            exception.printStackTrace(err);
        }
    }
}
