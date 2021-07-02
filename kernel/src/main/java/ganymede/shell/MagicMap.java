package ganymede.shell;
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
import ganymede.util.ServiceProviderMap;
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
    private static final long serialVersionUID = 6930042409603983399L;

    /** @serial */ private final ServiceProviderMap<Magic> map = new ServiceProviderMap<>(Magic.class, null);
    /** @serial */ private final Consumer<Magic> initializer;

    /**
     * Sole constructor.
     *
     * @param   initializer     The {@link Consumer} to apply to newly
     *                          allocated {@link Magic} providers.
     */
    public MagicMap(Consumer<Magic> initializer) {
        this.initializer = initializer;
    }

    /**
     * Reload the underlying {@link ServiceProviderMap} and add the
     * corresponding entries.
     *
     * @return  {@link.this}
     */
    public MagicMap reload() {
        var iterator = map.reload().values().iterator();

        while (iterator.hasNext()) {
            var magic = iterator.next();

            try {
                var keys = Set.of(magic.getMagicNames());

                if (! keySet().containsAll(keys)) {
                    if (initializer != null) {
                        initializer.accept(magic);
                    }
                }

                for (var key : keys) {
                    computeIfAbsent(key, k -> magic);
                }
            } catch (Exception exception) {
            }
        }

        return this;
    }
}
