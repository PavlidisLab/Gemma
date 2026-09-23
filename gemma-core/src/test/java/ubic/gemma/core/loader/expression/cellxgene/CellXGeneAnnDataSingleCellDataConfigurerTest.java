package ubic.gemma.core.loader.expression.cellxgene;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ubic.gemma.core.loader.expression.singleCell.AnnDataSingleCellDataLoaderConfig;
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataType;
import ubic.gemma.core.loader.expression.singleCell.transform.SingleCellDataSortBySample;
import ubic.gemma.core.loader.expression.singleCell.transform.SingleCellDataTransformationFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CellXGeneAnnDataSingleCellDataConfigurerTest {

    @TempDir
    Path dir;

    /**
     * The sorted file was written in place in the pre-transposed directory, and every later load uses a file found
     * there without checking it, so a sort that failed midway left a partial file that was loaded from then on.
     */
    @Test
    void whenSortingFails_noFileIsLeftInThePreTransposedDirectory() throws IOException {
        Path annDataFile = dir.resolve( "dataset.h5ad" );
        Path transposedDir = dir.resolve( "transposed" );
        SingleCellDataTransformationFactory factory = mock();
        SingleCellDataSortBySample sortBySample = mock();
        when( factory.getTransformation( SingleCellDataSortBySample.class ) ).thenReturn( sortBySample );
        AtomicReference<Path> output = new AtomicReference<>();
        doAnswer( inv -> {
            output.set( inv.getArgument( 0 ) );
            return null;
        } ).when( sortBySample ).setOutputFile( any(), eq( SingleCellDataType.ANNDATA ) );
        doAnswer( inv -> {
            Files.write( output.get(), "partial".getBytes( StandardCharsets.UTF_8 ) );
            throw new IOException( "sort-by-sample-anndata.py failed" );
        } ).when( sortBySample ).perform();

        // no on-disk transpose, so the sort is the only transformation
        AnnDataSingleCellDataLoaderConfig config = AnnDataSingleCellDataLoaderConfig.builder().transpose( false ).build();
        assertThatThrownBy( () -> new CellXGeneAnnDataSingleCellDataConfigurer( annDataFile, factory, transposedDir ).configureLoader( config ) )
                .hasRootCauseMessage( "sort-by-sample-anndata.py failed" );

        assertThat( transposedDir.resolve( "dataset.h5ad" ) ).doesNotExist();
        try ( Stream<Path> files = Files.list( transposedDir ) ) {
            assertThat( files ).isEmpty();
        }
    }
}
