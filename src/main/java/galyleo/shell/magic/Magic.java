package galyleo.shell.magic;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Base64;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.Stream;
import jdk.jshell.JShell;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toMap;

/**
 * {@link Magic} service interface.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
public interface Magic {

    /**
     * Cell {@link Magic} indicator.
     */
    public static final String CELL = "%%";

    /**
     * The {@link Map} of {@link Magic} service providers.
     */
    public static final Map<String,Magic> MAP =
        ServiceLoader.load(Magic.class).stream()
        .map(ServiceLoader.Provider::get)
        .flatMap(v -> Stream.of(v.getNames()).map(k -> Map.entry(k, v)))
        .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

    /**
     * See {@link #decode(String)}.
     */
    public static final Base64.Decoder DECODER = Base64.getDecoder();

    /**
     * See {@link #encode(String)}.
     */
    public static final Base64.Encoder ENCODER = Base64.getEncoder();

    /**
     * Method to get the names associated with {@link.this} {@link Magic}.
     *
     * @return  The names.
     */
    public String[] getNames();

    /**
     * Entry-point method.  Executed in the {@link galyleo.shell.Shell}.
     *
     * @param   jshell          The {@link JShell}.
     * @param   in              The {@code in} {@link InputStream}.
     * @param   out             The {@code out} {@link PrintStream}.
     * @param   err             The {@code err} {@link PrintStream}.
     * @param   magic           The initial magic line.
     * @param   code            The remainder of the cell.
     */
    default void execute(JShell jshell,
                         InputStream in, PrintStream out, PrintStream err,
                         String magic, String code) throws Exception {
        sendTo(jshell, getNames()[0], magic, code);
    }

    /**
     * Implementation method.  Executed in the {@link JShell} instance.
     *
     * @param   magic           The initial magic line.
     * @param   code            The remainder of the cell.
     */
    public void execute(String magic, String code) throws Exception;

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
     * Static method to send a request to be executed in the {@link JShell}
     * instance.  The {@link #sendTo(JShell,String,String,String)} method
     * packs the arguments and creates a
     * {@link #receive(String,String,String)} expression which is evaluated
     * in the {@link JShell}.
     *
     * @param   jshell          The {@link JShell}.
     * @param   name            The magic name (key into {@link #MAP}.
     * @param   magic           The initial magic line.
     * @param   code            The remainder of the cell.
     */
    public static void sendTo(JShell jshell, String name, String magic, String code) throws Exception {
        evaluate(jshell,
                 String.format("%s.receive(\"%s\", \"%s\", \"%s\")",
                               Magic.class.getCanonicalName(),
                               name, encode(magic), encode(code)));
    }

    /**
     * Static method to receive a request in the {@link JShell} instance.
     * The {@link #sendTo(JShell,String,String,String)} method packs the
     * arguments and creates a {@link #receive(String,String,String)}
     * expression which is evaluated in the {@link JShell}.
     *
     * @param   name            The magic name (key into {@link #MAP}.
     * @param   magic           The initial magic line.
     * @param   code            The remainder of the cell.
     */
    public static void receive(String name, String magic, String code) throws Exception {
        if (MAP.containsKey(name)) {
            MAP.get(name).execute(decode(magic), decode(code));
        } else {
            throw new IllegalStateException("Magic " + name + " not found");
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
        return new String(DECODER.decode(string), UTF_8);
    }

    /**
     * Convenience method to {@link Base64}-encode a {@link String}.
     *
     * @param   string          The un-encoded {@link String}.
     *
     * @return  The encoded {@link String}.
     */
    public static String encode(String string) {
        return ENCODER.encodeToString(((string != null) ? string : "").getBytes(UTF_8));
    }

    /**
     * Method to evaluate an expression.
     *
     * @param   jshell          The {@link JShell}.
     * @param   expression      The expression to evaluate.
     *
     * @return  The result of evaluating the expression.
     */
    public static String evaluate(JShell jshell, String expression) throws Exception {
        var analyzer = jshell.sourceCodeAnalysis();
        var info = analyzer.analyzeCompletion(expression);

        if (! info.completeness().isComplete()) {
            throw new IllegalArgumentException(expression);
        }

        return jshell.eval(info.source()).get(0).value();
    }
}
