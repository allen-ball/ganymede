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
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.TreeMap;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toList;

/**
 * {@link ServiceLoader Service} {@link java.util.Map}.
 *
 * @param       <T>             The service type.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
public class ServiceProviderMap<T> extends TreeMap<Class<? extends T>,T> {
    private static final long serialVersionUID = 1150156295503292563L;

    /** @serial */ private final Class<T> service;
    /** @serial */ private final ServiceLoader<T> loader;

    /**
     * Sole constructor.
     *
     * @param   service         The service {@link Class type}.
     */
    public ServiceProviderMap(Class<T> service) {
        super(Comparator.comparing(Class::getName));

        this.service = service;
        this.loader = ServiceLoader.load(service, service.getClassLoader());

        reload();
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
    public ServiceProviderMap<T> reload() { return reload(t -> true); }

    /**
     * Reload {@link ServiceLoader} and {@link #put(Object,Object)} newly
     * discovered entries.  A {@link Predicate} may be supplied to test a
     * provider {@link Class} before attempting to load and might be used to
     * test that other required {@link Class}es are loaded before attempting
     * to load.
     *
     * @param   predicate       A {@link Predicate} to test whether or not
     *                          the provider {@link Class} should be
     *                          loaded.
     *
     * @return  {@link.this}
     */
    public ServiceProviderMap<T> reload(Predicate<Class<?>> predicate) {
        loader.reload();

        var providers = loader.stream().collect(toList());

        for (var provider : providers) {
            try {
                var type = provider.type();

                if (predicate.test(type)) {
                    computeIfAbsent(type, k -> provider.get());
                }
            } catch (ServiceConfigurationError error) {
            }
        }

        return this;
    }
}
