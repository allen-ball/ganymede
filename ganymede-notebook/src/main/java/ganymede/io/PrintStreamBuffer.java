package ganymede.io;
/*-
 * ##########################################################################
 * Ganymede
 * %%
 * Copyright (C) 2021, 2022 Allen D. Ball
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
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * {@link PrintStream} buffer.
 *
 * @see ByteArrayOutputStream
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
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

    @Override
    public void close() { }
}
