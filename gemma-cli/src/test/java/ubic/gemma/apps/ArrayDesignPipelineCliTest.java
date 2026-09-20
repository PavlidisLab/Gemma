package ubic.gemma.apps;

import org.junit.jupiter.api.AfterEach;
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
import org.springframework.test.context.TestPropertySource;
import ubic.gemma.cli.audit.CliArrayDesignAuditService;
import ubic.gemma.cli.util.EntityLocator;
import ubic.gemma.cli.util.test.BaseCliTest5;
import ubic.gemma.core.analysis.report.ArrayDesignReportService;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.goldenpath.GoldenPathSequenceAnalysisFactory;
import ubic.gemma.core.loader.expression.arrayDesign.ArrayDesignProbeMapperService;
import ubic.gemma.core.loader.expression.arrayDesign.ArrayDesignSequenceAlignmentService;
import ubic.gemma.core.loader.expression.arrayDesign.ArrayDesignSequenceProcessingService;
import ubic.gemma.core.security.authentication.ManualAuthenticationService;
import ubic.gemma.core.util.GemmaRestApiClient;
import ubic.gemma.model.common.auditAndSecurity.AuditAction;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignRepeatAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignSequenceAnalysisEvent;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.genome.biosequence.BioSequenceService;
import ubic.gemma.persistence.service.genome.sequenceAnalysis.BlatAssociationService;
import ubic.gemma.persistence.service.genome.sequenceAnalysis.BlatResultService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static ubic.gemma.cli.util.test.Assertions.assertThat;

/**
 * Platform pipeline CLIs (blatPlatform, platformRepeatScan, ...) run against mocked services.
 */
@ContextConfiguration
@TestPropertySource(properties = { "repeatMasker.exe=RepeatMasker", "gemma.goldenpath.db.rat=rn7" })
@TestExecutionListeners(value = WithSecurityContextTestExecutionListener.class,
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
class ArrayDesignPipelineCliTest extends BaseCliTest5 {

    @Configuration
    @TestComponent
    static class ArrayDesignPipelineCliTestContextConfiguration {

        @Bean
        @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
        public ArrayDesignBlatCli arrayDesignBlatCli() {
            return new ArrayDesignBlatCli();
        }

        @Bean
        @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
        public ArrayDesignRepeatScanCli arrayDesignRepeatScanCli() {
            return new ArrayDesignRepeatScanCli();
        }

        @Bean
        @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
        public ArrayDesignProbeMapperCli arrayDesignProbeMapperCli() {
            return new ArrayDesignProbeMapperCli();
        }

        @Bean
        @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
        public BioSequenceCleanupCli bioSequenceCleanupCli() {
            return new BioSequenceCleanupCli();
        }

        @Bean
        @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
        public ArrayDesignSequenceAssociationCli arrayDesignSequenceAssociationCli() {
            return new ArrayDesignSequenceAssociationCli();
        }

        @Bean
        @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
        public ArrayDesignReportCli arrayDesignReportCli() {
            return new ArrayDesignReportCli();
        }

        @Bean
        public ManualAuthenticationService manualAuthenticationService() {
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
        public ArrayDesignReportService arrayDesignReportService() {
            return mock();
        }

        @Bean
        public ArrayDesignService arrayDesignService() {
            return mock();
        }

        @Bean
        public ArrayDesignSequenceAlignmentService arrayDesignSequenceAlignmentService() {
            return mock();
        }

        @Bean
        public TaxonService taxonService() {
            return mock();
        }

        @Bean
        public BioSequenceService bioSequenceService() {
            return mock();
        }

        @Bean
        public ArrayDesignProbeMapperService arrayDesignProbeMapperService() {
            return mock();
        }

        @Bean
        public ExternalDatabaseService externalDatabaseService() {
            return mock();
        }

        @Bean
        public CompositeSequenceService compositeSequenceService() {
            return mock();
        }

        @Bean
        public GoldenPathSequenceAnalysisFactory goldenPathSequenceAnalysisFactory() {
            return mock();
        }

        @Bean
        public BlatAssociationService blatAssociationService() {
            return mock();
        }

        @Bean
        public BlatResultService blatResultService() {
            return mock();
        }

        @Bean
        public ArrayDesignSequenceProcessingService arrayDesignSequenceProcessingService() {
            return mock();
        }

        @Bean
        public CliArrayDesignAuditService cliArrayDesignAuditService() {
            return mock();
        }

        @Bean
        public GemmaRestApiClient gemmaRestApiClient() {
            return mock();
        }

        @Bean
        public EntityLocator entityLocator() {
            return mock();
        }
    }

    @Autowired
    private ObjectProvider<ArrayDesignBlatCli> blatCli;

    @Autowired
    private ObjectProvider<ArrayDesignRepeatScanCli> repeatScanCli;

    @Autowired
    private ObjectProvider<ArrayDesignProbeMapperCli> probeMapperCli;

    @Autowired
    private ObjectProvider<BioSequenceCleanupCli> seqCleanupCli;

    @Autowired
    private ObjectProvider<ArrayDesignSequenceAssociationCli> sequenceAssociationCli;

    @Autowired
    private ObjectProvider<ArrayDesignReportCli> reportCli;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private ArrayDesignReportService arrayDesignReportService;

    @Autowired
    private ArrayDesignProbeMapperService arrayDesignProbeMapperService;

    @Autowired
    private BioSequenceService bioSequenceService;

    @Autowired
    private ArrayDesignSequenceProcessingService arrayDesignSequenceProcessingService;

    @Autowired
    private EntityLocator entityLocator;

    @Autowired
    private ArrayDesignSequenceAlignmentService arrayDesignSequenceAlignmentService;

    @Autowired
    private AuditEventService auditEventService;

    @Autowired
    private CliArrayDesignAuditService cliArrayDesignAuditService;

    @AfterEach
    void tearDown() {
        reset( arrayDesignService, arrayDesignSequenceAlignmentService, auditEventService, cliArrayDesignAuditService,
                arrayDesignProbeMapperService, bioSequenceService, arrayDesignSequenceProcessingService, entityLocator,
                arrayDesignReportService );
    }

    /**
     * A platform that blatPlatform skips must not end the run for the platforms after it.
     */
    @Test
    @WithMockUser
    void blatSkippedPlatformDoesNotEndTheRun() {
        ArrayDesign skipped = platform( "GPL1", TechnologyType.GENELIST );
        ArrayDesign next = platform( "GPL2", TechnologyType.ONECOLOR );
        when( arrayDesignService.loadAll() ).thenReturn( Arrays.asList( skipped, next ) );
        when( arrayDesignService.thaw( any( ArrayDesign.class ) ) ).thenAnswer( a -> a.getArgument( 0 ) );

        assertThat( blatCli.getObject() )
                .withArguments( "-all" )
                .succeeds();

        verify( arrayDesignSequenceAlignmentService ).processArrayDesign( next, false );
        verify( arrayDesignSequenceAlignmentService, never() ).processArrayDesign( same( skipped ), anyBoolean() );
    }

    /**
     * A platform that platformRepeatScan skips must not end the run for the platforms after it.
     */
    @Test
    @WithMockUser
    void repeatScanSkippedPlatformDoesNotEndTheRun() {
        ArrayDesign recentlyScanned = platform( "GPL1", TechnologyType.ONECOLOR );
        ArrayDesign next = platform( "GPL2", TechnologyType.ONECOLOR );
        when( arrayDesignService.loadAll() ).thenReturn( Arrays.asList( recentlyScanned, next ) );
        when( arrayDesignService.thaw( any( ArrayDesign.class ) ) ).thenAnswer( a -> a.getArgument( 0 ) );
        when( auditEventService.getEvents( recentlyScanned ) )
                .thenReturn( Collections.singletonList( repeatScanEvent( new Date() ) ) );

        assertThat( repeatScanCli.getObject() )
                .withArguments( "-all", "-mdate", "2020-01-01" )
                .succeeds();

        verify( cliArrayDesignAuditService ).recordRepeatAnalysis( same( next ), anyString() );
        verify( cliArrayDesignAuditService, never() ).recordRepeatAnalysis( same( recentlyScanned ), anyString() );
    }

    /*
     * -markOnly. BLAT results and gene mappings are held per sequence, so running one platform does the work for
     * every platform sharing its sequences, while the audit event propagates only down the merge tree: those
     * platforms end up with fresh data and a trail saying they were never processed.
     */

    @Test
    @WithMockUser
    void blatMarkOnlyRecordsTheEventAndRunsNoBlat() {
        ArrayDesign platform = platform( "GPL6885", TechnologyType.ONECOLOR );
        when( entityLocator.locateArrayDesign( "GPL6885" ) ).thenReturn( platform );

        assertThat( blatCli.getObject() )
                .withArguments( "-a", "GPL6885", "-markOnly", "sequences aligned by GPL6887" )
                .succeeds();

        verify( cliArrayDesignAuditService ).recordSequenceAnalysis( same( platform ), eq( "sequences aligned by GPL6887" ) );
        verifyNoInteractions( arrayDesignSequenceAlignmentService );
    }

    @Test
    @WithMockUser
    void probeMapperMarkOnlyRecordsTheEventAndMapsNothing() {
        ArrayDesign platform = platform( "GPL6885", TechnologyType.ONECOLOR );
        when( entityLocator.locateArrayDesign( "GPL6885" ) ).thenReturn( platform );

        assertThat( probeMapperCli.getObject() )
                .withArguments( "-a", "GPL6885", "-markOnly", "sequences mapped by GPL6887" )
                .succeeds();

        verify( cliArrayDesignAuditService ).recordAlignmentBasedGeneMapping( same( platform ), eq( "sequences mapped by GPL6887" ) );
        verifyNoInteractions( arrayDesignProbeMapperService );
    }

    /**
     * The event says the platform was processed, so marking a set nobody enumerated would put a claim in the audit
     * trail about platforms no one looked at.
     */
    @Test
    @WithMockUser
    void markOnlyRefusesPlatformsItWasNotGiven() {
        assertThat( probeMapperCli.getObject() )
                .withArguments( "-auto", "-markOnly", "sequences mapped by GPL6887" )
                .fails()
                .exitCause()
                .hasMessageContaining( "name the platforms to mark with -a or -f" );

        verifyNoInteractions( arrayDesignProbeMapperService, cliArrayDesignAuditService );
    }

    @Test
    @WithMockUser
    void markOnlyRefusesTheOptionsThatWouldDoWork() {
        assertThat( blatCli.getObject() )
                .withArguments( "-a", "GPL6885", "-sensitive", "-markOnly", "sequences aligned by GPL6887" )
                .fails()
                .exitCause()
                .hasMessageContaining( "cannot be combined with -sensitive" );

        verifyNoInteractions( arrayDesignSequenceAlignmentService, cliArrayDesignAuditService );
    }

    /*
     * The modes below select their own platforms (or none) and were rejected by the base class's "No platforms
     * matched" guard before they were reached.
     */

    @Test
    @WithMockUser
    void blatTaxonModeRunsTheTaxonsPlatforms() {
        Taxon human = Taxon.Factory.newInstance( "human" );
        ArrayDesign platform = platform( "GPL1", TechnologyType.ONECOLOR );
        when( entityLocator.locateTaxon( "human" ) ).thenReturn( human );
        when( arrayDesignService.findByTaxon( human ) ).thenReturn( Collections.singletonList( platform ) );
        when( arrayDesignService.thaw( any( ArrayDesign.class ) ) ).thenAnswer( a -> a.getArgument( 0 ) );

        assertThat( blatCli.getObject() )
                .withArguments( "-t", "human" )
                .succeeds();

        verify( arrayDesignSequenceAlignmentService ).processArrayDesign( platform, false );
    }

    @Test
    @WithMockUser
    void repeatScanLimitingDateModeScansAllPlatforms() {
        ArrayDesign recentlyScanned = platform( "GPL1", TechnologyType.ONECOLOR );
        ArrayDesign due = platform( "GPL2", TechnologyType.ONECOLOR );
        when( arrayDesignService.loadAll() ).thenReturn( Arrays.asList( recentlyScanned, due ) );
        when( arrayDesignService.thaw( any( ArrayDesign.class ) ) ).thenAnswer( a -> a.getArgument( 0 ) );
        when( auditEventService.getEvents( recentlyScanned ) )
                .thenReturn( Collections.singletonList( repeatScanEvent( new Date() ) ) );

        // the recently scanned platform is skipped with a warning, which does not fail the run
        assertThat( repeatScanCli.getObject() )
                .withArguments( "-mdate", "2020-01-01" )
                .succeeds();

        verify( cliArrayDesignAuditService, atLeastOnce() ).recordRepeatAnalysis( same( due ), anyString() );
        verify( cliArrayDesignAuditService, never() ).recordRepeatAnalysis( same( recentlyScanned ), anyString() );
    }

    @Test
    @WithMockUser
    void probeMapperTaxonModeMapsTheTaxonsPlatforms() {
        Taxon human = Taxon.Factory.newInstance( "human" );
        ArrayDesign platform = readyToMap( "GPL1", human );
        when( entityLocator.locateTaxon( "human" ) ).thenReturn( human );
        when( arrayDesignService.findByTaxon( human ) ).thenReturn( Collections.singletonList( platform ) );
        when( arrayDesignService.getTaxaFromBioSequences( platform ) ).thenReturn( Collections.singleton( human ) );

        assertThat( probeMapperCli.getObject() )
                .withArguments( "-t", "human" )
                .succeeds();

        verify( arrayDesignProbeMapperService ).processArrayDesign( same( platform ), any(), eq( true ) );
        verify( cliArrayDesignAuditService ).recordAlignmentBasedGeneMapping( same( platform ), anyString() );
    }

    @Test
    @WithMockUser
    void probeMapperLimitingDateModeMapsAllPlatforms() {
        ArrayDesign platform = readyToMap( "GPL1", Taxon.Factory.newInstance( "human" ) );
        when( arrayDesignService.loadAll() ).thenReturn( Collections.singletonList( platform ) );

        assertThat( probeMapperCli.getObject() )
                .withArguments( "-mdate", "2020-01-01" )
                .succeeds();

        verify( arrayDesignProbeMapperService ).processArrayDesign( same( platform ), any(), eq( true ) );
    }

    @Test
    @WithMockUser
    void probeMapperAutoModeMapsAllPlatforms() {
        ArrayDesign platform = readyToMap( "GPL1", Taxon.Factory.newInstance( "human" ) );
        when( arrayDesignService.loadAll() ).thenReturn( Collections.singletonList( platform ) );

        assertThat( probeMapperCli.getObject() )
                .withArguments( "-auto" )
                .succeeds();

        verify( arrayDesignProbeMapperService ).processArrayDesign( same( platform ), any(), eq( true ) );
    }

    /**
     * With -nodb nothing is written, so the batch path must not record a gene mapping event (the single-platform
     * path already did not).
     */
    @Test
    @WithMockUser
    void probeMapperBatchWithNoDbRecordsNoMappingEvent() {
        Taxon human = Taxon.Factory.newInstance( "human" );
        ArrayDesign platform = readyToMap( "GPL1", human );
        when( entityLocator.locateTaxon( "human" ) ).thenReturn( human );
        when( arrayDesignService.findByTaxon( human ) ).thenReturn( Collections.singletonList( platform ) );
        when( arrayDesignService.getTaxaFromBioSequences( platform ) ).thenReturn( Collections.singleton( human ) );

        assertThat( probeMapperCli.getObject() )
                .withArguments( "-t", "human", "-nodb" )
                .succeeds();

        verify( arrayDesignProbeMapperService ).processArrayDesign( same( platform ), any(), eq( false ) );
        verifyNoInteractions( cliArrayDesignAuditService );
    }

    @Test
    @WithMockUser
    void probeMapperProbesWithoutPlatformStillFails() {
        when( entityLocator.locateTaxon( "human" ) ).thenReturn( Taxon.Factory.newInstance( "human" ) );

        assertThat( probeMapperCli.getObject() )
                .withArguments( "-t", "human", "-probes", "p1" )
                .fails()
                .exitCause()
                .hasMessageStartingWith( "No platforms matched the given options" );
        verifyNoInteractions( arrayDesignProbeMapperService );
    }

    @Test
    @WithMockUser
    void seqCleanupChecksTheSequencesListedInTheFile( @TempDir Path dir ) throws Exception {
        Path ids = Files.write( dir.resolve( "ids.txt" ), Arrays.asList( "1", "", "2" ) );
        when( bioSequenceService.load( anyCollection() ) ).thenReturn( Collections.emptyList() );
        when( bioSequenceService.thaw( anyCollection() ) ).thenAnswer( a -> a.getArgument( 0 ) );

        assertThat( seqCleanupCli.getObject() )
                .withArguments( "-b", ids.toString(), "-dryrun" )
                .succeeds();

        verify( bioSequenceService ).load( new HashSet<>( Arrays.asList( 1L, 2L ) ) );
    }

    /**
     * This used to be caught and discarded with no log, so a bad file ended the run with exit 0.
     */
    @Test
    @WithMockUser
    void seqCleanupFailsOnAnUnreadableFile( @TempDir Path dir ) {
        assertThat( seqCleanupCli.getObject() )
                .withArguments( "-b", dir.resolve( "missing.txt" ).toString(), "-dryrun" )
                .fails();
        verifyNoInteractions( bioSequenceService );
    }

    @Test
    @WithMockUser
    void addPlatformSequencesUpdatesASingleAccession() {
        when( arrayDesignSequenceProcessingService.processSingleAccession( eq( "AB000001" ), any(), eq( false ) ) )
                .thenReturn( BioSequence.Factory.newInstance() );

        assertThat( sequenceAssociationCli.getObject() )
                .withArguments( "-s", "AB000001", "-y", "EST" )
                .succeeds();

        verify( arrayDesignSequenceProcessingService ).processSingleAccession( eq( "AB000001" ), any(), eq( false ) );
    }

    @Test
    @WithMockUser
    void addPlatformSequencesFailsWhenTheAccessionIsNotFound() {
        assertThat( sequenceAssociationCli.getObject() )
                .withArguments( "-s", "AB000001", "-y", "EST" )
                .fails();
    }

    /**
     * A report that could not be written used to be logged by the report service and counted as written here.
     */
    @Test
    @WithMockUser
    void updatePlatformReportsFailsWhenAReportCannotBeWritten() {
        ArrayDesign unwritable = platform( "GPL1", TechnologyType.ONECOLOR );
        unwritable.setId( 1L );
        ArrayDesign next = platform( "GPL2", TechnologyType.ONECOLOR );
        next.setId( 2L );
        ArrayDesignValueObject unwritableVo = new ArrayDesignValueObject( 1L );
        ArrayDesignValueObject nextVo = new ArrayDesignValueObject( 2L );
        when( arrayDesignService.loadAll() ).thenReturn( Arrays.asList( unwritable, next ) );
        when( arrayDesignService.loadValueObjectsByIds( Collections.singleton( 1L ) ) )
                .thenReturn( Collections.singletonList( unwritableVo ) );
        when( arrayDesignService.loadValueObjectsByIds( Collections.singleton( 2L ) ) )
                .thenReturn( Collections.singletonList( nextVo ) );
        doThrow( new UncheckedIOException( new IOException( "No space left on device" ) ) )
                .when( arrayDesignReportService ).generateArrayDesignReport( same( unwritableVo ) );

        assertThat( reportCli.getObject() )
                .withArguments( "-all" )
                .fails();

        verify( arrayDesignReportService ).generateArrayDesignReport( same( nextVo ) );
    }

    /**
     * A platform that probe mapping will accept: a microarray with a sequence analysis on record.
     */
    private ArrayDesign readyToMap( String shortName, Taxon taxon ) {
        ArrayDesign ad = platform( shortName, TechnologyType.ONECOLOR );
        ad.setPrimaryTaxon( taxon );
        when( arrayDesignService.thaw( any( ArrayDesign.class ) ) ).thenAnswer( a -> a.getArgument( 0 ) );
        when( arrayDesignService.thawLite( any( ArrayDesign.class ) ) ).thenAnswer( a -> a.getArgument( 0 ) );
        when( auditEventService.getEvents( ad ) ).thenReturn( Collections.singletonList(
                AuditEvent.Factory.newInstance( new Date(), AuditAction.UPDATE, "", null, null, new ArrayDesignSequenceAnalysisEvent() ) ) );
        return ad;
    }

    static ArrayDesign platform( String shortName, TechnologyType technologyType ) {
        ArrayDesign ad = ArrayDesign.Factory.newInstance( shortName, null );
        ad.setTechnologyType( technologyType );
        return ad;
    }

    static AuditEvent repeatScanEvent( Date date ) {
        return AuditEvent.Factory.newInstance( date, AuditAction.UPDATE, "", null, null, new ArrayDesignRepeatAnalysisEvent() );
    }
}
