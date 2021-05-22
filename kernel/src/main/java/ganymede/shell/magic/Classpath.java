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
import ball.annotation.ServiceProviderFor;
import ganymede.shell.Magic;
import ganymede.shell.Shell;
import ganymede.util.PathPropertyMap;
import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.stream.Stream;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

/**
 * {@link Classpath} {@link Magic}.  See
 * {@link jdk.jshell.JShell#addToClasspath(String)}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@ServiceProviderFor({ Magic.class })
@Description("Add to or print JShell classpath")
@NoArgsConstructor @ToString @Log4j2
public class Classpath extends JShell {
    private static final String SEPARATOR = System.getProperty("path.separator");

    @Override
    public void execute(Shell shell,
                        InputStream in, PrintStream out, PrintStream err,
                        String line0, String code) throws Exception {
        if (! code.isBlank()) {
            var files =
                HELPER.replacePlaceholders(code, System.getProperties())
                .lines()
                .filter(t -> (! t.isBlank()))
                .map(String::strip)
                .flatMap(t -> Stream.of(t.split(SEPARATOR)))
                .filter(t -> (! t.isBlank()))
                .map(String::strip)
                .map(File::new)
                .toArray(File[]::new);

            shell.addToClasspath(files);
        } else {
            var map = new PathPropertyMap();

            shell.resolver().classpath().stream()
                .map(Object::toString)
                .map(map::shorten)
                .forEach(out::println);
        }
    }
}
