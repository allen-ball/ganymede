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
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.Base64;
import java.util.LinkedList;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.Data;

import static java.io.StreamTokenizer.TT_EOF;
import static java.io.StreamTokenizer.TT_EOL;
import static java.io.StreamTokenizer.TT_WORD;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toMap;

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
     * See {@link #decode(String)}.
     */
    public static final Base64.Decoder BASE64_DECODER = Base64.getDecoder();

    /**
     * See {@link #encode(String)}.
     */
    public static final Base64.Encoder BASE64_ENCODER = Base64.getEncoder();

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
     * Entry-point method.  Executed in the {@link ganymede.shell.Shell}.
     * Default implementation sends to the
     * {@link #execute(NotebookContext,String,String)} method in the
     * {@link jdk.jshell.JShell} instance.
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
        sendTo(shell, getMagicNames()[0], line0, code);
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
     * Implementation method.  Executed in the {@link jdk.jshell.JShell}
     * instance.
     *
     * @param   __              The {@link NotebookContext}.
     * @param   line0           The initial magic line.
     * @param   code            The remainder of the cell.
     */
    public void execute(NotebookContext __, String line0, String code) throws Exception;

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
        var list = new LinkedList<String>();

        try (var reader = new StringReader(code)) {
            var tokenizer = new StreamTokenizer(reader);

            tokenizer.eolIsSignificant(true);
            tokenizer.lowerCaseMode(false);
            tokenizer.commentChar('#');
            tokenizer.wordChars('0', '9');

            for (int i = 0; i < CELL.length(); i += 1) {
                tokenizer.nextToken();

                if (tokenizer.ttype != CELL.charAt(i)) {
                    throw new IllegalArgumentException();
                }
            }

            tokenizer.nextToken();

            switch (tokenizer.ttype) {
            case TT_WORD:
                list.add(tokenizer.sval);
                break;

            default:
                list.add(Character.toString(tokenizer.ttype));
                break;

            case TT_EOF:
            case TT_EOL:
                throw new IllegalArgumentException();
                /* break; */
            }

            IntStream.range(0, 256)
                .filter(t -> "\"\\#".indexOf(t) == -1)
                .filter(t -> (! Character.isWhitespace(t)))
                .forEach(t -> tokenizer.wordChars(t, t));

            for (;;) {
                tokenizer.nextToken();

                if (tokenizer.ttype == TT_EOF || tokenizer.ttype == TT_EOL) {
                    break;
                }

                switch (tokenizer.ttype) {
                case TT_WORD:
                    list.add(tokenizer.sval);
                    break;

                default:
                    list.add(Character.toString(tokenizer.ttype));
                    break;
                }
            }
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException(code.split("\\R", 2)[0]);
        }

        return list.toArray(new String[] { });
    }

    /**
     * Static method to send a request to be executed in the
     * {@link jdk.jshell.JShell} instance.  The
     * {@link #sendTo(Shell,String,String,String)} method packs the
     * arguments and creates a
     * {@link #receive(String,NotebookContext,String,String)} expression which
     * is evaluated in the {@link jdk.jshell.JShell}.
     *
     * @param   shell           The {@link Shell}.
     * @param   name            The magic name.
     * @param   line0           The initial magic line.
     * @param   code            The remainder of the cell.
     */
    public static void sendTo(Shell shell, String name, String line0, String code) throws Exception {
        var expression =
            String.format("__.invokeStaticMethod(\"%s\", \"%s\", new Class<?>[] { String.class, ganymede.notebook.NotebookContext.class, String.class, String.class }, \"%s\", __, \"%s\", \"%s\")",
                          Magic.class.getName(), "receive",
                          name, encode(line0), encode(code));

        shell.evaluate(expression);
    }

    /**
     * Static {@link MagicMap} instance used by
     * {@link #receive(String,NotebookContext,String,String)}.
     */
    public static MagicMap MAP = new MagicMap();

    /**
     * Static method to receive a request in the {@link jdk.jshell.JShell}
     * instance.  The {@link #sendTo(Shell,String,String,String)} method
     * packs the arguments and creates a
     * {@link #receive(String,NotebookContext,String,String)} expression which
     * is evaluated in the {@link jdk.jshell.JShell}.
     *
     * @param   name            The magic name.
     * @param   __              The {@link NotebookContext}.
     * @param   line0           The initial magic line.
     * @param   code            The remainder of the cell.
     */
    public static void receive(String name, NotebookContext __,
                               String line0, String code) throws Exception {
        MAP.reload();

        if (MAP.containsKey(name)) {
            MAP.get(name).execute(__, decode(line0), decode(code));
        } else {
            throw new IllegalStateException("Magic '" + name + "' not found");
        }
    }

    /**
     * Convenience method to {@link Base64}-decode a {@link String}.
     *
     * @param   string          The encoded {@link String}.
     *
     * @return  The decoded {@link String}.
     */
    public static String decode(String string) {
        return new String(BASE64_DECODER.decode(string), UTF_8);
    }

    /**
     * Convenience method to {@link Base64}-encode a {@link String}.
     *
     * @param   string          The un-encoded {@link String}.
     *
     * @return  The encoded {@link String}.
     */
    public static String encode(String string) {
        return BASE64_ENCODER.encodeToString(((string != null) ? string : "").getBytes(UTF_8));
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
