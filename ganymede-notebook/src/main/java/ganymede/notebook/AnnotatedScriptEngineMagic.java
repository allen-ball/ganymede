package ganymede.notebook;
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
import java.util.List;

/**
 * {@link AbstractScriptEngineMagic} annotation services.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
public interface AnnotatedScriptEngineMagic extends AnnotatedMagic {

    /**
     * Method to get the target {@link javax.script.ScriptEngine}
     * {@link ScriptEngineName}.
     *
     * @return  The target {@link javax.script.ScriptEngine} name.
     */
    default String getScriptEngineName() {
        var annotation = getClass().getAnnotation(ScriptEngineName.class);
        var value = (annotation != null) ? annotation.value() : null;

        if (value == null) {
            value = getMagicNames()[0];
        }

        return value;
    }

    /**
     * Method to get the target {@link javax.script.ScriptEngine}
     * {@link Extensions}.  Creates a default {@link List} from
     * {@link #getScriptEngineName()} if there is no {@link Extensions}
     * annotation.
     *
     * @return  The target {@link javax.script.ScriptEngine} extensions.
     */
    default List<String> getExtensions() {
        var annotation = getClass().getAnnotation(Extensions.class);
        var value = (annotation != null) ? annotation.value() : null;

        return (value != null) ? List.of(value) : List.of(getScriptEngineName());
    }
}
