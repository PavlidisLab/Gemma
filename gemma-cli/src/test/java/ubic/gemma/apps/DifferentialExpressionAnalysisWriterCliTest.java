package ubic.gemma.apps;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import ubic.gemma.cli.util.EntityLocator;
import ubic.gemma.cli.util.test.BaseCliTest5;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.analysis.service.ExpressionDataFileUtils;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.core.util.FileUtils;
import ubic.gemma.core.util.GemmaRestApiClient;
import ubic.gemma.core.util.locking.LockedPath;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSetService;
import ubic.gemma.persistence.util.EntityUrlBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static ubic.gemma.cli.util.test.Assertions.assertThat;

/**
 * {@code getDiffExAnalysis} reported "Wrote differential expression analysis files to ..." for an archive that
 * {@code writeOrLocate} only located, including a 0-byte one left by a failed build.
 */
@ContextConfiguration
@TestExecutionListeners(value = WithSecurityContextTestExecutionListener.class,
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
class DifferentialExpressionAnalysisWriterCliTest extends BaseCliTest5 {

    @Configuration
    @TestComponent
    static class CC {

        @Bean
        @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
        public DifferentialExpressionAnalysisWriterCli differentialExpressionAnalysisWriterCli() {
            return new DifferentialExpressionAnalysisWriterCli();
        }

        @Bean
        public ExpressionDataFileService expressionDataFileService() {
            return mock();
        }

        @Bean
        public DifferentialExpressionAnalysisService differentialExpressionAnalysisService() {
            return mock();
        }

        @Bean
        public ExpressionExperimentService eeService() {
            return mock();
        }

        @Bean
        public ExpressionExperimentSetService expressionExperimentSetService() {
            return mock();
        }

        @Bean
        public SearchService searchService() {
            return mock();
        }

        @Bean
        public ArrayDesignService arrayDesignService() {
            return mock();
        }

        @Bean
        public AuditTrailService auditTrailService() {
            return mock();
        }

        @Bean
        public AuditEventService auditEventService() {
            return mock();
        }

        @Bean
        public EntityLocator entityLocator() {
            return mock();
        }

        @Bean
        public EntityUrlBuilder entityUrlBuilder() {
            return new EntityUrlBuilder( "http://localhost:8080" );
        }

        @Bean
        public GemmaRestApiClient gemmaRestApiClient() {
            return mock();
        }
    }

    @Autowired
    private ObjectProvider<DifferentialExpressionAnalysisWriterCli> cliProvider;

    @Autowired
    private ExpressionDataFileService expressionDataFileService;

    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;

    @Autowired
    private EntityLocator entityLocator;

    @TempDir
    Path dataDir;

    private ExpressionExperiment ee;
    private DifferentialExpressionAnalysis analysis;
    private Path archive;

    @BeforeEach
    void setUp() throws IOException {
        ee = new ExpressionExperiment();
        ee.setId( 1L );
        ee.setShortName( "GSE1" );
        analysis = new DifferentialExpressionAnalysis();
        analysis.setId( 2L );
        analysis.setExperimentAnalyzed( ee );
        String archiveName = ExpressionDataFileUtils.getDiffExArchiveFileName( analysis );
        archive = dataDir.resolve( archiveName );
        when( entityLocator.locateExpressionExperiment( eq( "GSE1" ), anyBoolean() ) ).thenReturn( ee );
        when( entityLocator.locateDiffExAnalysis( ee, "2" ) ).thenReturn( analysis );
        when( differentialExpressionAnalysisService.findByExperiment( ee, true ) ).thenReturn( Collections.singleton( analysis ) );
        when( expressionDataFileService.getDataFile( archiveName, false ) ).thenAnswer( inv -> lockedPath( archive ) );
    }

    @AfterEach
    void tearDown() {
        reset( expressionDataFileService, differentialExpressionAnalysisService, entityLocator );
    }

    @Test
    @WithMockUser
    void existingArchiveIsReportedAsFound() throws IOException {
        Files.write( archive, new byte[0] );
        when( expressionDataFileService.writeOrLocateDiffExAnalysisArchiveFileById( 2L, false ) )
                .thenAnswer( inv -> lockedPath( archive ) );

        assertThat( cliProvider.getObject() )
                .withArguments( "-e", "GSE1", "-a", "2" )
                .succeeds()
                .standardOutput()
                .asString( StandardCharsets.UTF_8 )
                .contains( "Found an existing differential expression analysis file at " + archive )
                .doesNotContain( "Wrote" );
    }

    @Test
    @WithMockUser
    void existingArchivesAreReportedAsFound() throws IOException {
        Files.write( archive, new byte[0] );
        when( expressionDataFileService.writeOrLocateDiffExAnalysisArchiveFileById( 2L, false ) )
                .thenAnswer( inv -> lockedPath( archive ) );

        assertThat( cliProvider.getObject() )
                .withArguments( "-e", "GSE1" )
                .succeeds()
                .standardOutput()
                .asString( StandardCharsets.UTF_8 )
                .contains( "Found existing differential expression analysis files at " + archive )
                .doesNotContain( "Wrote" );
    }

    @Test
    @WithMockUser
    void newArchiveIsReportedAsWritten() throws IOException {
        when( expressionDataFileService.writeOrLocateDiffExAnalysisArchiveFileById( 2L, false ) )
                .thenAnswer( inv -> writeArchive() );

        assertThat( cliProvider.getObject() )
                .withArguments( "-e", "GSE1", "-a", "2" )
                .succeeds()
                .standardOutput()
                .asString( StandardCharsets.UTF_8 )
                .contains( "Wrote differential expression analysis file to " + archive )
                .doesNotContain( "Found" );
    }

    @Test
    @WithMockUser
    void regeneratedArchiveIsReportedAsWritten() throws IOException {
        Files.write( archive, new byte[0] );
        when( expressionDataFileService.writeOrLocateDiffExAnalysisArchiveFileById( 2L, true ) )
                .thenAnswer( inv -> writeArchive() );

        assertThat( cliProvider.getObject() )
                .withArguments( "-e", "GSE1", "-a", "2", "-force" )
                .succeeds()
                .standardOutput()
                .asString( StandardCharsets.UTF_8 )
                .contains( "Wrote differential expression analysis file to " + archive )
                .doesNotContain( "Found" );
    }

    private LockedPath writeArchive() throws IOException {
        FileUtils.writeAtomically( archive, tmp -> Files.write( tmp, "archive".getBytes( StandardCharsets.UTF_8 ) ) );
        return lockedPath( archive );
    }

    private static LockedPath lockedPath( Path path ) {
        LockedPath lockedPath = mock();
        when( lockedPath.getPath() ).thenReturn( path );
        return lockedPath;
    }
}
