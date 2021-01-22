package galyleo.install;

import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Galyleo Jupyter {@link galyleo.kernel.Kernel} {@link Install}er.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@SpringBootApplication
@NoArgsConstructor @ToString @Log4j2
public class Install implements ApplicationRunner {
    @Override
    public void run(ApplicationArguments arguments) throws Exception {
        log.info("{}", arguments);
    }
}
