package galyleo.shell.magic;

/**
 * {@link Magic} annotation services.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
public interface AnnotatedMagic extends Magic {
    @Override
    default String[] getNames() {
        Names annotation = getClass().getAnnotation(Names.class);
        String[] value = (annotation != null) ? annotation.value() : null;

        if (value == null || value.length == 0) {
            value = new String[] { getClass().getSimpleName().toLowerCase() };
        }

        return value;
    }
}
