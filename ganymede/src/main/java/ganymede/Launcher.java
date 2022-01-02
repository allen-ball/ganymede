package ganymede;
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
import ganymede.connect.Connect;
import ganymede.install.Install;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine;

import static org.springframework.boot.WebApplicationType.NONE;

/**
 * Ganymede application launcher.  Starts {@link Connect}
 * unless {@code --install} is specified in which case it dispatches to
 * {@link Install}.
 *
 * {@injected.fields}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@Command
@NoArgsConstructor @ToString @Log4j2
public class Launcher implements ApplicationRunner {

    /**
     * Standard {@link org.springframework.boot.SpringApplication}
     * {@code main(String[])}
     * entry point.
     *
     * @param   argv            The command line argument vector.
     *
     * @throws  Exception       If the function does not catch
     *                          {@link Exception}.
     */
    public static void main(String[] argv) throws Exception {
        new SpringApplicationBuilder(Launcher.class).web(NONE).run(argv);
    }

    @Option(description = { "Install Ganymede kernel" }, names = { "-i" })
    @Value("${install:#{null}}")
    private Boolean install = null;

    @Option(description = { "connection_file" }, names = { "-f" }, arity = "1")
    @Value("${connection-file:#{null}}")
    private String connection_file = null;

    @Override
    public void run(ApplicationArguments arguments) throws Exception {
        new CommandLine(this)
            .setCaseInsensitiveEnumValuesAllowed(true)
            .parseArgs(arguments.getNonOptionArgs().toArray(new String [] { }));

        if (install == null) {
            install = arguments.getOptionNames().contains("install");
        }

        if (install ^ connection_file != null) {
            Class<?> type = Connect.class;

            if (install) {
                type = Install.class;
            }

            var profile = type.getSimpleName().toLowerCase();

            new SpringApplicationBuilder(type)
                .profiles(profile)
                .run(arguments.getSourceArgs());
        } else {
            throw new IllegalArgumentException("Exactly one of '--install' or '--connection-file' must be specified");
        }
    }
}
