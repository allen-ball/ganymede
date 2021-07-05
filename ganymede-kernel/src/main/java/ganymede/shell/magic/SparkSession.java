package ganymede.shell.magic;
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
import ball.annotation.ServiceProviderFor;
import ganymede.shell.Magic;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.apache.spark.SparkConf;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;

import static org.apache.spark.sql.SparkSession.builder;
import static org.apache.spark.sql.SparkSession.getDefaultSession;
import static org.apache.spark.sql.SparkSession.setActiveSession;
import static org.apache.spark.sql.SparkSession.setDefaultSession;

/**
 * {@link SparkSession} {@link Magic}.
 *
 * @see org.apache.spark.sql.SparkSession
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@ServiceProviderFor({ Magic.class })
@MagicNames({ "spark-session" })
@Description("Configure and start a Spark session")
@NoArgsConstructor @ToString @Log4j2
public class SparkSession extends AbstractPropertiesMagic {
    @Override
    public void execute(String line0, String code) throws Exception {
        try {
            var argv = Magic.getCellMagicCommand(line0);
            var arguments = new Arguments();

            parse(argv, arguments);

            var config = new SparkConf();

            compile(code).entrySet().stream()
                .forEach(t -> config.set(t.getKey().toString(),
                                         String.valueOf(t.getValue())));

            if (arguments.getMaster() != null) {
                config.setMaster(arguments.getMaster());
            }

            if (arguments.getAppName() != null) {
                config.setAppName(arguments.getAppName());
            }

            var builder = builder().config(config);

            if (arguments.isEnableHiveIfAvailable()) {
                try {
                    builder.enableHiveSupport();
                } catch (Exception exception) {
                    System.err.println(exception.getMessage());
                }
            }

            var session = builder.getOrCreate();

            if (getDefaultSession().isEmpty()) {
                setDefaultSession(session);
            }

            setActiveSession(session);

            context.display(session);
        } catch (ParameterException exception) {
            System.err.println(exception.getMessage());
            System.err.println();
            exception.getCommandLine().usage(System.err);
        } catch (NoClassDefFoundError error) {
            System.err.format("Apache Spark not found on classpath: %s:\n", error);
        } catch (Throwable throwable) {
            throwable.printStackTrace(System.err);
        }
    }

    @Override
    public String getUsage() { return getUsage(new Arguments()); }

    @Command @Data
    private class Arguments {
        @Parameters(description = { "Spark master" },
                    index = "0", arity = "0..1")
        private String master = null;

        @Parameters(description = { "Spark appName" },
                    index = "1", arity = "0..1", defaultValue = "root")
        private String appName = null;

        @Option(description = { "Enable Hive if available.  true by default" },
                names = "--no-enable-hive-if-available", negatable = true)
        private boolean enableHiveIfAvailable = true;
    }
}
