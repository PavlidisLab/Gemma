package ubic.gemma.persistence.service;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import ubic.gemma.core.lang.Nullable;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.persistence.service.analysis.expression.diff.ExpressionAnalysisResultSetService;
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Test all possible filterable properties for filtering and sorting results.
 * @author poirigui
 */
public class FilteringVoEnabledServiceIntegrationTest extends BaseSpringContextTest {

    @Autowired
    private Map<String, FilteringVoEnabledService<?, ?>> filteringServices;

    @Autowired
    private MessageSource messageSource;

    @Test
    @Category(SlowTest.class)
    public void testFilteringByAllFilterableProperties() {
        for ( Map.Entry<String, FilteringVoEnabledService<?, ?>> entry : filteringServices.entrySet() ) {
            FilteringVoEnabledService<?, ?> filteringService = entry.getValue();
            for ( String property : filteringService.getFilterableProperties() ) {
                List<Object> allowedValues = filteringService.getFilterablePropertyAllowedValues( property );
                if ( allowedValues != null ) {
                    List<MessageSourceResolvable> allowedValuesLabels = filteringService.getFilterablePropertyResolvableAllowedValuesLabels( property );
                    assertThat( allowedValuesLabels )
                            .isNotNull()
                            .hasSameSizeAs( allowedValues )
                            .allSatisfy( label -> {
                                assertThatNoException()
                                        .isThrownBy( () -> messageSource.getMessage( label, Locale.getDefault() ) );
                            } );
                }
                Filter filter = filteringService.getFilter( property, Filter.Operator.eq, getStubForPropType( filteringService, property ) );
                log.info( String.format( "%s.loadValueObjects(%s, null, 0, 1)", entry.getKey(), filter ) );
                Slice<?> slice = filteringService.loadValueObjects( Filters.by( filter ), null, 0, 1 );
                long count = filteringService.count( Filters.by( filter ) );
                assertThat( slice.getTotalElements() ).isEqualTo( count );
            }
        }
    }

    @Test
    @Category(SlowTest.class)
    public void testSortingByAllFilterableProperties() {
        for ( Map.Entry<String, FilteringVoEnabledService<?, ?>> entry : filteringServices.entrySet() ) {
            FilteringVoEnabledService<?, ?> filteringService = entry.getValue();
            for ( String property : filteringService.getFilterableProperties() ) {
                if ( filteringService instanceof ExpressionAnalysisResultSetService && property.endsWith( ".size" ) ) {
                    log.warn( "Skipping collection size test with the Criteria API." );
                    continue;
                }
                Sort sort = filteringService.getSort( property, Sort.Direction.ASC );
                log.info( String.format( "%s.loadValueObjects(null, %s, 0, 1)", entry.getKey(), sort ) );
                filteringService.loadValueObjects( null, sort, 0, 1 );
            }
        }
    }

    @Nullable
    private static String getStubForPropType( FilteringService<?> filteringService, String prop ) {
        List<Object> allowedValues = filteringService.getFilterablePropertyAllowedValues( prop );
        if ( allowedValues != null ) {
            return allowedValues.stream().findAny().map( String::valueOf ).orElse( null );
        }
        Class<?> clazz = filteringService.getFilterablePropertyType( prop );
        if ( Byte.class.isAssignableFrom( clazz ) ) {
            return "0";
        } else if ( Integer.class.isAssignableFrom( clazz ) ) {
            return "0";
        } else if ( Long.class.isAssignableFrom( clazz ) ) {
            return "0";
        } else if ( Double.class.isAssignableFrom( clazz ) ) {
            return "0";
        } else if ( Float.class.isAssignableFrom( clazz ) ) {
            return "0";
        } else if ( Date.class.isAssignableFrom( clazz ) ) {
            return "2022-01-01";
        } else if ( URL.class.isAssignableFrom( clazz ) ) {
            return "https://example.com/";
        } else {
            return "";
        }
    }
}