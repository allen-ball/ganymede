package ganymede.server;
/*-
 * ##########################################################################
 * Ganymede
 * $Id$
 * $HeadURL$
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
        var iterator = map.reload().values().iterator();

        while (iterator.hasNext()) {
            var renderer = iterator.next();

            try {
                putIfAbsent(renderer.getForType(), renderer);
            } catch (TypeNotPresentException exception) {
                iterator.remove();
            }
        }

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
