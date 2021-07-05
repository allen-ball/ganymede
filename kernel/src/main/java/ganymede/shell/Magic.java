package ganymede.shell;
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
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.Data;
import org.codehaus.plexus.util.cli.CommandLineUtils;

/**
 * {@link Magic} service interface.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
public interface Magic {

    /**
     * Cell {@link Magic} indicator.
     */
    public static final String CELL = "%%";

    /**
     * Special-case {@link Magic} name.
     */
    public static final String BANG = "!";

    /**
     * Method to get a configured instance of this {@link Magic}.
     * Subclasses may override to return an instance that requires
     * additional resources and/or configuration.  Default implementaion
     * returns {@link.this}.
     *
     * @return  An {@link Optional} containing the configured instance.
     */
    public default Optional<? extends Magic> instance() {
        return Optional.of(this);
    }

    /**
     * Method to get the names associated with {@link.this} {@link Magic}.
     *
     * @return  The names.
     */
    public String[] getMagicNames();

    /**
     * Method to get the description of {@link.this} {@link Magic}.
     *
     * @return  The description.
     */
    public String getDescription();

    /**
     * Method to get the usage of {@link.this} {@link Magic}.
     *
     * @return  The usage.
     */
    public String getUsage();

    /**
     * {@link ganymede.shell.Shell} execution method.  Default
     * implementation sends to the {@link #execute(String,String)} method in
     * the {@link jdk.jshell.JShell} instance.
     *
     * @param   shell           The {@link Shell}.
     * @param   in              The {@code in} {@link InputStream}.
     * @param   out             The {@code out} {@link PrintStream}.
     * @param   err             The {@code err} {@link PrintStream}.
     * @param   line0           The initial magic line.
     * @param   code            The remainder of the cell.
     */
    default void execute(Shell shell,
                         InputStream in, PrintStream out, PrintStream err,
                         String line0, String code) throws Exception {
        NotebookContext.magic(shell, getMagicNames()[0], line0, code);
    }

    /**
     * Method to determine code's {@link Message.completeness completeness}.
     *
     * @param   line0           The initial magic line.
     * @param   code            The code to execute.
     *
     * @return  The code's {@link Message.completeness completeness}.
     */
    public Message.completeness isComplete(String line0, String code);

    /**
     * {@link jdk.jshell.JShell} configuation method.
     *
     * @param   context         The {@link NotebookContext}.
     */
    public void configure(NotebookContext context);

    /**
     * {@link jdk.jshell.JShell} execution method.
     *
     * @param   line0           The initial magic line.
     * @param   code            The remainder of the cell.
     */
    public void execute(String line0, String code) throws Exception;

    /**
     * Method to determine if the code is cell magic (starts with
     * '{@code %%}').
     *
     * @param   code            The code to test.
     *
     * @return  {@code true} if cell magic; {@code false} otherwise.
     */
    public static boolean isCellMagic(String code) {
        return code.startsWith(CELL);
    }

    /**
     * Method to parse a cell magic command.  The code must start with
     * "{@code %%}".  "{@code #}" to the EOL is a comment.  The command is
     * either a word or a single punctuation character.  Words may be quoted
     * with double quotes.
     *
     * @param   code            The code to parse.
     *
     * @return  The array of command arguments.
     *
     * @throws  IllegalArgumentException
     *                          If the magic line can't be parsed.
     */
    public static String[] getCellMagicCommand(String code) {
        var line0 = code.split("\\R", 2)[0];
        var argv = new String[] { line0 };

        try {
            argv = CommandLineUtils.translateCommandline(argv[0]);

            if (argv[0].startsWith(CELL)) {
                argv[0] = argv[0].substring(CELL.length()).trim();

                if (argv[0].isBlank()) {
                    argv = Stream.of(argv).skip(1).toArray(String[]::new);
                }

                if (argv[0].startsWith(BANG) && argv[0].length() > BANG.length()) {
                    argv =
                        Stream.concat(Stream.of(BANG, argv[0].substring(BANG.length())),
                                      Stream.of(argv).skip(1))
                        .toArray(String[]::new);
                }
            } else {
                throw new IllegalArgumentException();
            }
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException(line0);
        }

        return argv;
    }

    /**
     * {@link Magic} {@link Application Application} instance to parse code,
     * find {@link Magic}, and execute.
     *
     * {@bean.info}
     */
    @Data
    public static class Application {
        private final String line0;
        private final String code;
        private final String[] argv;

        /**
         * Sole public constructor.
         *
         * @param       code    The complete cell code.
         */
        public Application(String code) {
            this(isCellMagic(code) ? code.split("\\R", 2) : new String[] { null, code });
        }

        private Application(String[] lines) {
            this(lines[0], (lines.length > 1) ? lines[1] : "");
        }

        private Application(String line0, String code) {
            this.line0 = line0;
            this.code = code;
            this.argv = (line0 != null) ? getCellMagicCommand(line0) : new String[] { };
        }

        /**
         * Method to determine if the code has a "magic" line.
         *
         * @return      {@code true} or {@code false}.
         */
        public boolean hasMagicLine() { return (line0 != null); }

        /**
         * Method to get the specified {@link Magic} name.
         *
         * @return      The specified {@link Magic} name; {@code null} if
         *              none is specified.
         */
        public String getMagicName() {
            return (argv.length > 0) ? argv[0] : null;
        }

        /**
         * Method to invoke the {@link Magic}.
         *
         * @param       magic   The {@link Magic} to apply.
         * @param       shell   The {@link Shell}.
         * @param       in      The {@code in} {@link InputStream}.
         * @param       out     The {@code out} {@link PrintStream}.
         * @param       err     The {@code err} {@link PrintStream}.
         */
        public void apply(Magic magic, Shell shell, InputStream in, PrintStream out, PrintStream err) throws Exception {
            magic.execute(shell, in, out, err, getLine0(), getCode());
        }
    }
}
