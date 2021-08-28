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
import java.util.Properties;
import java.util.regex.Pattern;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.core.io.ClassPathResource;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * {@link Javadoc} utilities.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@NoArgsConstructor @ToString
public class Javadoc {
    @ToString.Exclude
    private final Properties properties = new Properties();

    {
        try (var in = new ClassPathResource("javadoc-map.properties").getInputStream()) {
            properties.load(in);
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    /**
     * Get a Javadoc link href from a canonical class name.
     *
     * @param   name            The canonical class name.
     *
     * @return  The URL (as a {@link String}) or {@code null} if the package
     *          is not known.
     */
    public String href(String name) {
        String url = null;

        if (name != null) {
            var offset = name.lastIndexOf(".");

            if (offset > 0) {
                url = href(name.substring(0, offset), name.substring(offset + 1));
            }
        }

        return url;
    }

    /**
     * Get a Javadoc link href from a possibly simple class name and a value
     * ({@link Object}).
     *
     * @param   name            The (possibly simple) class name.
     * @param   value           The {@link Object}.
     *
     * @return  The URL (as a {@link String}) or {@code null} if the package
     *          cannot be determined.
     */
    public String href(String name, Object value) {
        return href(name, (value != null) ? value.getClass() : null);
    }

    /**
     * Get a Javadoc link href from a possibly simple class name and an
     * implementation type ({@link Class}).
     *
     * @param   name            The (possibly simple) class name.
     * @param   type            The {@link Class}.
     *
     * @return  The URL (as a {@link String}) or {@code null} if the package
     *          cannot be determined.
     */
    public String href(String name, Class<?> type) {
        String url = null;

        if (name != null) {
            if (name.lastIndexOf(".") != -1) {
                url = href(name);
            } else {
                if (type != null) {
                    if (name.equals(type.getSimpleName())) {
                        url = href(type.getPackage().getName(), name);
                    } else {
                        url = href(name, type.getSuperclass());

                        if (url == null) {
                            for (Class<?> supertype : type.getInterfaces()) {
                                url = href(name, supertype);

                                if (url != null) {
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }

        return url;
    }

    private String href(String pkg, String name) {
        String url = null;

        if (isNotBlank(pkg) && isNotBlank(name)) {
            url = properties.getProperty(pkg);

            if (url != null) {
                var module = properties.getProperty(pkg + "-module");

                if (module != null) {
                    url += module + "/";
                }

                url += String.join("/", pkg.split(Pattern.quote(".")));
                url += "/" + name + ".html" + "?is-external=true";
            }
        }

        return url;
    }
}
