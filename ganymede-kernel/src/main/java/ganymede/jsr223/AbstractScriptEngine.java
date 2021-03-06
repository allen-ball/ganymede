package ganymede.jsr223;
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
import java.io.Reader;
import java.util.Scanner;
import java.util.stream.Stream;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import picocli.CommandLine;
import picocli.CommandLine.ParseResult;

import static javax.script.ScriptContext.ENGINE_SCOPE;
import static lombok.AccessLevel.PROTECTED;

/**
 * Abstract {@link javax.script.ScriptEngine} base class.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@NoArgsConstructor(access = PROTECTED) @ToString @Log4j2
public abstract class AbstractScriptEngine extends javax.script.AbstractScriptEngine {

    /**
     * Method to parse {@link ScriptContext} {@link #ARGV ARGV} value to an
     * annotated command {@link Object}.
     *
     * @param   context         The {@link ScriptContext}.
     * @param   command         The annotated command {@link Object}.
     *
     * @return  The {@link ParseResult}.
     *
     * @see CommandLine
     * @see CommandLine.Command
     * @see CommandLine.Option
     * @see CommandLine.Parameters
     */
    protected <T> ParseResult parse(ScriptContext context, T command) {
        var bindings = context.getBindings(ENGINE_SCOPE);
        var argv = (String[]) bindings.get(ARGV);

        if (argv == null) {
            argv = new String[] { };
        }

        var name = (String) null;

        if (argv.length > 0) {
            name = argv[0];
            argv = Stream.of(argv).skip(1).toArray(String[]::new);
        }

        var line =
            new CommandLine(command)
            .setCommandName(name)
            .setCaseInsensitiveEnumValuesAllowed(true)
            .setUnmatchedArgumentsAllowed(false);
        var result = line.parseArgs(argv);

        return result;
    }

    @Override
    public abstract String eval(String script, ScriptContext context) throws ScriptException;

    @Override
    public String eval(Reader reader, ScriptContext context) throws ScriptException {
        try (var scanner = new Scanner(reader)) {
            return eval(scanner.useDelimiter("\\A").next(), context);
        }
    }

    @Override
    public Bindings createBindings() { return new SimpleBindings(); }
}
