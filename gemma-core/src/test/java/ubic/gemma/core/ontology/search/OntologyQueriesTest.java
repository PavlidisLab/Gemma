package ubic.gemma.core.ontology.search;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the retrieval constraint that stops one shared token from being a match.
 *
 * <p>Without it, {@code Gorlin Goltz Syndrome} returned {@code down syndrome} as its first MONDO
 * hit and two different queries containing "syndrome" returned the same terms.</p>
 */
class OntologyQueriesTest {

    private static BooleanQuery shoulds( int n ) {
        BooleanQuery.Builder b = new BooleanQuery.Builder();
        for ( int i = 0; i < n; i++ ) {
            b.add( new TermQuery( new Term( "text", "t" + i ) ), BooleanClause.Occur.SHOULD );
        }
        return b.build();
    }

    @Test
    void aSingleTermQueryIsLeftAlone() {
        // Nothing to constrain: "Myelopathy" must still match documents containing it.
        Query q = OntologyQueries.withMinimumShouldMatch( shoulds( 1 ), 0.67 );
        assertThat( ( ( BooleanQuery ) q ).getMinimumNumberShouldMatch() ).isZero();
        assertThat( OntologyQueries.requiredClauses( 1, 0.67 ) ).isEqualTo( 1 );
    }

    @Test
    void multiTermQueriesRequireMoreThanOneToken() {
        // 3 terms → 2 required, which is what excludes `down syndrome` from `Gorlin Goltz Syndrome`.
        assertThat( OntologyQueries.requiredClauses( 3, 0.67 ) ).isEqualTo( 2 );
        assertThat( OntologyQueries.requiredClauses( 4, 0.67 ) ).isEqualTo( 2 );
        assertThat( OntologyQueries.requiredClauses( 6, 0.67 ) ).isEqualTo( 4 );

        Query q = OntologyQueries.withMinimumShouldMatch( shoulds( 3 ), 0.67 );
        assertThat( ( ( BooleanQuery ) q ).getMinimumNumberShouldMatch() ).isEqualTo( 2 );
    }

    /**
     * 67% of two rounds down to one, which would leave the commonest shape — a two-word disease
     * name — exactly as unconstrained as before. The floor of two is the whole point for those.
     */
    @Test
    void twoTermQueriesAreNotLeftUnconstrained() {
        assertThat( OntologyQueries.requiredClauses( 2, 0.67 ) ).isEqualTo( 2 );
    }

    @Test
    void neverRequiresMoreClausesThanExist() {
        assertThat( OntologyQueries.requiredClauses( 3, 1.0 ) ).isEqualTo( 3 );
        assertThat( OntologyQueries.requiredClauses( 2, 5.0 ) ).isEqualTo( 2 );
        assertThat( OntologyQueries.requiredClauses( 4, -1.0 ) ).isEqualTo( 2 );
    }

    /**
     * A query carrying explicit operators is the caller stating their own requirements; layering a
     * minimum on top would silently change what they asked for.
     */
    @Test
    void explicitOperatorsAreRespected() {
        BooleanQuery q = new BooleanQuery.Builder()
                .add( new TermQuery( new Term( "text", "a" ) ), BooleanClause.Occur.MUST )
                .add( new TermQuery( new Term( "text", "b" ) ), BooleanClause.Occur.SHOULD )
                .build();

        Query out = OntologyQueries.withMinimumShouldMatch( q, 0.67 );

        assertThat( out ).isSameAs( q );
        assertThat( ( ( BooleanQuery ) out ).getMinimumNumberShouldMatch() ).isZero();
    }

    @Test
    void anAlreadyConstrainedQueryIsNotSecondGuessed() {
        BooleanQuery q = new BooleanQuery.Builder()
                .add( new TermQuery( new Term( "text", "a" ) ), BooleanClause.Occur.SHOULD )
                .add( new TermQuery( new Term( "text", "b" ) ), BooleanClause.Occur.SHOULD )
                .add( new TermQuery( new Term( "text", "c" ) ), BooleanClause.Occur.SHOULD )
                .setMinimumNumberShouldMatch( 1 )
                .build();

        assertThat( OntologyQueries.withMinimumShouldMatch( q, 0.67 ) ).isSameAs( q );
    }

    @Test
    void nonBooleanQueriesPassThrough() {
        Query q = new TermQuery( new Term( "text", "hippocampus" ) );
        assertThat( OntologyQueries.withMinimumShouldMatch( q, 0.67 ) ).isSameAs( q );
    }
}
