package galyleo.shell.java;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.NoArgsConstructor;

import static galyleo.server.Server.OBJECT_MAPPER;
import static lombok.AccessLevel.PRIVATE;

/**
 * {@link galyleo.shell.Java} Notebook static imported methods.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@NoArgsConstructor(access = PRIVATE)
public abstract class Imports {

    /**
     * Method to print from a Notebook cell.
     *
     * @param   object          The {@link Object} to print.
     */
    public static void print(Object object) {
        try {
            var node = OBJECT_MAPPER.createObjectNode();
            var data = node.with("data");
            var metadata = node.with("metadata");

            if (object instanceof JsonNode) {
                var type = "application/json";

                data.set(type, (JsonNode) object);
                metadata.with(type);
            }

            var type = "text/plain";

            data.put(type, String.valueOf(object));
            metadata.with(type);
            /*
             * Exports.execute_result = OBJECT_MAPPER.writeValueAsString(node);
             */
            System.out.println(object);
        } catch (Exception exception) {
            System.out.println(object);
            exception.printStackTrace(System.err);
        }
    }
}
