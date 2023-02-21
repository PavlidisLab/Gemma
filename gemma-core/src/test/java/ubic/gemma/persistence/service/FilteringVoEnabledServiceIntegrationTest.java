package ubic.gemma.persistence.service;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.persistence.service.analysis.expression.diff.ExpressionAnalysisResultSetService;
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test all possible filterable properties for filtering and sorting results.
 * @author poirigui
 */
public class FilteringVoEnabledServiceIntegrationTest extends BaseSpringContextTest {

    @Autowired
    private List<FilteringVoEnabledService<?, ?>> filteringServices;


    @Test
    @Category(SlowTest.class)
    public void testFilteringByAllFilterableProperties() {
        for ( FilteringVoEnabledService<?, ?> filteringService : filteringServices ) {
            for ( String property : filteringService.getFilterableProperties() ) {
                Filter filter = filteringService.getFilter( property, Filter.Operator.eq, getStubForPropType( filteringService, property ) );
                log.info( String.format( "Testing %s with %s", filteringService, filter ) );
                Slice<?> slice = filteringService.loadValueObjects( Filters.by( filter ), null, 0, 1 );
                long count = filteringService.count( Filters.by( filter ) );
                assertThat( slice.getTotalElements() ).isEqualTo( count );
            }
        }
    }

    @Test
    @Category(SlowTest.class)
    public void testSortingByAllFilterableProperties() {
        for ( FilteringVoEnabledService<?, ?> filteringService : filteringServices ) {
            for ( String property : filteringService.getFilterableProperties() ) {
                if ( filteringService instanceof ExpressionAnalysisResultSetService && property.endsWith( ".size" ) ) {
                    log.warn( "Skipping collection size test with the Criteria API." );
                    continue;
                }
                Sort sort = filteringService.getSort( property, Sort.Direction.ASC );
                filteringService.loadValueObjects( null, sort, 0, 1 );
            }
        }
    }

    private static String getStubForPropType( FilteringService<?> filteringService, String prop ) {
        List<Object> availableValues = filteringService.getFilterablePropertyAllowedValues( prop );
        if ( availableValues != null ) {
            return availableValues.stream().findAny().map( String::valueOf ).orElse( null );
        }
        Class<?> clazz = filteringService.getFilterablePropertyType( prop );
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