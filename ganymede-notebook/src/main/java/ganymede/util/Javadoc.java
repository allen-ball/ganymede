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
import ball.xml.FluentDocument;
import ball.xml.FluentDocumentBuilderFactory;
import ball.xml.FluentNode;
import ball.xml.HTMLTemplates;
import ball.xml.XalanConstants;
import java.io.StringWriter;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.core.io.ClassPathResource;
import org.w3c.dom.Element;

import static java.util.stream.Collectors.toMap;
import static javax.xml.transform.OutputKeys.INDENT;
import static javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION;

/**
 * {@link Javadoc} utilities.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@NoArgsConstructor @ToString
public class Javadoc implements HTMLTemplates, XalanConstants {
    private static final Map<String,Class<?>> PRIMITIVES =
        Stream.of(Boolean.TYPE, Byte.TYPE, Character.TYPE, Double.TYPE,
                  Float.TYPE, Integer.TYPE, Long.TYPE, Short.TYPE, Void.TYPE)
        .collect(toMap(k -> k.getName(), v -> v));

    @ToString.Exclude private final Properties properties = new Properties();
    @ToString.Exclude private final Transformer transformer;
    @ToString.Exclude private final FluentDocument document;

    {
        try (var in = new ClassPathResource("javadoc-map.properties").getInputStream()) {
            properties.load(in);

            transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OMIT_XML_DECLARATION, YES);
            transformer.setOutputProperty(INDENT, NO);

            document =
                FluentDocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .newDocument();
            document
                .add(element("html",
                             element("head",
                                     element("meta",
                                             attr("charset", "utf-8"))),
                             element("body")));
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    /**
     * Create an {@code <a/>} element from a possibly simple class name and
     * an implementation type ({@link Class}).
     *
     * @param   name            The (possibly simple) class name.
     * @param   type            The {@link Class}.
     *
     * @return  {@code <a/>} {@link Element} (serialized to {@link String})
     */
    public String link(String name, Class<?> type) {
        var writer = new StringWriter();
        var declaredType = type;

        if (name == null) {
            if (type != null) {
                var dimensions = "";

                while (type.isArray()) {
                    dimensions += "[]";
                    type = type.getComponentType();
                }

                name = getCanonicalName(type);
                name += dimensions;
            }
        }

        if (name != null) {
            var substrings = name.split("\\[|<", 2);
            var suffix = name.substring(substrings[0].length());

            name = substrings[0];

            declaredType = typeOf(name, List.of("java.lang"));

            if (declaredType == null) {
                if (type != null) {
                    while (type.isArray()) {
                        type = type.getComponentType();
                    }

                    declaredType = declaredTypeOf(name, type);
                }
            }

            if (declaredType != null) {
                name = getCanonicalName(declaredType);
            }

            name += suffix;
        }

        if (name != null) {
            var node = code(name);
            var href = href(declaredType);

            if (href != null) {
                node = a(href, node);
                ((Element) node).setAttribute("target", "_newtab");
            }

            try {
                transformer.transform(new DOMSource(node), new StreamResult(writer));
            } catch (RuntimeException exception) {
                throw exception;
            } catch (Error error) {
                throw error;
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }

        return writer.toString();
    }

    private Class<?> typeOf(String name, List<String> packages) {
        var type = PRIMITIVES.get(name);

        if (type == null) {
            try {
                type = Class.forName(name);
            } catch (Exception exception) {
            }
        }

        if (type == null) {
            for (var pkg : packages) {
                try {
                    type = Class.forName(pkg + "." + type);
                    break;
                } catch (Exception exception) {
                    continue;
                }
            }
        }

        return type;
    }

    private Class<?> declaredTypeOf(String name, Class<?> type) {
        Class<?> declaredType = null;

        if (type != null) {
            if (type.isPrimitive()
                || type.getName().equals(name)
                || type.getName().endsWith("." + name)
                || getCanonicalName(type).equals(name)
                || getCanonicalName(type).endsWith("." + name)) {
                declaredType = type;
            } else {
                declaredType = declaredTypeOf(name, type.getSuperclass());

                if (declaredType == null) {
                    for (Class<?> supertype : type.getInterfaces()) {
                        declaredType = declaredTypeOf(name, supertype);

                        if (declaredType != null) {
                            break;
                        }
                    }
                }
            }
        }

        return declaredType;
    }

    private URI href(Class<?> type) {
        String uri = null;

        if (type != null && (! type.isPrimitive())) {
            var pkg = type.getPackageName();

            uri = properties.getProperty(pkg);

            if (uri != null) {
                var module = properties.getProperty(pkg + "-module");

                if (module != null) {
                    uri += module + "/";
                }

                uri += String.join("/", pkg.split(Pattern.quote(".")));

                var name = type.getSimpleName();

                while (type.getEnclosingClass() != null) {
                    type = type.getEnclosingClass();
                    name = type.getSimpleName() + "." + name;
                }

                uri += "/" + name + ".html";
                uri += "?is-external=true";
            }
        }

        return (uri != null) ? URI.create(uri) : null;
    }

    private String getCanonicalName(Class<?> type) {
        var name = type.getName();

        return Objects.requireNonNullElse(type.getCanonicalName(), name);
    }

    private String getPackageQualifiedName(Class<?> type) {
        var name = getCanonicalName(type);

        if (! type.isPrimitive()) {
            var prefix = type.getPackageName() + ".";

            if (name.startsWith(prefix)) {
                name = name.substring(prefix.length());
            }
        }

        return name;
    }

    @Override
    public FluentDocument document() { return document; }
}
