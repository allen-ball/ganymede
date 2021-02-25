package ganymede.server.renderer;

import ganymede.server.Renderer;

/**
 * {@link Renderer} annotation services.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
public interface AnnotatedRenderer extends Renderer {
    @Override
    default Class<?> getForType() {
        ForType annotation = getClass().getAnnotation(ForType.class);

        return (annotation != null) ? annotation.value() : null;
    }
}
