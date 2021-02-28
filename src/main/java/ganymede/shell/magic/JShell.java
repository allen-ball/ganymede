package ganymede.shell.magic;

import javax.script.Bindings;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

/**
 * Abstract base class for {@link jdk.jshell.JShell}
 * {@link ganymede.shell.Magic}s.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@NoArgsConstructor(access = PROTECTED)
public abstract class JShell extends AbstractMagic {
    @Override
    public void execute(Bindings bindings,
                        String magic, String code) throws Exception {
        throw new IllegalArgumentException(magic);
    }
}
