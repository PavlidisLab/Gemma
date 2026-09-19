package ubic.gemma.core.analysis.service;

import org.apache.commons.io.file.PathUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.analysis.preprocess.batcheffects.ExpressionExperimentBatchInformationService;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.core.util.locking.FileLockManager;
import ubic.gemma.core.util.locking.FileLockManagerImpl;
import ubic.gemma.core.util.locking.LockedPath;
import ubic.gemma.core.util.test.BaseTest5;
import ubic.gemma.core.util.test.TestPropertyPlaceholderConfigurer;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.diff.ExpressionAnalysisResultSetService;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.bioAssayData.RawAndProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentMetaFileType;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSubSetReadService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSubSetService;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;
import ubic.gemma.persistence.util.EntityUrlBuilder;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.persistence.service.expression.bioAssayData.RandomExpressionDataMatrixUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ubic.gemma.core.analysis.service.ExpressionDataFileUtils.TABULAR_BULK_DATA_FILE_SUFFIX;
import static ubic.gemma.core.analysis.service.ExpressionDataFileUtils.getDataOutputFilename;
import static org.mockito.Mockito.mock;

@ContextConfiguration
public class ExpressionDataFileServiceTest extends BaseTest5 {

    @Configuration
    @TestComponent
    @Import(BuildInfo.class)
    static class ExpressionDataFileServiceTestContextConfiguration {

        @Bean
        public static TestPropertyPlaceholderConfigurer propertyPlaceholderConfigurer() throws IOException {
            return new TestPropertyPlaceholderConfigurer(
                    "gemma.appdata.home=" + Files.createTempDirectory( "gemmaData" ),
                    "gemma.hosturl=https://gemma.msl.ubc.ca" );
        }

        @Bean
        public ConversionService conversionService() {
            DefaultFormattingConversionService service = new DefaultFormattingConversionService();
            service.addConverter( String.class, Path.class, source -> Paths.get( ( String ) source ) );
            return service;
        }

        @Bean
        public ExpressionDataFileService expressionDataFileService() {
            return new ExpressionDataFileServiceImpl();
        }

        @Bean
        public ExpressionDataFileHelperService expressionDataFileHelperService() {
            return new ExpressionDataFileHelperService();
        }

        @Bean
        public FileLockManager fileLockManager() {
            return new FileLockManagerImpl();
        }

        @Bean
        public ArrayDesignService arrayDesignService() {
            return mock( ArrayDesignService.class );
        }

        @Bean
        public DifferentialExpressionAnalysisService differentialExpressionAnalysisService() {
            return mock( DifferentialExpressionAnalysisService.class );
        }

        @Bean
        public ExpressionAnalysisResultSetService expressionAnalysisResultSetService() {
            return mock( ExpressionAnalysisResultSetService.class );
        }

        @Bean
        public ExpressionAnalysisResultSetFileService expressionAnalysisResultSetFileService() {
            return mock( ExpressionAnalysisResultSetFileService.class );
        }

        @Bean
        public ExpressionDataMatrixService expressionDataMatrixService() {
            return mock( ExpressionDataMatrixService.class );
        }

        @Bean
        public ExpressionExperimentService expressionExperimentService() {
            return mock( ExpressionExperimentService.class );
        }

        @Bean
        public ExpressionExperimentSubSetService expressionExperimentSubSetService() {
            return mock();
        }

        @Bean
        public ExpressionExperimentSubSetReadService expressionExperimentSubSetReadService() {
            return mock( ExpressionExperimentSubSetReadService.class );
        }

        @Bean
        public ExpressionExperimentBatchInformationService expressionExperimentBatchInformationService() {
            return mock();
        }

        @Bean
        public RawAndProcessedExpressionDataVectorService rawAndProcessedExpressionDataVectorService() {
            return mock( RawAndProcessedExpressionDataVectorService.class );
        }

        @Bean
        public SingleCellExpressionExperimentService singleCellExpressionExperimentService() {
            return mock();
        }

        @Bean
        public QuantitationTypeService quantitationTypeService() {
            return mock();
        }

        @Bean
        public ArrayDesignAnnotationService arrayDesignAnnotationService() {
            return mock();
        }

        @Bean
        public EntityUrlBuilder entityUrlBuilder() {
            // real instance: the matrix writers build header URLs through a call chain a
            // plain mock would answer with null
            return new EntityUrlBuilder( "https://gemma.msl.ubc.ca" );
        }

        @Bean
        public AsyncTaskExecutor expressionDataFileTaskExecutor() {
            return mock();
        }
    }

    @Autowired
    private ExpressionDataFileService expressionDataFileService;

    @Autowired
    private ExpressionDataMatrixService expressionDataMatrixService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private ArrayDesignAnnotationService arrayDesignAnnotationService;

    @Autowired
    private FileLockManager fileLockManager;

    @Autowired
    private ExpressionExperimentBatchInformationService expressionExperimentBatchInformationService;

    @Value("${gemma.appdata.home}")
    private Path appdataHome;

    /**
     * An in-memory experiment with 8 samples on one 100-probe platform, wired so the processed
     * matrix path runs end-to-end against the mocked collaborators. The shortName is per-test so
     * cache files never collide across tests.
     */
    private ExpressionExperiment setUpExperimentWithRandomMatrix( String shortName, long id ) throws IOException {
        RandomExpressionDataMatrixUtils.setSeed( 123L );
        ExpressionExperiment ee = new ExpressionExperiment();
        // distinct ids: entity equality is id-based, and the context-shared mocks are not reset
        // between tests, so equal experiments would pool their stubs and invocation counts
        ee.setId( id );
        ee.setShortName( shortName );
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        for ( int i = 0; i < 100; i++ ) {
            ad.getCompositeSequences().add( CompositeSequence.Factory.newInstance( "cs" + i, ad ) );
        }
        for ( int i = 0; i < 8; i++ ) {
            BioMaterial bm = BioMaterial.Factory.newInstance( "bm" + i );
            BioAssay ba = BioAssay.Factory.newInstance( "ba" + i, ad, bm );
            bm.getBioAssaysUsedIn().add( ba );
            ee.getBioAssays().add( ba );
        }
        when( expressionExperimentService.thawLite( ee ) ).thenReturn( ee );
        when( expressionDataMatrixService.getProcessedExpressionDataMatrix( ee ) )
                .thenReturn( RandomExpressionDataMatrixUtils.randomLog2Matrix( ee ) );
        when( arrayDesignService.thaw( anyCollection() ) ).thenAnswer( inv -> inv.getArgument( 0 ) );
        when( arrayDesignAnnotationService.readAnnotationFile( any() ) ).thenReturn( Collections.emptyMap() );
        return ee;
    }

    private String gunzip( Path file ) throws IOException {
        StringBuilder sb = new StringBuilder();
        try ( Reader r = new InputStreamReader( new GZIPInputStream( Files.newInputStream( file ) ), StandardCharsets.UTF_8 ) ) {
            char[] buf = new char[8192];
            for ( int n; ( n = r.read( buf ) ) != -1; ) {
                sb.append( buf, 0, n );
            }
        }
        return sb.toString();
    }

    @Test
    public void testStreamAndWriteTeesOneBuildIntoStreamAndCache() throws Exception {
        ExpressionExperiment ee = setUpExperimentWithRandomMatrix( "teeHappy", 101L );
        Path cacheFile = appdataHome.resolve( "dataFiles" )
                .resolve( getDataOutputFilename( ee, false, TABULAR_BULK_DATA_FILE_SUFFIX ) );

        StringWriter dest = new StringWriter();
        expressionDataFileService.streamAndWriteProcessedExpressionData( ee, false, false, dest, true );

        assertThat( dest.toString() ).contains( "cs0" ).contains( "cs99" );
        assertThat( cacheFile ).exists();
        assertThat( gunzip( cacheFile ) )
                .as( "the cache file and the stream come from one pass and must be identical" )
                .isEqualTo( dest.toString() );

        // The point of the tee: a second call serves from the cache, so the matrix was built ONCE.
        StringWriter dest2 = new StringWriter();
        expressionDataFileService.streamAndWriteProcessedExpressionData( ee, false, false, dest2, true );
        assertThat( dest2.toString() ).isEqualTo( dest.toString() );
        verify( expressionDataMatrixService, times( 1 ) ).getProcessedExpressionDataMatrix( ee );
    }

    /**
     * The caller going away mid-stream must not abort the cache build — otherwise an impatient
     * client re-cools the cold path forever. This was the one property the replaced
     * fire-and-forget executor build had that a naive tee would lose.
     */
    @Test
    public void testCallerDeathDoesNotAbortTheCacheBuild() throws Exception {
        ExpressionExperiment ee = setUpExperimentWithRandomMatrix( "teeDeadCaller", 102L );
        Path cacheFile = appdataHome.resolve( "dataFiles" )
                .resolve( getDataOutputFilename( ee, false, TABULAR_BULK_DATA_FILE_SUFFIX ) );

        Writer dying = new Writer() {
            private int writes = 0;

            @Override
            public void write( char[] cbuf, int off, int len ) throws IOException {
                if ( ++writes > 1 ) {
                    throw new IOException( "client went away" );
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };

        expressionDataFileService.streamAndWriteProcessedExpressionData( ee, false, false, dying, true );

        assertThat( cacheFile ).exists();
        assertThat( gunzip( cacheFile ) )
                .as( "the cache file must be complete even though the caller died on the second write" )
                .contains( "cs0" ).contains( "cs99" );
    }

    /** A concurrent builder holding the cache file degrades the call to a plain stream. */
    @Test
    public void testConcurrentWriterDegradesToPlainStream() throws Exception {
        ExpressionExperiment ee = setUpExperimentWithRandomMatrix( "teeContended", 103L );
        Path cacheFile = appdataHome.resolve( "dataFiles" )
                .resolve( getDataOutputFilename( ee, false, TABULAR_BULK_DATA_FILE_SUFFIX ) );

        CountDownLatch held = new CountDownLatch( 1 );
        CountDownLatch release = new CountDownLatch( 1 );
        Thread other = new Thread( () -> {
            try ( LockedPath ignored = fileLockManager.acquirePathLock( cacheFile, true ) ) {
                held.countDown();
                release.await();
            } catch ( Exception e ) {
                throw new RuntimeException( e );
            }
        }, "concurrent-cache-builder" );
        other.start();
        held.await();
        try {
            StringWriter dest = new StringWriter();
            expressionDataFileService.streamAndWriteProcessedExpressionData( ee, false, false, dest, true );
            assertThat( dest.toString() ).contains( "cs0" ).contains( "cs99" );
            assertThat( cacheFile ).as( "no cache file: the other writer owns it" ).doesNotExist();
        } finally {
            release.countDown();
            other.join();
        }
    }

    @Test
    public void testDeleteAll() throws IOException {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setShortName( "test" );
        Path reportFile = appdataHome.resolve( "metadata/test/MultiQCReports/multiqc_report.html" );
        PathUtils.createParentDirectories( reportFile );
        PathUtils.touch( reportFile );
        assertThat( reportFile ).exists();

        expressionDataFileService.deleteAllFiles( ee );

        // make sure that metadata is not touched
        assertThat( reportFile ).exists();
    }

    @Test
    public void testGetMetadata() throws IOException {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setShortName( "test" );
        Path reportFile = appdataHome.resolve( "metadata/test/MultiQCReports/multiqc_report.html" );
        PathUtils.createParentDirectories( reportFile );
        PathUtils.touch( reportFile );
        assertThat( reportFile ).exists();

        assertThat( expressionDataFileService.getMetadataFile( ee, ExpressionExperimentMetaFileType.RNASEQ_PIPELINE_REPORT, false )
                .map( LockedPath::closeAndGetPath ) )
                .hasValueSatisfying( p -> {
                    assertThat( p )
                            .exists()
                            .isEqualTo( reportFile );
                } );

        // ensure that metadata of a split is stored in its original directory
        ee.setShortName( "test.1" );
        assertThat( expressionDataFileService.getMetadataFile( ee, ExpressionExperimentMetaFileType.RNASEQ_PIPELINE_REPORT, false )
                .map( LockedPath::closeAndGetPath ) )
                .hasValue( appdataHome.resolve( "metadata/test.1/MultiQCReports/multiqc_report.html" ) );

        ee.setShortName( "test.1.2" );
        assertThat( expressionDataFileService.getMetadataFile( ee, ExpressionExperimentMetaFileType.RNASEQ_PIPELINE_REPORT, false )
                .map( LockedPath::closeAndGetPath ) )
                .hasValue( appdataHome.resolve( "metadata/test.1.2/MultiQCReports/multiqc_report.html" ) );
    }

    @Test
    public void testCopyMetadata() throws IOException {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setShortName( "test" );
        Path tmpReportFile = Files.createTempFile( null, "multiqc_report.html" );
        expressionDataFileService.copyMetadataFile( ee, tmpReportFile, ExpressionExperimentMetaFileType.RNASEQ_PIPELINE_REPORT, false );
        Path reportFile = appdataHome.resolve( "metadata/test/MultiQCReports/multiqc_report.html" );
        assertThat( expressionDataFileService.getMetadataFile( ee, ExpressionExperimentMetaFileType.RNASEQ_PIPELINE_REPORT, false )
                .map( LockedPath::closeAndGetPath ) )
                .hasValue( reportFile );
    }

    /**
     * The catch that deletes the destination after a failed copy also enclosed the lock acquisition, so a lock that
     * could not be acquired deleted a valid metadata file this call never wrote.
     */
    @Test
    public void testCopyMetadataFileWhenTheLockFailsKeepsTheExistingFile() throws IOException {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setShortName( "lockFailure" );
        Path destination = appdataHome.resolve( "metadata/lockFailure/notes.txt" );
        PathUtils.createParentDirectories( destination );
        Files.write( destination, "valid".getBytes( StandardCharsets.UTF_8 ) );
        // a directory where the lock file goes makes acquiring the lock fail
        Files.createDirectories( destination.resolveSibling( "notes.txt.lock" ) );
        Path source = Files.createTempFile( "notes", ".txt" );

        assertThatThrownBy( () -> expressionDataFileService.copyMetadataFile( ee, source, "notes.txt", true ) )
                .hasMessageStartingWith( "Failed to acquire exclusive lock" );
        assertThat( destination ).hasContent( "valid" );
    }

    @Test
    public void testDeleteMetadata() throws IOException {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setShortName( "test" );
        Path reportFile = appdataHome.resolve( "metadata/test/MultiQCReports/multiqc_report.html" );
        PathUtils.createParentDirectories( reportFile );
        PathUtils.touch( reportFile );
        assertThat( expressionDataFileService.deleteMetadataFile( ee, ExpressionExperimentMetaFileType.RNASEQ_PIPELINE_REPORT ) )
                .isTrue();
        assertThat( expressionDataFileService.deleteMetadataFile( ee, ExpressionExperimentMetaFileType.RNASEQ_PIPELINE_REPORT ) )
                .isFalse();
        assertThat( expressionDataFileService.getMetadataFile( ee, ExpressionExperimentMetaFileType.RNASEQ_PIPELINE_REPORT, false )
                .map( LockedPath::closeAndGetPath ) )
                .hasValue( reportFile );
    }

    /**
     * A failed DEA archive build must not leave the truncated file behind: the next request finds it and serves it as
     * the archive. 304 of the 774 archives on gemma2 were 0 bytes.
     */
    @Test
    public void testFailedDiffExArchiveBuildLeavesNoFile() {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 9001L );
        ee.setShortName( "failedDiffExArchive" );
        DifferentialExpressionAnalysis analysis = new DifferentialExpressionAnalysis();
        analysis.setId( 9002L );
        analysis.setExperimentAnalyzed( ee );
        when( expressionExperimentBatchInformationService.hasSignificantBatchConfound( ( BioAssaySet ) ee ) )
                .thenThrow( new IllegalStateException( "confound test failed" ) );

        assertThatThrownBy( () -> expressionDataFileService.writeOrLocateDiffExAnalysisArchiveFile( analysis, false ) )
                .hasMessage( "confound test failed" );
        assertThat( appdataHome.resolve( "dataFiles" ).resolve( ExpressionDataFileUtils.getDiffExArchiveFileName( analysis ) ) )
                .doesNotExist();
    }

    /**
     * Nothing may be at the archive's path while it is being built. A JVM that dies at that point, or a daemon writer
     * thread halted by {@code System.exit}, leaves whatever is there, and it is then served as the archive.
     */
    @Test
    public void testDiffExArchiveIsNotAtItsPathWhileBeingBuilt() {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 9003L );
        ee.setShortName( "diffExArchiveInFlight" );
        DifferentialExpressionAnalysis analysis = new DifferentialExpressionAnalysis();
        analysis.setId( 9004L );
        analysis.setExperimentAnalyzed( ee );
        Path archive = appdataHome.resolve( "dataFiles" ).resolve( ExpressionDataFileUtils.getDiffExArchiveFileName( analysis ) );
        AtomicReference<Boolean> existedDuringBuild = new AtomicReference<>();
        when( expressionExperimentBatchInformationService.hasSignificantBatchConfound( ( BioAssaySet ) ee ) )
                .thenAnswer( inv -> {
                    existedDuringBuild.set( Files.exists( archive ) );
                    throw new IllegalStateException( "build stopped" );
                } );

        assertThatThrownBy( () -> expressionDataFileService.writeOrLocateDiffExAnalysisArchiveFile( analysis, false ) )
                .hasMessage( "build stopped" );
        assertThat( existedDuringBuild.get() ).isFalse();
    }

    /**
     * An {@link Error} is not caught by a catch of {@link Exception}, so the partial archive stayed at its path.
     */
    @Test
    public void testErrorDuringDiffExArchiveBuildLeavesNoFile() throws IOException {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 9005L );
        ee.setShortName( "diffExArchiveError" );
        DifferentialExpressionAnalysis analysis = new DifferentialExpressionAnalysis();
        analysis.setId( 9006L );
        analysis.setExperimentAnalyzed( ee );
        String archiveName = ExpressionDataFileUtils.getDiffExArchiveFileName( analysis );
        when( expressionExperimentBatchInformationService.hasSignificantBatchConfound( ( BioAssaySet ) ee ) )
                .thenThrow( new StackOverflowError( "simulated" ) );

        assertThatThrownBy( () -> expressionDataFileService.writeOrLocateDiffExAnalysisArchiveFile( analysis, false ) )
                .isInstanceOf( StackOverflowError.class );
        assertThat( appdataHome.resolve( "dataFiles" ).resolve( archiveName ) ).doesNotExist();
        try ( Stream<Path> files = Files.list( appdataHome.resolve( "dataFiles" ) ) ) {
            assertThat( files.map( f -> f.getFileName().toString() ) )
                    .noneMatch( name -> name.contains( archiveName ) );
        }
    }

    /**
     * The JSON processed-data writer returned {@code toShared()} of the shared lock that {@code toExclusive()} had
     * already closed, which throws {@link IllegalStateException}.
     */
    @Test
    public void testWriteOrLocateJsonProcessedDataFile() throws Exception {
        ExpressionExperiment ee = setUpExperimentWithRandomMatrix( "jsonProcessed", 104L );
        when( expressionExperimentService.hasProcessedExpressionData( ee ) ).thenReturn( true );
        assertThat( expressionDataFileService.writeOrLocateJSONProcessedExpressionDataFile( ee, false, false )
                .map( LockedPath::closeAndGetPath ) )
                .hasValueSatisfying( p -> assertThat( p ).exists().isNotEmptyFile() );
    }

    /**
     * The delete-on-failure catch also covered the lock upgrade, so a timed-out upgrade deleted the file that another
     * holder had locked.
     */
    @Test
    public void testTimedOutLockUpgradeDoesNotDeleteTheFile() throws Exception {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 9007L );
        ee.setShortName( "timedOutUpgrade" );
        QuantitationType qt = new QuantitationType();
        qt.setId( 9008L );
        qt.setName( "counts" );
        Path rawFile = appdataHome.resolve( "dataFiles" ).resolve( getDataOutputFilename( ee, qt, TABULAR_BULK_DATA_FILE_SUFFIX ) );
        PathUtils.createParentDirectories( rawFile );
        Files.write( rawFile, "complete".getBytes( StandardCharsets.UTF_8 ) );

        CountDownLatch held = new CountDownLatch( 1 );
        CountDownLatch release = new CountDownLatch( 1 );
        Thread reader = new Thread( () -> {
            try ( LockedPath ignored = fileLockManager.acquirePathLock( rawFile, false ) ) {
                held.countDown();
                release.await();
            } catch ( Exception e ) {
                throw new RuntimeException( e );
            }
        }, "raw-data-file-reader" );
        reader.start();
        held.await();
        try {
            assertThatThrownBy( () -> expressionDataFileService.writeOrLocateRawExpressionDataFile( ee, qt, true, 100, TimeUnit.MILLISECONDS ) )
                    .isInstanceOf( TimeoutException.class );
            assertThat( rawFile ).hasContent( "complete" );
        } finally {
            release.countDown();
            reader.join();
        }
    }
}
