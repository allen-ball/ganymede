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
import com.fasterxml.jackson.databind.node.ObjectNode;
import ganymede.notebook.ForClass;
import ganymede.notebook.ForClassName;
import java.util.Base64;
import java.util.Optional;

/**
 * {@link ganymede.server.Message#mime_bundle(Object,Object...)} output
 * {@link Renderer}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
public interface Renderer {
    public static final String DATA = "data";
    public static final String METADATA = "metadata";

    /**
     * Singleton {@link RendererMap} instance.
     */
    public static final RendererMap MAP = new RendererMap();

    /**
     * {@link Base64.Encoder} instance.
     */
    public static final Base64.Encoder BASE64_ENCODER = Base64.getEncoder();

    /**
     * Method to get a configured instance of this {@link Renderer}.
     * Subclasses may override to return an instance that requires
     * additional resources and/or configuration.  Default implementaion
     * returns {@link.this}.
     *
     * @return  An {@link Optional} containing the configured instance.
     */
    public default Optional<? extends Renderer> instance() {
        return Optional.of(this);
    }

    /**
     * Default method to analyze {@link.this} instance for {@link ForClass}
     * and {@link ForClassName} annotations.  May return {@code null} if a
     * {@link Class} cannot be loaded.
     *
     * @return  The {@link Class type} the argument renders (may be
     *          {@code null}).
     */
    public default Class<?> getRenderType() {
        return getRenderType(getClass());
    }

    /**
     * Method to render an {@link Object} to a {@code mime-bundle}.
     *
     * @param   bundle          The {@link ganymede.server.Message}
     *                          {@code mime-bundle}.
     * @param   object          The {@link Object} to render.
     */
    public void renderTo(ObjectNode bundle, Object object);

    /**
     * Static method to analyze a class for {@link ForClass} and
     * {@link ForClassName} annotations.  May return {@code null} if a
     * {@link Class} cannot be loaded.
     *
     * @param   type            The {@link Class} to analyze.
     *
     * @return  The {@link Class type} the argument renders (may be
     *          {@code null}).
     */
    public static Class<?> getRenderType(Class<?> type) {
        Class<?> renderType = null;

        if (type != null) {
            try {
                if (type.isAnnotationPresent(ForClassName.class)) {
                    var name = type.getAnnotation(ForClassName.class).value();

                    renderType = Class.forName(name, false, type.getClassLoader());
                } else {
                    renderType = type.getAnnotation(ForClass.class).value();
                }
            } catch (Throwable throwable) {
            }

            if (renderType == null) {
                var superType = type.getSuperclass();

                if (superType != null && Renderer.class.isAssignableFrom(superType)) {
                    renderType = getRenderType(superType);
                }
            }
        }

        return renderType;
    }
}
