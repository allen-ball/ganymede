package ganymede.connect;

import ganymede.kernel.Kernel;
import java.io.File;
import java.util.Optional;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

import static ganymede.server.Server.OBJECT_MAPPER;
import static org.springframework.boot.WebApplicationType.NONE;
import static org.springframework.boot.WebApplicationType.SERVLET;

/**
 * Ganymede Jupyter {@link ganymede.kernel.Kernel} {@link Connect} CLI.
 * Parses {@code --connection-file} to look for a running
 * {@link Kernel} and connects if one is found; otherwise starts a new
 * {@link Kernel} instance.
 *
 * {@injected.fields}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@SpringBootApplication
@NoArgsConstructor @ToString @Log4j2
public class Connect implements ApplicationRunner {
    @Value("${JPY_PARENT_PID:#{-1}}")
    private long jpy_parent_pid = -1;

    @Value("${connection-file:#{null}}")
    private String connection_file = null;

    @Value("${start-web-server:#{null}}")
    private Boolean start_web_server = null;

    @Override
    public void run(ApplicationArguments arguments) throws Exception {
        if (start_web_server == null) {
            start_web_server = arguments.getOptionNames().contains("start-web-server");
        }

        try {
            var file = new File(connection_file);
            var node = OBJECT_MAPPER.readTree(file);
            var isAlive =
                Optional.of("pid")
                .filter(t -> node.hasNonNull(t))
                .map(t -> node.get(t).asLong())
                .flatMap(ProcessHandle::of)
                .map(ProcessHandle::isAlive)
                .orElse(false);

            if (! isAlive) {
                new SpringApplicationBuilder(Kernel.class)
                    .web(start_web_server ? SERVLET : NONE)
                    .run(arguments.getSourceArgs());
            } else {
                log.warn("Kernel already running");
            }
        } catch (Exception exception) {
            log.warn("{}", connection_file, exception);
        }
    }
}
