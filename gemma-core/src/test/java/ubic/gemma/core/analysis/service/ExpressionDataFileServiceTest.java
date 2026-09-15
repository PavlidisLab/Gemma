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
import java.util.zip.GZIPInputStream;

import static org.assertj.core.api.Assertions.assertThat;
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
}