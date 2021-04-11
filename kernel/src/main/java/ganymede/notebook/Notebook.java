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
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * {@link Notebook} Spring launcher.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@SpringBootApplication
@NoArgsConstructor @ToString
public class Notebook {

    /**
     * Static factory method to create an {@link NotebookContext}.  Also
     * initializes the Spring environment.
     *
     * @see SpringApplicationBuilder
     *
     * @return  An initialized {@link NotebookContext}.
     */
    public static NotebookContext newNotebookContext() {
        var __ = new NotebookContext();

        try {
            var type = Notebook.class;
            var profile = type.getSimpleName().toLowerCase();

            new SpringApplicationBuilder(type)
                .profiles(profile)
                .run();
        } catch (Exception exception) {
            exception.printStackTrace(System.err);
        }

        return __;
    }
}
