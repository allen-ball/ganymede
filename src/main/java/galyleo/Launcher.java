package galyleo;

import galyleo.connect.Connect;
import galyleo.install.Install;
import java.util.stream.Stream;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.builder.SpringApplicationBuilder;

import static org.springframework.boot.WebApplicationType.NONE;

/**
 * Galyleo application launcher.  Starts {@link Connect}
 * unless {@code --install} is specified in which case it dispatches to
 * {@link Install}.
 *
 * {@injected.fields}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@NoArgsConstructor @ToString @Log4j2
public class Launcher implements ApplicationRunner {

    /**
     * Standard {@link org.springframework.boot.SpringApplication}
     * {@code main(String[])}
     * entry point.
     *
     * @param   argv            The command line argument vector.
     *
     * @throws  Exception       If the function does not catch
     *                          {@link Exception}.
     */
    public static void main(String[] argv) throws Exception {
        new SpringApplicationBuilder(Launcher.class).web(NONE).run(argv);
    }

    @Value("${install:#{null}}")
    private Boolean install = null;

    @Value("${connection-file:#{null}}")
    private String connection_file = null;

    @Override
    public void run(ApplicationArguments arguments) throws Exception {
        if (install == null) {
            install = arguments.getOptionNames().contains("install");
        }

        if (install ^ connection_file != null) {
            Class<?> type = Connect.class;

            if (install) {
                type = Install.class;
            }

            new SpringApplicationBuilder(type)
                .web(NONE)
                .run(arguments.getSourceArgs());
        } else {
            throw new IllegalArgumentException("Exactly one of '--install' or '--connection-file' must be specified");
        }
    }
}
