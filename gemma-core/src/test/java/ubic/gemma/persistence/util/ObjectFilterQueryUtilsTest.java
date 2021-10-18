package ubic.gemma.persistence.util;

import org.hibernate.Query;
import org.junit.Test;
import ubic.gemma.persistence.service.ObjectFilterException;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static ubic.gemma.persistence.util.ObjectFilterQueryUtils.addRestrictionParameters;
import static ubic.gemma.persistence.util.ObjectFilterQueryUtils.formRestrictionClause;

public class ObjectFilterQueryUtilsTest {

    @Test
    public void testComplexClause() throws ObjectFilterException {
        Filters filters = new Filters();
        filters.add( ObjectFilter.parseObjectFilter( "ee", "shortName", String.class, ObjectFilter.Operator.like, "GSE" ) );
        filters.add( ObjectFilter.parseObjectFilter( "ee", "id", Long.class, ObjectFilter.Operator.in, "(1,2,3,4)" ) );
        filters.add( ObjectFilter.parseObjectFilter( "ad", "taxonId", Long.class, ObjectFilter.Operator.eq, "9606" ) );
        filters.add( ObjectFilter.parseObjectFilter( "ee", "id", Long.class, ObjectFilter.Operator.in, "(1,2,3,4)" ),
                ObjectFilter.parseObjectFilter( "ee", "id", Long.class, ObjectFilter.Operator.in, "(5,6,7,8)" ) );
        assertThat( formRestrictionClause( filters ) )
                .isEqualTo( " and (ee.shortName like :eeshortName1) and (ee.id in (:eeid1)) and (ad.taxonId = :adtaxonId1) and (ee.id in (:eeid2) or ee.id in (:eeid3))" );

        Query mockedQuery = mock( Query.class );
        addRestrictionParameters( mockedQuery, filters );
        verify( mockedQuery ).setParameter( "eeshortName1", "%GSE%" );
        verify( mockedQuery ).setParameterList( "eeid1", Arrays.asList( 1L, 2L, 3L, 4L ) );
        verify( mockedQuery ).setParameter( "adtaxonId1", 9606L );
        verify( mockedQuery ).setParameterList( "eeid2", Arrays.asList( 1L, 2L, 3L, 4L ) );
        verify( mockedQuery ).setParameterList( "eeid3", Arrays.asList( 5L, 6L, 7L, 8L ) );
    }

    @Test
    public void testRestrictionClauseWithCollection() throws ObjectFilterException {
        Filters filters = Filters.singleFilter( ObjectFilter.parseObjectFilter( "ee", "id", Long.class, ObjectFilter.Operator.in, "(1,2,3,4)" ) );
        assertThat( formRestrictionClause( filters ) )
                .isEqualTo( " and (ee.id in (:eeid1))" );

        Query mockedQuery = mock( Query.class );
        addRestrictionParameters( mockedQuery, filters );
        verify( mockedQuery ).setParameterList( "eeid1", Arrays.asList( 1L, 2L, 3L, 4L ) );
    }

    @Test
    public void testRestrictionClauseWithNullRequiredValue() {
        Filters filters = Filters.singleFilter( new ObjectFilter( "ee", "id", Long.class, ObjectFilter.Operator.eq, null ) );
        assertThat( formRestrictionClause( filters ) )
                .isEqualTo( " and (ee.id is :eeid1)" );

        Query mockedQuery = mock( Query.class );
        addRestrictionParameters( mockedQuery, filters );
        verify( mockedQuery ).setParameter( "eeid1", null );
    }
}