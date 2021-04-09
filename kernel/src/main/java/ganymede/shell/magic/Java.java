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
import ganymede.notebook.NotebookContext;
import ganymede.server.renderer.ThymeleafRenderer;
import ganymede.shell.Magic;
import java.util.Map;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

import static ganymede.notebook.NotebookFunctions.print;
import static javax.script.ScriptContext.ENGINE_SCOPE;

/**
 * {@link Java} {@link Magic}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@ServiceProviderFor({ Magic.class })
@MagicNames({ "java" })
@Description("Execute code in Java REPL")
@NoArgsConstructor @ToString @Log4j2
public class Java extends AbstractMagic {
    @Override
    public void execute(NotebookContext __, String line0, String code) throws Exception {
        if (code.isBlank()) {
            var resource = getClass().getSimpleName();
            var map =
                Map.<String,Object>of("bindings", __.context.getBindings(ENGINE_SCOPE),
                                      "types", __.types);
            var html = ThymeleafRenderer.process(getClass(), resource + ".html", "html", map);

            print(html);
        } else {
            throw new IllegalStateException();
        }
    }
}
