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
import com.google.inject.Guice;
import ganymede.shell.Shell;
import ganymede.util.PathPropertyMap;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.ConfigurationProperties;
import org.eclipse.aether.DefaultRepositoryCache;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.repository.WorkspaceRepository;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.transfer.TransferCancelledException;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;
import org.springframework.boot.system.ApplicationHome;

import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * Dependency {@link Resolver}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@NoArgsConstructor @ToString @Log4j2
public class Resolver extends Analyzer {
    private static final Set<String> LOGGING_IGNORE = Set.of("commons-logging:commons-logging:jar");
    private static final Set<String> JCL_BRIDGES =
        Set.of("org.slf4j:jcl-over-slf4j:jar", "org.springframework:spring-jcl:jar");
    private static final Set<String> SLF4J_BINDINGS =
        Set.of("ch.qos.logback:logback-classic:jar", "org.apache.logging.log4j:log4j-slf4j-impl:jar",
               "org.slf4j:slf4j-log4j12:jar", "org.slf4j:slf4j-reload4j:jar");

    private static final String DEPENDENCIES_FORMAT = "/META-INF/%s.dependencies";

    private final RepositorySystem system;
    private final POM pom;
    private final Set<File> classpath = new LinkedHashSet<>();
    private final RepositoryImpl repository = new RepositoryImpl();
    private final PathPropertyMap pathMap = new PathPropertyMap();

    {
        try {
            system = Guice.createInjector(new ResolverModule()).getInstance(RepositorySystem.class);
            pom = POM.getDefault();

            classpath.add(new ApplicationHome(getClass()).getSource());
            classpath.stream()
                .flatMap(t -> getShadedArtifactSet(t).stream())
                .forEach(repository::resolve);
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    /**
     * Method to get the current {@link POM}.
     *
     * @return  The current {@link POM}.
     */
    public POM pom() { return pom; }

    /**
     * Method to get the current classpath.
     *
     * @return  The {@link Set} of {@link File}s.
     */
    public Set<File> classpath() { return classpath; }

    /**
     * Method to add a {@link File}(s) to the repository
     * ({@link WorkspaceReader}).
     *
     * @param   files           The {@link File}(s).
     */
    public void addToRepository(File... files) {
        Stream.of(files)
            .map(File::getAbsoluteFile)
            .map(this::getShadedArtifactSet)
            .forEach(repository::resolve);
    }

    /**
     * Method to add a {@link File}(s) to the {@link #classpath()}.  May
     * quietly ignore requests to add {@link File}s that violate internal
     * heuristics (e.g.,
     * {@link.uri http://www.slf4j.org/codes.html#multiple_bindings target=newtab multiple SLF4J bindings}).
     *
     * @param   files           The {@link File}(s).
     *
     * @return  The {@link List} of {@link File}s added to the
     *          {@link #classpath()}.
     */
    public List<File> addToClasspath(File... files) {
        var list = new LinkedList<File>();

        for (var file : files) {
            file = file.getAbsoluteFile();

            var artifacts =
                getShadedArtifactSet(file).stream()
                .map(repository::resolve)
                .collect(toSet());

            if (! artifacts.isEmpty()) {
                var array = artifacts.toArray(new Artifact[] { });

                list.addAll(addToClasspath(array));
            } else {
                if (classpath.add(file)) {
                    list.add(file);
                }
            }
        }

        return list;
    }

    /**
     * Method to add a {@link Artifact}(s) to the {@link #classpath()}.
     * May quietly ignore requests to add {@link Artifact}s that violate
     * internal heuristics (e.g.,
     * {@link.uri http://www.slf4j.org/codes.html#multiple_bindings target=newtab multiple SLF4J bindings}).
     *
     * @param   artifacts       The {@link Artifact}(s).
     *
     * @return  The {@link List} of {@link File}s added to the
     *          {@link #classpath()}.
     */
    public List<File> addToClasspath(Artifact... artifacts) {
        var list = new LinkedList<File>();
        var resolved =
            Stream.of(artifacts)
            .map(repository::resolve)
            .collect(toList());

        for (var artifact : resolved) {
            var ignore = false;
            var id = ArtifactIdUtils.toVersionlessId(artifact);

            if (! ignore) {
                ignore |= LOGGING_IGNORE.contains(id);
            }

            if (! ignore) {
                if (SLF4J_BINDINGS.contains(id)) {
                    ignore |=
                        repository.getArtifactsOn(classpath)
                        .map(ArtifactIdUtils::toVersionlessId)
                        .anyMatch(t -> SLF4J_BINDINGS.contains(t));
                }
            }

            if (! ignore) {
                if (JCL_BRIDGES.contains(id)) {
                    ignore |=
                        repository.getArtifactsOn(classpath)
                        .map(ArtifactIdUtils::toVersionlessId)
                        .anyMatch(t -> JCL_BRIDGES.contains(t));
                }
            }

            if (! ignore) {
                if (! classpath.contains(artifact.getFile())) {
                    var installed =
                        repository.getArtifactsOn(classpath)
                        .filter(t -> ArtifactIdUtils.equalsVersionlessId(artifact, t))
                        .findFirst().orElse(null);

                    if (installed == null) {
                        if (classpath.add(artifact.getFile())) {
                            list.add(artifact.getFile());
                        }
                    } else {
                        log.debug("Ignored resolved artifact {}", artifact);
                        log.debug("    for {} @ {}",
                                  () -> installed.getVersion(),
                                  () -> pathMap.shorten(installed.getFile()));
                    }
                }
            }
        }

        return list;
    }

    /**
     * Method to add known dependencies found within a parent directory to
     * the {@link #classpath()}.  An artifact
     * {@code artifactId-version.packaging} is a known artifact if there is
     * {@code META-INF/artifactId-version.packaging.dependencies} file on
     * the classpath which contains the artifact's dependencies.
     *
     * @param   parent          The parent {@link File} to analyze.
     *
     * @return  The {@link List} of {@link File}s added to the
     *          {@link #classpath()}.
     */
    public List<File> addKnownDependenciesToClasspath(File parent) {
        var list = new LinkedList<File>();

        try (var stream = Files.newDirectoryStream(parent.toPath(), "*.jar")) {
            for (var entry : stream) {
                var resource = String.format(DEPENDENCIES_FORMAT, entry.getFileName());

                try (var in = getClass().getResourceAsStream(resource)) {
                    if (in != null) {
                        var artifacts =
                            parse(in)
                            .map(t -> t.setFile(new File(parent,
                                                         t.getArtifactId()
                                                         + "-" + t.getVersion()
                                                         + "." + t.getExtension())
                                                .getAbsoluteFile()))
                            .filter(t -> t.getFile().exists())
                            .toArray(Artifact[]::new);

                        list.addAll(addToClasspath(artifacts));
                    }
                } catch (Exception exception) {
                    log.warn("Could not read {}", resource, exception);
                }
            }
        } catch (Exception exception) {
            log.warn("{}: {}", parent, exception);
        }

        return list;
    }

    /**
     * Merge the argument {@link POM} and resolve any dependencies.
     *
     * @param   shell           The {@link Shell}.
     * @param   out             The {@code stdout} {@link PrintStream}.
     * @param   err             The {@code stderr} {@link PrintStream}.
     * @param   pom             The {@link POM} to merge.
     *
     * @return  The {@link List} of {@link File}s added to the
     *          {@link #classpath()}.
     */
    public List<File> resolve(Shell shell, PrintStream out, PrintStream err, POM pom) {
        var files = new ArrayList<File>();

        pom().merge(pom);

        var session = session(out, err);
        var repositories = pom().getRepositories().stream().collect(toList());
        var scope = JavaScopes.RUNTIME;
        var filter = DependencyFilterUtils.classpathFilter(scope);
        var iterator = pom().getDependencies().iterator();

        while (iterator.hasNext()) {
            var dependency = iterator.next();

            try {
                var request = new DependencyRequest(new CollectRequest(dependency, repositories), filter);

                for (var result : system.resolveDependencies(session, request).getArtifactResults()) {
                    if (result.isResolved()) {
                        var artifact = repository.resolve(result.getArtifact());

                        if (! classpath.contains(artifact.getFile())) {
                            var installed =
                                repository.getArtifactsOn(classpath)
                                .filter(t -> ArtifactIdUtils.equalsVersionlessId(artifact, t))
                                .findFirst().orElse(null);

                            if (installed == null) {
                                files.addAll(addToClasspath(artifact));
                            } else {
                                log.debug("Ignored resolved artifact {}", artifact);
                                log.debug("    for {} @ {}",
                                          () -> installed.getVersion(),
                                          () -> pathMap.shorten(installed.getFile()));
                            }
                        }
                    }

                    if (result.isMissing()) {
                        result.getExceptions().stream()
                            .forEach(t -> err.println(t.getMessage()));
                    }
                }
                continue;
            } catch (DependencyResolutionException exception) {
                err.println(exception.getMessage());
            } catch (Exception exception) {
                exception.printStackTrace(err);
            }

            iterator.remove();
        }

        return files;
    }

    private RepositorySystemSession session(PrintStream out, PrintStream err) {
        var session = MavenRepositorySystemUtils.newSession();
        var properties = new LinkedHashMap<Object,Object>();

        properties.put(ConfigurationProperties.USER_AGENT,
                       String.format("Ganymede/(Java %s; %s)",
                                     System.getProperty("java.version"),
                                     System.getProperty("os.version")));
        /*
         * properties.putAll((Map<?,?>) project.getProperties());
         */
        /*
         * ${settings.servers}
         * AntRepoSys: processServerConfiguration(properties);
         */
        session.setConfigProperties(properties);
        session.setOffline(Objects.requireNonNullElse(pom().getOffline(), false));
        /*
         * AntRepoSys: session.setUserProperties(project.getUserProperties());
         */
        /*
         * ${settings.servers}, ${settings.mirrors}
         * session.setProxySelector(getProxySelector());
         * session.setMirrorSelector(getMirrorSelector());
         * session.setAuthenticationSelector(getAuthSelector());
         */
        session.setCache(new DefaultRepositoryCache());

        var path =
            Stream.of(pom().getLocalRepository(), System.getProperty("maven.repo.local"))
            .filter(Objects::nonNull)
            .map(Paths::get)
            .findFirst().orElse(Paths.get(System.getProperty("user.home"), ".m2", "repository"));
        var local = new LocalRepository(path.toFile());
        var manager = system.newLocalRepositoryManager(session, local);

        session.setLocalRepositoryManager(manager);
        session.setWorkspaceReader(repository);
        session.setRepositoryListener(new RepositoryListener(out, err));
        session.setTransferListener(new TransferListener(out, err));

        return session;
    }

    @NoArgsConstructor
    private class RepositoryImpl extends TreeMap<String,Artifact> implements WorkspaceReader {
        private static final long serialVersionUID = -1L;

        public Set<Artifact> resolve(Set<Artifact> set) {
            var resolved =
                set.stream()
                .map(this::resolve)
                .collect(toCollection(LinkedHashSet::new));

            return resolved;
        }

        public Artifact resolve(Artifact artifact) {
            if (artifact.getFile() == null) {
                throw new IllegalArgumentException();
            }

            return computeIfAbsent(ArtifactIdUtils.toId(artifact), k -> artifact);
        }

        public Stream<Artifact> getArtifactsOn(Set<File> classpath) {
            return values().stream().filter(t -> classpath.contains(t.getFile()));
        }

        @Override
        public WorkspaceRepository getRepository() {
            return new WorkspaceRepository(getClass().getPackage().getName());
        }

        @Override
        public File findArtifact(Artifact artifact) {
            var id = ArtifactIdUtils.toId(artifact);
            var file =
                values().stream()
                .filter(t -> Objects.equals(id, ArtifactIdUtils.toId(t)))
                .map(Artifact::getFile)
                .findFirst().orElse(null);

            return file;
        }

        @Override
        public List<String> findVersions(Artifact artifact) {
            var list =
                values().stream()
                .filter(t -> ArtifactIdUtils.equalsVersionlessId(artifact, t))
                .map(Artifact::getVersion)
                .collect(toList());

            return list;
        }
    }

    @RequiredArgsConstructor @ToString
    private class RepositoryListener extends AbstractRepositoryListener {
        private final PrintStream out;
        private final PrintStream err;

        @Override
        public void artifactDescriptorInvalid(RepositoryEvent event) {
        }

        @Override
        public void artifactDescriptorMissing(RepositoryEvent event) {
        }

        @Override
        public void metadataInvalid(RepositoryEvent event) {
        }
    }

    @RequiredArgsConstructor @ToString
    private class TransferListener extends AbstractTransferListener {
        private final PrintStream out;
        private final PrintStream err;

        @Override
        public void transferCorrupted(TransferEvent event) throws TransferCancelledException {
            err.format("%s%s: %s\n",
                       event.getResource().getRepositoryUrl(),
                       event.getResource().getResourceName(),
                       event.getException().getMessage());
        }
    }
}
