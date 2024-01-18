package ubic.gemma.core.search.source;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.core.genome.gene.service.GeneSetService;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchSource;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSetService;
import ubic.gemma.persistence.service.genome.biosequence.BioSequenceService;
import ubic.gemma.persistence.service.genome.gene.GeneProductService;
import ubic.gemma.persistence.util.TestComponent;

import static org.mockito.Mockito.*;

@ContextConfiguration
public class DatabaseSearchSourceTest extends AbstractJUnit4SpringContextTests {

    @Configuration
    @TestComponent
    static class DatabaseSearchSourceTestContextConfiguration {

        @Bean
        public SearchSource databaseSearchSource() {
            return new DatabaseSearchSource();
        }

        @Bean
        public BioSequenceService bioSequenceService() {
            return mock( BioSequenceService.class );
        }

        @Bean
        public CompositeSequenceService compositeSequenceService() {
            return mock( CompositeSequenceService.class );
        }

        @Bean
        public ExpressionExperimentService expressionExperimentService() {
            return mock( ExpressionExperimentService.class );
        }

        @Bean
        public GeneService geneService() {
            return mock( GeneService.class );
        }

        @Bean
        public GeneProductService geneProductService() {
            return mock( GeneProductService.class );
        }

        @Bean
        public GeneSetService geneSetService() {
            return mock( GeneSetService.class );
        }

        @Bean
        public ExpressionExperimentSetService experimentSetService() {
            return mock( ExpressionExperimentSetService.class );
        }
    }

    @Autowired
    @Qualifier("databaseSearchSource")
    private SearchSource databaseSearchSource;

    @Autowired
    private GeneService geneService;

    @After
    public void tearDown() {
        reset( geneService );
    }

    @Test
    public void test_whenQueryContainsQuote_thenStripThem() throws SearchException {
        databaseSearchSource.searchGene( SearchSettings.geneSearch( "\"BRCA1\"", null ) );
        verify( geneService ).findByAccession( "BRCA1", null );
        verify( geneService ).findByOfficialSymbolInexact( "BRCA1%" );
    }

    @Test
    public void test_whenQueryContainsLikePatterns_thenEscape() throws SearchException {
        databaseSearchSource.searchGene( SearchSettings.geneSearch( "BRCA%", null ) );
        verify( geneService ).findByOfficialSymbolInexact( "BRCA\\%%" );
    }

    @Test
    public void test_whenQueryContainsAsterisk_thenSubstituteForPercent() throws SearchException {
        databaseSearchSource.searchGene( SearchSettings.geneSearch( "BRCA?*", null ) );
        verify( geneService ).findByOfficialSymbolInexact( "BRCA_%" );
    }

    @Test
    public void test_quotedTerms() throws SearchException {
        databaseSearchSource.searchGene( SearchSettings.geneSearch( "\"BRCA1\" \"BRCA2\"", null ) );
        verify( geneService ).findByOfficialSymbol( "BRCA1 BRCA2" );
    }
}