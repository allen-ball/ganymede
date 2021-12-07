package ganymede.notebook;
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
import java.io.StringReader;
import java.util.Objects;
import java.util.Properties;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

import static lombok.AccessLevel.PROTECTED;

/**
 * Abstract {@link Properties} {@link Magic} base class.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@NoArgsConstructor(access = PROTECTED) @ToString @Log4j2
public abstract class AbstractPropertiesMagic extends AbstractMagic {

    /**
     * Method to convert the cell {@code code} to a {@link Properties}
     * instance.
     *
     * @param   code            The code to parse.
     *
     * @return  The {@link Properties}.
     *
     * @see #HELPER
     */
    protected Properties compile(String code) throws Exception {
        try (var reader = new StringReader(code)) {
            var in = new Properties(System.getProperties());

            in.load(reader);

            var out = new Properties(in);
            var changed = true;

            while (changed) {
                changed = false;

                for (var key : in.keySet()) {
                    if (key instanceof String) {
                        var string = (String) key;
                        var value = HELPER.replacePlaceholders(in.getProperty(string), out);

                        changed |= (! Objects.equals(value, out.put(string, value)));
                    }
                }
            }

            return out;
        }
    }
}
