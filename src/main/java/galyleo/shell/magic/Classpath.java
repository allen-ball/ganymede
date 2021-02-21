package galyleo.shell.magic;

import ball.annotation.ServiceProviderFor;
import galyleo.shell.Magic;
import galyleo.shell.Shell;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.stream.Stream;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.springframework.util.PropertyPlaceholderHelper;

/**
 * {@link Classpath} {@link galyleo.shell.Magic}.  See
 * {@link jdk.jshell.JShell#addToClasspath(String)}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@ServiceProviderFor({ Magic.class })
@Description("Add to or print JShell classpath")
@NoArgsConstructor @ToString @Log4j2
public class Classpath extends JShell {
    private static final String SEPARATOR = System.getProperty("path.separator");

    private final PropertyPlaceholderHelper helper = new PropertyPlaceholderHelper("${", "}", ":", true);

    @Override
    public void execute(Shell shell,
                        InputStream in, PrintStream out, PrintStream err,
                        String magic, String code) throws Exception {
        if (! code.isBlank()) {
            var classpath =
                helper.replacePlaceholders(code, System.getProperties())
                .lines()
                .filter(t -> (! t.isBlank()))
                .map(String::strip)
                .flatMap(t -> Stream.of(t.split(SEPARATOR)))
                .filter(t -> (! t.isBlank()))
                .map(String::strip)
                .toArray(String[]::new);

            shell.addToClasspath(classpath);
        } else {
            shell.classpath().stream().forEach(out::println);
        }
    }
}
