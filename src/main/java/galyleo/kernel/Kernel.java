package galyleo.kernel;

import galyleo.server.Server;
import galyleo.shell.Java;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Named;
import javax.inject.Singleton;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Galyleo Jupyter {@link Kernel}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@Named @Singleton
@SpringBootApplication
@NoArgsConstructor @ToString @Log4j2
public class Kernel extends Server implements ApplicationRunner {
    private final Java java = new Java();

    @PostConstruct
    public void init() throws Exception { restart(); }

    @PreDestroy
    public void destroy() { shutdown(); }

    @Override
    protected void restart() throws Exception {
        super.restart();

        java.restart(getIn(), getOut(), getErr());

        setSession(String.join("-",
                               Kernel.class.getCanonicalName(),
                               String.valueOf(ProcessHandle.current().pid()),
                               String.valueOf(java.restarts())));
    }

    @Override
    protected void execute(String code) throws Exception {
        java.execute(code);
    }

    @Override
    protected String evaluate(String expression) throws Exception {
        return java.evaluate(expression);
    }

    @Override
    protected void interrupt() {
        var java = this.java;

        if (java != null) {
            java.stop();
        }
    }

    @Override
    public void run(ApplicationArguments arguments) throws Exception {
        var paths = arguments.getOptionValues("connection-file");

        if (! (paths == null || paths.isEmpty())) {
            for (var path : paths) {
                try {
                    bind(path);
                } catch (Exception exception) {
                    log.warn("{}", exception);
                }
            }
        } else {
            throw new IllegalArgumentException("No connection file specified");
        }
    }
}
