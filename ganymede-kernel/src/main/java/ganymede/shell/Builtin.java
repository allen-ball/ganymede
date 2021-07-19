package ganymede.shell;
/*-
 * ##########################################################################
 * Ganymede
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
import ganymede.shell.magic.AbstractMagic;
import java.io.InputStream;
import java.io.PrintStream;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

/**
 * Abstract base class for built-in {@link ganymede.shell.Magic}s (executed
 * locally in the {@link ganymede.shell.Shell}).
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@NoArgsConstructor(access = PROTECTED)
public abstract class Builtin extends AbstractMagic {

    /**
     * {@link Shell} execution method.
     *
     * @param   shell           The {@link Shell}.
     * @param   in              The {@code in} {@link InputStream}.
     * @param   out             The {@code out} {@link PrintStream}.
     * @param   err             The {@code err} {@link PrintStream}.
     * @param   application     The {@link Application} instance.
     */
    public abstract void execute(Shell shell,
                                 InputStream in, PrintStream out, PrintStream err,
                                 Application application) throws Exception;

    @Override
    public void execute(String line0, String code) throws Exception {
        throw new IllegalStateException(line0);
    }
}
