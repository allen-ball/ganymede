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
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Base64;

import static ganymede.server.Server.OBJECT_MAPPER;

/**
 * {@link Message#mime_bundle(Object,Object...)} output {@link Renderer}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
public interface Renderer {
    public static final String DATA = "data";
    public static final String METADATA = "metadata";

    /**
     * {@link Base64.Encoder} instance.
     */
    public static final Base64.Encoder BASE64_ENCODER = Base64.getEncoder();

    /**
     * Method to render an {@link Object} to a {@code mime-bundle}.
     *
     * @param   bundle          The {@link Message} {@code mime-bundle}.
     * @param   object          The {@link Object} to render.
     */
    public void renderTo(ObjectNode bundle, Object object);

    /**
     * Static {@link RendererMap} instance used by
     * {@link #render(Object,Object...)}.
     */
    public static RendererMap MAP = new RendererMap();

    /**
     * Method to render an {@link Object} to an
     * {@link Message#execute_result(int,ObjectNode)}.
     *
     * @param   object          The {@link Object} to encode.
     * @param   alternates      Optional alternate representations.
     *
     * @return  The {@link Message} {@code mime-bundle}.
     */
    public static ObjectNode render(Object object, Object... alternates) {
        var bundle = OBJECT_MAPPER.createObjectNode();

        MAP.renderTo(bundle, object, alternates);

        return bundle;
    }
}
