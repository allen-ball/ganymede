package ganymede.notebook;
/*-
 * ##########################################################################
 * Ganymede
 * %%
 * Copyright (C) 2021, 2022 Allen D. Ball
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
import ganymede.util.ServiceProviderMap;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * {@link Magic} {@link java.util.Map}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
public class MagicMap extends TreeMap<String,Magic> {
    private static final long serialVersionUID = -5870351830510141326L;

    /** @serial */ private final Consumer<Magic> initializer;
    /** @serial */ private final ServiceProviderMap<Magic> map;

    /**
     * Sole constructor.
     *
     * @param   subtype         The {@link Magic} subtype.
     * @param   initializer     The {@link Consumer} to apply to newly
     *                          allocated {@link Magic} providers.
     */
    public MagicMap(Class<? extends Magic> subtype, Consumer<Magic> initializer) {
        this.initializer = initializer;
        this.map = new ServiceProviderMap<>(subtype.asSubclass(Magic.class), this::compute);
    }

    private Magic compute(ServiceLoader.Provider<Magic> provider) {
        var value = provider.get().instance().orElse(null);

        if (value != null) {
            if (initializer != null) {
                initializer.accept(value);
            }

            Set.of(value.getMagicNames()).stream()
                .forEach(t -> computeIfAbsent(t, k -> value));
        }

        return value;
    }

    /**
     * Reload the underlying {@link ServiceProviderMap} and add the
     * corresponding entries.
     *
     * @return  {@link.this}
     */
    public MagicMap reload() {
        map.reload();

        return this;
    }
}
