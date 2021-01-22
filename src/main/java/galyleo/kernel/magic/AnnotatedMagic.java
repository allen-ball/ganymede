package galyleo.kernel.magic;

/**
 * {@link Magic} annotation services.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
public interface AnnotatedMagic extends Magic {
    @Override
    default String getName() {
        Name annotation = getClass().getAnnotation(Name.class);

        return (annotation != null) ? annotation.value() : getClass().getSimpleName();
    }
}
