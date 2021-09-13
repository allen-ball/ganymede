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
import java.util.Properties;
import java.util.regex.Pattern;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.core.io.ClassPathResource;
import org.w3c.dom.Element;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static javax.xml.transform.OutputKeys.INDENT;
import static javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION;

/**
 * {@link Javadoc} utilities.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@NoArgsConstructor @ToString
public class Javadoc implements HTMLTemplates, XalanConstants {
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
     * Create am {@code <a/>} element from a possibly simple class name and
     * an implementation type ({@link Class}).
     *
     * @param   name            The (possibly simple) class name.
     * @param   type            The {@link Class}.
     *
     * @return  {@code <a/>} {@link Element} (serialized to {@link String})
     */
    public String a(String name, Class<?> type) {
        var writer = new StringWriter();
        var href = href(name, type);

        if (name != null || href != null) {
            var node = a(href, name);

            ((Element) node).setAttribute("target", "_newtab");

            try {
                transformer.transform(new DOMSource(node), new StreamResult(writer));
            } catch (RuntimeException exception) {
                throw exception;
            } catch (Error error) {
                throw error;
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }

        return writer.toString();
    }

    /**
     * Get a Javadoc link href from a canonical class name.
     *
     * @param   name            The canonical class name.
     *
     * @return  The URL (as a {@link String}) or {@code null} if the package
     *          is not known.
     */
    public URI href(String name) {
        URI uri = null;

        if (name != null) {
            var offset = name.lastIndexOf(".");

            if (offset > 0) {
                uri = href(name.substring(0, offset), name.substring(offset + 1));
            }
        }

        return uri;
    }

    /**
     * Get a Javadoc link href from a possibly simple class name and a value
     * ({@link Object}).
     *
     * @param   name            The (possibly simple) class name.
     * @param   value           The {@link Object}.
     *
     * @return  The URL (as a {@link URI}) or {@code null} if the package
     *          cannot be determined.
     */
    public URI href(String name, Object value) {
        return href(name, (value != null) ? value.getClass() : null);
    }

    /**
     * Get a Javadoc link href from a possibly simple class name and an
     * implementation type ({@link Class}).
     *
     * @param   name            The (possibly simple) class name.
     * @param   type            The {@link Class}.
     *
     * @return  The URL (as a {@link URI}) or {@code null} if the package
     *          cannot be determined.
     */
    public URI href(String name, Class<?> type) {
        URI uri = null;

        if (name != null) {
            if (name.lastIndexOf(".") != -1) {
                uri = href(name);
            } else {
                if (type != null) {
                    if (name.equals(type.getSimpleName())) {
                        uri = href(type.getPackage().getName(), name);
                    } else {
                        uri = href(name, type.getSuperclass());

                        if (uri == null) {
                            for (Class<?> supertype : type.getInterfaces()) {
                                uri = href(name, supertype);

                                if (uri != null) {
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }

        return uri;
    }

    private URI href(String pkg, String name) {
        String string = null;

        if (isNotBlank(pkg) && isNotBlank(name)) {
            string = properties.getProperty(pkg);

            if (string != null) {
                var module = properties.getProperty(pkg + "-module");

                if (module != null) {
                    string += module + "/";
                }

                string += String.join("/", pkg.split(Pattern.quote(".")));
                string += "/" + name + ".html" + "?is-external=true";
            }
        }

        return (string != null) ? URI.create(string) : null;
    }

    @Override
    public FluentDocument document() { return document; }
}
