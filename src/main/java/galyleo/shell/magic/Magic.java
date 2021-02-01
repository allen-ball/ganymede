package galyleo.shell.magic;

import galyleo.shell.Java;
import java.util.Base64;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toMap;

/**
 * {@link Magic} service interface.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
public interface Magic {
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
     * Method to get the names associated with {@link.this} {@link Magic}.
     *
     * @return  The names.
     */
    public String[] getNames();

    /**
     * Implementation method.  Executed in the {@link Java} {@code JShell}.
     *
     * @param   magic           The initial magic line.
     * @param   code            The remainder of the cell.
     */
    public void execute(String magic, String code) throws Exception;

    /**
     * Method to determine if the code is cell magic (starts with
     * '{@code %%}'.
     *
     * @param   code            The code to test.
     *
     * @return  {@code true} if cell magic; {@code false} otherwise.
     */
    public static boolean isCellMagic(String code) {
        return code.startsWith(CELL);
    }

    /**
     * Method to request execution in the {@link Java} {@code JShell}.
     *
     * @param   java            The target {@link Java} shell.
     * @param   code            The cell code.
     */
    public static void execute(Java java, String code) throws Exception {
        if (! isCellMagic(code)) {
            throw new IllegalStateException("Code is not magic");
        }

        var encoder = Base64.getEncoder();
        var expression =
            String.format("%s.execute(\"%s\")",
                          Magic.class.getCanonicalName(),
                          encoder.encodeToString(code.getBytes(UTF_8)));

        java.execute(expression);
    }

    /**
     * Target method to execute in the {@link Java} {@code JShell}.
     *
     * @param   base64          The cell code (Base64-encoded).
     */
    public static void execute(String base64) throws Exception {
        var code = new String(Base64.getDecoder().decode(base64), UTF_8);

        if (! isCellMagic(code)) {
            throw new IllegalStateException("Code is not magic");
        }

        var pair = code.split("\\R", 2);
        var magic = pair[0];
        var rest = (pair.length > 1) ? pair[1] : "";
        var name = magic.substring(CELL.length()).split("\\W+", 2)[0];

        if (MAP.containsKey(name)) {
            MAP.get(name).execute(magic, rest);
        } else {
            throw new IllegalArgumentException(magic);
        }
    }
}
