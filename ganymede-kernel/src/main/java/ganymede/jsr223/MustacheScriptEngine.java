package ganymede.jsr223;
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
import com.samskivert.mustache.Mustache;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParameterException;

import static javax.script.ScriptContext.ENGINE_SCOPE;
import static lombok.AccessLevel.PROTECTED;

/**
 * {@link Mustache} {@link javax.script.ScriptEngine}.
 *
 * {@bean.info}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@RequiredArgsConstructor(access = PROTECTED) @Getter @ToString @Log4j2
public class MustacheScriptEngine extends AbstractScriptEngine {
    private final MustacheScriptEngineFactory factory;

    @Override
    public String eval(String script, ScriptContext context) throws ScriptException {
        var out = "";

        try {
            var arguments = new Arguments();
            var result = parse(context, arguments);
            var template = Mustache.compiler().compile(script);

            out = template.execute(context.getBindings(ENGINE_SCOPE));
        } catch (ParameterException exception) {
            System.err.println(exception.getMessage());
            System.err.println();
            exception.getCommandLine().usage(System.err);
        } catch (Exception exception) {
            exception.printStackTrace(System.err);
        }

        return out;
    }

    @Command @Data
    private class Arguments { }
}
