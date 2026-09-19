package ubic.gemma.apps;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
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
import ubic.gemma.core.loader.expression.arrayDesign.ArrayDesignSequenceAlignmentService;
import ubic.gemma.core.security.authentication.ManualAuthenticationService;
import ubic.gemma.core.util.GemmaRestApiClient;
import ubic.gemma.model.common.auditAndSecurity.AuditAction;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignRepeatAnalysisEvent;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.genome.biosequence.BioSequenceService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static ubic.gemma.cli.util.test.Assertions.assertThat;

/**
 * Platform pipeline CLIs (blatPlatform, platformRepeatScan, ...) run against mocked services.
 */
@ContextConfiguration
@TestPropertySource(properties = { "repeatMasker.exe=RepeatMasker" })
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
    private ArrayDesignService arrayDesignService;

    @Autowired
    private ArrayDesignSequenceAlignmentService arrayDesignSequenceAlignmentService;

    @Autowired
    private AuditEventService auditEventService;

    @Autowired
    private CliArrayDesignAuditService cliArrayDesignAuditService;

    @AfterEach
    void tearDown() {
        reset( arrayDesignService, arrayDesignSequenceAlignmentService, auditEventService, cliArrayDesignAuditService );
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

    static ArrayDesign platform( String shortName, TechnologyType technologyType ) {
        ArrayDesign ad = ArrayDesign.Factory.newInstance( shortName, null );
        ad.setTechnologyType( technologyType );
        return ad;
    }

    static AuditEvent repeatScanEvent( Date date ) {
        return AuditEvent.Factory.newInstance( date, AuditAction.UPDATE, "", null, null, new ArrayDesignRepeatAnalysisEvent() );
    }
}
