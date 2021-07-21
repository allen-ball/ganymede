package ganymede.util;
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
import java.io.File;
import java.util.Comparator;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Path to Property {@link java.util.Map}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
public class PathPropertyMap extends TreeMap<String,String> {
    private static final long serialVersionUID = 3186771870028981288L;

    private static final String SEPARATOR = System.getProperty("path.separator");

    /**
     * Sole constructor.
     */
    public PathPropertyMap() {
        super(Comparator
              .comparingInt(String::length).reversed()
              .thenComparing(String::toString, Comparator.naturalOrder()));

        Stream.of(System.getenv(), System.getProperties())
            .flatMap(t -> t.entrySet().stream())
            .filter(t -> (! t.getKey().toString().endsWith(".path")))
            .filter(t -> t.getValue() != null)
            .filter(t -> t.getValue().toString().length() > 1)
            .filter(t -> (! t.getValue().toString().contains(SEPARATOR)))
            .filter(t -> new File(t.getValue().toString()).isAbsolute())
            .forEach(t -> put(t.getValue().toString(),
                              "${" + t.getKey().toString() + "}"));
    }

    /**
     * Method to shorten a {@link File} path by substituting placeholder
     * references from the {@link PathPropertyMap}.
     *
     * @param   file            The {@link File}.
     *
     * @return  The possibly shortened {@link File} path.
     */
    public String shorten(File file) {
        return (file != null) ? shorten(file.getAbsolutePath()) : null;
    }

    /**
     * Method to shorten a {@link String} by substituting placeholder
     * references for paths from the {@link PathPropertyMap}.
     *
     * @param   string          The candidate {@link String} to be
     *                          shortened.
     *
     * @return  The possibly shortened {@link String} with substitutions.
     */
    public String shorten(String string) {
        var shortened = string;

        for (var key : keySet()) {
            if (shortened.startsWith(key)) {
                shortened = shortened.replace(key, get(key));
            }
        }

        return shortened;
    }
}
