package ubic.gemma.core.apps;

import gemma.gsec.authentication.ManualAuthenticationService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.analysis.service.ExpressionMetadataChangelogFileService;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.loader.expression.DataUpdater;
import ubic.gemma.core.loader.expression.sequencing.SequencingMetadata;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.core.util.EntityLocator;
import ubic.gemma.core.util.GemmaRestApiClient;
import ubic.gemma.core.util.test.BaseCliTest;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSetService;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.persistence.util.EntityUrlBuilder;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static ubic.gemma.core.util.test.Assertions.assertThat;

@ContextConfiguration
@TestExecutionListeners(WithSecurityContextTestExecutionListener.class)
public class RNASeqDataAddCliTest extends BaseCliTest {

    @Configuration
    @TestComponent
    static class RNASeqDataAddCliTestContextConfiguration {

        @Bean
        @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
        public RNASeqDataAddCli rnaSeqDataAddCli() {
            return new RNASeqDataAddCli();
        }

        @Bean
        public DataUpdater dataUpdater() {
            return mock();
        }

        @Bean
        public GeneService geneService() {
            return mock();
        }

        @Bean
        public TaxonService taxonService() {
            return mock();
        }

        @Bean
        public SearchService searchService() {
            return mock();
        }

        @Bean
        public ExpressionDataFileService expressionDataFileService() {
            return mock();
        }

        @Bean
        public ArrayDesignService arrayDesignService() {
            return mock();
        }

        @Bean
        public ExpressionExperimentService expressionExperimentService() {
            return mock();
        }

        @Bean
        public ExpressionExperimentSetService expressionExperimentSetService() {
            return mock();
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
        public GemmaRestApiClient gemmaRestApiClient() {
            return mock();
        }

        @Bean
        public EntityLocator entityLocator() {
            return mock();
        }

        @Bean
        public EntityUrlBuilder entityUrlBuilder() {
            return new EntityUrlBuilder( "https://gemma.msl.ubc.ca" );
        }

        @Bean
        public ExpressionMetadataChangelogFileService expressionMetadataChangelogFileService() {
            return mock();
        }
    }

    @Autowired
    private RNASeqDataAddCli cli;

    @Autowired
    private DataUpdater dataUpdater;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private EntityLocator entityLocator;

    private ArrayDesign ad;
    private ExpressionExperiment ee;
    private String rpkmFile;

    @Before
    public void setUp() throws IOException {
        ad = new ArrayDesign();
        ee = new ExpressionExperiment();
        ee.setId( 1L );
        for ( int i = 0; i < 4; i++ ) {
            ee.getBioAssays().add( BioAssay.Factory.newInstance( "ba" + i ) );
        }
        rpkmFile = new ClassPathResource( "ubic/gemma/core/apps/test.rpkm.txt" ).getFile().getAbsolutePath();
        when( entityLocator.locateExpressionExperiment( "GSE000001", false ) ).thenReturn( ee );
        when( expressionExperimentService.thawLite( any() ) ).thenAnswer( a -> a.getArgument( 0 ) );
        when( entityLocator.locateArrayDesign( "test" ) ).thenReturn( ad );
    }

    @After
    public void tearDown() {
        reset( expressionExperimentService, arrayDesignService );
    }

    @Test
    @WithMockUser
    public void testPaired() {
        assertThat( cli )
                .withArguments( "-e", "GSE000001", "-rpkm", rpkmFile, "-a", "test", "-rlen", "36:paired" )
                .succeeds();
        ArgumentCaptor<Map<BioAssay, SequencingMetadata>> captor = ArgumentCaptor.captor();
        verify( dataUpdater ).addCountData( same( ee ), same( ad ), isNull(), any(), captor.capture(), eq( false ) );
        assertThat( captor.getValue() )
                .hasSize( 4 )
                .values()
                .allSatisfy( sm -> {
                    assertThat( sm.getReadCount() ).isNull();
                    assertThat( sm.getReadLength() ).isEqualTo( 36 );
                    assertThat( sm.getIsPaired() ).isTrue();
                } );
    }

    @Test
    @WithMockUser
    public void testUnpaired() {
        assertThat( cli )
                .withArguments( "-e", "GSE000001", "-rpkm", rpkmFile, "-a", "test", "-rlen", "36:unpaired" )
                .succeeds();
        ArgumentCaptor<Map<BioAssay, SequencingMetadata>> captor = ArgumentCaptor.captor();
        verify( dataUpdater ).addCountData( same( ee ), same( ad ), isNull(), any(), captor.capture(), eq( false ) );
        assertThat( captor.getValue() )
                .hasSize( 4 )
                .values()
                .allSatisfy( sm -> {
                    assertThat( sm.getReadCount() ).isNull();
                    assertThat( sm.getReadLength() ).isEqualTo( 36 );
                    assertThat( sm.getIsPaired() ).isFalse();
                } );
    }

    @Test
    @WithMockUser
    public void testWhenPairednessIsUnknown() {
        assertThat( cli )
                .withArguments( "-e", "GSE000001", "-rpkm", rpkmFile, "-a", "test", "-rlen", "36" )
                .succeeds();
        ArgumentCaptor<Map<BioAssay, SequencingMetadata>> captor = ArgumentCaptor.captor();
        verify( dataUpdater ).addCountData( same( ee ), same( ad ), isNull(), any(), captor.capture(), eq( false ) );
        assertThat( captor.getValue() )
                .hasSize( 4 )
                .values()
                .allSatisfy( sm -> {
                    assertThat( sm.getReadCount() ).isNull();
                    assertThat( sm.getReadLength() ).isEqualTo( 36 );
                    assertThat( sm.getIsPaired() ).isNull();
                } );
    }
}