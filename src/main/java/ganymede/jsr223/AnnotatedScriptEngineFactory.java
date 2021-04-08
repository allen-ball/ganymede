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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import javax.script.ScriptEngineFactory;

import static java.util.stream.Collectors.toMap;

/**
 * {@link ScriptEngineFactory} annotation services.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
public interface AnnotatedScriptEngineFactory extends ScriptEngineFactory {
    @Override
    default List<String> getNames() {
        var annotation = getClass().getAnnotation(Names.class);
        var value = (annotation != null) ? annotation.value() : null;

        return (value != null) ? List.of(value) : List.of();
    }

    @Override
    default List<String> getExtensions() {
        var annotation = getClass().getAnnotation(Extensions.class);
        var value = (annotation != null) ? annotation.value() : null;

        return (value != null) ? List.of(value) : List.of();
    }

    @Override
    default List<String> getMimeTypes() {
        var annotation = getClass().getAnnotation(MimeTypes.class);
        var value = (annotation != null) ? annotation.value() : null;

        return (value != null) ? List.of(value) : List.of();
    }

    default Map<String,String> getParameters() {
        var annotation = getClass().getAnnotation(Parameters.class);
        var map =
            Stream.of(annotation)
            .filter(Objects::nonNull)
            .flatMap(t -> Stream.of(t.value()))
            .collect(toMap(k -> k.name(), v -> v.value()));

        return map;
    }
}
