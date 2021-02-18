package galyleo.server;

import galyleo.util.ServiceProviderMap;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * {@link Renderer} {@link Map}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
public class RendererMap extends TreeMap<Class<?>,Renderer> {
    private static final long serialVersionUID = -6534802176270830152L;

    private static final Comparator<Class<?>> COMPARATOR =
        new IsAssignableFromOrder().thenComparing(Class::getName);

    /** @serial */
    private final ServiceProviderMap<Renderer> map =
        new ServiceProviderMap<>(Renderer.class);

    /**
     * Sole constructor.
     */
    public RendererMap() {
        super(COMPARATOR);

        reload();
    }

    /**
     * Reload the underlying {@link ServiceProviderMap} and add the
     * corresponding entries.
     *
     * @return  {@link.this}
     */
    public RendererMap reload() {
        map.reload().values().stream()
            .forEach(t -> putIfAbsent(t.getForType(), t));

        return this;
    }

    @NoArgsConstructor @ToString
    private static class IsAssignableFromOrder implements Comparator<Class<?>> {
        @Override
        public int compare(Class<?> left, Class<?> right) {
            boolean ordered = right.isAssignableFrom(left);
            boolean reversed = left.isAssignableFrom(right);

            return (ordered ^ reversed) ? (ordered ? -1 : 1) : 0;
        }
    }
}
