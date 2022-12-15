package ubic.gemma.persistence.util;

import org.hibernate.Query;
import org.junit.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static ubic.gemma.persistence.util.FilterQueryUtils.*;

public class FilterQueryUtilsTest {

    @Test
    public void testFormParamName() {
        assertThat( formParamName( "ee", "id" ) ).isEqualTo( "ee_id" );
        assertThat( formParamName( "ee", "curationDetails.troubled" ) ).isEqualTo( "ee_curationDetails_troubled" );
        assertThat( formParamName( null, "id" ) ).isEqualTo( "id" );
    }

    @Test
    public void testComplexClause() {
        Filters filters = Filters.empty()
                .and( Filter.parse( "ee", "shortName", String.class, Filter.Operator.like, "GSE" ) )
                .and( Filter.parse( "ee", "id", Long.class, Filter.Operator.in, "(1,2,3,4)" ) )
                .and( Filter.parse( "ad", "taxonId", Long.class, Filter.Operator.eq, "9606" ) )
                .and()
                .or( Filter.parse( "ee", "id", Long.class, Filter.Operator.in, "(1,2,3,4)" ) )
                .or( Filter.parse( "ee", "id", Long.class, Filter.Operator.in, "(5,6,7,8)" ) )
                .build();
        assertThat( formRestrictionClause( filters ) )
                .isEqualTo( " and (ee.shortName like :ee_shortName1) and (ee.id in (:ee_id2)) and (ad.taxonId = :ad_taxonId3) and (ee.id in (:ee_id4) or ee.id in (:ee_id5))" );

        Query mockedQuery = mock( Query.class );
        addRestrictionParameters( mockedQuery, filters );
        verify( mockedQuery ).setParameter( "ee_shortName1", "%GSE%" );
        verify( mockedQuery ).setParameterList( "ee_id2", Arrays.asList( 1L, 2L, 3L, 4L ) );
        verify( mockedQuery ).setParameter( "ad_taxonId3", 9606L );
        verify( mockedQuery ).setParameterList( "ee_id4", Arrays.asList( 1L, 2L, 3L, 4L ) );
        verify( mockedQuery ).setParameterList( "ee_id5", Arrays.asList( 5L, 6L, 7L, 8L ) );
    }

    @Test
    public void testRestrictionClauseWithCollection() {
        Filters filters = Filters.by( Filter.parse( "ee", "id", Long.class, Filter.Operator.in, "(1,2,3,4)" ) );
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
                .or( "ee", "id", Long.class, Filter.Operator.eq, null )
                .or( "ee", "id", Long.class, Filter.Operator.notEq, null )
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
        assertThat( formRestrictionAndGroupByAndOrderByClauses( filters, "ee", Sort.by( "ee", "shortName" ) ) )
                .isEqualTo( " and (id = :id1) group by ee order by ee.shortName" );
    }

    @Test
    public void testFormRestriction() {
        assertThat( formRestrictionClause( Filters.by( "ee", "bioAssays.size", Integer.class, Filter.Operator.greaterThan, 4 ) ) )
                .isEqualTo( " and (size(ee.bioAssays) > :ee_bioAssays_size1)" );
    }

    @Test
    public void testSortByCollectionSize() {
        assertThat( formOrderByClause( Sort.by( "ee", "bioAssays.size" ) ) )
                .isEqualTo( " order by size(ee.bioAssays)" );
    }
}