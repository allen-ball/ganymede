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
import ganymede.notebook.NotebookContext;
import ganymede.server.Message;
import ganymede.shell.Shell;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.stream.Stream;
import lombok.NoArgsConstructor;
import org.springframework.util.PropertyPlaceholderHelper;
import picocli.CommandLine;
import picocli.CommandLine.ParseResult;

import static lombok.AccessLevel.PROTECTED;

/**
 * Abstract base class for {@link ganymede.shell.Magic}s.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@NoArgsConstructor(access = PROTECTED)
public abstract class AbstractMagic implements AnnotatedMagic {
    /** Value received from {@link #configure(NotebookContext)}. */
    protected NotebookContext context = null;

    /**
     * Common static {@link PropertyPlaceholderHelper}.
     */
    protected static final PropertyPlaceholderHelper HELPER =
        new PropertyPlaceholderHelper("${", "}", ":", true);

    @Override
    public void execute(Shell shell,
                        InputStream in, PrintStream out, PrintStream err,
                        String line0, String code) throws Exception {
        AnnotatedMagic.super.execute(shell, in, out, err, line0, code);
    }

    @Override
    public Message.completeness isComplete(String line0, String code) {
        return Message.completeness.complete;
    }

    @Override
    public void configure(NotebookContext context) { this.context = context; }

    /**
     * Method to parse {@code argv} value to an annotated command
     * {@link Object}.
     *
     * @param   argv            The argument vector (array of
     *                          {@link String}s).
     * @param   command         The annotated command {@link Object}.
     *
     * @return  The {@link ParseResult}.
     *
     * @see CommandLine
     * @see CommandLine.Command
     * @see CommandLine.Option
     * @see CommandLine.Parameters
     */
    protected <T> ParseResult parse(String[] argv, T command) {
        var name = (String) null;

        if (argv.length > 0) {
            name = argv[0];
            argv = Stream.of(argv).skip(1).toArray(String[]::new);
        }

        var line =
            new CommandLine(command)
            .setCommandName(name)
            .setCaseInsensitiveEnumValuesAllowed(true)
            .setUnmatchedArgumentsAllowed(false);
        var result = line.parseArgs(argv);

        return result;
    }

    /**
     * Method to get usage message from an annotated command {@link Object}.
     *
     * @param   command         The annotated command {@link Object}.
     *
     * @return  The usage message.
     *
     * @see CommandLine
     * @see CommandLine.Command
     * @see CommandLine.Option
     * @see CommandLine.Parameters
     */
    protected String getUsage(Object command) {
        var line =
            new CommandLine(command)
            .setCommandName(getMagicNames()[0])
            .setCaseInsensitiveEnumValuesAllowed(true)
            .setUnmatchedArgumentsAllowed(false);

        return line.getUsageMessage();
    }
}
