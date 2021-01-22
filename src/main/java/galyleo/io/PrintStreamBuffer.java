package galyleo.io;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * {@link PrintStream} buffer.
 *
 * @see ByteArrayOutputStream
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
public class PrintStreamBuffer extends PrintStream {
    private final ByteArrayOutputStream buffer;

    /**
     * Sole constructor.
     */
    public PrintStreamBuffer() { this(new ByteArrayOutputStream()); }

    private PrintStreamBuffer(ByteArrayOutputStream buffer) {
        super(buffer, true, UTF_8);

        this.buffer = buffer;
    }

    /**
     * Discards all currently accumulated output.
     */
    public void reset() { buffer.reset(); }

    /**
     * Returns the buffer's contents as newly allocated byte array.
     *
     * @return  The newly allocated byte array.
     */
    public byte[] toByteArray() { return buffer.toByteArray(); }

    /**
     * Returns the buffer's contents.
     *
     * @return  The buffer's contents.
     */
    @Override
    public String toString() { return buffer.toString(UTF_8); }
}
