package ubic.gemma.persistence.util;

import org.hibernate.query.Query;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static ubic.gemma.persistence.util.FilterQueryUtils.*;

public class FilterQueryUtilsTest {

    @Test
    public void testLikeRestrictionClause() {
        Filters filters = Filters.empty()
                .and( "ee", "shortName", String.class, Filter.Operator.like, "%_" );
        Query mockedQuery = mock( Query.class );
        addRestrictionParameters( mockedQuery, filters );
        verify( mockedQuery ).setParameter( "ee_shortName1", "~%~_%" );
    }

    /**
     * A value containing an underscore must still match its own literal text.
     * <p>
     * Underscore is a single-character SQL wildcard, so it has to be escaped — but escaping it is
     * only half the job. Without a matching {@code escape} clause the pattern relies on the
     * backslash default, which MySQL disables under {@code NO_BACKSLASH_ESCAPES}; the escaped
     * wildcard then matches NOTHING. Measured against production before the fix:
     * {@code name like AFFX-BioB-3} returned 1 row and {@code name like AFFX-BioB-3_at} returned 0,
     * so every Affymetrix probe name — nearly all of which carry an underscore — was unsearchable.
     */
    @Test
    public void testLikeWithUnderscoreEmitsAMatchingEscapeClause() {
        Filters filters = Filters.empty()
                .and( "cs", "name", String.class, Filter.Operator.like, "1007_s_at" );

        assertThat( formRestrictionClause( filters ) )
                .as( "an escape clause must accompany the escaped pattern" )
                .contains( "escape '~'" );

        Query mockedQuery = mock( Query.class );
        addRestrictionParameters( mockedQuery, filters );
        // Each underscore escaped with the same character the clause declares, trailing % for the
        // prefix match the operator implies.
        verify( mockedQuery ).setParameter( "cs_name1", "1007~_s~_at%" );
    }

    /**
     * The escape character must itself be escaped when it occurs in user data, or a value containing
     * a tilde would silently change meaning.
     */
    @Test
    public void testLikeEscapesTheEscapeCharacterItself() {
        Filters filters = Filters.empty()
                .and( "cs", "name", String.class, Filter.Operator.like, "a~b_c" );
        Query mockedQuery = mock( Query.class );
        addRestrictionParameters( mockedQuery, filters );
        verify( mockedQuery ).setParameter( "cs_name1", "a~~b~_c%" );
    }

    @Test
    public void testComplexClause() {
        Filters filters = Filters.empty()
                .and( Filter.parse( "ee", "shortName", String.class, Filter.Operator.like, "GSE" ) )
                .and( Filter.parse( "ee", "id", Long.class, Filter.Operator.in, Arrays.asList( "1", "2", "3", "4" ) ) )
                .and( Filter.parse( "ad", "taxonId", Long.class, Filter.Operator.eq, "9606" ) )
                .and()
                .or( Filter.parse( "ee", "id", Long.class, Filter.Operator.in, Arrays.asList( "1", "2", "3", "4" ) ) )
                .or( Filter.parse( "ee", "id", Long.class, Filter.Operator.in, Arrays.asList( "5", "6", "7", "8" ) ) )
                .build();
        assertThat( formRestrictionClause( filters ) )
                .isEqualTo( " and (ee.shortName like :ee_shortName1 escape '~') and (ee.id in (:ee_id2)) and (ad.taxonId = :ad_taxonId3) and (ee.id in (:ee_id4) or ee.id in (:ee_id5))" );

        Query mockedQuery = mock( Query.class );
        addRestrictionParameters( mockedQuery, filters );
        verify( mockedQuery ).setParameter( "ee_shortName1", "GSE%" );
        verify( mockedQuery ).setParameterList( "ee_id2", Arrays.asList( 1L, 2L, 3L, 4L ) );
        verify( mockedQuery ).setParameter( "ad_taxonId3", 9606L );
        verify( mockedQuery ).setParameterList( "ee_id4", Arrays.asList( 1L, 2L, 3L, 4L ) );
        verify( mockedQuery ).setParameterList( "ee_id5", Arrays.asList( 5L, 6L, 7L, 8L ) );
    }

    @Test
    public void testRestrictionClauseWithCollection() {
        Filters filters = Filters.by( Filter.parse( "ee", "id", Long.class, Filter.Operator.in, Arrays.asList( "1", "2", "3", "4" ) ) );
        assertThat( formRestrictionClause( filters ) )
                .isEqualTo( " and (ee.id in (:ee_id1))" );

        Query mockedQuery = mock( Query.class );
        addRestrictionParameters( mockedQuery, filters );
        verify( mockedQuery ).setParameterList( "ee_id1", Arrays.asList( 1L, 2L, 3L, 4L ) );
    }

    @Test
    public void testRestrictionClauseWithNullRequiredValue() {
        Filters filters = Filters.empty()
                .and()
                .or( "ee", "id", Long.class, Filter.Operator.eq, ( Long ) null )
                .or( "ee", "id", Long.class, Filter.Operator.notEq, ( Long ) null )
                .build();
        assertThat( formRestrictionClause( filters ) )
                .isEqualTo( " and (ee.id is :ee_id1 or ee.id is not :ee_id2)" );

        Query mockedQuery = mock( Query.class );
        addRestrictionParameters( mockedQuery, filters );
        verify( mockedQuery ).setParameter( "ee_id1", null );
        verify( mockedQuery ).setParameter( "ee_id2", null );
    }

    @Test
    public void testRestrictionClauseWithNullObjectAlias() {
        Filters filters = Filters.by( null, "id", Long.class, Filter.Operator.eq, 12L );
        assertThat( formRestrictionClause( filters ) )
                .isEqualTo( " and (id = :id1)" );

        Query mockedQuery = mock( Query.class );
        addRestrictionParameters( mockedQuery, filters );
        verify( mockedQuery ).setParameter( "id1", 12L );
    }

    @Test
    public void testFormRestrictionAndGroupByAndOrderByClauses() {
        Filters filters = Filters.by( null, "id", Long.class, Filter.Operator.eq, 12L );
        Sort sort = Sort.by( "ee", "shortName", null, Sort.NullMode.DEFAULT );
        String queryString = "";
        queryString += FilterQueryUtils.formRestrictionClause( filters );
        queryString += FilterQueryUtils.formOrderByClause( sort );
        assertThat( queryString )
                .isEqualTo( " and (id = :id1) order by ee.shortName" );
    }

    @Test
    public void testFormRestriction() {
        assertThat( formRestrictionClause( Filters.by( "ee", "bioAssays.size", Integer.class, Filter.Operator.greaterThan, 4 ) ) )
                .isEqualTo( " and (size(ee.bioAssays) > :ee_bioAssays_size1)" );
    }

    @Test
    public void testSortByCollectionSize() {
        assertThat( formOrderByClause( Sort.by( "ee", "bioAssays.size", null, Sort.NullMode.DEFAULT ) ) )
                .isEqualTo( " order by size(ee.bioAssays)" );
    }

    @Test
    public void testSubquery() {
        assertThat( formRestrictionClause( Filters.by( "ee", "id", Integer.class, Filter.Operator.inSubquery,
                new Subquery( "ExpressionExperiment", "id", Collections.emptyList(),
                        Filter.by( null, "id", Long.class, Filter.Operator.eq, 1L ) ) ) ) )
                .isEqualTo( " and (ee.id in (select e.id from ExpressionExperiment e where e.id = :id1))" );
    }

    @Test
    public void testNestedSubquery() {
        assertThat( formRestrictionClause( Filters.by( "ee", "id", Long.class, Filter.Operator.inSubquery,
                new Subquery( "ExpressionExperiment", "id", Collections.emptyList(),
                        Filter.by( null, "id", Long.class, Filter.Operator.inSubquery,
                                new Subquery( "ExpressionExperiment", "id", Collections.emptyList(),
                                        Filter.by( null, "id", Long.class, Filter.Operator.in,
                                                Arrays.asList( 1L, 2L, 3L ) ) ),
                                "id" ) ) ) ) )
                .isEqualTo( " and (ee.id in (select e.id from ExpressionExperiment e where e.id in (select e.id from ExpressionExperiment e where e.id in (:id1))))" );
    }

    @Test
    public void testSubqueryWithEmptyPrefix() {
        assertThat( formRestrictionClause( Filters.by( "ee", "id", Integer.class, Filter.Operator.inSubquery,
                new Subquery( "ExpressionExperiment", "id", Collections.singletonList( new Subquery.Alias( null, "", "ee" ) ),
                        Filter.by( "ee", "id", Long.class, Filter.Operator.eq, 1L ) ) ) ) )
                .isEqualTo( " and (ee.id in (select ee.id from ExpressionExperiment ee where ee.id = :ee_id1))" );
    }
}