package galyleo;

import java.util.concurrent.TimeUnit;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Galyleo {@link SpringBootApplication} launcher.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@SpringBootApplication
@NoArgsConstructor @ToString @Log4j2
public class Launcher implements ApplicationRunner {

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
        var application = new SpringApplication(Launcher.class);

        application.run(argv);
    }

    @Autowired private ConnectionService service = null;

    @Override
    public void run(ApplicationArguments arguments) throws Exception {
        if (arguments.getNonOptionArgs().isEmpty()) {
            throw new IllegalArgumentException("No connection file specified");
        }

        var connection =
            service.newConnection(arguments.getNonOptionArgs().get(0));
        var kernel = new Kernel(connection);

        kernel.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
}
