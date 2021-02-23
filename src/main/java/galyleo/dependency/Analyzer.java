package galyleo.dependency;

import ball.annotation.CompileTimeCheck;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

import static java.util.stream.Collectors.toList;

/**
 * Dependency {@link Analyzer}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@NoArgsConstructor @ToString @Log4j2
public class Analyzer {
    @CompileTimeCheck
    private static final Pattern POM_PROPERTIES =
        Pattern.compile("(?i)^META-INF/maven/(?<g>[^/]+)/(?<a>[^/]+)/pom.properties$");

    /**
     * Get the Maven {@link Artifact}s the argument JAR implements.  Returns
     * an empty {@link Set} if no {@code pom.properties} are found.
     *
     * @param   file            The {@link File} ({@link JarFile}).
     *
     * @return  The {@link Set} of implemented {@link Artifact}s.
     */
    public Set<Artifact> getJarArtifacts(File file) {
        var set = new LinkedHashSet<Artifact>();

        if (! file.isDirectory()) {
            try (var jar = new JarFile(file)) {
                var list =
                    jar.stream()
                    .map(JarEntry::getName)
                    .map(POM_PROPERTIES::matcher)
                    .filter(Matcher::matches)
                    .filter(t -> (! Objects.equals(t.group("a"), "unused")))
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

                    if (set.isEmpty()) {
                        var artifact = getManifestArtifact(file, jar.getManifest());

                        if (artifact != null) {
                            set.add(artifact);
                        }
                    }
                }
            } catch (Exception exception) {
                log.warn("{}", file, exception);
            }
        }

        return set;
    }

    private DefaultArtifact getManifestArtifact(File file, Manifest manifest) {
        var coordinates =
            List.of(List.of("Bundle-SymbolicName", "Bundle-Name", "Bundle-Version"),
                    List.of("Implementation-Vendor-Id", "Implementation-Title", "Implementation-Version"),
                    List.of("Implementation-Vendor", "Implementation-Title", "Implementation-Version"))
            .stream()
            .map(t -> t.stream()
                       .map(u -> manifest.getMainAttributes().get(u))
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
        return artifact(coordinates.get(0), coordinates.get(2), coordinates.get(3), file);
    }
}
