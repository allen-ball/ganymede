package galyleo.shell.jshell;

import com.fasterxml.jackson.databind.node.ArrayNode;
import galyleo.shell.Shell;
import lombok.NoArgsConstructor;

import static galyleo.server.Server.OBJECT_MAPPER;
import static lombok.AccessLevel.PRIVATE;

/**
 * {@link galyleo.shell.Shell} JSON event channel.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@NoArgsConstructor(access = PRIVATE)
public abstract class ExecutionEvents {

    /**
     * Collected output.
     */
    public static ArrayNode json = OBJECT_MAPPER.createArrayNode();

    public static ArrayNode get() {
        var node = json;

        json = OBJECT_MAPPER.createArrayNode();

        return node;
    }

    public static String getAsString() {
        return get().toPrettyString();
    }

    public static ArrayNode get(Shell shell) {
        var node = OBJECT_MAPPER.createArrayNode();

        try {
            var expression =
                String.format("%s.getAsString()",
                              ExecutionEvents.class.getCanonicalName());
            var content = shell.evaluate(expression);

            node = (ArrayNode) OBJECT_MAPPER.readTree(content);
        } catch (Exception exception) {
        }

        return node;
    }
}
