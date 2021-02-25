package galyleo.server;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Base64;

import static galyleo.server.Server.OBJECT_MAPPER;

/**
 * {@link Message#mime_bundle(Object)} output {@link Renderer}.
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
     * Method to get the {@link Class type} {@link.this} {@link Renderer}
     * provides.
     *
     * @return  The array of {@link Class type}.
     */
    public Class<?> getForType();

    /**
     * Method to render an {@link Object} to a {@code mime-bundle}.
     *
     * @param   bundle          The {@link Message} {@code mime-bundle}.
     * @param   object          The {@link Object} to render (assignable to
     *                          {@link #getForType()}.
     */
    public void renderTo(ObjectNode bundle, Object object);

    /**
     * Method to render an {@link Object} to
     * {@link Message#execute_result(int,ObjectNode)}.
     *
     * @param   object          The {@link Object} to encode.
     *
     * @return  The {@link Message} {@code mime-bundle}.
     */
    public static ObjectNode render(Object object) {
        var bundle = OBJECT_MAPPER.createObjectNode();
        var type = (object != null) ? object.getClass() : null;

        new RendererMap().entrySet().stream()
            .filter(t -> t.getKey().isAssignableFrom(type))
            .forEach(t -> t.getValue().renderTo(bundle, object));

        return bundle;
    }
}
