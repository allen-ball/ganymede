package galyleo.kernel;

import galyleo.shell.jshell.ExecutionEvents;
import galyleo.shell.jshell.StaticImports;
import java.nio.file.Paths;
import java.util.stream.Stream;
import org.springframework.boot.system.ApplicationHome;
import com.fasterxml.jackson.databind.node.ArrayNode;
import galyleo.server.Server;
import galyleo.shell.Shell;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Named;
import javax.inject.Singleton;
import jdk.jshell.JShell;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;
import static java.util.stream.Collectors.toSet;
import static jdk.jshell.Snippet.Status.REJECTED;

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
        shell.addToClasspath(new ApplicationHome(getClass()).getSource().toString());

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
        initialize(shell.jshell());

        setSession(String.join("-",
                               Kernel.class.getCanonicalName(),
                               String.valueOf(ProcessHandle.current().pid()),
                               String.valueOf(shell.restarts())));
    }

    private void initialize(JShell jshell) throws Exception {
        var analyzer = jshell.sourceCodeAnalysis();
        var imports =
            Stream.of(StaticImports.class.getDeclaredMethods())
            .filter(t -> isPublic(t.getModifiers()) && isStatic(t.getModifiers()))
            .map(t -> String.format("import static %s.%s;",
                                    t.getDeclaringClass().getName(), t.getName()))
            .map(t -> analyzer.analyzeCompletion(t))
            .collect(toSet());

        for (var analysis : imports) {
            jshell.eval(analysis.source())
                .stream()
                .filter(t -> t.status().equals(REJECTED))
                .forEach(t -> log.warn("{}: {}",
                                       t.status(), t.snippet().source()));
        }
    }

    @Override
    protected void execute(String code) throws Exception {
        shell.execute(code);
    }

    @Override
    protected ArrayNode getExecutionEvents() {
        var node = ExecutionEvents.get();

        node.addAll(ExecutionEvents.get(shell));

        return node;
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
