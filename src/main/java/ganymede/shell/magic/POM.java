package ganymede.shell.magic;
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
import ball.annotation.ServiceProviderFor;
import com.fasterxml.jackson.core.JsonProcessingException;
import ganymede.shell.Magic;
import ganymede.shell.Shell;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.stream.Stream;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

/**
 * {@link POM} {@link ganymede.shell.Magic}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@ServiceProviderFor({ Magic.class })
@Description("Define the Notebook's Project Object Model")
@NoArgsConstructor @ToString @Log4j2
public class POM extends JShell {
    @Override
    public void execute(Shell shell,
                        InputStream in, PrintStream out, PrintStream err,
                        String line0, String code) throws Exception {
        try {
            if (! code.isBlank()) {
                shell.resolve(ganymede.dependency.POM.parse(code));
            } else {
                shell.resolver().pom().writeTo(out);
            }
        } catch (JsonProcessingException exception) {
            err.println(exception.getMessage());
        } catch (Exception exception) {
            exception.printStackTrace(err);
        }
    }
}
