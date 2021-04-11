package ganymede.shell.magic;
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
import ganymede.notebook.NotebookContext;
import ganymede.server.Message;
import lombok.NoArgsConstructor;
import org.springframework.util.PropertyPlaceholderHelper;

import static lombok.AccessLevel.PROTECTED;

/**
 * Abstract base class for {@link ganymede.shell.Magic}s.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@NoArgsConstructor(access = PROTECTED)
public abstract class AbstractMagic implements AnnotatedMagic {
    /** Value received from {@link #configure(NotebookContext)}. */
    protected NotebookContext context = null;

    /**
     * Common static {@link PropertyPlaceholderHelper}.
     */
    protected static final PropertyPlaceholderHelper HELPER =
        new PropertyPlaceholderHelper("${", "}", ":", true);

    @Override
    public Message.completeness isComplete(String line0, String code) {
        return Message.completeness.complete;
    }

    @Override
    public void configure(NotebookContext context) { this.context = context; }
}
