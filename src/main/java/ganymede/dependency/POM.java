package ganymede.dependency;
/*-
 * ##########################################################################
 * Ganymede
 * $Id$
 * $HeadURL$
 * %%
 * Copyright (C) 2021 Allen D. Ball
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.util.artifact.JavaScopes;

import static java.util.stream.Collectors.joining;
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
        .addDeserializer(RemoteRepository.class, new RemoteRepositoryDeserializer())
        .addSerializer(RemoteRepository.class, new RemoteRepositorySerializer())
        .addDeserializer(RepositoryPolicy.class, new RepositoryPolicyDeserializer());
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
    public static POM getDefault() throws IOException {
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
        private static final long serialVersionUID = -1L;

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
                /*
                 * groupId:artifactId[:type[:classifier]]:version
                 */
                artifact =
                    new DefaultArtifact(strings[0], strings[1],
                                        (strings.length > 4) ? strings[3] : null,
                                        (strings.length > 3) ? strings[2] : "jar",
                                        (strings.length > 2) ? strings[strings.length - 1] : null,
                                        Map.of(), (File) null);
            }

            return new Dependency(artifact, asText(node, "scope", JavaScopes.RUNTIME));
        }
    }

    @ToString
    private static class DependencySerializer extends StdSerializer<Dependency> {
        private static final long serialVersionUID = -1L;

        public DependencySerializer() { super(Dependency.class); }

        @Override
        public void serialize(Dependency value, JsonGenerator generator,
                              SerializerProvider provider) throws IOException,
                                                                  JsonProcessingException {
            var artifact = value.getArtifact();
            var string =
                Stream.of(artifact.getGroupId(), artifact.getArtifactId(),
                          artifact.getExtension(), artifact.getClassifier(),
                          artifact.getVersion())
                .filter(Objects::nonNull)
                .filter(t -> (! t.isBlank()))
                .collect(joining(":"));

            generator.writeString(string);
        }
    }

    @ToString
    private static class RemoteRepositoryDeserializer extends StdDeserializer<RemoteRepository> {
        private static final long serialVersionUID = -1L;

        public RemoteRepositoryDeserializer() { super(RemoteRepository.class); }

        @Override
        public RemoteRepository deserialize(JsonParser parser,
                                            DeserializationContext context) throws IOException,
                                                                                   JsonProcessingException {
            JsonNode node = parser.getCodec().readTree(parser);
            var builder =
                new RemoteRepository.Builder(asText(node, "id"),
                                             asText(node, "layout", "default"),
                                             asText(node, "url"));

            if (node.has("releases")) {
                var policy =
                    parser.getCodec().treeToValue(node.get("releases"), RepositoryPolicy.class);

                builder.setReleasePolicy(policy);
            }

            if (node.has("snapshots")) {
                var policy =
                    parser.getCodec().treeToValue(node.get("snapshots"), RepositoryPolicy.class);

                builder.setSnapshotPolicy(policy);
            }

            return builder.build();
        }
    }

    @ToString
    private static class RemoteRepositorySerializer extends StdSerializer<RemoteRepository> {
        private static final long serialVersionUID = -1L;

        public RemoteRepositorySerializer() { super(RemoteRepository.class); }

        @Override
        public void serialize(RemoteRepository value, JsonGenerator generator,
                              SerializerProvider provider) throws IOException,
                                                                  JsonProcessingException {
            var map = new LinkedHashMap<String,Object>();

            map.put("id", value.getId());
            map.put("layout", value.getContentType());
            map.put("url", value.getUrl());
            map.put("releases", value.getPolicy(false));
            map.put("snapshots", value.getPolicy(true));

            generator.writeObject(map);
        }
    }

    @ToString
    private static class RepositoryPolicyDeserializer extends StdDeserializer<RepositoryPolicy> {
        private static final long serialVersionUID = -1L;

        public RepositoryPolicyDeserializer() { super(RepositoryPolicy.class); }

        @Override
        public RepositoryPolicy deserialize(JsonParser parser,
                                            DeserializationContext context) throws IOException,
                                                                                   JsonProcessingException {
            JsonNode node = parser.getCodec().readTree(parser);

            return new RepositoryPolicy(node.has("enabled") ? node.get("enabled").asBoolean() : true,
                                        asText(node, "updatePolicy", RepositoryPolicy.UPDATE_POLICY_DAILY),
                                        asText(node, "checksumPolicy", RepositoryPolicy.CHECKSUM_POLICY_WARN));
        }
    }
}
