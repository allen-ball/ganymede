package galyleo.dependency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.apache.maven.settings.Repository;

import static java.util.stream.Collectors.joining;

/**
 * Target class for YAML representation of POM elements.
 *
 * {@bean.info}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@JsonPropertyOrder({ "localRepository", "repositories", "dependencies" })
@Data @NoArgsConstructor @ToString @Log4j2
public class POM {
    private static final YAMLFactory YAML_FACTORY =
        new YAMLFactory()
        .enable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);
    private static final ObjectMapper OBJECT_MAPPER =
        new ObjectMapper(YAML_FACTORY)
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
    private List<Dependency> dependencies = List.of();
    private List<Repository> repositories = List.of();

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

    /**
     * {@link POM} {@link Dependency Dependency}.
     *
     * {@bean.info}
     */
    @JsonPropertyOrder({ "groupId", "artifactId", "version", "type" })
    @Data
    public static class Dependency {
        private String artifactId = "unknown";
        private String groupId = "unknown";
        private String version = null;
        private String scope = null;
        private String type = null;
        private String classifier = null;

        /**
         * No-argument constructor.
         */
        public Dependency() { }

        /**
         * G-A-V constructor.
         *
         * @param       gav     The "{@code groupId:artifactId:version}"
         *                      {@link String}.
         */
        public Dependency(String gav) { this(gav.split(":")); }

        private Dependency(String[] strings) {
            this();

            setGroupId(strings[0]);
            setArtifactId(strings[1]);
            setVersion((strings.length > 2) ? strings[2] : /* LATEST_VERSION */ null);
            setScope((strings.length > 3) ? strings[3] : /* "runtime" */ null);
            setType((strings.length > 4) ? strings[4] : /* "jar" */ null);
            setClassifier((strings.length > 5) ? strings[5] : null);
        }

        @Override
        public String toString() {
            var string =
                Stream.of(artifactId, groupId, version, type)
                .filter(Objects::nonNull)
                .collect(joining(":"));

            return string;
        }
    }
}
