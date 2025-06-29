package ubic.gemma.core.datastructure.matrix.io;

import no.uib.cipr.matrix.io.MatrixInfo;
import no.uib.cipr.matrix.io.MatrixSize;
import no.uib.cipr.matrix.io.MatrixVectorReader;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.file.PathUtils;
import org.junit.Before;
import org.junit.Test;
import ubic.gemma.core.datastructure.matrix.SingleCellExpressionDataDoubleMatrix;
import ubic.gemma.core.datastructure.matrix.SingleCellExpressionDataIntMatrix;
import ubic.gemma.core.util.concurrent.Executors;
import ubic.gemma.model.common.quantitationtype.*;
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
import static ubic.gemma.core.analysis.service.ExpressionDataFileUtils.formatBioAssayFilename;
import static ubic.gemma.persistence.service.expression.bioAssayData.RandomSingleCellDataUtils.randomSingleCellVectors;

public class MexMatrixWriterTest {

    private MexMatrixWriter writer;
    private int numDesignElements;
    private int numCellsPerBioAssay;
    private int nnz;

    @Before
    public void setUp() {
        writer = new MexMatrixWriter();
        numDesignElements = 100;
        numCellsPerBioAssay = 1000;
        nnz = ( int ) ( 0.1 * numDesignElements * numCellsPerBioAssay );
    }

    @Test
    public void testWriteDoubleVectorsToDisk() throws IOException {
        Collection<SingleCellExpressionDataVector> vectors = randomSingleCellVectors( numDesignElements, 8, numCellsPerBioAssay, 0.9, ScaleType.LINEAR );
        ExpressionExperiment ee = vectors.iterator().next().getExpressionExperiment();
        Path outDir = Files.createTempDirectory( null );
        try {
            Map<BioAssay, Long> nnzBySample = ee.getBioAssays().stream().collect( Collectors.toMap( ba -> ba, ba -> ( long ) nnz ) );
            writer.write( vectors.stream(), vectors.size(), nnzBySample, null, outDir.resolve( "test" ) );
            for ( BioAssay ba : ee.getBioAssays() ) {
                String dirName = formatBioAssayFilename( ba );
                assertThat( outDir )
                        .isDirectoryRecursivelyContaining( "glob:**/" + dirName + "/features.tsv.gz" )
                        .isDirectoryRecursivelyContaining( "glob:**/" + dirName + "/barcodes.tsv.gz" )
                        .isDirectoryRecursivelyContaining( "glob:**/" + dirName + "/matrix.mtx.gz" );
                assertThat( new GZIPInputStream( Files.newInputStream( outDir.resolve( "test" ).resolve( dirName ).resolve( "features.tsv.gz" ) ) ) )
                        .asString( StandardCharsets.UTF_8 ).hasLineCount( numDesignElements );
                assertThat( new GZIPInputStream( Files.newInputStream( outDir.resolve( "test" ).resolve( dirName ).resolve( "barcodes.tsv.gz" ) ) ) )
                        .asString( StandardCharsets.UTF_8 ).hasLineCount( numCellsPerBioAssay );
                try ( MatrixVectorReader mvr = new MatrixVectorReader( new InputStreamReader( new GZIPInputStream( Files.newInputStream( outDir.resolve( "test" ).resolve( dirName ).resolve( "matrix.mtx.gz" ) ) ) ) ) ) {
                    MatrixInfo mi = mvr.readMatrixInfo();
                    assertThat( mi.isReal() ).isTrue();
                    MatrixSize size = mvr.readMatrixSize( mi );
                    assertThat( size.numRows() ).isEqualTo( numDesignElements );
                    assertThat( size.numColumns() ).isEqualTo( numCellsPerBioAssay );
                    assertThat( size.numEntries() ).isEqualTo( nnz );
                    int[] rows = new int[size.numEntries()], cols = new int[size.numEntries()];
                    double[] data = new double[size.numEntries()];
                    mvr.readCoordinate( rows, cols, data );
                    assertThat( data ).hasSize( nnz );
                }
            }
        } finally {
            PathUtils.deleteDirectory( outDir );
        }
    }


    @Test
    public void testWriteFloatVectorsToDisk() throws IOException {
        QuantitationType qt = new QuantitationType();
        qt.setName( "Counts" );
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.COUNT );
        qt.setScale( ScaleType.COUNT );
        qt.setRepresentation( PrimitiveType.FLOAT );
        Collection<SingleCellExpressionDataVector> vectors = randomSingleCellVectors( numDesignElements, 8, numCellsPerBioAssay, 0.9, qt );
        ExpressionExperiment ee = vectors.iterator().next().getExpressionExperiment();
        Path outDir = Files.createTempDirectory( null );
        try {
            Map<BioAssay, Long> nnzBySample = ee.getBioAssays().stream().collect( Collectors.toMap( ba -> ba, ba -> ( long ) nnz ) );
            writer.write( vectors.stream(), vectors.size(), nnzBySample, null, outDir.resolve( "test" ) );
            for ( BioAssay ba : ee.getBioAssays() ) {
                String dirName = formatBioAssayFilename( ba );
                assertThat( outDir )
                        .isDirectoryRecursivelyContaining( "glob:**/" + dirName + "/features.tsv.gz" )
                        .isDirectoryRecursivelyContaining( "glob:**/" + dirName + "/barcodes.tsv.gz" )
                        .isDirectoryRecursivelyContaining( "glob:**/" + dirName + "/matrix.mtx.gz" );
                assertThat( new GZIPInputStream( Files.newInputStream( outDir.resolve( "test" ).resolve( dirName ).resolve( "features.tsv.gz" ) ) ) )
                        .asString( StandardCharsets.UTF_8 ).hasLineCount( numDesignElements );
                assertThat( new GZIPInputStream( Files.newInputStream( outDir.resolve( "test" ).resolve( dirName ).resolve( "barcodes.tsv.gz" ) ) ) )
                        .asString( StandardCharsets.UTF_8 ).hasLineCount( numCellsPerBioAssay );
                try ( MatrixVectorReader mvr = new MatrixVectorReader( new InputStreamReader( new GZIPInputStream( Files.newInputStream( outDir.resolve( "test" ).resolve( dirName ).resolve( "matrix.mtx.gz" ) ) ) ) ) ) {
                    MatrixInfo mi = mvr.readMatrixInfo();
                    assertThat( mi.isReal() ).isTrue();
                    MatrixSize size = mvr.readMatrixSize( mi );
                    assertThat( size.numRows() ).isEqualTo( numDesignElements );
                    assertThat( size.numColumns() ).isEqualTo( numCellsPerBioAssay );
                    assertThat( size.numEntries() ).isEqualTo( nnz );
                    int[] rows = new int[size.numEntries()], cols = new int[size.numEntries()];
                    double[] data = new double[size.numEntries()];
                    mvr.readCoordinate( rows, cols, data );
                    assertThat( data ).hasSize( nnz );
                }
            }
        } finally {
            PathUtils.deleteDirectory( outDir );
        }
    }

    @Test
    public void testWriteConvertedFloatVectorsToDisk() throws IOException {
        QuantitationType qt = new QuantitationType();
        qt.setName( "Counts" );
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.COUNT );
        qt.setScale( ScaleType.COUNT );
        qt.setRepresentation( PrimitiveType.FLOAT );
        Collection<SingleCellExpressionDataVector> vectors = randomSingleCellVectors( numDesignElements, 8, numCellsPerBioAssay, 0.9, qt );
        ExpressionExperiment ee = vectors.iterator().next().getExpressionExperiment();
        Path outDir = Files.createTempDirectory( null );
        try {
            Map<BioAssay, Long> nnzBySample = ee.getBioAssays().stream().collect( Collectors.toMap( ba -> ba, ba -> ( long ) nnz ) );
            writer.setScaleType( ScaleType.LOG1P );
            writer.write( vectors.stream(), vectors.size(), nnzBySample, null, outDir.resolve( "test" ) );
            for ( BioAssay ba : ee.getBioAssays() ) {
                String dirName = formatBioAssayFilename( ba );
                assertThat( outDir )
                        .isDirectoryRecursivelyContaining( "glob:**/" + dirName + "/features.tsv.gz" )
                        .isDirectoryRecursivelyContaining( "glob:**/" + dirName + "/barcodes.tsv.gz" )
                        .isDirectoryRecursivelyContaining( "glob:**/" + dirName + "/matrix.mtx.gz" );
                assertThat( new GZIPInputStream( Files.newInputStream( outDir.resolve( "test" ).resolve( dirName ).resolve( "features.tsv.gz" ) ) ) )
                        .asString( StandardCharsets.UTF_8 ).hasLineCount( numDesignElements );
                assertThat( new GZIPInputStream( Files.newInputStream( outDir.resolve( "test" ).resolve( dirName ).resolve( "barcodes.tsv.gz" ) ) ) )
                        .asString( StandardCharsets.UTF_8 ).hasLineCount( numCellsPerBioAssay );
                try ( MatrixVectorReader mvr = new MatrixVectorReader( new InputStreamReader( new GZIPInputStream( Files.newInputStream( outDir.resolve( "test" ).resolve( dirName ).resolve( "matrix.mtx.gz" ) ) ) ) ) ) {
                    MatrixInfo mi = mvr.readMatrixInfo();
                    assertThat( mi.isReal() ).isTrue();
                    MatrixSize size = mvr.readMatrixSize( mi );
                    assertThat( size.numRows() ).isEqualTo( numDesignElements );
                    assertThat( size.numColumns() ).isEqualTo( numCellsPerBioAssay );
                    assertThat( size.numEntries() ).isEqualTo( nnz );
                    int[] rows = new int[size.numEntries()], cols = new int[size.numEntries()];
                    double[] data = new double[size.numEntries()];
                    mvr.readCoordinate( rows, cols, data );
                    assertThat( data ).hasSize( nnz );
                }
            }
        } finally {
            PathUtils.deleteDirectory( outDir );
        }
    }

    @Test
    public void testWriteIntVectorsToDisk() throws IOException {
        QuantitationType qt = new QuantitationType();
        qt.setName( "Counts" );
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.COUNT );
        qt.setScale( ScaleType.COUNT );
        qt.setRepresentation( PrimitiveType.INT );
        Collection<SingleCellExpressionDataVector> vectors = randomSingleCellVectors( numDesignElements, 8, numCellsPerBioAssay, 0.9, qt );
        ExpressionExperiment ee = vectors.iterator().next().getExpressionExperiment();
        Path outDir = Files.createTempDirectory( null );
        try {
            Map<BioAssay, Long> nnzBySample = ee.getBioAssays().stream().collect( Collectors.toMap( ba -> ba, ba -> ( long ) nnz ) );
            writer.write( vectors.stream(), vectors.size(), nnzBySample, null, outDir.resolve( "test" ) );
            for ( BioAssay ba : ee.getBioAssays() ) {
                String dirName = formatBioAssayFilename( ba );
                assertThat( outDir )
                        .isDirectoryRecursivelyContaining( "glob:**/" + dirName + "/features.tsv.gz" )
                        .isDirectoryRecursivelyContaining( "glob:**/" + dirName + "/barcodes.tsv.gz" )
                        .isDirectoryRecursivelyContaining( "glob:**/" + dirName + "/matrix.mtx.gz" );
                assertThat( new GZIPInputStream( Files.newInputStream( outDir.resolve( "test" ).resolve( dirName ).resolve( "features.tsv.gz" ) ) ) )
                        .asString( StandardCharsets.UTF_8 ).hasLineCount( numDesignElements );
                assertThat( new GZIPInputStream( Files.newInputStream( outDir.resolve( "test" ).resolve( dirName ).resolve( "barcodes.tsv.gz" ) ) ) )
                        .asString( StandardCharsets.UTF_8 ).hasLineCount( numCellsPerBioAssay );
                try ( MatrixVectorReader mvr = new MatrixVectorReader( new InputStreamReader( new GZIPInputStream( Files.newInputStream( outDir.resolve( "test" ).resolve( dirName ).resolve( "matrix.mtx.gz" ) ) ) ) ) ) {
                    MatrixInfo mi = mvr.readMatrixInfo();
                    assertThat( mi.isInteger() ).isTrue();
                    MatrixSize size = mvr.readMatrixSize( mi );
                    assertThat( size.numRows() ).isEqualTo( numDesignElements );
                    assertThat( size.numColumns() ).isEqualTo( numCellsPerBioAssay );
                    assertThat( size.numEntries() ).isEqualTo( nnz );
                    int[] rows = new int[size.numEntries()], cols = new int[size.numEntries()];
                    double[] data = new double[size.numEntries()];
                    mvr.readCoordinate( rows, cols, data );
                    assertThat( data ).hasSize( nnz );
                }
            }
        } finally {
            PathUtils.deleteDirectory( outDir );
        }
    }

    @Test
    public void testWriteConvertedIntVectorsToDisk() throws IOException {
        QuantitationType qt = new QuantitationType();
        qt.setName( "Counts" );
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.COUNT );
        qt.setScale( ScaleType.COUNT );
        qt.setRepresentation( PrimitiveType.INT );
        Collection<SingleCellExpressionDataVector> vectors = randomSingleCellVectors( numDesignElements, 8, numCellsPerBioAssay, 0.9, qt );
        ExpressionExperiment ee = vectors.iterator().next().getExpressionExperiment();
        Path outDir = Files.createTempDirectory( null );
        try {
            Map<BioAssay, Long> nnzBySample = ee.getBioAssays().stream().collect( Collectors.toMap( ba -> ba, ba -> ( long ) nnz ) );
            writer.setScaleType( ScaleType.LOG1P );
            writer.write( vectors.stream(), vectors.size(), nnzBySample, null, outDir.resolve( "test" ) );
            for ( BioAssay ba : ee.getBioAssays() ) {
                String dirName = formatBioAssayFilename( ba );
                assertThat( outDir )
                        .isDirectoryRecursivelyContaining( "glob:**/" + dirName + "/features.tsv.gz" )
                        .isDirectoryRecursivelyContaining( "glob:**/" + dirName + "/barcodes.tsv.gz" )
                        .isDirectoryRecursivelyContaining( "glob:**/" + dirName + "/matrix.mtx.gz" );
                assertThat( new GZIPInputStream( Files.newInputStream( outDir.resolve( "test" ).resolve( dirName ).resolve( "features.tsv.gz" ) ) ) )
                        .asString( StandardCharsets.UTF_8 ).hasLineCount( numDesignElements );
                assertThat( new GZIPInputStream( Files.newInputStream( outDir.resolve( "test" ).resolve( dirName ).resolve( "barcodes.tsv.gz" ) ) ) )
                        .asString( StandardCharsets.UTF_8 ).hasLineCount( numCellsPerBioAssay );
                try ( MatrixVectorReader mvr = new MatrixVectorReader( new InputStreamReader( new GZIPInputStream( Files.newInputStream( outDir.resolve( "test" ).resolve( dirName ).resolve( "matrix.mtx.gz" ) ) ) ) ) ) {
                    MatrixInfo mi = mvr.readMatrixInfo();
                    assertThat( mi.isReal() ).isTrue();
                    MatrixSize size = mvr.readMatrixSize( mi );
                    assertThat( size.numRows() ).isEqualTo( numDesignElements );
                    assertThat( size.numColumns() ).isEqualTo( numCellsPerBioAssay );
                    assertThat( size.numEntries() ).isEqualTo( nnz );
                    int[] rows = new int[size.numEntries()], cols = new int[size.numEntries()];
                    double[] data = new double[size.numEntries()];
                    mvr.readCoordinate( rows, cols, data );
                    assertThat( data ).hasSize( nnz );
                }
            }
        } finally {
            PathUtils.deleteDirectory( outDir );
        }
    }

    @Test
    public void testWriteLongVectorsToDisk() throws IOException {
        QuantitationType qt = new QuantitationType();
        qt.setName( "Counts" );
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.COUNT );
        qt.setScale( ScaleType.COUNT );
        qt.setRepresentation( PrimitiveType.LONG );
        Collection<SingleCellExpressionDataVector> vectors = randomSingleCellVectors( numDesignElements, 8, numCellsPerBioAssay, 0.9, qt );
        ExpressionExperiment ee = vectors.iterator().next().getExpressionExperiment();
        Path outDir = Files.createTempDirectory( null );
        try {
            Map<BioAssay, Long> nnzBySample = ee.getBioAssays().stream().collect( Collectors.toMap( ba -> ba, ba -> ( long ) nnz ) );
            writer.setScaleType( ScaleType.LOG1P );
            writer.write( vectors.stream(), vectors.size(), nnzBySample, null, outDir.resolve( "test" ) );
            for ( BioAssay ba : ee.getBioAssays() ) {
                String dirName = formatBioAssayFilename( ba );
                assertThat( outDir )
                        .isDirectoryRecursivelyContaining( "glob:**/" + dirName + "/features.tsv.gz" )
                        .isDirectoryRecursivelyContaining( "glob:**/" + dirName + "/barcodes.tsv.gz" )
                        .isDirectoryRecursivelyContaining( "glob:**/" + dirName + "/matrix.mtx.gz" );
                assertThat( new GZIPInputStream( Files.newInputStream( outDir.resolve( "test" ).resolve( dirName ).resolve( "features.tsv.gz" ) ) ) )
                        .asString( StandardCharsets.UTF_8 ).hasLineCount( numDesignElements );
                assertThat( new GZIPInputStream( Files.newInputStream( outDir.resolve( "test" ).resolve( dirName ).resolve( "barcodes.tsv.gz" ) ) ) )
                        .asString( StandardCharsets.UTF_8 ).hasLineCount( numCellsPerBioAssay );
                try ( MatrixVectorReader mvr = new MatrixVectorReader( new InputStreamReader( new GZIPInputStream( Files.newInputStream( outDir.resolve( "test" ).resolve( dirName ).resolve( "matrix.mtx.gz" ) ) ) ) ) ) {
                    MatrixInfo mi = mvr.readMatrixInfo();
                    assertThat( mi.isReal() ).isTrue();
                    MatrixSize size = mvr.readMatrixSize( mi );
                    assertThat( size.numRows() ).isEqualTo( numDesignElements );
                    assertThat( size.numColumns() ).isEqualTo( numCellsPerBioAssay );
                    assertThat( size.numEntries() ).isEqualTo( nnz );
                    int[] rows = new int[size.numEntries()], cols = new int[size.numEntries()];
                    double[] data = new double[size.numEntries()];
                    mvr.readCoordinate( rows, cols, data );
                    assertThat( data ).hasSize( nnz );
                }
            }
        } finally {
            PathUtils.deleteDirectory( outDir );
        }
    }

    @Test
    public void testWriteMatrixToStream() throws IOException {
        Collection<SingleCellExpressionDataVector> vectors = randomSingleCellVectors( numDesignElements, 8, numCellsPerBioAssay, 0.9, ScaleType.COUNT );
        SingleCellExpressionDataDoubleMatrix matrix = new SingleCellExpressionDataDoubleMatrix( vectors );
        byte[] blob;
        try ( ByteArrayOutputStream baos = new ByteArrayOutputStream() ) {
            writer.write( matrix, null, baos );
            blob = baos.toByteArray();
        }
        List<BioAssay> bas = vectors.iterator().next().getSingleCellDimension().getBioAssays();
        List<String> expectedEntries = new ArrayList<>();
        for ( BioAssay ba : bas ) {
            String dirName = formatBioAssayFilename( ba );
            expectedEntries.addAll( Arrays.asList(
                    dirName + "/barcodes.tsv", dirName + "/features.tsv", dirName + "/matrix.mtx"
            ) );
        }

        int i = 0;
        try ( TarArchiveInputStream is = new TarArchiveInputStream( new ByteArrayInputStream( blob ) ) ) {
            TarArchiveEntry entry;
            while ( ( entry = is.getNextEntry() ) != null ) {
                assertThat( entry.getName() ).isEqualTo( expectedEntries.get( i ) );
                if ( i % 3 == 0 ) {
                    assertThat( is ).asString( StandardCharsets.UTF_8 ).hasLineCount( numCellsPerBioAssay );
                } else if ( i % 3 == 1 ) {
                    assertThat( is ).asString( StandardCharsets.UTF_8 ).hasLineCount( numDesignElements );
                } else {
                    try ( MatrixVectorReader mvr = new MatrixVectorReader( new InputStreamReader( is ) ) ) {
                        MatrixInfo mi = mvr.readMatrixInfo();
                        MatrixSize size = mvr.readMatrixSize( mi );
                        assertThat( size.numRows() ).isEqualTo( numDesignElements );
                        assertThat( size.numColumns() ).isEqualTo( numCellsPerBioAssay );
                        assertThat( size.numEntries() ).isEqualTo( nnz );
                        int[] rows = new int[size.numEntries()], cols = new int[size.numEntries()];
                        double[] data = new double[size.numEntries()];
                        mvr.readCoordinate( rows, cols, data );
                        assertThat( data ).hasSize( nnz );
                    }
                }
                i++;
            }
        }
    }

    @Test
    public void testWriteAndConvertMatrixToStream() throws IOException {
        writer.setScaleType( ScaleType.LOG1P );
        Collection<SingleCellExpressionDataVector> vectors = randomSingleCellVectors( numDesignElements, 8, numCellsPerBioAssay, 0.9, ScaleType.COUNT );
        SingleCellExpressionDataDoubleMatrix matrix = new SingleCellExpressionDataDoubleMatrix( vectors );
        byte[] blob;
        try ( ByteArrayOutputStream baos = new ByteArrayOutputStream() ) {
            writer.write( matrix, null, baos );
            blob = baos.toByteArray();
        }
        List<BioAssay> bas = vectors.iterator().next().getSingleCellDimension().getBioAssays();
        List<String> expectedEntries = new ArrayList<>();
        for ( BioAssay ba : bas ) {
            String dirName = formatBioAssayFilename( ba );
            expectedEntries.addAll( Arrays.asList(
                    dirName + "/barcodes.tsv", dirName + "/features.tsv", dirName + "/matrix.mtx"
            ) );
        }

        int i = 0;
        try ( TarArchiveInputStream is = new TarArchiveInputStream( new ByteArrayInputStream( blob ) ) ) {
            TarArchiveEntry entry;
            while ( ( entry = is.getNextEntry() ) != null ) {
                assertThat( entry.getName() ).isEqualTo( expectedEntries.get( i ) );
                if ( i % 3 == 0 ) {
                    assertThat( is ).asString( StandardCharsets.UTF_8 ).hasLineCount( numCellsPerBioAssay );
                } else if ( i % 3 == 1 ) {
                    assertThat( is ).asString( StandardCharsets.UTF_8 ).hasLineCount( numDesignElements );
                } else {
                    try ( MatrixVectorReader mvr = new MatrixVectorReader( new InputStreamReader( is ) ) ) {
                        MatrixInfo mi = mvr.readMatrixInfo();
                        MatrixSize size = mvr.readMatrixSize( mi );
                        assertThat( size.numRows() ).isEqualTo( numDesignElements );
                        assertThat( size.numColumns() ).isEqualTo( numCellsPerBioAssay );
                        assertThat( size.numEntries() ).isEqualTo( nnz );
                        int[] rows = new int[size.numEntries()], cols = new int[size.numEntries()];
                        double[] data = new double[size.numEntries()];
                        mvr.readCoordinate( rows, cols, data );
                        assertThat( data ).hasSize( nnz );
                    }
                }
                i++;
            }
        }
    }

    @Test
    public void testWriteMatrixToDisk() throws IOException {
        writer.setExecutorService( Executors.newFixedThreadPool( 4 ) );
        Collection<SingleCellExpressionDataVector> vectors = randomSingleCellVectors( numDesignElements, 8, numCellsPerBioAssay, 0.9, ScaleType.COUNT );
        ExpressionExperiment ee = vectors.iterator().next().getExpressionExperiment();
        Path outDir = Files.createTempDirectory( null );
        try {
            writer.write( new SingleCellExpressionDataDoubleMatrix( vectors ), null, outDir.resolve( "test" ) );
            for ( BioAssay ba : ee.getBioAssays() ) {
                String dirName = formatBioAssayFilename( ba );
                assertThat( outDir )
                        .isDirectoryRecursivelyContaining( "glob:**/" + dirName + "/features.tsv.gz" )
                        .isDirectoryRecursivelyContaining( "glob:**/" + dirName + "/barcodes.tsv.gz" )
                        .isDirectoryRecursivelyContaining( "glob:**/" + dirName + "/matrix.mtx.gz" );
                assertThat( new GZIPInputStream( Files.newInputStream( outDir.resolve( "test" ).resolve( dirName ).resolve( "features.tsv.gz" ) ) ) )
                        .asString( StandardCharsets.UTF_8 ).hasLineCount( numDesignElements );
                assertThat( new GZIPInputStream( Files.newInputStream( outDir.resolve( "test" ).resolve( dirName ).resolve( "barcodes.tsv.gz" ) ) ) )
                        .asString( StandardCharsets.UTF_8 ).hasLineCount( numCellsPerBioAssay );
                try ( MatrixVectorReader mvr = new MatrixVectorReader( new InputStreamReader( new GZIPInputStream( Files.newInputStream( outDir.resolve( "test" ).resolve( dirName ).resolve( "matrix.mtx.gz" ) ) ) ) ) ) {
                    MatrixInfo mi = mvr.readMatrixInfo();
                    assertThat( mi.isReal() ).isTrue();
                    MatrixSize size = mvr.readMatrixSize( mi );
                    assertThat( size.numRows() ).isEqualTo( numDesignElements );
                    assertThat( size.numColumns() ).isEqualTo( numCellsPerBioAssay );
                    assertThat( size.numEntries() ).isEqualTo( nnz );
                    int[] rows = new int[size.numEntries()], cols = new int[size.numEntries()];
                    double[] data = new double[size.numEntries()];
                    mvr.readCoordinate( rows, cols, data );
                    assertThat( data ).hasSize( nnz );
                }
            }
        } finally {
            PathUtils.deleteDirectory( outDir );
        }
    }

    @Test
    public void testWriteToDiskAndConvert() throws IOException {
        writer.setScaleType( ScaleType.LOG1P );
        writer.setExecutorService( Executors.newFixedThreadPool( 4 ) );
        QuantitationType qt = new QuantitationType();
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.COUNT );
        qt.setScale( ScaleType.COUNT );
        qt.setRepresentation( PrimitiveType.INT );
        Collection<SingleCellExpressionDataVector> vectors = randomSingleCellVectors( numDesignElements, 8, numCellsPerBioAssay, 0.9, qt );
        ExpressionExperiment ee = vectors.iterator().next().getExpressionExperiment();
        Path outDir = Files.createTempDirectory( null );
        try {
            writer.write( new SingleCellExpressionDataIntMatrix( vectors ), null, outDir.resolve( "test" ) );
            for ( BioAssay ba : ee.getBioAssays() ) {
                String dirName = formatBioAssayFilename( ba );
                assertThat( outDir )
                        .isDirectoryRecursivelyContaining( "glob:**/" + dirName + "/features.tsv.gz" )
                        .isDirectoryRecursivelyContaining( "glob:**/" + dirName + "/barcodes.tsv.gz" )
                        .isDirectoryRecursivelyContaining( "glob:**/" + dirName + "/matrix.mtx.gz" );
                assertThat( new GZIPInputStream( Files.newInputStream( outDir.resolve( "test" ).resolve( dirName ).resolve( "features.tsv.gz" ) ) ) )
                        .asString( StandardCharsets.UTF_8 ).hasLineCount( numDesignElements );
                assertThat( new GZIPInputStream( Files.newInputStream( outDir.resolve( "test" ).resolve( dirName ).resolve( "barcodes.tsv.gz" ) ) ) )
                        .asString( StandardCharsets.UTF_8 ).hasLineCount( numCellsPerBioAssay );
                try ( MatrixVectorReader mvr = new MatrixVectorReader( new InputStreamReader( new GZIPInputStream( Files.newInputStream( outDir.resolve( "test" ).resolve( dirName ).resolve( "matrix.mtx.gz" ) ) ) ) ) ) {
                    MatrixInfo mi = mvr.readMatrixInfo();
                    assertThat( mi.isReal() ).isTrue();
                    MatrixSize size = mvr.readMatrixSize( mi );
                    assertThat( size.numRows() ).isEqualTo( numDesignElements );
                    assertThat( size.numColumns() ).isEqualTo( numCellsPerBioAssay );
                    assertThat( size.numEntries() ).isEqualTo( nnz );
                    int[] rows = new int[size.numEntries()], cols = new int[size.numEntries()];
                    double[] data = new double[size.numEntries()];
                    mvr.readCoordinate( rows, cols, data );
                    assertThat( data ).hasSize( nnz );
                }
            }
        } finally {
            PathUtils.deleteDirectory( outDir );
        }
    }
}