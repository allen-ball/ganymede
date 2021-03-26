package ganymede.shell.magic;

import ball.annotation.ServiceProviderFor;
import ganymede.server.renderer.ForType;
import ganymede.shell.Magic;
import java.util.stream.Stream;
import javax.script.ScriptContext;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

/**
 * {@link HTML} template {@link ganymede.shell.Magic}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@ServiceProviderFor({ Magic.class })
@Description("HTML template evaluator")
@NoArgsConstructor @ToString @Log4j2
public class HTML extends Thymeleaf {
    @Override
    protected void execute(ScriptContext context,
                           String[] argv, String code) throws Exception {
        argv =
            Stream.of(new String[] { super.getMagicNames()[0] }, argv)
            .flatMap(Stream::of)
            .toArray(String[]::new);

        super.execute(context, argv, code);
    }
}
