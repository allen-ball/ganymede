package galyleo.dependency.aether;

import org.apache.maven.settings.RepositoryPolicy;
import galyleo.dependency.POM;
import java.util.ArrayList;
import java.util.Objects;
import org.apache.maven.settings.Repository;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * {@link POM} {@link RemoteRepository} {@link java.util.List}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
public class POMRemoteRepositoryList extends ArrayList<RemoteRepository> {
    private static final long serialVersionUID = 7678062416229074632L;

    /** @serial */ private final POM pom;

    /**
     * Sole constructor.
     *
     * @param   pom             The {@link POM}.
     */
    public POMRemoteRepositoryList(POM pom) {
        this.pom = Objects.requireNonNull(pom);

        pom.getRepositories().stream()
            .forEach(t -> add(asRemoteRepository(t)));
    }

    private RemoteRepository asRemoteRepository(Repository repository) {
        var builder =
            new RemoteRepository.Builder(repository.getId(),
                                         repository.getLayout(),
                                         repository.getUrl());

        if (repository.getReleases() != null) {
            /*
             * TBD
             */
        }

        if (repository.getSnapshots() != null) {
            /*
             * TBD
             */
        }

        return builder.build();
    }
}
