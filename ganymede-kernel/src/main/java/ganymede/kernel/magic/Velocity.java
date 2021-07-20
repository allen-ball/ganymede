package ganymede.kernel.magic;
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
import ganymede.notebook.AbstractScriptEngineMagic;
import ganymede.notebook.Description;
import ganymede.notebook.Magic;
import java.io.StringWriter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.apache.velocity.script.VelocityScriptEngine;

/**
 * {@link Velocity} {@link Magic}.
 *
 * @see VelocityScriptEngine
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@ServiceProviderFor({ Magic.class })
@Description("Velocity template evaluator")
@NoArgsConstructor @ToString @Log4j2
public class Velocity extends AbstractScriptEngineMagic {
    private final StringWriter out = new StringWriter();

    @Override
    protected VelocityScriptEngine engine() {
        return (VelocityScriptEngine) super.engine();
    }

    @Override
    protected void execute(String code) {
        var stdout = context.context.getWriter();

        try {
            out.getBuffer().setLength(0);

            context.context.setWriter(out);

            super.execute(code);
        } finally {
            context.context.setWriter(stdout);
        }
    }

    @Override
    protected void render(Object object) {
        context.print(out.toString());
    }
}
