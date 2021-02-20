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
 * {@link Types} {@link galyleo.shell.Magic}.  See
 * {@link jdk.jshell.JShell#types()}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@ServiceProviderFor({ Magic.class })
@Description("Print JShell types")
@NoArgsConstructor @ToString @Log4j2
public class Types implements AnnotatedMagic {
    @Override
    public void execute(Shell shell,
                        InputStream in, PrintStream out, PrintStream err,
                        String magic, String code) throws Exception {
        shell.jshell().types()
            .forEach(t -> out.println(t.source()));
    }

    @Override
    public void execute(String magic, String code) throws Exception {
        throw new IllegalArgumentException(magic);
    }
}
