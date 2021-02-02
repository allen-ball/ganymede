package galyleo.kernel;

import galyleo.server.Server;
import galyleo.shell.Shell;
import java.nio.file.Paths;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Named;
import javax.inject.Singleton;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Galyleo Jupyter {@link Kernel}.
 *
 * {@injected.fields}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@Named @Singleton
@SpringBootApplication
@NoArgsConstructor @ToString @Log4j2
public class Kernel extends Server implements ApplicationRunner {
    @Value("${connection-file:}")
    private List<String> paths = null;
    @Value("${spark-home:}")
    private String sparkHome = null;

    private final Shell shell = new Shell();

    @PostConstruct
    public void init() throws Exception {
        try {
            if (sparkHome != null) {
                shell.addJarsToClasspath(Paths.get(sparkHome, "jars").toString());
            }
        } catch (Exception exception) {
            log.warn("{}: {}", sparkHome, exception);
        }

        restart();
    }

    @PreDestroy
    public void destroy() { shutdown(); }

    @Override
    protected void restart() throws Exception {
        super.restart();

        shell.restart(getIn(), getOut(), getErr());

        setSession(String.join("-",
                               Kernel.class.getCanonicalName(),
                               String.valueOf(ProcessHandle.current().pid()),
                               String.valueOf(shell.restarts())));
    }

    @Override
    protected void execute(String code) throws Exception {
        shell.execute(code);
    }

    @Override
    protected String evaluate(String expression) throws Exception {
        return shell.evaluate(expression);
    }

    @Override
    protected void interrupt() {
        var shell = this.shell;

        if (shell != null) {
            shell.stop();
        }
    }

    @Override
    public void run(ApplicationArguments arguments) throws Exception {
        if (paths != null && (! paths.isEmpty())) {
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
