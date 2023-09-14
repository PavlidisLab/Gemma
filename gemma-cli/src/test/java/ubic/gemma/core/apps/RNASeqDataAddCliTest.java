package ubic.gemma.core.apps;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.core.loader.expression.DataUpdater;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.core.util.test.BaseCliTest;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.persistence.util.TestComponent;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration
public class RNASeqDataAddCliTest extends BaseCliTest {

    @TestComponent
    @Configuration
    static class RNASeqDataAddCliTestContextConfiguration extends BaseCliTestContextConfiguration {

        @Bean
        public RNASeqDataAddCli rnaSeqDataAddCli() {
            return new RNASeqDataAddCli();
        }

        @Bean
        public DataUpdater dataUpdater() {
            return mock( DataUpdater.class );
        }

        @Bean
        public GeneService geneService() {
            return mock( GeneService.class );
        }

        @Bean
        public TaxonService taxonService() {
            return mock( TaxonService.class );
        }

        @Bean
        public SearchService searchService() {
            return mock( SearchService.class );
        }

        @Bean
        public ExpressionDataFileService expressionDataFileService() {
            return mock( ExpressionDataFileService.class );
        }

        @Bean
        public ArrayDesignService arrayDesignService() {
            return mock( ArrayDesignService.class );
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

    @Test
    @WithMockUser
    public void test() {
        ExpressionExperiment ee = new ExpressionExperiment();
        ArrayDesign ad = new ArrayDesign();
        when( expressionExperimentService.findByShortName( "GSE00001" ) ).thenReturn( ee );
        when( arrayDesignService.findByShortName( "Generic_human_ncbiIds" ) ).thenReturn( ad );
        assertEquals( AbstractCLI.SUCCESS, cli.executeCommand( new String[] {
                "-e", "GSE00001", "-a", "Generic_human_ncbiIds",
                "-rpkm", getClass().getResource( "/GSE00001.rpkm.txt" ).getFile(),
                "-count", getClass().getResource( "/GSE00001.counts.txt" ).getFile() } ) );
        verify( dataUpdater ).addCountData( same( ee ), same( ad ), isNotNull(), isNotNull(), isNull(),
                isNull(), eq( false ), eq( false ) );
    }

    @Test
    @WithMockUser
    public void testSkipLog2cpm() {
        ExpressionExperiment ee = new ExpressionExperiment();
        ArrayDesign ad = new ArrayDesign();
        when( expressionExperimentService.findByShortName( "GSE00001" ) ).thenReturn( ee );
        when( arrayDesignService.findByShortName( "Generic_human_ncbiIds" ) ).thenReturn( ad );
        assertEquals( AbstractCLI.SUCCESS, cli.executeCommand( new String[] {
                "-e", "GSE00001", "-a", "Generic_human_ncbiIds",
                "-rpkm", getClass().getResource( "/GSE00001.rpkm.txt" ).getFile(),
                "-count", getClass().getResource( "/GSE00001.counts.txt" ).getFile(),
                "-noLog2cpm" } ) );
        verify( dataUpdater ).addCountData( same( ee ), same( ad ), isNotNull(), isNotNull(), isNull(),
                isNull(), eq( false ), eq( true ) );
    }
}