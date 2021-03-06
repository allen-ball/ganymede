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
import ball.annotation.ServiceProviderFor;
import ganymede.notebook.Description;
import ganymede.notebook.Magic;
import ganymede.notebook.ScriptEngineName;
import java.util.stream.Stream;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

/**
 * {@link HTML} template {@link Magic}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@ServiceProviderFor({ Magic.class })
@Description("HTML template evaluator")
@ScriptEngineName("thymeleaf")
@NoArgsConstructor @ToString @Log4j2
public class HTML extends Thymeleaf {
    @Override
    protected void execute(String[] argv, String code) throws Exception {
        argv =
            Stream.of(new String[] { "thymeleaf" }, argv)
            .flatMap(Stream::of)
            .toArray(String[]::new);

        super.execute(argv, code);
    }
}
