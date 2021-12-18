package ganymede.notebook;
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
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ganymede.util.ServiceProviderMap;
import java.util.Comparator;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.TreeMap;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * {@link Renderer} {@link java.util.Map}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
public class RendererMap extends TreeMap<Class<?>,Renderer> {
    private static final long serialVersionUID = -8180874215822796778L;

    private static final Comparator<Class<?>> COMPARATOR =
        new IsAssignableFromOrder().thenComparing(Class::getName);

    /** @serial */
    private final ServiceProviderMap<Renderer> map;

    /**
     * Sole constructor.
     */
    public RendererMap() {
        super(COMPARATOR);

        this.map = new ServiceProviderMap<>(Renderer.class, this::compute);
    }

    private Renderer compute(ServiceLoader.Provider<Renderer> provider) {
        var value = provider.get().instance().orElse(null);
        var key = (value != null) ? value.getRenderType() : null;

        return (key != null) ? computeIfAbsent(key, k -> configure(value)) : null;
    }

    private Renderer configure(Renderer renderer) {
        if (renderer != null) {
            renderer.configure(this);
        }

        return renderer;
    }

    /**
     * Reload the underlying {@link ServiceProviderMap} and add the
     * corresponding entries.
     *
     * @return  {@link.this}
     */
    public RendererMap reload() {
        map.reload();

        return this;
    }

    /**
     * Method to create a
     * {@link ganymede.server.Message#execute_result(int,ObjectNode) mime bundle}
     * and render an {@link Object} and any alternatives.
     *
     * @param   object          The {@link Object} to encode.
     * @param   alternates      Optional alternate representations.
     *
     * @return  The {@link ganymede.server.Message} {@code mime-bundle}.
     */
    public ObjectNode render(Object object, Object... alternates) {
        var bundle = new ObjectNode(JsonNodeFactory.instance);

        renderTo(bundle, object, alternates);

        return bundle;
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
