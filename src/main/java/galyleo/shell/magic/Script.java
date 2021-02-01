package galyleo.shell.magic;

import ball.annotation.ServiceProviderFor;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

import static java.lang.ProcessBuilder.Redirect.PIPE;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * {@link Script} {@link Magic}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@ServiceProviderFor({ Magic.class })
@NoArgsConstructor @ToString @Log4j2
public class Script extends AbstractMagic {
    @Override
    public void execute(String magic, String code) throws Exception {
        var command = magic.substring(CELL.length()).trim();
        var pair = command.split("\\W+", 2);

        if (pair.length != 2) {
            throw new IllegalArgumentException(magic);
        }

        command = pair[1];

        var process =
            new ProcessBuilder(command.split("\\W+"))
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
