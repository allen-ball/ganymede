package galyleo.util;

import java.util.Comparator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.TreeMap;

import static java.util.stream.Collectors.toList;

/**
 * {@link ServiceLoader Service} {@link java.util.Map}.
 *
 * @param       <T>             The service type.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
public class ServiceProviderMap<T> extends TreeMap<Class<? extends T>,T> {
    private static final long serialVersionUID = 3185134891562929754L;

    /** @serial */ private final ServiceLoader<T> loader;

    /**
     * Sole constructor.
     *
     * @param   service         The service {@link Class type}.
     */
    public ServiceProviderMap(Class<T> service) {
        super(Comparator.comparing(Class::getName));

        loader = ServiceLoader.load(service, service.getClassLoader());

        reload();
    }

    /**
     * Reload {@link ServiceLoader} and {@link #put(Object,Object)} newly
     * discovered entries.
     *
     * @return  {@link.this}
     */
    public ServiceProviderMap<T> reload() {
        loader.reload();

        var providers =
            loader.stream()
            .filter(t -> (! containsKey(t.type())))
            .collect(toList());

        for (var provider : providers) {
            try {
                put(provider.type(), provider.get());
            } catch (ServiceConfigurationError error) {
            }
        }

        return this;
    }
}
