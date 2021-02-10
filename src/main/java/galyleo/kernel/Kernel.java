package galyleo.kernel;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import galyleo.server.Message;
import galyleo.server.Server;
import galyleo.shell.Shell;
import galyleo.shell.jshell.ExecutionEvents;
import galyleo.shell.jshell.StaticImports;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import jdk.jshell.JShell;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.boot.web.servlet.context.ServletWebServerInitializedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;

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
@SpringBootApplication
@NoArgsConstructor @ToString @Log4j2
public class Kernel extends Server implements ApplicationContextAware,
                                              ApplicationRunner {
    @Value("${JPY_PARENT_PID:#{-1}}")
    private long jpy_parent_pid = -1;

    @Value("${connection-file:#{null}}")
    private String connection_file = null;

    @Value("${spark-home:#{null}}")
    private String spark_home = null;

    @Value("${hadoop-home:#{null}}")
    private String hadoop_home = null;

    private final Shell shell = new Shell();
    private ApplicationContext context = null;
    private int port = -1;

    @PostConstruct
    public void init() throws Exception {
        shell.addToClasspath(new ApplicationHome(getClass()).getSource());

        try {
            if (spark_home != null) {
                shell.addJarsToClasspath(Paths.get(spark_home, "jars"));
            }
        } catch (Exception exception) {
            log.warn("{}: {}", spark_home, exception);
        }

        try {
            if (hadoop_home != null && (! Objects.equals(hadoop_home, spark_home))) {
                shell.addJarsToClasspath(Paths.get(hadoop_home, "jars"));
            }
        } catch (Exception exception) {
            log.warn("{}: {}", hadoop_home, exception);
        }

        restart();
    }

    @PreDestroy
    public void destroy() { super.shutdown(); }

    @Override
    public void setApplicationContext(ApplicationContext context) {
        this.context = context;
    }

    @EventListener({ ServletWebServerInitializedEvent.class })
    public void onApplicationEvent(ServletWebServerInitializedEvent event) {
        port = event.getWebServer().getPort();
    }

    @EventListener({ ContextClosedEvent.class })
    public void onApplicationEvent(ContextClosedEvent event) {
        super.shutdown();
    }

    @Override
    protected void bind(String id, File file) throws IOException {
        super.bind(id, file);

        var node = (ObjectNode) OBJECT_MAPPER.readTree(file);

        node.put("pid", ProcessHandle.current().pid());

        if (port > 0) {
            node.put("port", port);
        }

        OBJECT_MAPPER.writeValue(file, node);

        file.deleteOnExit();
    }

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
    protected Message.completeness isComplete(String code) throws Exception {
        return shell.isComplete(code);
    }

    @Override
    protected void interrupt() {
        var shell = this.shell;

        if (shell != null) {
            shell.stop();
        }
    }

    @Override
    public void shutdown() {
        ((ConfigurableApplicationContext) context).close();
    }

    @Override
    public void run(ApplicationArguments arguments) throws Exception {
        try {
            if (connection_file != null) {
                bind(connection_file);
            }
        } catch (Exception exception) {
            log.warn("{}", exception);
        }
    }
}
