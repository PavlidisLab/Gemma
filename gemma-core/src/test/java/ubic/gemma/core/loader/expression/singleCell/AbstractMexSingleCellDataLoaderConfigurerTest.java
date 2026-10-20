package ubic.gemma.core.loader.expression.singleCell;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for MEX directory resolution in {@link AbstractMexSingleCellDataLoaderConfigurer}, in particular the
 * fallback that locates the triplet under a raw 10x Cell Ranger {@code outs/filtered_feature_bc_matrix} nesting.
 */
public class AbstractMexSingleCellDataLoaderConfigurerTest {

    @Rule
    public final TemporaryFolder tmp = new TemporaryFolder();

    /**
     * Minimal configurer exposing a single sample directory. Vector detection/filtering is never reached because the
     * tests pass {@code ignoreDataVectors}, so the {@code detect10x*} hooks are irrelevant.
     */
    private static class TestConfigurer extends AbstractMexSingleCellDataLoaderConfigurer {

        private final Path sampleDir;

        TestConfigurer( Path sampleDir ) {
            super( null );
            this.sampleDir = sampleDir;
        }

        @Override
        protected List<String> getSampleNames() {
            return Collections.singletonList( "GSM1" );
        }

        @Override
        protected List<Path> getSampleDirs() {
            return Collections.singletonList( sampleDir );
        }

        @Override
        protected String detect10xGenome( String sampleName, Path sampleDir ) {
            throw new UnsupportedOperationException();
        }

        @Nullable
        @Override
        protected String detect10xChemistry( String sampleName, Path sampleDir ) {
            throw new UnsupportedOperationException();
        }
    }

    private static SingleCellDataLoaderConfig ignoreVectorsConfig() {
        return SingleCellDataLoaderConfig.builder().ignoreDataVectors( true ).build();
    }

    private static void writeTriplet( Path dir ) throws IOException {
        Files.createDirectories( dir );
        Files.createFile( dir.resolve( "barcodes.tsv.gz" ) );
        Files.createFile( dir.resolve( "features.tsv.gz" ) );
        Files.createFile( dir.resolve( "matrix.mtx.gz" ) );
    }

    @Test
    public void whenTripletIsDirectlyInSampleDir_thenItIsUsed() throws IOException {
        // flat layout: this is what Gemma produces when it downloads MEX from GEO (the automatic pipeline path)
        Path sampleDir = tmp.getRoot().toPath().resolve( "GSM1" );
        writeTriplet( sampleDir );
        MexSingleCellDataLoader loader = new TestConfigurer( sampleDir ).configureLoader( ignoreVectorsConfig() );
        assertThat( loader.getSampleNames() ).containsExactly( "GSM1" );
    }

    @Test
    public void whenTripletIsNestedUnderCellRangerOuts_thenItIsResolved() throws IOException {
        // raw 10x Cell Ranger layout: GSM1/outs/filtered_feature_bc_matrix/{barcodes,features,matrix}
        Path sampleDir = tmp.getRoot().toPath().resolve( "GSM1" );
        writeTriplet( sampleDir.resolve( "outs" ).resolve( "filtered_feature_bc_matrix" ) );
        MexSingleCellDataLoader loader = new TestConfigurer( sampleDir ).configureLoader( ignoreVectorsConfig() );
        assertThat( loader.getSampleNames() ).containsExactly( "GSM1" );
    }

    @Test
    public void whenTripletIsNestedUnderRawMatrix_thenItIsResolved() throws IOException {
        Path sampleDir = tmp.getRoot().toPath().resolve( "GSM1" );
        writeTriplet( sampleDir.resolve( "outs" ).resolve( "raw_feature_bc_matrix" ) );
        MexSingleCellDataLoader loader = new TestConfigurer( sampleDir ).configureLoader( ignoreVectorsConfig() );
        assertThat( loader.getSampleNames() ).containsExactly( "GSM1" );
    }

    @Test
    public void whenTripletIsMissingEntirely_thenItThrows() throws IOException {
        Path sampleDir = tmp.getRoot().toPath().resolve( "GSM1" );
        Files.createDirectories( sampleDir );
        assertThatThrownBy( () -> new TestConfigurer( sampleDir ).configureLoader( ignoreVectorsConfig() ) )
                .isInstanceOf( IllegalStateException.class )
                .hasMessageContaining( "Expected MEX files are missing" );
    }
}
