package ganymede.shell.magic;

import ganymede.shell.Magic;

/**
 * {@link ganymede.shell.Magic} annotation services.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
public interface AnnotatedMagic extends Magic {
    @Override
    default String[] getMagicNames() {
        MagicNames annotation = getClass().getAnnotation(MagicNames.class);
        String[] value = (annotation != null) ? annotation.value() : null;

        if (value == null || value.length == 0) {
            value = new String[] { getClass().getSimpleName().toLowerCase() };
        }

        return value;
    }

    @Override
    default String getDescription() {
        Description annotation = getClass().getAnnotation(Description.class);
        String value = (annotation != null) ? annotation.value() : null;

        if (value == null) {
            value = "Description not available";
        }

        return value;
    }
}
