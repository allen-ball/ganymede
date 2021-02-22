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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.apache.maven.artifact.Artifact;

import static java.util.stream.Collectors.toMap;
import static org.apache.maven.artifact.ArtifactUtils.versionlessKey;

/**
 * Dependency {@link Analyzer}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@NoArgsConstructor @ToString @Log4j2
public class Analyzer {
    @CompileTimeCheck
    private static final Pattern POM_PROPERTIES_ENTRY_PATTERN =
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
        var set = Set.<Artifact>of();

        if (! file.isDirectory()) {
            try (var jar = new JarFile(file)) {
                set = getJarArtifacts(jar);
            } catch (Exception exception) {
                log.warn("{}", file, exception);
            }
        }

        return set;
    }

    /**
     * Get the Maven {@link Artifact}s the argument JAR implements.  Returns
     * an empty {@link Set} if no {@code pom.properties} are found.
     *
     * @param   jar             The {@link JarFile}.
     *
     * @return  The {@link Set} of implemented {@link Artifact}s.
     */
    public Set<Artifact> getJarArtifacts(JarFile jar) {
        var set = new LinkedHashSet<Artifact>();

        try {
            var map =
                jar.stream()
                .map(JarEntry::getName)
                .map(POM_PROPERTIES_ENTRY_PATTERN::matcher)
                .filter(Matcher::matches)
                .filter(t -> (! Objects.equals(t.group("a"), "unused")))
                .collect(toMap(k -> k.group(),
                               v -> versionlessKey(v.group("g"), v.group("a"))));

            for (var entry : map.entrySet()) {
                try (var in = jar.getInputStream(jar.getJarEntry(entry.getKey()))) {
                    var properties = new Properties();

                    properties.load(in);

                    var artifact = artifact(properties, "groupId", "artifactId", "version");

                    if (! entry.getValue().equals(versionlessKey(artifact))) {
                        log.warn("{} does not specify {}",
                                 jar.getName() + "!/" + entry.getKey(), entry.getValue());
                    }

                    set.add(artifact);
                } catch (IOException exception) {
                    log.warn("{}", jar.getName() + "!/" + entry.getKey(), exception);
                }

                if (set.isEmpty()) {
                    var manifest = jar.getManifest();
                    var artifact =
                        List.of(List.of("Bundle-SymbolicName", "Bundle-Name", "Bundle-Version"),
                                List.of("Implementation-Vendor-Id", "Implementation-Title", "Implementation-Version"),
                                List.of("Implementation-Vendor", "Implementation-Title", "Implementation-Version"))
                        .stream()
                        .map(t -> artifact(manifest.getMainAttributes(),
                                           t.get(0), t.get(1), t.get(2)))
                        .filter(Objects::nonNull)
                        .findFirst().orElse(null);

                    if (artifact != null) {
                        set.add(artifact);
                    }
                }
            }
        } catch (Exception exception) {
            log.warn("{}", jar.getName(), exception);
        }

        return set;
    }

    private Artifact artifact(Map<?,?> map, String g, String a, String v) {
        return artifact(Objects.toString(map.get(g), null),
                        Objects.toString(map.get(a), null),
                        Objects.toString(map.get(v), null));
    }

    private Artifact artifact(String g, String a, String v) {
        return (g != null && a != null && v != null) ? new POM.Dependency(g, a, v) : null;
    }
}
