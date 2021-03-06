package ganymede.kernel.magic;
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
import com.fasterxml.jackson.databind.JsonNode;
import ball.annotation.ServiceProviderFor;
import ganymede.notebook.AbstractPropertiesMagic;
import ganymede.notebook.Description;
import ganymede.notebook.Magic;
import java.util.Properties;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

/**
 * {@link Env} {@link Magic}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@ServiceProviderFor({ Magic.class })
@Description("Add/Update or print the environment")
@NoArgsConstructor @ToString @Log4j2
public class Env extends AbstractPropertiesMagic {
    @Override
    public void execute(String line0, String code, JsonNode metadata) throws Exception {
        if (! code.isBlank()) {
            var properties = compile(code);

            for (var entry : properties.entrySet()) {
                throw new UnsupportedOperationException(System.getProperty("os.name") + ": Cannot set " + entry);
            }
        } else {
            var properties = new Properties();

            properties.putAll(System.getenv());
            properties.store(System.out, null);
        }
    }
}
