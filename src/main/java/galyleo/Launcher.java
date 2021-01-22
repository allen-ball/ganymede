package galyleo;

import galyleo.kernel.Kernel;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;

import static lombok.AccessLevel.PRIVATE;

/**
 * Galyleo application launcher.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@NoArgsConstructor(access = PRIVATE) @ToString @Log4j2
public abstract class Launcher {

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
        var application = new SpringApplication(Kernel.class);

        application.run(argv);
    }
}
