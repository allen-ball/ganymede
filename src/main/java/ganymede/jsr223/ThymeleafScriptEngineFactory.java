package ganymede.jsr223;
/*-
 * ##########################################################################
 * Ganymede
 * $Id$
 * $HeadURL$
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
import java.io.Reader;
import java.util.Scanner;
import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import static javax.script.ScriptContext.ENGINE_SCOPE;

/**
 * Thymeleaf {@link ScriptEngineFactory}.
 *
 * @see TemplateEngine
 * @see StringTemplateResolver
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@ServiceProviderFor({ ScriptEngineFactory.class })
@Parameter(name = ScriptEngine.NAME, value = "thymeleaf")
@Parameter(name = ScriptEngine.ENGINE, value = "thymeleaf")
@Parameter(name = ScriptEngine.ENGINE_VERSION, value = "3.0")
@Parameter(name = ScriptEngine.LANGUAGE, value = "thymeleaf")
@Parameter(name = ScriptEngine.LANGUAGE_VERSION, value = "3.0")
@NoArgsConstructor @ToString @Log4j2
public class ThymeleafScriptEngineFactory extends AbstractScriptEngineFactory {
    @Override
    public ScriptEngine getScriptEngine() {
        ScriptEngine engine = null;

        try {
            engine = new ScriptEngineImpl();
        } catch (Throwable throwable) {
            log.warn("{}", throwable);
        }

        return engine;
    }

    @NoArgsConstructor @ToString
    private class ScriptEngineImpl extends AbstractScriptEngine {
        private StringTemplateResolver resolver = new StringTemplateResolver();

        @Override
        public Object eval(String script, ScriptContext context) throws ScriptException {
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

        @Override
        public Object eval(Reader reader, ScriptContext context) throws ScriptException {
            Object result = null;

            try (var scanner = new Scanner(reader)) {
                result = eval(scanner.useDelimiter("\\A").next(), context);
            }

            return result;
        }

        @Override
        public Bindings createBindings() { return new SimpleBindings(); }

        @Override
        public ScriptEngineFactory getFactory() {
            return ThymeleafScriptEngineFactory.this;
        }
    }
}
