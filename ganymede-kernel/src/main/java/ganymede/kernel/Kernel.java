package ganymede.kernel;
/*-
 * ##########################################################################
 * Ganymede
 * %%
 * Copyright (C) 2021, 2022 Allen D. Ball
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ganymede.notebook.Magic;
import ganymede.server.Server;
import ganymede.shell.Shell;
import ganymede.util.ObjectMappers;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
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
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine;

import static java.util.stream.Collectors.toList;

/**
 * Ganymede Jupyter {@link Kernel}.
 *
 * {@injected.fields}
 *
 * {@bean.info}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@SpringBootApplication @RestController
@Command
@NoArgsConstructor @ToString @Log4j2
public class Kernel extends Server implements KernelApi, ApplicationContextAware, ApplicationRunner {
    private static final String JSE_HELP_LINK_TEXT_FORMAT = "Java SE %1$s & JDK %1$s";
    private static final String JSE_HELP_LINK_URL_FORMAT = "https://docs.oracle.com/en/java/javase/%1$s/docs/api/";

    @Option(description = { "connection_file" }, names = { "-f" }, arity = "1")
    @Value("${connection-file:#{null}}")
    private String connection_file = null;

    @Value("${spark-home:#{null}}")
    private String spark_home = null;

    @Value("${hive-home:#{null}}")
    private String hive_home = null;

    @Value("${kernel.version}")
    private String kernel_version = null;

    @Value("classpath:kernel_info_reply.yml")
    private Resource kernel_info_reply = null;

    private final Shell shell = new Shell(this);
    private ApplicationContext context = null;
    private int port = -1;
    private ObjectNode kernel_info_reply_content = null;

    /**
     * Method to get the {@link Kernel} REST server port.
     *
     * @return  The port.
     */
    public int getPort() { return port; }

    @PostConstruct
    public void init() throws Exception {
        try (var in = kernel_info_reply.getInputStream()) {
            kernel_info_reply_content = (ObjectNode) ObjectMappers.YAML.readTree(in).with("content");
        } catch (Exception exception) {
            log.warn("{}", exception);
        }

        var jse_help_link = new ObjectNode(JsonNodeFactory.instance);
        var version = System.getProperty("java.specification.version");

        jse_help_link.put("text", String.format(JSE_HELP_LINK_TEXT_FORMAT, version));
        jse_help_link.put("url", String.format(JSE_HELP_LINK_URL_FORMAT, version));

        kernel_info_reply_content.put("protocol_version", PROTOCOL_VERSION.toString());
        kernel_info_reply_content.withArray("help_links").add(jse_help_link);

        if (spark_home != null) {
            var parent = Paths.get(spark_home, "jars").toFile();

            shell.addKnownDependenciesToClasspath(parent);
        }

        if (hive_home != null && (! Objects.equals(hive_home, spark_home))) {
            var parent = Paths.get(hive_home, "lib").toFile();

            shell.addKnownDependenciesToClasspath(parent);
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
    protected void bind(File file) throws IOException {
        super.bind(file);

        var node = (ObjectNode) ObjectMappers.JSON.readTree(file);

        node.put("pid", ProcessHandle.current().pid());

        if (port > 0) {
            node.put("port", port);
        }

        ObjectMappers.JSON.writeValue(file, node);
    }

    @Override
    protected void restart() throws Exception {
        super.restart();

        shell.restart(getIn(), getOut(), getErr());

        setKernelSessionId(UUID.randomUUID());
    }

    @Override
    protected ObjectNode getKernelInfo() { return kernel_info_reply_content; }

    @Override
    protected void execute(String code) throws Exception {
        shell.execute(code);
    }

    @Override
    protected String evaluate(String expression) throws Exception {
        return shell.evaluate(expression);
    }

    @Override
    protected Magic.completeness isComplete(String code) throws Exception {
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
    public ResponseEntity<UUID> kernelId() {
        return new ResponseEntity<>(getKernelId(), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<JsonNode> getExecuteRequest() {
        return new ResponseEntity<>(request.asObjectNode(), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> display(JsonNode body) {
        var request = this.request;

        if (request != null) {
            var silent = request.content().at("/silent").asBoolean();

            if (! silent) {
                pub(request.display_data(body.deepCopy()));
            }
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> print(JsonNode body) {
        var request = this.request;

        if (request != null) {
            var silent = request.content().at("/silent").asBoolean();

            if (! silent) {
                pub(request.execute_result(execution_count.intValue(), body.deepCopy()));
            }
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<String>> classpath() {
        var list = shell.classpath().stream().map(File::getAbsolutePath).collect(toList());

        return new ResponseEntity<>(list, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<String>> imports() {
        var list = shell.imports().stream().collect(toList());

        return new ResponseEntity<>(list, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Map<String,String>> variables() {
        return new ResponseEntity<>(shell.variables(), HttpStatus.OK);
    }

    @Override
    public void run(ApplicationArguments arguments) throws Exception {
        new CommandLine(this)
            .setCaseInsensitiveEnumValuesAllowed(true)
            .parseArgs(arguments.getNonOptionArgs().toArray(new String [] { }));

        try {
            if (connection_file != null) {
                bind(connection_file);
            }
        } catch (Exception exception) {
            log.warn("{}", exception);
        }
    }
}
