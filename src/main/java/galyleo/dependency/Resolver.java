package galyleo.dependency;

import galyleo.dependency.aether.NotebookRepositoryListener;
import galyleo.dependency.aether.NotebookTransferListener;
import galyleo.dependency.aether.POMDependencyList;
import galyleo.dependency.aether.POMRemoteRepositoryList;
import galyleo.shell.Shell;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.NoArgsConstructor;
import lombok.Synchronized;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.ConfigurationProperties;
import org.eclipse.aether.DefaultRepositoryCache;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.classpath.ClasspathTransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;

/**
 * Dependency {@link Resolver}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@NoArgsConstructor @ToString @Log4j2
public class Resolver extends Analyzer {
    private final POM pom;
    private RepositorySystem system = null;
    private RepositorySystemSession session = null;

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
     * Merge the argument {@link POM} and resolve any dependencies.
     *
     * @param   shell           The {@link Shell}.
     * @param   pom             The {@link POM} to merge.
     * @param   out             The {@code stdout} {@link PrintStream}.
     * @param   err             The {@code stderr} {@link PrintStream}.
     */
    public void resolve(Shell shell, POM pom, PrintStream out, PrintStream err) {
        try {
            boolean modified = pom().merge(pom);

            if (modified) {
                session = null;
            }

            var results =
                getRepositorySystem()
                .resolveDependencies(getRepositorySystemSession(),
                                     dependencyRequest());
results.getArtifactResults().stream().forEach(out::println);
        } catch (Exception exception) {
            exception.printStackTrace(err);
        }
    }

    @Synchronized
    private RepositorySystem getRepositorySystem() {
        if (system == null) {
            var locator = MavenRepositorySystemUtils.newServiceLocator();

            locator.addService(RepositoryConnectorFactory.class,
                               BasicRepositoryConnectorFactory.class);
            locator.addService(TransporterFactory.class,
                               FileTransporterFactory.class);
            locator.addService(TransporterFactory.class,
                               HttpTransporterFactory.class);
            locator.addService(TransporterFactory.class,
                               ClasspathTransporterFactory.class);
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
    private RepositorySystemSession getRepositorySystemSession() {
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
        session.setTransferListener(new NotebookTransferListener());
        session.setRepositoryListener(new NotebookRepositoryListener());

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

        return getRepositorySystem().newLocalRepositoryManager(session, repository);
    }

    private DependencyRequest dependencyRequest() {
        var scope = JavaScopes.RUNTIME;
        var filter = DependencyFilterUtils.classpathFilter(scope);

        return new DependencyRequest(collectRequest(), filter);
    }

    private CollectRequest collectRequest() {
        var scope = JavaScopes.RUNTIME;

        return new CollectRequest(new POMDependencyList(pom(), scope),
                                  List.of(),
                                  new POMRemoteRepositoryList(pom()));
    }
}
