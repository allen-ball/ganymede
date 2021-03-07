package ganymede.dependency;

import ball.annotation.CompileTimeCheck;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
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
 * @version $Revision$
 */
@NoArgsConstructor @ToString @Log4j2
public class Analyzer {
    private static final String SHADED_ARTIFACTS = "META-INF/shaded-artifacts";

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
                var entry = jar.getJarEntry(SHADED_ARTIFACTS);

                if (entry != null) {
                    try (var in = jar.getInputStream(entry)) {
                        new BufferedReader(new InputStreamReader(in, UTF_8))
                            .lines()
                            .map(String::strip)
                            .map(t -> t.split(":"))
                            .filter(t -> t.length > 3)
                            .map(t -> new DefaultArtifact(t[0], t[1], null, t[2], t[3], Map.of(), file))
                            .forEach(set::add);
                    }
                } else {
                    var attributes = (manifest != null) ? manifest.getMainAttributes() : null;
                    var list =
                        jar.stream()
                        .map(JarEntry::getName)
                        .map(POM_PROPERTIES::matcher)
                        .filter(Matcher::matches)
                        // .filter(t -> (! Objects.equals(t.group("a"), "unused")))
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
