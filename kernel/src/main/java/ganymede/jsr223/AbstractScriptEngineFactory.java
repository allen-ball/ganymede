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
import java.util.Map;
import java.util.TreeMap;
import javax.script.ScriptEngine;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

import static lombok.AccessLevel.PROTECTED;

/**
 * Abstract {@link javax.script.ScriptEngineFactory} base class.
 *
 * {@bean.info}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@NoArgsConstructor(access = PROTECTED)  @ToString @Log4j2
public abstract class AbstractScriptEngineFactory implements AnnotatedScriptEngineFactory {
    /** See {@link #getParameters()}. */
    protected Map<String,String> parameters = null;

    @Override
    public String getEngineName() {
        return getParameters().get(ScriptEngine.NAME);
    }

    @Override
    public String getEngineVersion() {
        return getParameters().get(ScriptEngine.ENGINE_VERSION);
    }

    @Override
    public String getLanguageName() {
        return getParameters().get(ScriptEngine.LANGUAGE);
    }

    @Override
    public String getLanguageVersion() {
        return getParameters().get(ScriptEngine.LANGUAGE_VERSION);
    }

    @Override
    public Map<String,String> getParameters() {
        if (parameters == null) {
            parameters = new TreeMap<>();
            parameters.putAll(AnnotatedScriptEngineFactory.super.getParameters());
        }

        return parameters;
    }

    @Override
    public String getParameter(String name) {
        return getParameters().get(name);
    }

    @Override
    public String getMethodCallSyntax(String object, String method, String... argv) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getOutputStatement(String toDisplay) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getProgram(String... statements) {
        throw new UnsupportedOperationException();
    }
}
