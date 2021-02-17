package galyleo.shell.jshell;

import com.fasterxml.jackson.databind.node.ArrayNode;
import galyleo.server.Message;
import galyleo.shell.Shell;
import lombok.NoArgsConstructor;

import static galyleo.server.Server.OBJECT_MAPPER;
import static lombok.AccessLevel.PRIVATE;

/**
 * {@link galyleo.shell.Shell} remote method implementations and JSON event
 * channel.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@NoArgsConstructor(access = PRIVATE)
public abstract class RemoteRuntime {

    /**
     * Method to print from a Notebook cell.
     *
     * @param   object          The {@link Object} to print.
     */
    public static void print(Object object) {
        try {
            json.add(Message.mime_bundle(object));
        } catch (Exception exception) {
            System.out.println(object);
            exception.printStackTrace(System.err);
        }
    }

    /**
     * Collected output.
     */
    public static ArrayNode json = OBJECT_MAPPER.createArrayNode();

    public static ArrayNode getExecutionEvents() {
        var node = json;

        json = OBJECT_MAPPER.createArrayNode();

        return node;
    }

    public static String getExecutionEventsAsString() {
        return getExecutionEvents().toPrettyString();
    }

    public static ArrayNode getExecutionEvents(Shell shell) {
        var node = OBJECT_MAPPER.createArrayNode();

        try {
            var expression =
                String.format("__.invokeStaticMethod(\"%s\", \"%s\")",
                              RemoteRuntime.class.getName(),
                              "getExecutionEventsAsString");
            var content = shell.evaluate(expression);

            node = (ArrayNode) OBJECT_MAPPER.readTree(content);
        } catch (Exception exception) {
        }

        return node;
    }
}
