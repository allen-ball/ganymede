package galyleo.server;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.stream.Stream;

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
     * Method to get the mime-type {@link.this} {@link Renderer} provides.
     *
     * @return  The {@code mime-type}.
     */
    public String getMimeType();

    /**
     * Method to get the {@link Class type} {@link.this} {@link Renderer} provides.
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
     * @param   __              The {@link ClassLoader}.
     * @param   object          The {@link Object} to encode.
     *
     * @return  The {@link Message} {@code mime-bundle}.
     */
    public static ObjectNode render(ClassLoader __, Object object) {
        var list = new ArrayList<Renderer>();
        var loader = ServiceLoader.load(Renderer.class, __);

        loader.reload();

        var iterator = loader.stream().iterator();

        while (iterator.hasNext()) {
            try {
                list.add(iterator.next().get());
            } catch (ServiceConfigurationError error) {
            }
        }

        var bundle = OBJECT_MAPPER.createObjectNode();
        var type = (object != null) ? object.getClass() : null;

        for (var renderer : list) {
            if (renderer.getForType().isAssignableFrom(type)) {
                renderer.renderTo(bundle, object);
            }
        }

        return bundle;
    }
}
