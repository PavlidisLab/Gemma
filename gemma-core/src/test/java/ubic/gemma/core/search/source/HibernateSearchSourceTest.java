package ubic.gemma.core.search.source;

import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.util.TestComponent;

import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration
public class HibernateSearchSourceTest extends BaseDatabaseTest {

    @Configuration
    @TestComponent
    static class HibernateSearchSourceContextConfiguration extends BaseDatabaseTestContextConfiguration {

        @Bean
        public HibernateSearchSource hibernateSearchSource() {
            return new HibernateSearchSource();
        }
    }

    @Autowired
    private HibernateSearchSource hibernateSearchSource;

    @Test
    public void test() {
        assertThat( hibernateSearchSource.searchExpressionExperiment( SearchSettings.expressionExperimentSearch( "hello" ) ) )
                .isEmpty();
        assertThat( hibernateSearchSource.searchArrayDesign( SearchSettings.expressionExperimentSearch( "hello" ) ) )
                .isEmpty();
        assertThat( hibernateSearchSource.searchGene( SearchSettings.expressionExperimentSearch( "hello" ) ) )
                .isEmpty();
        assertThat( hibernateSearchSource.searchGeneSet( SearchSettings.expressionExperimentSearch( "hello" ) ) )
                .isEmpty();
        assertThat( hibernateSearchSource.searchBioSequence( SearchSettings.expressionExperimentSearch( "hello" ) ) )
                .isEmpty();
        assertThat( hibernateSearchSource.searchBibliographicReference( SearchSettings.expressionExperimentSearch( "hello" ) ) )
                .isEmpty();
        assertThat( hibernateSearchSource.searchExperimentSet( SearchSettings.expressionExperimentSearch( "hello" ) ) )
                .isEmpty();
        assertThat( hibernateSearchSource.searchCompositeSequence( SearchSettings.expressionExperimentSearch( "hello" ) ) )
                .isEmpty();
    }

    @Test
    public void testSearchExpressionExperiment() {
        FullTextSession fts = Search.getFullTextSession( sessionFactory.getCurrentSession() );

        assertThat( hibernateSearchSource.searchExpressionExperiment( SearchSettings.expressionExperimentSearch( "hello" ) ) )
                .isEmpty();

        Taxon taxon = new Taxon();
        fts.persist( taxon );
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setShortName( "hello" );
        ee.setTaxon( taxon );
        fts.persist( ee );
        fts.flushToIndexes();

        assertThat( hibernateSearchSource.searchExpressionExperiment( SearchSettings.expressionExperimentSearch( "hello" ) ) )
                .extracting( "resultObject" )
                .contains( ee );
    }
}