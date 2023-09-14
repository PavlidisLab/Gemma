package ubic.gemma.core.search.source;

import org.apache.lucene.search.highlight.Formatter;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.search.Highlighter;
import ubic.gemma.core.search.lucene.SimpleHTMLFormatter;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.model.common.description.BibliographicReference;
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
    public void test() throws HibernateSearchException {
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
    public void testSearchExpressionExperiment() throws HibernateSearchException {
        FullTextSession fts = Search.getFullTextSession( sessionFactory.getCurrentSession() );

        assertThat( hibernateSearchSource.searchExpressionExperiment( SearchSettings.expressionExperimentSearch( "hello" ) ) )
                .isEmpty();

        Taxon taxon = new Taxon();
        fts.persist( taxon );
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setShortName( "hello" );
        ee.setDescription( "hello world!" );
        ee.setTaxon( taxon );
        BibliographicReference bibref = new BibliographicReference();
        fts.persist( bibref );
        bibref.setTitle( "The greatest hello world!" );
        ee.setPrimaryPublication( bibref );
        fts.persist( ee );
        fts.flushToIndexes();

        assertThat( hibernateSearchSource.searchExpressionExperiment( SearchSettings.expressionExperimentSearch( "hello" )
                .withHighlighter( new TestHighlighter() ) ) )
                .anySatisfy( result -> {
                    assertThat( result.getResultObject() ).isEqualTo( ee );
                    assertThat( result.getHighlights() )
                            .containsEntry( "description", "<b>hello</b> world!" )
                            .containsEntry( "primaryPublication.title", "The greatest <b>hello</b> world!" );
                } );
    }

    private static final class TestHighlighter implements Highlighter {

        @Override
        public Formatter getLuceneFormatter() {
            return new SimpleHTMLFormatter();
        }
    }
}