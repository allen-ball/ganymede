package galyleo.install;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.system.ApplicationHome;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.util.FileCopyUtils.copy;
import static org.springframework.util.FileCopyUtils.copyToByteArray;
import static org.springframework.util.FileSystemUtils.deleteRecursively;

/**
 * Galyleo Jupyter {@link galyleo.kernel.Kernel} {@link Install}er.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@SpringBootApplication
@NoArgsConstructor @ToString @Log4j2
public class Install implements ApplicationRunner {
    private final ObjectMapper mapper =
        new ObjectMapper()
        .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_VALUES)
        .enable(SerializationFeature.INDENT_OUTPUT);

    @Value("${id:galyleo-${project.version}-java-${java.version}}")
    private String id = null;
    @Value("${display-name:Galyleo ${project.version} (Java ${java.version})}")
    private String display_name = null;
    @Value("${env:}")
    private List<String> envvars = null;
    @Value("${copy-jar:true}")
    private boolean copyJar = true;

    @Override
    public void run(ApplicationArguments arguments) throws Exception {
        var sysPrefix = false;

        for (var argument : arguments.getSourceArgs()) {
            if (argument.equals("--sys-prefix")) {
                sysPrefix = true;
            } else if (argument.equals("--user")) {
                sysPrefix = false;
            }
        }

        var tmp = Files.createTempDirectory(getClass().getPackage().getName() + "-");

        try {
            /*
             * java
             */
            var java = ProcessHandle.current().info().command().orElse("java");
            /*
             * kernel.jar
             */
            var jarPath = new ApplicationHome().getSource().toPath();
            /*
             * which python
             */
            var python = which("python");
            /*
             * which jupyter
             */
            var jupyter = which("jupyter");
            /*
             * jupyter --paths --json
             */
            var paths = getOutputAsJson(jupyter, "--paths", "--json");
            /*
             * kernelspec
             */
            var kernelspec = tmp.resolve(id);

            Files.createDirectory(kernelspec);
            /*
             * kernel.json
             */
            var kernel = mapper.createObjectNode();
            var argv = kernel.withArray("argv");
            var jar = jarPath.toAbsolutePath().toString();
            if (copyJar) {
                copy(jarPath.toFile(),
                     kernelspec.resolve(jarPath.getFileName()).toFile());

                jar =
                    Paths.get(paths.at("/data").get(sysPrefix ? 1 : 0).asText(),
                              "kernels", id, jarPath.getFileName().toString())
                    .toString();
            }

            Stream.of(java, "-Dmode=kernel",
                      "-jar", jar,
                      "--connection-file={connection_file}")
                .map(Object::toString)
                .forEach(t -> argv.add(t));

            kernel.put("display_name", display_name);

            var env = kernel.with("env");

            for (var envvar : envvars) {
                var pair = envvar.split("=", 2);

                env.put(pair[0], (pair.length > 1) ? pair[1] : "");
            }

            kernel.put("interrupt_mode", "message");
            kernel.put("language", "java");

            mapper.writeValue(kernelspec.resolve("kernel.json").toFile(), kernel);
            /*
             * logo-16x16.png and logo-32x32.png
             */
            for (var png : List.of("16x16.png", "32x32.png")) {
                var path = "/static/images/ball-java-jar-" + png;

                try (var in = getClass().getResourceAsStream(path)) {
                    var bytes = copyToByteArray(in);

                    copy(bytes, kernelspec.resolve("logo-" + png).toFile());
                } catch (Exception exception) {
                    log.warn("Could not copy resource: {}", path, exception);
                }
            }
            /*
             * jupyter kernelspec install <kernelspec>
             */
            new ProcessBuilder(jupyter, "kernelspec", "install", kernelspec.toString(),
                               sysPrefix ? "--sys-prefix" : "--user",
                               "--replace")
                .inheritIO()
                .start().waitFor();
        } catch (Exception exception) {
            log.warn("{}", exception.getMessage(), exception);
        } finally {
            deleteRecursively(tmp);
        }
    }

    private String which(String command) throws Exception {
        var bytes = new byte[] { };
        var process =
            new ProcessBuilder("which", command)
            .inheritIO()
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start();

        try (var in = process.getInputStream()) {
            bytes = in.readAllBytes();

            var status = process.waitFor();

            if (status != 0) {
                throw new IOException("Cannot locate '" + command + "'");
            }
        }

        return new String(bytes, UTF_8).trim();
    }

    private JsonNode getOutputAsJson(String... argv) throws Exception {
        var node = mapper.nullNode();
        var process =
            new ProcessBuilder(argv)
            .inheritIO()
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start();

        try (var in = process.getInputStream()) {
            node = mapper.readTree(in);
        }

        return node;
    }
}
