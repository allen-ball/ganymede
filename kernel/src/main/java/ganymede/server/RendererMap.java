package ganymede.server;
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
import com.fasterxml.jackson.databind.node.ObjectNode;
import ganymede.server.renderer.ForClass;
import ganymede.server.renderer.ForClassName;
import ganymede.server.renderer.RequiredClassNames;
import ganymede.util.ServiceProviderMap;
import java.util.Comparator;
import java.util.Optional;
import java.util.TreeMap;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * {@link Renderer} {@link java.util.Map}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
public class RendererMap extends TreeMap<Class<?>,Renderer> {
    private static final long serialVersionUID = 5660512908511151101L;

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
        var iterator = map.reload(this::accept).values().iterator();

        while (iterator.hasNext()) {
            var renderer = iterator.next();

            try {
                var key = getRenderType(renderer.getClass());

                computeIfAbsent(key, k -> renderer);
            } catch (Exception exception) {
            }
        }

        return this;
    }

    private boolean accept(Class<?> provider) {
        boolean accept = true;

        try {
            getRenderType(provider);
        } catch (Exception exception) {
            accept = false;
        }

        return accept;
    }

    private Class<?> getRenderType(Class<?> provider) throws Exception {
        Class<?> type = null;

        if (provider.isAnnotationPresent(RequiredClassNames.class)) {
            var names = provider.getAnnotation(RequiredClassNames.class).value();

            if (names != null) {
                for (var name : names) {
                    Class.forName(name, false, map.getClassLoader());
                }
            }
        }

        if (provider.isAnnotationPresent(ForClassName.class)) {
            var name = provider.getAnnotation(ForClassName.class).value();

            type = Class.forName(name, false, map.getClassLoader());
        } else {
            type = provider.getAnnotation(ForClass.class).value();
        }

        return type;
    }

    /**
     * Method to render an {@link Object} to a {@code mime-bundle}.
     *
     * @param   bundle          The {@code mime-bundle}.
     * @param   object          The {@link Object} to render.
     * @param   alternates      Optional alternate representations.
     */
    public void renderTo(ObjectNode bundle, Object object, Object... alternates) {
        var type = (object != null) ? object.getClass() : Object.class;

        reload().find(type).ifPresent(t -> t.renderTo(bundle, object));

        if (alternates != null) {
            for (var alternate : alternates) {
                if (alternate != null) {
                    find(alternate.getClass())
                        .ifPresent(t -> t.renderTo(bundle, alternate));
                }
            }
        }
    }

    private Optional<Renderer> find(Class<?> type) {
        var value =
            entrySet().stream()
            .filter(t -> t.getKey().isAssignableFrom(type))
            .map(t -> t.getValue())
            .findFirst();

        return value;
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
