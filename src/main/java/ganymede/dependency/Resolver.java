package ganymede.dependency;

import ganymede.shell.Shell;
import java.io.File;
import java.io.PrintStream;
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
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.transport.classpath.ClasspathTransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
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
 * @version $Revision$
 */
@NoArgsConstructor @ToString @Log4j2
public class Resolver extends Analyzer {
    private final POM pom;
    private final Set<File> classpath = new LinkedHashSet<>();
    private final RepositoryImpl repository = new RepositoryImpl();
    private RepositorySystem system = null;

    {
        try {
            pom = POM.getDefault();

            classpath.add(new ApplicationHome(getClass()).getSource());
            classpath.stream()
                .flatMap(t -> getJarArtifactSet(t).stream())
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
     * Method to add a {@link File}(s) to the {@link #classpath()}.
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

            var artifacts = getJarArtifactSet(file);

            if (! artifacts.isEmpty()) {
                for (var artifact : artifacts) {
                    if (classpath.add(artifact.getFile())) {
                        list.add(artifact.getFile());
                    }
                }
            } else {
                if (classpath.add(file)) {
                    list.add(file);
                }
            }
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

        var system = system();
        var session = session();
        var repositories = pom().getRepositories().stream().collect(toList());
        var scope = JavaScopes.RUNTIME;
        var filter = DependencyFilterUtils.classpathFilter(scope);
        var iterator = pom().getDependencies().iterator();

        while (iterator.hasNext()) {
            var dependency = iterator.next();

            try {
                var request = new DependencyRequest(new CollectRequest(dependency, repositories), filter);

                for (var result :
                         system.resolveDependencies(session, request)
                         .getArtifactResults()) {
                    if (result.isResolved()) {
                        var artifact = result.getArtifact();
                        var file = artifact.getFile();

                        if (classpath.add(file)) {
                            files.add(file);
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
        session.setLocalRepositoryManager(getLocalRepositoryManager(session));
        session.setRepositoryListener(new RepositoryListener());
        session.setTransferListener(new TransferListener());
        session.setWorkspaceReader(repository);

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

    @NoArgsConstructor @ToString
    private class RepositoryListener extends AbstractRepositoryListener {
    }

    @NoArgsConstructor @ToString
    private class TransferListener extends AbstractTransferListener {
    }
}
