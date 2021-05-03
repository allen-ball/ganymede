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
import ball.annotation.ServiceProviderFor;
import freemarker.template.Configuration;
import java.util.Map;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

/**
 * FreeMarker {@link ScriptEngineFactory}.
 *
 * {@bean.info}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@ServiceProviderFor({ ScriptEngineFactory.class })
@Names({ "freemarker" })
@Parameter(name = ScriptEngine.NAME, value = "freemarker")
@Parameter(name = ScriptEngine.ENGINE, value = "freemarker")
@Parameter(name = ScriptEngine.LANGUAGE, value = "freemarker")
@Extensions({ "ftl" })
@NoArgsConstructor @ToString @Log4j2
public class FreeMarkerScriptEngineFactory extends AbstractScriptEngineFactory {
    @Override
    public Map<String,String> getParameters() {
        if (parameters == null) {
            parameters = super.getParameters();

            var version = Configuration.VERSION_2_3_31.toString();

            parameters.put(ScriptEngine.ENGINE_VERSION, version);
            parameters.put(ScriptEngine.LANGUAGE_VERSION, version);
        }

        return parameters;
    }

    @Override
    public FreeMarkerScriptEngine getScriptEngine() {
        FreeMarkerScriptEngine engine = null;

        try {
            engine = new FreeMarkerScriptEngine(this);
        } catch (Throwable throwable) {
            log.warn("{}", throwable);
        }

        return engine;
    }
}
