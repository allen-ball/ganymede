package ganymede.kernel.magic;
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
import ball.annotation.ServiceProviderFor;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ganymede.jsr223.ThymeleafScriptEngine;
import ganymede.kernel.renderer.ThymeleafRenderer;
import ganymede.notebook.AbstractScriptEngineMagic;
import ganymede.notebook.Description;
import ganymede.notebook.Magic;
import ganymede.notebook.ScriptEngineName;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

/**
 * {@link Thymeleaf} template {@link Magic}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@ServiceProviderFor({ Magic.class })
@Description("Thymeleaf template evaluator")
@ScriptEngineName("thymeleaf")
@NoArgsConstructor @ToString @Log4j2
public class Thymeleaf extends AbstractScriptEngineMagic {
    @Override
    protected void render(Object object) {
        var engine = (ThymeleafScriptEngine) engine();
        var mode = engine.getResolver().getTemplateMode();

        context.print(new ThymeleafRenderer.Output(mode, String.valueOf(object)));
    }
}
