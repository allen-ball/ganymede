package galyleo.dependency;

import galyleo.shell.Shell;
import java.io.PrintStream;
import java.util.Objects;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.settings.building.DefaultSettingsBuilderFactory;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuilder;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.classpath.ClasspathTransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;

import static java.util.stream.Collectors.toSet;
import static org.apache.maven.artifact.ArtifactUtils.versionlessKey;

/**
 * Dependency {@link Resolver}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@NoArgsConstructor @ToString @Log4j2
public class Resolver extends Analyzer {
    private static final ModelBuilder MODEL_BUILDER =
        new DefaultModelBuilderFactory().newInstance();
    private static final SettingsBuilder SETTINGS_BUILDER =
        new DefaultSettingsBuilderFactory().newInstance();

    private final POM pom;

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
            merge(pom);

            var locator = MavenRepositorySystemUtils.newServiceLocator();
/*
            locator.setErrorHandler(new AntServiceLocatorErrorHandler(project));
            locator.setServices(Logger.class, new AntLogger(project));
*/
            locator.setServices(ModelBuilder.class, MODEL_BUILDER);
            locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
            locator.addService(TransporterFactory.class, FileTransporterFactory.class);
            locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
            locator.addService(TransporterFactory.class, ClasspathTransporterFactory.class);
/*
            DefaultSettingsBuildingRequest request = new DefaultSettingsBuildingRequest();

            request.setUserSettingsFile(getUserSettings());
            request.setGlobalSettingsFile(getGlobalSettings());
            request.setSystemProperties(getSystemProperties());
            request.setUserProperties(getUserProperties());

            try {
                settings = SETTINGS_BUILDER.build(request).getEffectiveSettings();
            } catch (SettingsBuildingException exception) {
                log.warn("Could not process settings.xml: {}",
                         exception.getMessage(), exception);
            }

            SettingsDecryptionResult result =
                SETTINGS_DECRYPTER.decrypt(new DefaultSettingsDecryptionRequest(settings));
            settings.setServers(result.getServers());
            settings.setProxies(result.getProxies());
*/
        } catch (Exception exception) {
            exception.printStackTrace(err);
        }
    }

    private void merge(POM update) {
        if (update.getLocalRepository() != null) {
            pom.setLocalRepository(update.getLocalRepository());
        }

        var ids =
            update.getRepositories().stream()
            .map(t -> t.getId())
            .filter(Objects::nonNull)
            .collect(toSet());
        var urls =
            update.getRepositories().stream()
            .map(t -> t.getUrl())
            .filter(Objects::nonNull)
            .collect(toSet());

        pom.getRepositories()
            .removeIf(t -> ids.contains(t.getId()) || urls.contains(t.getUrl()));
        pom.getRepositories().addAll(update.getRepositories());

        var keys =
            update.getDependencies().stream()
            .map(t -> versionlessKey(t))
            .collect(toSet());

        pom.getDependencies().removeIf(t -> keys.contains(versionlessKey(t)));
        pom.getDependencies().addAll(update.getDependencies());
    }
}
