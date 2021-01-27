package galyleo.server;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import galyleo.io.PrintStreamBuffer;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.zeromq.ZMQ;

import static lombok.AccessLevel.PROTECTED;

/**
 * Jupyter {@link Server} (base class for kernel implementations).
 *
 * {@bean.info}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@Getter(PROTECTED) @Setter(PROTECTED) @Log4j2
public abstract class Server extends ScheduledThreadPoolExecutor {

    /**
     * Common {@link Server} static {@link ObjectMapper} instance.
     */
    public static final ObjectMapper OBJECT_MAPPER =
        new ObjectMapper()
        .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_VALUES)
        .enable(SerializationFeature.INDENT_OUTPUT);

    private final ZMQ.Context context = ZMQ.context(8);
    private String session = null;

    /**
     * Sole constructor.
     */
    protected Server() { super(16); }

    /**
     * Method to create a "standard" error node.
     *
     * @param   throwable       The {@link Throwable} source of the error.
     * @param   evalue          The expression value.
     *
     * @return  The corresponding {@link ObjectNode}.
     */
    protected ObjectNode toStandardErrorMessage(Throwable throwable, String evalue) {
        var node = OBJECT_MAPPER.createObjectNode();

        node.put("status", "error");
        node.put("ename", throwable.getClass().getCanonicalName());
        node.put("evalue", evalue);

        var array = node.putArray("traceback");
        var buffer = new PrintStreamBuffer();

        throwable.printStackTrace(buffer);
        Stream.of(buffer.toString().split("\\R")).forEach(t -> array.add(t));

        return node;
    }
}
