package galyleo.dependency;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;

import static java.util.stream.Collectors.toSet;
import static org.eclipse.aether.util.artifact.ArtifactIdUtils.toVersionlessId;

/**
 * Target class for YAML representation of POM elements.
 *
 * {@bean.info}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@JsonPropertyOrder({ "localRepository", "interactiveMode", "offline",
                     "repositories", "dependencies" })
@Data @NoArgsConstructor @ToString @Log4j2
public class POM {
    private static final YAMLFactory YAML_FACTORY =
        new YAMLFactory()
        .enable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);
    private static final SimpleModule MODULE =
        new SimpleModule()
        .addDeserializer(Dependency.class, new DependencyDeserializer())
        .addSerializer(Dependency.class, new DependencySerializer())
        .addDeserializer(RemoteRepository.class, new RemoteRepositoryDeserializer());
    private static final ObjectMapper OBJECT_MAPPER =
        new ObjectMapper(YAML_FACTORY)
        .registerModule(MODULE)
        .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
        .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_VALUES)
        .enable(SerializationFeature.INDENT_OUTPUT);

    /**
     * Static method to get default {@link POM}.
     *
     * @return  The parsed {@link POM}.
     */
    public static POM getDefault() throws Exception {
        POM pom = null;
        var resource = POM.class.getSimpleName() + ".yaml";

        try (var in = POM.class.getResourceAsStream(resource)) {
            pom = OBJECT_MAPPER.readValue(in, POM.class);
        }

        return pom;
    }

    /**
     * Static method to parse a YAML {@link String} to a {@link POM}.
     *
     * @param   yaml            The YAML {@link String}.
     *
     * @return  The parsed {@link POM}.
     */
    public static POM parse(String yaml) throws Exception {
        return OBJECT_MAPPER.readValue(yaml, POM.class);
    }

    private String localRepository = null;
    private Boolean interactiveMode = null;
    private Boolean offline = null;
    private Set<RemoteRepository> repositories = new LinkedHashSet<>();
    private Set<Dependency> dependencies = new LinkedHashSet<>();

    /**
     * Method to merge a {@link POM} into {@link.this} {@link POM}.
     *
     * @param   that            The {@link POM} to merge.
     */
    public void merge(POM that) {
        update(this, that, POM::getLocalRepository, this::setLocalRepository);
        update(this, that, POM::getInteractiveMode, this::setInteractiveMode);
        update(this, that, POM::getOffline, this::setOffline);

        var ids =
            that.getRepositories().stream()
            .map(t -> t.getId())
            .filter(Objects::nonNull)
            .collect(toSet());
        var urls =
            that.getRepositories().stream()
            .map(t -> t.getUrl())
            .filter(Objects::nonNull)
            .collect(toSet());

        this.getRepositories()
            .removeIf(t -> ids.contains(t.getId()) || urls.contains(t.getUrl()));
        this.getRepositories().addAll(that.getRepositories());

        var keys =
            that.getDependencies().stream()
            .map(Dependency::getArtifact)
            .map(t -> toVersionlessId(t))
            .collect(toSet());

        this.getDependencies()
            .removeIf(t -> keys.contains(toVersionlessId(t.getArtifact())));
        this.getDependencies().addAll(that.getDependencies());
    }

    private <T,U> boolean update(T left, T right, Function<T,U> get, Consumer<U> set) {
        var value = get.apply(right);
        var modified = (value != null && (! Objects.equals(get.apply(left), value)));

        if (modified) {
            set.accept(value);
        }

        return modified;
    }

    /**
     * Method to write {@link.this} {@link POM}'s YAML representation.
     *
     * @param   out             The target {@link OutputStream}.
     *
     * @throws  IOException     If the {@link POM} cannot be written for any
     *                          reason.
     */
    public void writeTo(OutputStream out) throws IOException {
        OBJECT_MAPPER.writeValue(out, this);
    }

    private static String asText(JsonNode node, String name) {
        return asText(node, name, null);
    }

    private static String asText(JsonNode node, String name, String defaultValue) {
        return node.has(name) ? node.get(name).asText() : defaultValue;
    }

    @ToString
    private static class DependencyDeserializer extends StdDeserializer<Dependency> {

        public DependencyDeserializer() { super(Dependency.class); }

        @Override
        public Dependency deserialize(JsonParser parser,
                                      DeserializationContext context) throws IOException,
                                                                             JsonProcessingException {
            JsonNode node = parser.getCodec().readTree(parser);
            Artifact artifact = null;

            if (node instanceof ObjectNode) {
                artifact =
                    new DefaultArtifact(asText(node, "groupId"),
                                        asText(node, "artifactId"),
                                        asText(node, "classifier"),
                                        asText(node, "type", "jar"),
                                        asText(node, "version"),
                                        Map.of(), (File) null);
            } else {
                var strings = node.asText().split(":");

                artifact =
                    new DefaultArtifact(strings[0], strings[1],
                                        null, "jar", strings[2],
                                        Map.of(), (File) null);
            }

            return new Dependency(artifact, asText(node, "scope", "runtime"));
        }
    }

    @ToString
    private static class DependencySerializer extends StdSerializer<Dependency> {

        public DependencySerializer() { super(Dependency.class); }

        @Override
        public void serialize(Dependency value, JsonGenerator generator,
                              SerializerProvider provider) throws IOException,
                                                                  JsonProcessingException {
            generator.writeString(value.getArtifact().toString());
        }
    }

    @ToString
    private static class RemoteRepositoryDeserializer extends StdDeserializer<RemoteRepository> {

        public RemoteRepositoryDeserializer() { super(RemoteRepository.class); }

        @Override
        public RemoteRepository deserialize(JsonParser parser,
                                            DeserializationContext context) throws IOException,
                                                                                   JsonProcessingException {
            JsonNode node = parser.getCodec().readTree(parser);
            var builder =
                new RemoteRepository.Builder(asText(node, "id"),
                                             asText(node, "layout"),
                                             asText(node, "url"));

            if (node.has("releases")) {
                /*
                 * TBD
                 */
            }

            if (node.has("snapshots")) {
                /*
                 * TBD
                 */
            }

            return builder.build();
        }
    }
}
