package galyleo.dependency;

import galyleo.shell.Shell;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import lombok.NoArgsConstructor;
import lombok.Synchronized;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.ConfigurationProperties;
import org.eclipse.aether.DefaultRepositoryCache;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.repository.WorkspaceRepository;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.transport.classpath.ClasspathTransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;

import static java.util.stream.Collectors.toList;

/**
 * Dependency {@link Resolver}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@NoArgsConstructor @ToString @Log4j2
public class Resolver extends Analyzer implements WorkspaceReader {
    private final POM pom;
    private RepositorySystem system = null;
    private final Map<File,Set<Artifact>> classpath = new LinkedHashMap<>();

    {
        try {
            pom = POM.getDefault();
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
    public Set<File> classpath() { return classpath.keySet(); }

    /**
     * Method to add a {@link File} to the {@link #classpath()}.
     *
     * @param   file            The {@link File}.
     */
    public void addToClasspath(File file) {
        var key = file.getAbsoluteFile();

        if (! classpath.containsKey(key)) {
            classpath
                .computeIfAbsent(file, k -> new LinkedHashSet<>())
                .addAll(getJarArtifacts(key));
        }
    }

    /**
     * Merge the argument {@link POM} and resolve any dependencies.
     *
     * @param   shell           The {@link Shell}.
     * @param   pom             The {@link POM} to merge.
     * @param   out             The {@code stdout} {@link PrintStream}.
     * @param   err             The {@code stderr} {@link PrintStream}.
     *
     * @return  The {@link List} of {@link File}s added to the
     *          {@link #classpath()}.
     */
    public List<File> resolve(Shell shell, POM pom, PrintStream out, PrintStream err) {
        var added = new ArrayList<File>();

        try {
            pom().merge(pom);

            var results =
                system().resolveDependencies(session(), dependencyRequest());

            for (var result : results.getArtifactResults()) {
                var artifact = result.getArtifact();

                if (result.isResolved()) {
                    var file = artifact.getFile().getAbsoluteFile();

                    if (! classpath.containsKey(file)) {
                        added.add(file);
                    }

                    classpath
                        .computeIfAbsent(file, k -> new LinkedHashSet<>())
                        .add(artifact);
                }

                if (result.isMissing()) {
                    /*
                     * TBD -- Not working
                     */
                    pom().getDependencies()
                        .removeIf(t -> ArtifactIdUtils.equalsVersionlessId(t.getArtifact(), artifact));
                    result.getExceptions().stream()
                        .forEach(t -> t.printStackTrace(err));
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace(err);
        }

        return added;
    }

    @Synchronized
    private RepositorySystem system() {
        if (system == null) {
            var locator = MavenRepositorySystemUtils.newServiceLocator();

            locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
            locator.addService(TransporterFactory.class, FileTransporterFactory.class);
            locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
            locator.addService(TransporterFactory.class, ClasspathTransporterFactory.class);
            /* locator.setServices(Logger.class, log); */
            locator
                .setErrorHandler(new DefaultServiceLocator.ErrorHandler() {
                        @Override
                        public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable exception) {
                            log.error("Service creation failed for {} with implementation {}",
                                      type, impl, exception);
                        }
                });

            system = locator.getService(RepositorySystem.class);
        }

        return system;
    }

    @Synchronized
    private RepositorySystemSession session() {
        var session = MavenRepositorySystemUtils.newSession();
        var properties = new LinkedHashMap<Object,Object>();

        properties.put(ConfigurationProperties.USER_AGENT,
                       String.format("Galyleo/(Java %s; %s)",
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
        session.setLocalRepositoryManager(getLocalRepositoryManager(session));
        session.setRepositoryListener(new RepositoryListener());
        session.setTransferListener(new TransferListener());
        session.setWorkspaceReader(this);

        return session;
    }

    private LocalRepositoryManager getLocalRepositoryManager(RepositorySystemSession session) {
        var path =
            Stream.of(pom().getLocalRepository(),
                      System.getProperty("maven.repo.local"))
            .filter(Objects::nonNull)
            .map(Paths::get)
            .findFirst()
            .orElse(Paths.get(System.getProperty("user.home"), ".m2", "repository"));
        var repository = new LocalRepository(path.toFile());

        return system().newLocalRepositoryManager(session, repository);
    }

    private DependencyRequest dependencyRequest() {
        var scope = JavaScopes.RUNTIME;
        var filter = DependencyFilterUtils.classpathFilter(scope);

        return new DependencyRequest(collectRequest(), filter);
    }

    private CollectRequest collectRequest() {
        var dependencies = pom().getDependencies().stream().collect(toList());
        var repositories = pom().getRepositories().stream().collect(toList());

        return new CollectRequest(dependencies, List.of(), repositories);
    }

    @Override
    public WorkspaceRepository getRepository() {
        return new WorkspaceRepository(getClass().getPackage().getName());
    }

    @Override
    public File findArtifact(Artifact artifact) {
        var id = ArtifactIdUtils.toId(artifact);
        var file =
            classpath.entrySet().stream()
            .flatMap(t -> t.getValue().stream().map(u -> Map.entry(t.getKey(), u)))
            .filter(t -> Objects.equals(id, ArtifactIdUtils.toId(t.getValue())))
            .map(t -> t.getKey())
            .findFirst().orElse(null);

        return file;
    }

    @Override
    public List<String> findVersions(Artifact artifact) {
        var list =
            classpath.values().stream()
            .flatMap(Set::stream)
            .filter(t -> ArtifactIdUtils.equalsVersionlessId(artifact, t))
            .map(Artifact::getVersion)
            .collect(toList());

        return list;
    }

    @NoArgsConstructor @ToString
    private class RepositoryListener extends AbstractRepositoryListener {
    }

    @NoArgsConstructor @ToString
    private class TransferListener extends AbstractTransferListener {
    }
}
