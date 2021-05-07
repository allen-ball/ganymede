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
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import static javax.script.ScriptContext.ENGINE_SCOPE;
import static lombok.AccessLevel.PROTECTED;

/**
 * Thymeleaf {@link ScriptEngine}.
 *
 * {@bean.info}
 *
 * @see TemplateEngine
 * @see StringTemplateResolver
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@RequiredArgsConstructor(access = PROTECTED) @Getter @ToString @Log4j2
public class ThymeleafScriptEngine extends AbstractTemplateScriptEngine {
    private final ThymeleafScriptEngineFactory factory;
    private final StringTemplateResolver resolver = new StringTemplateResolver();

    @Override
    public String eval(String script, ScriptContext context) throws ScriptException {
        var bindings = context.getBindings(ENGINE_SCOPE);
        var mode = StringTemplateResolver.DEFAULT_TEMPLATE_MODE;
        var argv = (String[]) bindings.get(ScriptEngine.ARGV);

        if (argv != null && argv.length > 1) {
            mode = TemplateMode.valueOf(argv[1].toUpperCase());
        }

        resolver.setTemplateMode(mode);

        var engine = new TemplateEngine();

        engine.setTemplateResolver(resolver);

        return engine.process(script, new Context(null, bindings));
    }
}
