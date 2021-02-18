package galyleo.shell.magic;

import ball.annotation.ServiceProviderFor;
import galyleo.shell.Magic;
import java.util.stream.Stream;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

import static java.lang.ProcessBuilder.Redirect.PIPE;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * {@link Script} {@link galyleo.shell.Magic}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@ServiceProviderFor({ Magic.class })
@MagicNames({ "script", "!" })
@NoArgsConstructor @ToString @Log4j2
public class Script implements AnnotatedMagic {
    @Override
    public void execute(String magic, String code) throws Exception {
        var argv =
            Stream.of(Magic.getCellMagicCommand(magic))
            .skip(1)
            .toArray(String[]::new);

        var process =
            new ProcessBuilder(argv)
            .redirectInput(PIPE)
            .redirectErrorStream(true)
            .redirectOutput(PIPE)
            .start();

        try (var in = process.getInputStream()) {
            try (var out = process.getOutputStream()) {
                out.write(code.getBytes(UTF_8));
            }

            in.transferTo(System.out);

            int status = process.waitFor();
        }
    }
}
