package ganymede.install;
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
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.system.ApplicationHome;

import static ganymede.server.Server.JSON_OBJECT_MAPPER;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;
import static org.springframework.util.FileCopyUtils.copy;
import static org.springframework.util.FileCopyUtils.copyToByteArray;
import static org.springframework.util.FileSystemUtils.deleteRecursively;

/**
 * Ganymede Jupyter {@link ganymede.kernel.Kernel} {@link Install}er.
 *
 * {@injected.fields}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@SpringBootApplication
@NoArgsConstructor @ToString @Log4j2
public class Install implements ApplicationRunner {
    @Value("${id-prefix}")              private String id_prefix = null;
    @Value("${id}")                     private String id = null;
    @Value("${id-suffix}")              private String id_suffix = null;
    @Value("${display-name-prefix}")    private String display_name_prefix = null;
    @Value("${display-name}")           private String display_name = null;
    @Value("${display-name-suffix}")    private String display_name_suffix = null;
    @Value("${env:}")                   private List<String> envvars = null;
    @Value("${copy-jar:true}")          private boolean copy_jar = true;

    @Override
    public void run(ApplicationArguments arguments) throws Exception {
        id =
            Stream.of(id_prefix, id, id_suffix)
            .map(String::strip)
            .filter(t -> (! t.isEmpty()))
            .collect(joining("-"));

        display_name =
            Stream.of(display_name_prefix, display_name, display_name_suffix)
            .map(String::strip)
            .filter(t -> (! t.isEmpty()))
            .collect(joining(" "));

        var sysPrefix = false;

        for (var argument : arguments.getSourceArgs()) {
            if (argument.equals("--sys-prefix")) {
                sysPrefix = true;
            } else if (argument.equals("--user")) {
                sysPrefix = false;
            }
        }

        var sysProperties = new LinkedHashMap<String,Object>();
        var tmp = Files.createTempDirectory(getClass().getPackage().getName() + "-");

        try {
            /*
             * java
             */
            var java = ProcessHandle.current().info().command().orElse("java");
            /*
             * kernel.jar
             */
            var jarPath = new ApplicationHome(getClass()).getSource().toPath();
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
             * jupyter --runtime-dir
             */
            var runtime_dir = getOutputAsString(jupyter, "--runtime-dir");
            /*
             * Maven local repository
             */
            if (sysPrefix) {
                var repository =
                    Paths.get(paths.at("/data").get(1).asText(), "repository");

                sysProperties.put("maven.repo.local", repository);

                try {
                    Files.createDirectories(repository);
                } catch (Exception exception) {
                    log.warn("{}: Could not create", repository, exception);
                }
            }
            /*
             * kernelspec
             */
            var kernelspec = tmp.resolve(id);

            Files.createDirectory(kernelspec);
            /*
             * kernel.json
             */
            var kernel = JSON_OBJECT_MAPPER.createObjectNode();
            var argv = kernel.withArray("argv");
            var jar = jarPath.toAbsolutePath().toString();

            if (copy_jar) {
                var name = "kernel.jar";

                copy(jarPath.toFile(), kernelspec.resolve(name).toFile());

                var prefix = paths.at("/data").get(sysPrefix ? 1 : 0).asText();

                jar = Paths.get(prefix, "kernels", id, name).toString();
            }

            Stream.of(Stream.of(java,
                                "--add-opens", "java.base/jdk.internal.misc=ALL-UNNAMED",
                                "--illegal-access=permit",
                                "-Djava.awt.headless=true",
                                "-Djdk.disableLastUsageTracking=true"),
                      sysProperties.entrySet().stream().map(t -> "-D" + t),
                      Stream.of("-jar", jar,
                                "--runtime-dir=" + runtime_dir,
                                "--connection-file={connection_file}"))
                .flatMap(Function.identity())
                .map(Object::toString)
                .forEach(argv::add);

            kernel.put("display_name", display_name);

            var env = kernel.with("env");

            for (var envvar : envvars) {
                var pair = envvar.split("=", 2);

                env.put(pair[0], (pair.length > 1) ? pair[1] : "");
            }

            kernel.put("interrupt_mode", "message");
            kernel.put("language", "java");

            JSON_OBJECT_MAPPER.writeValue(kernelspec.resolve("kernel.json").toFile(), kernel);
            /*
             * logo-16x16.png and logo-32x32.png
             */
            for (var png : List.of("16x16.png", "32x32.png")) {
                var from = "/static/images/ball-java-jar-" + png;
                var to = kernelspec.resolve("logo-" + png).toFile();

                try (var in = getClass().getResourceAsStream(from)) {
                    copy(copyToByteArray(in), to);
                } catch (Exception exception) {
                    log.warn("Could not copy resource {} to {}",
                             from, to, exception);
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
            log.fatal("{}", exception.getMessage(), exception);
        } finally {
            deleteRecursively(tmp);
        }
    }

    private String which(String command) throws Exception {
        return getOutputAsString("which", command);
    }

    private String getOutputAsString(String... argv) throws Exception {
        var bytes = new byte[] { };
        var process =
            new ProcessBuilder(argv)
            .inheritIO()
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start();

        try (var in = process.getInputStream()) {
            bytes = in.readAllBytes();

            var status = process.waitFor();

            if (status != 0) {
                throw new IOException("Cannot read output of " + List.of(argv));
            }
        }

        return new String(bytes, UTF_8).trim();
    }

    private JsonNode getOutputAsJson(String... argv) throws Exception {
        var node = JSON_OBJECT_MAPPER.nullNode();
        var process =
            new ProcessBuilder(argv)
            .inheritIO()
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start();

        try (var in = process.getInputStream()) {
            node = JSON_OBJECT_MAPPER.readTree(in);

            var status = process.waitFor();

            if (status != 0) {
                throw new IOException("Cannot read output of " + List.of(argv));
            }
        }

        return node;
    }
}
