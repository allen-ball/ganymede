package ganymede.kernel;
/*-
 * ##########################################################################
 * Ganymede
 * %%
 * Copyright (C) 2021 Allen D. Ball
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ##########################################################################
 */
import com.fasterxml.jackson.databind.node.ObjectNode;
import ganymede.server.Message;
import ganymede.server.Server;
import ganymede.shell.Shell;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.context.ServletWebServerInitializedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

/**
 * Ganymede Jupyter {@link Kernel}.
 *
 * {@injected.fields}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@SpringBootApplication
@RestController
@RequestMapping(value = { "/" },
                consumes = APPLICATION_JSON_VALUE,
                produces = APPLICATION_JSON_VALUE)
@NoArgsConstructor @ToString @Log4j2
public class Kernel extends Server implements ApplicationContextAware,
                                              ApplicationRunner {

    /**
     * The name of the {@link System} property containing the
     * {@link Kernel}'s REST server port.
     */
    public static final String PORT_PROPERTY = "kernel.port";

    @Value("${JPY_PARENT_PID:#{-1}}")
    private long jpy_parent_pid = -1;

    @Value("${runtime-dir:#{null}}")
    private String runtime_dir = null;

    @Value("${connection-file:#{null}}")
    private String connection_file = null;

    @Value("${spark-home:#{null}}")
    private String spark_home = null;

    @Value("${project.version}")
    private String project_version = null;

    private final Shell shell = new Shell(this);
    private ApplicationContext context = null;
    private int port = -1;
    private JupyterRestClient client = null;

    /**
     * Method to get the {@link Kernel} REST server port.
     *
     * @return  The port.
     */
    public int getPort() { return port; }

    @PostConstruct
    public void init() throws Exception {
        if (runtime_dir != null) {
            var glob = String.format("{nb,jp}server-%d.json", jpy_parent_pid);

            try (var stream = Files.newDirectoryStream(Paths.get(runtime_dir), glob)) {
                var path = stream.iterator().next();

                client = new JupyterRestClient(OBJECT_MAPPER.readTree(path.toFile()));
            } catch (NoSuchElementException exception) {
                log.warn("{}: No match found for '{}'", runtime_dir, glob);
            } catch (Exception exception) {
                log.warn("{}: {}", runtime_dir, exception);
            }
        }

        if (spark_home != null) {
            var parent = Paths.get(spark_home, "jars").toFile();

            shell.addKnownDependenciesToClasspath(parent);
        }

        restart();
    }

    @PreDestroy
    public void destroy() { super.shutdown(); }

    /**
     * REST method to retrieve the current {@link Shell#classpath()}.
     */
    @RequestMapping(method = { GET }, value = { "kernel/classpath" })
    public ResponseEntity<String> classpath() throws Exception {
        var json = OBJECT_MAPPER.writeValueAsString(shell.classpath());

        return new ResponseEntity<>(json, HttpStatus.OK);
    }

    /**
     * REST method to display MIME bundles from a sub-process.  See
     * {@link KernelRestClient#display(JsonNode)}.
     *
     * @param   bundle          The MIME bundle {@link ObjectNode}.
     */
    @RequestMapping(method = { PUT }, value = { "kernel/display" })
    public ResponseEntity<String> display(@RequestBody ObjectNode bundle) {
        var request = this.request;

        if (request != null) {
            var silent = request.content().at("/silent").asBoolean();

            if (! silent) {
                pub(request.display_data(bundle));
            }
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * REST method to print MIME bundles from a sub-process.  See
     * {@link KernelRestClient#print(JsonNode)}.
     *
     * @param   bundle          The MIME bundle {@link ObjectNode}.
     */
    @RequestMapping(method = { PUT }, value = { "kernel/print" })
    public ResponseEntity<String> print(@RequestBody ObjectNode bundle) {
        var request = this.request;

        if (request != null) {
            var silent = request.content().at("/silent").asBoolean();

            if (! silent) {
                pub(request.execute_result(execution_count.intValue(), bundle));
            }
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

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
    protected void bind(String kernelId, File file) throws IOException {
        super.bind(kernelId, file);

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

        setSessionId(UUID.randomUUID().toString());
    }

    @Override
    protected ObjectNode getKernelInfo() {
        var content = OBJECT_MAPPER.createObjectNode();

        content.put("protocol_version", PROTOCOL_VERSION);
        content.put("implementation", "ganymede");
        content.put("implementation_version", project_version);

        var language_info = content.with("language_info");

        language_info.put("name", "java");
        language_info.put("version", System.getProperty("java.specification.version"));
        language_info.put("mimetype", "text/x-java");
        language_info.put("file_extension", ".java");

        var help_links = content.with("help_links");

        return content;
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
    public void shutdown() { SpringApplication.exit(context, () -> 0); }

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
