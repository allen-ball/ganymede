package ganymede.connect;
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
import ganymede.kernel.Kernel;
import ganymede.util.ObjectMappers;
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
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine;

/**
 * Ganymede Jupyter {@link ganymede.kernel.Kernel} {@link Connect} CLI.
 * Parses {@code --connection-file} to look for a running
 * {@link Kernel} and connects if one is found; otherwise starts a new
 * {@link Kernel} instance.
 *
 * {@injected.fields}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@SpringBootApplication
@Command
@NoArgsConstructor @ToString @Log4j2
public class Connect implements ApplicationRunner {
    @Option(description = { "connection_file" }, names = { "-f" }, arity = "1")
    @Value("${connection-file:#{null}}")
    private String connection_file = null;

    @Override
    public void run(ApplicationArguments arguments) throws Exception {
        new CommandLine(this)
            .setCaseInsensitiveEnumValuesAllowed(true)
            .parseArgs(arguments.getNonOptionArgs().toArray(new String [] { }));

        try {
            var file = new File(connection_file);
            var node = ObjectMappers.JSON.readTree(file);
            var isAlive =
                Optional.of("pid")
                .filter(t -> node.hasNonNull(t))
                .map(t -> node.get(t).asLong())
                .flatMap(ProcessHandle::of)
                .map(ProcessHandle::isAlive)
                .orElse(false);

            if (! isAlive) {
                var type = Kernel.class;
                var profile = type.getSimpleName().toLowerCase();

                new SpringApplicationBuilder(type)
                    .profiles(profile)
                    .run(arguments.getSourceArgs());
            } else {
                log.warn("Kernel already running");
            }
        } catch (Exception exception) {
            log.warn("{}", connection_file, exception);
        }
    }
}
