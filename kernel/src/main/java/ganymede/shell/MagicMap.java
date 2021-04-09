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
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import lombok.NoArgsConstructor;

/**
 * {@link Magic} {@link Map}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@NoArgsConstructor
public class MagicMap extends TreeMap<String,Magic> {
    private static final long serialVersionUID = 194836601918125315L;

    /** @serial */
    private final ServiceProviderMap<Magic> map =
        new ServiceProviderMap<>(Magic.class);

    { reload(); }

    /**
     * Reload the underlying {@link ServiceProviderMap} and add the
     * corresponding entries.
     *
     * @return  {@link.this}
     */
    public MagicMap reload() {
        map.reload().values().stream()
            .flatMap(v -> Stream.of(v.getMagicNames()).map(k -> Map.entry(k, v)))
            .forEach(t -> putIfAbsent(t.getKey(), t.getValue()));

        return this;
    }
}
