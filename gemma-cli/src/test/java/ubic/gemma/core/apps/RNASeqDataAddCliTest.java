package ubic.gemma.core.apps;

import gemma.gsec.authentication.ManualAuthenticationService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
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
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.core.loader.expression.DataUpdater;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.core.util.GemmaRestApiClient;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSetService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.persistence.util.TestComponent;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ContextConfiguration
@TestExecutionListeners(WithSecurityContextTestExecutionListener.class)
public class RNASeqDataAddCliTest extends AbstractJUnit4SpringContextTests {

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
    }

    @Autowired
    private RNASeqDataAddCli cli;

    @Autowired
    private DataUpdater dataUpdater;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ArrayDesignService arrayDesignService;

    private ArrayDesign ad;
    private ExpressionExperiment ee;
    private String rpkmFile;

    @Before
    public void setUp() throws IOException {
        ad = new ArrayDesign();
        ee = new ExpressionExperiment();
        ee.setId( 1L );
        rpkmFile = new ClassPathResource( "ubic/gemma/core/apps/test.rpkm.txt" ).getFile().getAbsolutePath();
        when( expressionExperimentService.findByShortName( "GSE000001" ) ).thenReturn( ee );
        when( expressionExperimentService.thawLite( any() ) ).thenAnswer( a -> a.getArgument( 0 ) );
        when( arrayDesignService.findByShortName( "test" ) ).thenReturn( ad );
    }

    @After
    public void tearDown() {
        reset( expressionExperimentService, arrayDesignService );
    }

    @Test
    @WithMockUser
    public void testPaired() {
        cli.executeCommand( new String[] { "-e", "GSE000001", "-rpkm", rpkmFile, "-a", "test", "-rlen", "36:paired" } );
        verify( dataUpdater ).addCountData( same( ee ), same( ad ), isNull(), any(), eq( 36 ), eq( true ), eq( false ) );
    }

    @Test
    @WithMockUser
    public void testUnpaired() {
        cli.executeCommand( new String[] { "-e", "GSE000001", "-rpkm", rpkmFile, "-a", "test", "-rlen", "36:unpaired" } );
        verify( dataUpdater ).addCountData( same( ee ), same( ad ), isNull(), any(), eq( 36 ), eq( false ), eq( false ) );
    }

    @Test
    @WithMockUser
    public void testWhenPairednessIsUnknown() {
        cli.executeCommand( new String[] { "-e", "GSE000001", "-rpkm", rpkmFile, "-a", "test", "-rlen", "36" } );
        verify( dataUpdater ).addCountData( same( ee ), same( ad ), isNull(), any(), eq( 36 ), isNull(), eq( false ) );
    }
}