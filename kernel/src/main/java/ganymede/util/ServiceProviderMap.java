package ganymede.util;
/*-
 * ##########################################################################
 * Ganymede
 * %%
 * Copyright (C) 2021 Allen D. Ball
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ##########################################################################
 */
import java.util.Comparator;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * {@link ServiceLoader Service} {@link java.util.Map}.
 *
 * @param       <T>             The service type.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
public class ServiceProviderMap<T> extends TreeMap<Class<? extends T>,T> {
    private static final long serialVersionUID = 5423712518719767597L;

    private static final Comparator<Class<?>> COMPARATOR = Comparator.comparing(Class::getName);

    /** @serial */ private final Class<T> service;
    /** @serial */ private final ServiceLoader<T> loader;
    /** @serial */ private Function<ServiceLoader.Provider<T>,T> compute;

    /**
     * Sole constructor.
     *
     * @param   service         The service {@link Class type}.
     * @param   compute         The {@link Function} to obtain a value
     *                          instance from a
     *                          {@link ServiceLoader.Provider}.
     */
    public ServiceProviderMap(Class<T> service, Function<ServiceLoader.Provider<T>,T> compute) {
        super(COMPARATOR);

        this.service = service;
        this.loader = ServiceLoader.load(service, service.getClassLoader());
        this.compute = Objects.requireNonNullElse(compute, this::compute);
    }

    private T compute(ServiceLoader.Provider<T> provider) {
        return provider.get();
    }

    /**
     * Method to get the {@link ServiceLoader}'s {@link ClassLoader}.
     *
     * @return  The {@link ServiceLoader}'s {@link ClassLoader}.
     */
    public ClassLoader getClassLoader() { return service.getClassLoader(); }

    /**
     * Reload {@link ServiceLoader} and {@link #put(Object,Object)} newly
     * discovered entries.
     *
     * @return  {@link.this}
     */
    public ServiceProviderMap<T> reload() {
        loader.reload();

        var iterator = loader.stream().iterator();

        while (iterator.hasNext()) {
            var provider = iterator.next();

            try {
                computeIfAbsent(provider.type(), k -> compute.apply(provider));
            } catch (ServiceConfigurationError error) {
            }
        }

        return this;
    }
}
