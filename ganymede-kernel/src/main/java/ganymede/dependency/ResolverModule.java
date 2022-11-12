package ganymede.dependency;
/*-
 * ##########################################################################
 * Ganymede
 * %%
 * Copyright (C) 2022 Allen D. Ball
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
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Names;
import java.util.Map;
import java.util.Set;
import javax.inject.Named;
import javax.inject.Singleton;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.repository.internal.DefaultArtifactDescriptorReader;
import org.apache.maven.repository.internal.DefaultVersionRangeResolver;
import org.apache.maven.repository.internal.DefaultVersionResolver;
import org.apache.maven.repository.internal.SnapshotMetadataGeneratorFactory;
import org.apache.maven.repository.internal.VersionsMetadataGeneratorFactory;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.ArtifactDescriptorReader;
import org.eclipse.aether.impl.MetadataGeneratorFactory;
import org.eclipse.aether.impl.VersionRangeResolver;
import org.eclipse.aether.impl.VersionResolver;
import org.eclipse.aether.impl.guice.AetherModule;
import org.eclipse.aether.impl.guice.AetherModule;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.ChecksumExtractor;
import org.eclipse.aether.transport.http.HttpTransporterFactory;

/**
 * {@link com.google.inject.Guice} module for {@link Resolver}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@NoArgsConstructor @ToString @Log4j2
public class ResolverModule extends AbstractModule {
    @Override
    protected void configure() {
        install(new AetherModule());

        bind(ArtifactDescriptorReader.class)
            .to(DefaultArtifactDescriptorReader.class)
            .in(Singleton.class);
        bind(VersionResolver.class)
            .to(DefaultVersionResolver.class)
            .in(Singleton.class);
        bind(VersionRangeResolver.class)
            .to(DefaultVersionRangeResolver.class)
            .in(Singleton.class);
        bind(MetadataGeneratorFactory.class)
            .annotatedWith(Names.named("snapshot"))
            .to(SnapshotMetadataGeneratorFactory.class)
            .in(Singleton.class);
        bind(MetadataGeneratorFactory.class)
            .annotatedWith(Names.named("versions"))
            .to(VersionsMetadataGeneratorFactory.class)
            .in(Singleton.class);
        bind(RepositoryConnectorFactory.class)
            .annotatedWith(Names.named("basic"))
            .to(BasicRepositoryConnectorFactory.class);
        bind(TransporterFactory.class)
            .annotatedWith(Names.named("file"))
            .to(FileTransporterFactory.class);
        bind(TransporterFactory.class)
            .annotatedWith(Names.named("http"))
            .to(HttpTransporterFactory.class);
    }

    @Provides @Singleton
    public Map<String,ChecksumExtractor> provideChecksumExtractors() { return Map.of(); }

    @Provides @Singleton
    public Set<RepositoryConnectorFactory> provideRepositoryConnectorFactories(@Named("basic") RepositoryConnectorFactory basic)
    {
        return Set.of(basic);
    }

    @Provides @Singleton
    public Set<TransporterFactory> provideTransporterFactories(@Named("file") TransporterFactory file,
                                                               @Named("http") TransporterFactory http) {
        return Set.of(file, http);
    }

    @Provides @Singleton
    public Set<MetadataGeneratorFactory> provideMetadataGeneratorFactories(@Named("snapshot") MetadataGeneratorFactory snapshot,
                                                                           @Named("versions") MetadataGeneratorFactory versions) {
        return Set.of(snapshot, versions);
    }

    @Provides
    public ModelBuilder provideModelBuilder() { return new DefaultModelBuilderFactory().newInstance(); }
}
