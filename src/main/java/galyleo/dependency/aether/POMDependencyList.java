package galyleo.dependency.aether;

import galyleo.dependency.POM;
import java.util.ArrayList;
import java.util.Objects;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;

/**
 * {@link POM} {@link Dependency} {@link java.util.List}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
public class POMDependencyList extends ArrayList<Dependency> {
    private static final long serialVersionUID = -4860967763014841004L;

    /** @serial */ private final POM pom;

    /**
     * Sole constructor.
     *
     * @param   pom             The {@link POM}.
     * @param   scope           The scope.
     */
    public POMDependencyList(POM pom, String scope) {
        this.pom = Objects.requireNonNull(pom);

        pom.getDependencies().stream()
            .map(t -> new DefaultArtifact(t.getGroupId(), t.getArtifactId(),
                                          t.getClassifier(), t.getType(),
                                          t.getVersion()))
            .map(t -> new Dependency(t, scope))
            .forEach(this::add);
    }
}
