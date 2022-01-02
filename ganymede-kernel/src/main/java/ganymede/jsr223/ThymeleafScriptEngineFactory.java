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
import ball.annotation.ServiceProviderFor;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

/**
 * Thymeleaf {@link ScriptEngineFactory}.
 *
 * {@bean.info}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@ServiceProviderFor({ ScriptEngineFactory.class })
@Names({ "thymeleaf" })
@Parameter(name = ScriptEngine.NAME, value = "thymeleaf")
@Parameter(name = ScriptEngine.ENGINE, value = "thymeleaf")
@Parameter(name = ScriptEngine.ENGINE_VERSION, value = "3.0")
@Parameter(name = ScriptEngine.LANGUAGE, value = "thymeleaf")
@Parameter(name = ScriptEngine.LANGUAGE_VERSION, value = "3.0")
@Extensions({ })
@NoArgsConstructor @ToString @Log4j2
public class ThymeleafScriptEngineFactory extends AbstractScriptEngineFactory {
    @Override
    public ThymeleafScriptEngine getScriptEngine() {
        ThymeleafScriptEngine engine = null;

        try {
            engine = new ThymeleafScriptEngine(this);
        } catch (Throwable throwable) {
            log.warn("{}", throwable);
        }

        return engine;
    }
}
