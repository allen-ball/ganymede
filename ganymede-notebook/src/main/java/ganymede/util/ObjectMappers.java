package ganymede.util;
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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

/**
 * Common {@link ObjectMapper}s.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
public interface ObjectMappers {

    /**
     * Common static JSON {@link ObjectMapper} instance.
     */
    public static final ObjectMapper JSON =
        new ObjectMapper()
        .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_VALUES)
        .enable(SerializationFeature.INDENT_OUTPUT);

    /**
     * Common static YAML {@link ObjectMapper} instance.
     */
    public static final ObjectMapper YAML =
        new ObjectMapper(new YAMLFactory()
                         .enable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER))
        .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
        .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_VALUES)
        .enable(SerializationFeature.INDENT_OUTPUT);
}
