package galyleo.shell.magic;

import ball.annotation.ServiceProviderFor;
import galyleo.shell.Magic;
import galyleo.shell.Shell;
import java.io.InputStream;
import java.io.PrintStream;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

/**
 * {@link Imports} {@link galyleo.shell.Magic}.  See
 * {@link jdk.jshell.JShell#imports()}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@ServiceProviderFor({ Magic.class })
@Description("Print JShell imports")
@NoArgsConstructor @ToString @Log4j2
public class Imports extends JShell {
    @Override
    public void execute(Shell shell,
                        InputStream in, PrintStream out, PrintStream err,
                        String magic, String code) throws Exception {
        shell.jshell().imports()
            .forEach(t -> out.println(t.source()));
    }
}
