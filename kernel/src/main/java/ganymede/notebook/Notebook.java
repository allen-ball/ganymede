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
import java.lang.reflect.Method;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;

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

    /**
     * Method to generate the bootstrap code for a new
     * {@link jdk.jshell.JShell} instance.
     *
     * @return  The boostrap code.
     */
    public static String bootstrap() {
        var code =
            String.format("var __ = %s.newNotebookContext();\n",
                          Notebook.class.getCanonicalName());

        for (var method : NotebookFunctions.class.getDeclaredMethods()) {
            var modifiers = method.getModifiers();

            if (isPublic(modifiers) && isStatic(modifiers)) {
                code += makeWrapperFor(method);
            }
        }

        return code;
    }

    private static String makeWrapperFor(Method method) {
        var types = method.getGenericParameterTypes();
        var arguments = new String[types.length];
        var parameters = new String[types.length];

        for (int i = 0; i < arguments.length; i += 1) {
            arguments[i] = String.format("argument%d", i);
        }

        for (int i = 0; i < parameters.length; i += 1) {
            parameters[i] =
                String.format("%s %s", types[i].getTypeName(), arguments[i]);
        }

        var plist = String.join(", ", parameters);
        var alist = String.join(", ", arguments);

        return String.format("%1$s %2$s(%3$s) { %4$s%5$s.%2$s(%6$s); }\n",
                             method.getGenericReturnType().getTypeName(),
                             method.getName(), plist,
                             Void.TYPE.equals(method.getReturnType()) ? "" : "return ",
                             method.getDeclaringClass().getName(), alist);
    }
}
