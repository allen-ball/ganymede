package galyleo.jupyter;

import java.util.Objects;
import lombok.Getter;
import org.zeromq.SocketType;
import org.zeromq.ZMQ;

/**
 * Jupyter {@link Socket}.
 *
 * {@bean.info}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@Getter
public class Socket extends ZMQ.Socket {
    private final String address;

    /**
     * Sole constructor.
     *
     * @param   context         The {@link ZMQ.Context}.
     * @param   type            The {@link SocketType}.
     * @param   address         The {@link Socket}'s address.
     */
    public Socket(ZMQ.Context context, SocketType type, String address) {
        super(context, type);

        this.address = Objects.requireNonNull(address);
    }

    /**
     * See {@link ZMQ.Socket#connect(String)}.
     *
     * @return  {@code true} if the {@link Socket} was connected;
     *          {@code false} otherwise.
     */
    public boolean connect() { return connect(address); }
}
