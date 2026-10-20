package ubic.gemma.persistence.util;

import org.hibernate.query.Query;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static ubic.gemma.persistence.util.FilterQueryUtils.addRestrictionParameters;
import static ubic.gemma.persistence.util.FilterQueryUtils.formRestrictionClause;

/**
 * A {@link Subquery} may carry several conjoined filters, so that they bind to the SAME element of a
 * to-many relation.
 * <p>
 * The distinction this exists for: two separate subqueries ask "has a characteristic valued X" AND
 * "has a characteristic categorised Y", which is satisfied by a dataset where those are two different
 * characteristics. One subquery over two filters asks for a single characteristic that is both — the
 * only form that can express "this gene URI, as a genotype".
 */
public class SubqueryConjunctionTest {

    private static final String GENE_URI = "http://purl.org/commons/record/ncbi_gene/7124";
    private static final String GENOTYPE_URI = "http://www.ebi.ac.uk/efo/EFO_0000513";

    private static Subquery conjoined() {
        return new Subquery( "ExpressionExperiment", "id",
                SubqueryUtils.guessAliases( "allCharacteristics.", "ac" ),
                Arrays.asList(
                        Filter.parse( "ac", "valueUri", String.class, Filter.Operator.eq, GENE_URI ),
                        Filter.parse( "ac", "categoryUri", String.class, Filter.Operator.eq, GENOTYPE_URI ) ) );
    }

    /** Both conditions land in ONE subquery, joined by `and`, against one alias. */
    @Test
    public void bothConditionsAreRenderedInsideASingleSubquery() {
        Filters filters = Filters.by( Filter.by( "ee", "id", Long.class,
                Filter.Operator.inSubquery, conjoined() ) );

        String clause = formRestrictionClause( filters );

        assertThat( clause )
                .contains( "ac.valueUri = :ac_valueUri1" )
                .contains( "ac.categoryUri = :ac_categoryUri1_1" )
                .containsOnlyOnce( "select" );
        // one `where`, with both conditions conjoined under it
        assertThat( clause.split( " where ", -1 ) ).hasSize( 2 );
        assertThat( clause.substring( clause.indexOf( " where " ) ) )
                .startsWith( " where ac.valueUri = :ac_valueUri1 and ac.categoryUri = :ac_categoryUri1_1" );
    }

    /**
     * Rendering and binding derive parameter names independently; if they drift, the query builds and
     * then dies at bind time. Conjunct 0 must keep the name it had when a subquery held one filter.
     */
    @Test
    public void everyRenderedParameterIsBound() {
        Filters filters = Filters.by( Filter.by( "ee", "id", Long.class,
                Filter.Operator.inSubquery, conjoined() ) );

        Query mockedQuery = mock( Query.class );
        addRestrictionParameters( mockedQuery, filters );

        verify( mockedQuery ).setParameter( "ac_valueUri1", GENE_URI );
        verify( mockedQuery ).setParameter( "ac_categoryUri1_1", GENOTYPE_URI );
    }

    /** A single-filter subquery must render and bind exactly as it did before conjunctions existed. */
    @Test
    public void singleFilterSubqueryIsUnchanged() {
        Subquery single = new Subquery( "ExpressionExperiment", "id",
                SubqueryUtils.guessAliases( "allCharacteristics.", "ac" ),
                Filter.parse( "ac", "valueUri", String.class, Filter.Operator.eq, GENE_URI ) );
        Filters filters = Filters.by( Filter.by( "ee", "id", Long.class, Filter.Operator.inSubquery, single ) );

        assertThat( formRestrictionClause( filters ) )
                .contains( " where ac.valueUri = :ac_valueUri1" )
                .doesNotContain( "_1" );

        Query mockedQuery = mock( Query.class );
        addRestrictionParameters( mockedQuery, filters );
        verify( mockedQuery ).setParameter( "ac_valueUri1", GENE_URI );
    }

    @Test
    public void toStringRendersTheConjunction() {
        assertThat( conjoined().toString() )
                .contains( "ac.valueUri = " + GENE_URI + " and ac.categoryUri = " + GENOTYPE_URI );
    }

    /** getFilter() is only meaningful for a single filter; a conjunction must be read via getFilters(). */
    @Test
    public void getFilterRefusesToSpeakForAConjunction() {
        assertThat( conjoined().getFilters() ).hasSize( 2 );
        assertThatThrownBy( () -> conjoined().getFilter() )
                .isInstanceOf( IllegalStateException.class );
    }

    @Test
    public void aSubqueryMustCarryAtLeastOneFilter() {
        assertThatThrownBy( () -> new Subquery( "ExpressionExperiment", "id",
                Collections.emptyList(), Collections.emptyList() ) )
                .isInstanceOf( IllegalArgumentException.class );
    }
}
