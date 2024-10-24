package ubic.gemma.core.datastructure.matrix.io;

import no.uib.cipr.matrix.io.MatrixInfo;
import no.uib.cipr.matrix.io.MatrixSize;
import no.uib.cipr.matrix.io.MatrixVectorReader;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.file.PathUtils;
import org.junit.Test;
import ubic.gemma.core.datastructure.matrix.DoubleSingleCellExpressionDataMatrix;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static ubic.gemma.persistence.service.expression.experiment.SingleCellTestUtils.randomSingleCellVectors;

public class MexMatrixWriterTest {

    private final MexMatrixWriter writer = new MexMatrixWriter();

    @Test
    public void test() throws IOException {
        Collection<SingleCellExpressionDataVector> vectors = randomSingleCellVectors();
        DoubleSingleCellExpressionDataMatrix matrix = new DoubleSingleCellExpressionDataMatrix( vectors );
        byte[] blob;
        try ( ByteArrayOutputStream baos = new ByteArrayOutputStream() ) {
            writer.write( matrix, null, baos );
            blob = baos.toByteArray();
        }
        List<BioAssay> bas = vectors.iterator().next().getSingleCellDimension().getBioAssays();
        List<String> expectedEntries = new ArrayList<>();
        for ( BioAssay ba : bas ) {
            expectedEntries.addAll( Arrays.asList(
                    ba.getId() + "_" + ba.getName() + "/barcodes.tsv", ba.getId() + "_" + ba.getName() + "/features.tsv", ba.getId() + "_" + ba.getName() + "/matrix.mtx"
            ) );
        }

        int i = 0;
        try ( TarArchiveInputStream is = new TarArchiveInputStream( new ByteArrayInputStream( blob ) ) ) {
            TarArchiveEntry entry;
            while ( ( entry = is.getNextEntry() ) != null ) {
                assertThat( entry.getName() ).isEqualTo( expectedEntries.get( i ) );
                if ( i % 3 == 0 ) {
                    assertThat( is ).asString( StandardCharsets.UTF_8 ).hasLineCount( 1000 );
                } else if ( i % 3 == 1 ) {
                    assertThat( is ).asString( StandardCharsets.UTF_8 ).hasLineCount( 100 );
                } else {
                    try ( MatrixVectorReader mvr = new MatrixVectorReader( new InputStreamReader( is ) ) ) {
                        MatrixInfo mi = mvr.readMatrixInfo();
                        MatrixSize size = mvr.readMatrixSize( mi );
                        assertThat( size.numRows() ).isEqualTo( 100 );
                        assertThat( size.numColumns() ).isEqualTo( 1000 );
                        assertThat( size.numEntries() ).isEqualTo( 10000 );
                        int[] rows = new int[size.numEntries()], cols = new int[size.numEntries()];
                        double[] data = new double[size.numEntries()];
                        mvr.readCoordinate( rows, cols, data );
                        assertThat( data ).hasSize( 10000 );
                    }
                }
                i++;
            }
        }
    }

    @Test
    public void testWriteToDisk() throws IOException {
        Collection<SingleCellExpressionDataVector> vectors = randomSingleCellVectors();
        ExpressionExperiment ee = vectors.iterator().next().getExpressionExperiment();
        Path outDir = Files.createTempDirectory( null ).resolve( "test" );
        try {
            Map<BioAssay, Long> nnzBySample = ee.getBioAssays().stream().collect( Collectors.toMap( ba -> ba, ba -> 100L * 100 ) );
            writer.write( vectors.stream(), vectors.size(), nnzBySample, null, outDir );
            for ( BioAssay ba : ee.getBioAssays() ) {
                assertThat( outDir )
                        .isDirectoryRecursivelyContaining( "glob:**/" + ba.getId() + "_" + ba.getName() + "/features.tsv.gz" )
                        .isDirectoryRecursivelyContaining( "glob:**/" + ba.getId() + "_" + ba.getName() + "/barcodes.tsv.gz" )
                        .isDirectoryRecursivelyContaining( "glob:**/" + ba.getId() + "_" + ba.getName() + "/matrix.mtx.gz" );
                assertThat( new GZIPInputStream( Files.newInputStream( outDir.resolve( ba.getId() + "_" + ba.getName() ).resolve( "features.tsv.gz" ) ) ) )
                        .asString( StandardCharsets.UTF_8 ).hasLineCount( 100 );
                assertThat( new GZIPInputStream( Files.newInputStream( outDir.resolve( ba.getId() + "_" + ba.getName() ).resolve( "barcodes.tsv.gz" ) ) ) )
                        .asString( StandardCharsets.UTF_8 ).hasLineCount( 1000 );
                try ( MatrixVectorReader mvr = new MatrixVectorReader( new InputStreamReader( new GZIPInputStream( Files.newInputStream( outDir.resolve( ba.getId() + "_" + ba.getName() ).resolve( "matrix.mtx.gz" ) ) ) ) ) ) {
                    MatrixInfo mi = mvr.readMatrixInfo();
                    MatrixSize size = mvr.readMatrixSize( mi );
                    assertThat( size.numRows() ).isEqualTo( 100 );
                    assertThat( size.numColumns() ).isEqualTo( 1000 );
                    assertThat( size.numEntries() ).isEqualTo( 10000 );
                    int[] rows = new int[size.numEntries()], cols = new int[size.numEntries()];
                    double[] data = new double[size.numEntries()];
                    mvr.readCoordinate( rows, cols, data );
                    assertThat( data ).hasSize( 10000 );
                }
            }
        } finally {
            PathUtils.deleteDirectory( outDir );
        }
    }
}