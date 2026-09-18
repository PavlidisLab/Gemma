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
import ubic.gemma.cli.util.EntityLocator;
import ubic.gemma.cli.util.test.BaseCliTest5;
import ubic.gemma.core.analysis.report.ArrayDesignReportService;
import ubic.gemma.core.analysis.service.ArrayDesignAnnotationService;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.ontology.providers.GeneOntologyService;
import ubic.gemma.core.security.authentication.ManualAuthenticationService;
import ubic.gemma.core.util.GemmaRestApiClient;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.persistence.service.genome.taxon.TaxonReadService;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static ubic.gemma.cli.util.test.Assertions.assertThat;

/**
 * {@code makePlatformAnnotFiles --batch} matched no platforms at all once platform selection moved into
 * {@link ArrayDesignSequenceManipulatingCli#doAuthenticatedWork()}, because that rejected an empty {@code -a}/{@code -f}
 * selection before the batch option was consulted.
 */
@ContextConfiguration
@TestExecutionListeners(value = WithSecurityContextTestExecutionListener.class,
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
class ArrayDesignAnnotationFileCliTest extends BaseCliTest5 {

    @Configuration
    @TestComponent
    static class ArrayDesignAnnotationFileCliTestContextConfiguration {

        @Bean
        @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
        public ArrayDesignAnnotationFileCli arrayDesignAnnotationFileCli() {
            return new ArrayDesignAnnotationFileCli();
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
        public ArrayDesignAnnotationService arrayDesignAnnotationService() {
            return mock();
        }

        @Bean
        public GeneOntologyService geneOntologyService() {
            return mock();
        }

        @Bean
        public TaxonReadService taxonReadService() {
            return mock();
        }

        @Bean
        public GeneService geneService() {
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
    private ObjectProvider<ArrayDesignAnnotationFileCli> cliProvider;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private ArrayDesignAnnotationService arrayDesignAnnotationService;

    @Autowired
    private TaxonReadService taxonReadService;

    @AfterEach
    void tearDown() {
        reset( arrayDesignService, arrayDesignAnnotationService, taxonReadService );
    }

    @Test
    @WithMockUser
    void batchProcessesEveryEligiblePlatform() throws Exception {
        ArrayDesign eligible = platform( "GPL1", TechnologyType.ONECOLOR );
        ArrayDesign troubled = platform( "GPL2", TechnologyType.ONECOLOR );
        troubled.getCurationDetails().setTroubled( true );
        ArrayDesign sequencing = platform( "GPL3", TechnologyType.SEQUENCING );
        when( arrayDesignService.loadAll() ).thenReturn( Arrays.asList( eligible, troubled, sequencing ) );
        when( arrayDesignService.thawLite( any( ArrayDesign.class ) ) ).thenAnswer( a -> a.getArgument( 0 ) );

        assertThat( cliProvider.getObject() )
                .withArguments( "-b", "-nogo", "-k" )
                .succeeds();

        verify( arrayDesignAnnotationService ).create( eligible, false, false );
        verify( arrayDesignAnnotationService, never() ).create( same( troubled ), anyBoolean(), anyBoolean() );
        verify( arrayDesignAnnotationService, never() ).create( same( sequencing ), anyBoolean(), anyBoolean() );
    }

    @Test
    @WithMockUser
    void batchWithTaxonProcessesThatTaxonsPlatforms() throws Exception {
        Taxon human = Taxon.Factory.newInstance( "human" );
        ArrayDesign platform = platform( "GPL1", TechnologyType.ONECOLOR );
        when( taxonReadService.findByCommonName( "human" ) ).thenReturn( human );
        when( arrayDesignService.findByTaxon( human ) ).thenReturn( Collections.singletonList( platform ) );
        when( arrayDesignService.thawLite( any( ArrayDesign.class ) ) ).thenAnswer( a -> a.getArgument( 0 ) );

        assertThat( cliProvider.getObject() )
                .withArguments( "-b", "-t", "human", "-nogo", "-k" )
                .succeeds();

        verify( arrayDesignAnnotationService ).create( platform, false, false );
        verify( arrayDesignService, never() ).loadAll();
    }

    @Test
    @WithMockUser
    void noSelectionStillFails() {
        assertThat( cliProvider.getObject() )
                .withArguments( "-nogo" )
                .fails()
                .exitCause()
                .hasMessageStartingWith( "No platforms matched the given options" );
        verifyNoInteractions( arrayDesignAnnotationService );
    }

    private static ArrayDesign platform( String shortName, TechnologyType technologyType ) {
        ArrayDesign ad = ArrayDesign.Factory.newInstance( shortName, null );
        ad.setTechnologyType( technologyType );
        return ad;
    }
}
