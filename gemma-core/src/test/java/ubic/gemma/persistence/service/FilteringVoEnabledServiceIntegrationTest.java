package ubic.gemma.persistence.service;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.persistence.service.analysis.expression.diff.ExpressionAnalysisResultSetService;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.ObjectFilter;
import ubic.gemma.persistence.util.Sort;

import java.util.Date;
import java.util.List;

/**
 * Test all possible filterable properties for filtering and sorting results.
 * @author poirigui
 */
public class FilteringVoEnabledServiceIntegrationTest extends BaseSpringContextTest {

    @Autowired
    private List<FilteringVoEnabledService<?, ?>> filteringServices;


    @Test
    public void testFilteringByAllFilterableProperties() {
        for ( FilteringVoEnabledService<?, ?> filteringService : filteringServices ) {
            for ( String property : filteringService.getFilterableProperties() ) {
                ObjectFilter filter = filteringService.getObjectFilter( property, ObjectFilter.Operator.eq, getStubForPropType( filteringService.getFilterablePropertyType( property ) ) );
                filteringService.loadValueObjectsPreFilter( Filters.singleFilter( filter ), null, 0, 1 );
            }
        }
    }

    @Test
    public void testSortingByAllFilterableProperties() {
        for ( FilteringVoEnabledService<?, ?> filteringService : filteringServices ) {
            for ( String property : filteringService.getFilterableProperties() ) {
                if ( filteringService instanceof ExpressionAnalysisResultSetService && property.endsWith( ".size" ) ) {
                    log.warn( "Skipping collection size test with the Criteria API." );
                    continue;
                }
                Sort sort = filteringService.getSort( property, Sort.Direction.ASC );
                filteringService.loadValueObjectsPreFilter( null, sort, 0, 1 );
            }
        }
    }

    private static String getStubForPropType( Class<?> clazz ) {
        if ( Integer.class.isAssignableFrom( clazz ) ) {
            return "0";
        } else if ( Long.class.isAssignableFrom( clazz ) ) {
            return "0";
        } else if ( Double.class.isAssignableFrom( clazz ) ) {
            return "0";
        } else if ( Float.class.isAssignableFrom( clazz ) ) {
            return "0";
        } else if ( Date.class.isAssignableFrom( clazz ) ) {
            return "2022-01-01";
        } else {
            return "";
        }
    }
}