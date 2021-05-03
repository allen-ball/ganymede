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
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Version;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Objects;
import java.util.Scanner;
import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

import static javax.script.ScriptContext.ENGINE_SCOPE;

/**
 * FreeMarker {@link ScriptEngine}.
 *
 * {@bean.info}
 *
 * @see Configuration
 * @see StringTemplateLoader
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@Getter @ToString @Log4j2
public class FreeMarkerScriptEngine extends AbstractScriptEngine {
    private final FreeMarkerScriptEngineFactory factory;
    private final Configuration configuration;
    private final StringTemplateLoader loader = new StringTemplateLoader();

    /**
     * Sole constructor.
     */
    protected FreeMarkerScriptEngine(FreeMarkerScriptEngineFactory factory) {
        this.factory = factory;

        var version = new Version(factory.getEngineVersion());

        configuration = new Configuration(version);
        configuration.setTemplateLoader(loader);
    }

    @Override
    public String eval(String script, ScriptContext context) throws ScriptException {
        var bindings = context.getBindings(ENGINE_SCOPE);
        var out = new StringWriter();

        try {
            var name = String.class.getName() + "@" + Integer.toString(Objects.hashCode(script));

            loader.putTemplate(name, script);

            var template = configuration.getTemplate(name);

            template.process(bindings, out);
        } catch (Exception exception) {
            exception.printStackTrace(System.err);
        }

        return out.toString();
    }

    @Override
    public String eval(Reader reader, ScriptContext context) throws ScriptException {
        try (var scanner = new Scanner(reader)) {
            return eval(scanner.useDelimiter("\\A").next(), context);
        }
    }

    @Override
    public Bindings createBindings() { return new SimpleBindings(); }
}
