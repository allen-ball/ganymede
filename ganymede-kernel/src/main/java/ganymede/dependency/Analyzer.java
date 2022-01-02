package ganymede.dependency;
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
import ball.annotation.CompileTimeCheck;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;

/**
 * Dependency {@link Analyzer}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@NoArgsConstructor @ToString @Log4j2
public class Analyzer {
    private static final String SHADED_DEPENDENCIES = "META-INF/shaded.dependencies";

    @CompileTimeCheck
    private static final Pattern POM_PROPERTIES =
        Pattern.compile("(?i)^META-INF/maven/(?<g>[^/]+)/(?<a>[^/]+)/pom.properties$");

    /**
     * Get the Maven {@link Artifact}s the argument JAR includes.  Returns
     * an empty {@link Set} if the {@link File} is not a JAR or if the
     * analysis cannot identify any {@link Artifact}s.
     *
     * @param   file            The {@link File} ({@link JarFile}).
     *
     * @return  The {@link Set} of implemented {@link Artifact}s.
     */
    public Set<Artifact> getShadedArtifactSet(File file) {
        var set = new LinkedHashSet<Artifact>();

        if (! file.isDirectory()) {
            try (var jar = new JarFile(file)) {
                var manifest = jar.getManifest();
                var entry = jar.getJarEntry(SHADED_DEPENDENCIES);

                if (entry != null) {
                    try (var in = jar.getInputStream(entry)) {
                        parse(in)
                            .map(t -> t.setFile(file))
                            .forEach(set::add);
                    }
                } else {
                    var attributes = (manifest != null) ? manifest.getMainAttributes() : null;
                    var list =
                        jar.stream()
                        .map(JarEntry::getName)
                        .map(POM_PROPERTIES::matcher)
                        .filter(Matcher::matches)
                        .collect(toList());

                    for (var matcher : list) {
                        try (var in = jar.getInputStream(jar.getJarEntry(matcher.group()))) {
                            var properties = new Properties();

                            properties.load(in);

                            if (Objects.equals(matcher.group("g"), properties.get("groupId"))
                                && Objects.equals(matcher.group("a"), properties.get("artifactId"))) {
                                var artifact =
                                    artifact(matcher.group("g"), matcher.group("a"),
                                             properties.getProperty("version"), file);

                                set.add(artifact);
                            } else {
                                log.warn("{} does not specify {}:{}",
                                         jar.getName() + "!/" + matcher.group(),
                                         matcher.group("g"), matcher.group("a"));
                            }
                        } catch (IOException exception) {
                            log.warn("{}", jar.getName() + "!/" + matcher.group(), exception);
                        }
                    }

                    if (set.isEmpty()) {
                        if (attributes != null) {
                            var artifact = getArtifactFrom(attributes, file);

                            if (artifact != null) {
                                set.add(artifact);
                            }
                        }
                    }
                }
            } catch (Exception exception) {
                log.warn("{}", file, exception);
            }
        }

        return set;
    }

    /**
     * Method to convert an {@link InputStream} to a {@link Stream} of
     * {@link DefaultArtifact}s.
     *
     * @param   in              The {@link InputStream}.
     *
     * @return  The {@link Stream} of {@link DefaultArtifact}s.
     */
    protected Stream<DefaultArtifact> parse(InputStream in) {
        var stream =
            new BufferedReader(new InputStreamReader(in, UTF_8)).lines()
            .map(String::strip)
            .map(t -> t.split(":"))
            .filter(t -> t.length > 3)
            .map(t -> new DefaultArtifact(t[0], t[1], null, t[2], t[3]));

        return stream;
    }

    private static final List<List<String>> CANDIDATES =
        List.of(List.of("Bundle-SymbolicName", "Bundle-Name", "Bundle-Version"),
                List.of("Implementation-Vendor-Id", "Implementation-Title", "Implementation-Version"),
                List.of("Implementation-Vendor", "Implementation-Title", "Implementation-Version"));

    private DefaultArtifact getArtifactFrom(Attributes attributes, File file) {
        var coordinates =
            CANDIDATES.stream()
            .map(t -> t.stream()
                       .map(attributes::getValue)
                       .filter(Objects::nonNull)
                       .map(Object::toString)
                       .collect(toList()))
            .filter(t -> t.size() == 3)
            .findFirst().orElse(null);

        return (coordinates != null) ? artifact(coordinates, file) : null;
    }

    private DefaultArtifact artifact(String g, String a, String v, File file) {
        return new DefaultArtifact(g, a, null, "jar", v, Map.of(), file);
    }

    private DefaultArtifact artifact(List<String> coordinates, File file) {
        return artifact(coordinates.get(0), coordinates.get(1), coordinates.get(2), file);
    }
}
