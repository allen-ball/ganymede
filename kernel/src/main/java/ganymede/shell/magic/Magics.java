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
import com.fasterxml.jackson.databind.node.ObjectNode;
import ganymede.server.Message;
import ganymede.server.renderer.ThymeleafRenderer;
import ganymede.shell.Magic;
import ganymede.shell.Shell;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.TreeMap;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

import static java.util.stream.Collectors.toMap;

/**
 * {@link Magics} {@link Magic}: List configured {@link Magic}s.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@ServiceProviderFor({ Magic.class })
@Description("Lists available cell magics")
@NoArgsConstructor @ToString @Log4j2
public class Magics extends AbstractMagic {
    @Override
    public void execute(Shell shell,
                        InputStream in, PrintStream out, PrintStream err,
                        String line0, String code) throws Exception {
        var magics =
            shell.magics().values().stream()
            .distinct()
            .collect(toMap(k -> String.join(", ", k.getMagicNames()),
                           v -> v.getDescription(),
                           (t, u) -> t, () -> new TreeMap<>()));
        var resource = getClass().getSimpleName();
        var map = Map.<String,Object>of("magics", magics);
        var html = ThymeleafRenderer.process(getClass(), resource + ".html", "html", map);
        var text = ThymeleafRenderer.process(getClass(), resource + ".text", "text", map);

        shell.kernel().print(Message.mime_bundle(html, text));
    }

    @Override
    public void execute(String line0, String code) throws Exception {
        throw new IllegalArgumentException(line0);
    }
}
