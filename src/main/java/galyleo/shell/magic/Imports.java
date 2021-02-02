package galyleo.shell.magic;

import ball.annotation.ServiceProviderFor;
import java.io.InputStream;
import java.io.PrintStream;
import jdk.jshell.JShell;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

/**
 * {@link Imports} {@link Magic}.  See {@link JShell#imports()}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@ServiceProviderFor({ Magic.class })
@NoArgsConstructor @ToString @Log4j2
public class Imports extends Java {
    @Override
    public void execute(JShell jshell,
                        InputStream in, PrintStream out, PrintStream err,
                        String magic, String code) throws Exception {
        jshell.imports()
            .forEach(t -> out.println(t.source()));
    }
}
