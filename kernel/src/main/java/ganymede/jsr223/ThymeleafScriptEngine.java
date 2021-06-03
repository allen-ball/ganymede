package ganymede.jsr223;
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
import java.util.stream.Stream;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.extras.java8time.dialect.Java8TimeDialect;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;

import static javax.script.ScriptContext.ENGINE_SCOPE;
import static lombok.AccessLevel.PROTECTED;

/**
 * Thymeleaf {@link javax.script.ScriptEngine}.
 *
 * {@bean.info}
 *
 * @see TemplateEngine
 * @see StringTemplateResolver
 * @see Java8TimeDialect
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@RequiredArgsConstructor(access = PROTECTED) @Getter @ToString @Log4j2
public class ThymeleafScriptEngine extends AbstractScriptEngine {
    private final ThymeleafScriptEngineFactory factory;
    private final StringTemplateResolver resolver = new StringTemplateResolver();

    @Override
    public String eval(String script, ScriptContext context) throws ScriptException {
        var out = "";

        try {
            var arguments = new Arguments();
            var result = parse(context, arguments);

            resolver.setTemplateMode(arguments.getMode());

            var engine = new TemplateEngine();

            engine.setTemplateResolver(resolver);
            engine.addDialect(new Java8TimeDialect());

            var bindings = context.getBindings(ENGINE_SCOPE);

            out = engine.process(script, new Context(null, bindings));
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
    private class Arguments {
        @Parameters(index = "0", arity = "0..1", defaultValue = "HTML",
                    description = { "One of: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE})" })
        private TemplateMode mode = StringTemplateResolver.DEFAULT_TEMPLATE_MODE;
    }
}
