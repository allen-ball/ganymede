package galyleo.server.renderer;

import galyleo.server.Renderer;

/**
 * {@link Renderer} annotation services.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
public interface AnnotatedRenderer extends Renderer {
    @Override
    default String getMimeType() {
        MimeType annotation = getClass().getAnnotation(MimeType.class);

        return (annotation != null) ? annotation.value() : null;
    }

    @Override
    default Class<?> getForType() {
        ForType annotation = getClass().getAnnotation(ForType.class);

        return (annotation != null) ? annotation.value() : null;
    }
}
