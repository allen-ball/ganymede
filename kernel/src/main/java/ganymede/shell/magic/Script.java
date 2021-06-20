package ganymede.shell.magic;
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
import ball.annotation.ServiceProviderFor;
import ganymede.shell.Magic;
import java.util.stream.Stream;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

import static java.lang.ProcessBuilder.Redirect.PIPE;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * {@link Script} {@link Magic}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@ServiceProviderFor({ Magic.class })
@MagicNames({ Magic.BANG, "script" })
@Description("Execute script with the argument command")
@NoArgsConstructor @ToString @Log4j2
public class Script extends AbstractMagic {
    @Override
    public void execute(String line0, String code) throws Exception {
        var argv =
            Stream.of(Magic.getCellMagicCommand(line0))
            .skip(1)
            .toArray(String[]::new);
        var process =
            new ProcessBuilder(argv)
            .redirectInput(PIPE)
            .redirectErrorStream(true)
            .redirectOutput(PIPE)
            .start();

        try (var in = process.getInputStream()) {
            try (var out = process.getOutputStream()) {
                out.write(code.getBytes(UTF_8));
            }

            in.transferTo(System.out);

            int status = process.waitFor();
        }
    }
}
