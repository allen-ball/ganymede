package galyleo;

import galyleo.install.Install;
import galyleo.kernel.Kernel;
import java.util.Map;
import java.util.stream.Stream;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;

import static java.util.stream.Collectors.toMap;

/**
 * Galyleo application launcher.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@NoArgsConstructor @ToString @Log4j2
public class Launcher {

    /**
     * Defined {@link #MODES}.  The {@link Launcher} should be invoked with
     * {@code java -Dmode=<mode> ...} where mode is one of {@link MODES}
     * keys.
     *
     * {@include}
     */
    protected static final Map<String,Class<?>> MODES =
        Stream.of(Install.class, Kernel.class)
        .collect(toMap(k -> k.getSimpleName().toLowerCase(), v -> v));

    /**
     * Standard {@link SpringApplication} {@code main(String[])}
     * entry point.
     *
     * @param   argv            The command line argument vector.
     *
     * @throws  Exception       If the function does not catch
     *                          {@link Exception}.
     */
    public static void main(String[] argv) throws Exception {
        var mode = System.getProperty("mode", Kernel.class.getSimpleName());
        var type = MODES.get(mode.toLowerCase());

        if (type != null) {
            var application = new SpringApplication(type);

            application.run(argv);
        } else {
            throw new IllegalArgumentException("Invalid mode: " + mode);
        }
    }
}
