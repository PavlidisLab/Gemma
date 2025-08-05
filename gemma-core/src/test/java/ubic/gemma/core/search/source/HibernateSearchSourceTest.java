package ubic.gemma.core.search.source;

import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.search.DefaultHighlighter;
import ubic.gemma.core.search.SearchContext;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.model.genome.Taxon;

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
    public void test() throws SearchException {
        assertThat( hibernateSearchSource.searchExpressionExperiment( SearchSettings.expressionExperimentSearch( "hello" ), new SearchContext( null, null ) ) )
                .isEmpty();
        assertThat( hibernateSearchSource.searchArrayDesign( SearchSettings.expressionExperimentSearch( "hello" ), new SearchContext( null, null ) ) )
                .isEmpty();
        assertThat( hibernateSearchSource.searchGene( SearchSettings.expressionExperimentSearch( "hello" ), new SearchContext( null, null ) ) )
                .isEmpty();
        assertThat( hibernateSearchSource.searchGeneSet( SearchSettings.expressionExperimentSearch( "hello" ), new SearchContext( null, null ) ) )
                .isEmpty();
        assertThat( hibernateSearchSource.searchBioSequence( SearchSettings.expressionExperimentSearch( "hello" ), new SearchContext( null, null ) ) )
                .isEmpty();
        assertThat( hibernateSearchSource.searchBibliographicReference( SearchSettings.expressionExperimentSearch( "hello" ), new SearchContext( null, null ) ) )
                .isEmpty();
        assertThat( hibernateSearchSource.searchExperimentSet( SearchSettings.expressionExperimentSearch( "hello" ), new SearchContext( null, null ) ) )
                .isEmpty();
        assertThat( hibernateSearchSource.searchCompositeSequence( SearchSettings.expressionExperimentSearch( "hello" ), new SearchContext( null, null ) ) )
                .isEmpty();
    }

    @Test
    public void testSearchExpressionExperiment() throws SearchException {
        FullTextSession fts = Search.getFullTextSession( sessionFactory.getCurrentSession() );

        assertThat( hibernateSearchSource.searchExpressionExperiment( SearchSettings.expressionExperimentSearch( "hello" ), new SearchContext( null, null ) ) )
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

        assertThat( hibernateSearchSource.searchExpressionExperiment(
                SearchSettings.expressionExperimentSearch( "hello" ),
                new SearchContext( new DefaultHighlighter(), null ) ) )
                .anySatisfy( result -> {
                    assertThat( result.getResultObject() ).isEqualTo( ee );
                    assertThat( result.getHighlights() )
                            .containsEntry( "description", "<b>hello</b> world!" )
                            .containsEntry( "primaryPublication.title", "The greatest <b>hello</b> world!" );
                } );
    }

    @Test
    public void testSearchExpressionExperimentByStatementObject() throws SearchException {
        FullTextSession fts = Search.getFullTextSession( sessionFactory.getCurrentSession() );
        Taxon taxon = new Taxon();
        fts.persist( taxon );
        ExpressionExperiment ee = new ExpressionExperiment();
        ExperimentalDesign ed = new ExperimentalDesign();
        ExperimentalFactor ef = new ExperimentalFactor();
        ef.setType( FactorType.CATEGORICAL );
        ef.setExperimentalDesign( ed );
        FactorValue fv = new FactorValue();
        fv.setExperimentalFactor( ef );
        Statement s = new Statement();
        s.setSubject( "BRCA1" );
        s.setObject( "Overexpression" );
        fv.getCharacteristics().add( s );
        ef.getFactorValues().add( fv );
        ed.getExperimentalFactors().add( ef );
        ee.setExperimentalDesign( ed );
        fts.persist( ee );
        fts.flushToIndexes();
        assertThat( hibernateSearchSource.searchExpressionExperiment( SearchSettings.builder()
                .query( "BRCA1" )
                .build(), new SearchContext( null, null ) ) )
                .anySatisfy( r -> {
                    assertThat( r.getResultObject() ).isEqualTo( ee );
                } );
        assertThat( hibernateSearchSource.searchExpressionExperiment( SearchSettings.builder()
                .query( "Overexpression" )
                .build(), new SearchContext( null, null ) ) )
                .anySatisfy( r -> {
                    assertThat( r.getResultObject() ).isEqualTo( ee );
                } );
        assertThat( hibernateSearchSource.searchExpressionExperiment( SearchSettings.builder()
                .query( "BRCA1 Overexpression" )
                .build(), new SearchContext( null, null ) ) )
                .anySatisfy( r -> {
                    assertThat( r.getResultObject() ).isEqualTo( ee );
                } );
    }

    @Test
    public void testSearchWithInvalidQuerySyntax() throws SearchException {
        hibernateSearchSource.searchExpressionExperiment( SearchSettings.builder().query( "\"" ).build(), new SearchContext( null, null ) );
    }
}