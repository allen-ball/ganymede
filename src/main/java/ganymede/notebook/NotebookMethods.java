package ganymede.notebook;
/*-
 * ##########################################################################
 * Ganymede
 * $Id$
 * $HeadURL$
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
import ganymede.kernel.KernelRestClient;
import ganymede.server.Message;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PRIVATE;

/**
 * Methods available in Notebook cells.  {@code public static} methods are
 * linked into the {@link jdk.jshell.JShell} environment.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@NoArgsConstructor(access = PRIVATE)
public abstract class NotebookMethods {

    /**
     * Method to display from a Notebook cell.
     *
     * @param   object          The {@link Object} to display.
     */
    public static void display(Object object) {
        try {
            new KernelRestClient().display(Message.mime_bundle(object));
        } catch (Exception exception) {
            System.out.println(object);
            exception.printStackTrace(System.err);
        }
    }

    /**
     * Method to print from a Notebook cell.
     *
     * @param   object          The {@link Object} to print.
     */
    public static void print(Object object) {
        try {
            new KernelRestClient().print(Message.mime_bundle(object));
        } catch (Exception exception) {
            System.out.println(object);
            exception.printStackTrace(System.err);
        }
    }
}
