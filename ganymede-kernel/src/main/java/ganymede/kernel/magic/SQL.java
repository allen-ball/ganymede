package ganymede.kernel.magic;
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
import ball.annotation.ServiceProviderFor;
import ganymede.notebook.AbstractMagic;
import ganymede.notebook.Description;
import ganymede.notebook.Magic;
import java.util.Collections;
import java.util.stream.Stream;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;

/**
 * {@link SQL} {@link Magic}.
 *
 * @see ganymede.notebook.NotebookContext#sql
 * @see DSLContext
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@ServiceProviderFor({ Magic.class })
@Description("Execute code in SQL REPL")
@NoArgsConstructor @ToString @Log4j2
public class SQL extends AbstractMagic {
    private DSLContext dsl = null;

    @Override
    public void execute(String line0, String code, JsonNode metadata) throws Exception {
        try {
            if (dsl != null && (! context.sql.values().contains(dsl))) {
                dsl = null;
            }

            var argv = Magic.getCellMagicCommand(line0);
            var arguments = new Arguments();

            parse(argv, arguments);

            if (arguments.getUrl() != null) {
                dsl = arguments.dsl();
            }

            if (! code.isBlank()) {
                if (dsl == null) {
                    dsl = arguments.dsl();
                }

                context.sql.queries.clear();
                context.sql.results.clear();

                var queries = dsl.parser().parse(code);

                Collections.addAll(context.sql.queries, queries.queries());

                for (var query : queries) {
                    var result = dsl.fetch(query.getSQL());

                    context.sql.results.add(result);

                    if (arguments.isPrint()) {
                        context.print(result);
                    }
                }
            } else {
                if (! context.sql.isEmpty()) {
                    context.sql.keySet().forEach(System.out::println);
                } else {
                    System.out.println("No JDBC connections have been established.");
                }
            }
        } catch (ParameterException exception) {
            System.err.println(exception.getMessage());
            System.err.println();
            exception.getCommandLine().usage(System.err);
        } catch (DataAccessException exception) {
            System.err.println(exception.getMessage());
        } catch (Exception exception) {
            exception.printStackTrace(System.err);
        }
    }

    @Override
    public String getUsage() { return getUsage(new Arguments()); }

    @Command @Data
    private class Arguments {
        @Parameters(description = { "JDBC Connection URL" },
                    index = "0", arity = "0..1")
        private String url = null;

        @Parameters(description = { "JDBC Connection Username" },
                    index = "1", arity = "0..1", defaultValue = "root")
        private String username = null;

        @Parameters(description = { "JDBC Connection Password" },
                    index = "2", arity = "0..1")
        private String password = null;

        @Option(names = { "--no-print" }, negatable = true,
                description = { "Print query results.  true by default" })
        private boolean print = true;

        public DSLContext dsl() {
            return context.sql.connect(getUrl(), getUsername(), getPassword());
        }
    }
}
