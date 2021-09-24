package ubic.gemma.web.services.rest.util.args;

import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import ubic.gemma.persistence.service.FilteringVoEnabledService;
import ubic.gemma.persistence.service.ObjectFilterException;
import ubic.gemma.persistence.util.ObjectFilter;
import ubic.gemma.web.services.rest.util.MalformedArgException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class FilterArgTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private FilteringVoEnabledService mockVoService;

    @Test
    public void testEmptyFilter() {
        assertThat( FilterArg.valueOf( null ) ).isSameAs( FilterArg.EMPTY_FILTER );
    }

    @Before
    @SneakyThrows(ObjectFilterException.class)
    public void setUp() {
        when( mockVoService.getObjectFilter( any(), any(), any( String.class ) ) )
                .thenAnswer( arg -> new ObjectFilter( "alias", arg.getArgument( 0, String.class ),
                        String.class,
                        arg.getArgument( 1, ObjectFilter.Operator.class ),
                        arg.getArgument( 2, String.class ) ) );
    }

    @Test
    public void testSimpleEquality() {
        List<ObjectFilter[]> filters = FilterArg.valueOf( "a = b" ).getObjectFilters( mockVoService );
        assertThat( filters.get( 0 )[0] )
                .hasFieldOrPropertyWithValue( "propertyName", "a" )
                .hasFieldOrPropertyWithValue( "operator", ObjectFilter.Operator.is )
                .hasFieldOrPropertyWithValue( "requiredValue", "b" );
    }

    @Test
    public void testInvalidFilter() {
        assertThatThrownBy( () -> FilterArg.valueOf( "a =b" ).getObjectFilters( mockVoService ) )
                .isInstanceOf( MalformedArgException.class );
    }

    @Test
    public void testConjunction() {
        List<ObjectFilter[]> filters;
        filters = FilterArg.valueOf( "a = b and c = d" ).getObjectFilters( mockVoService );
        assertThat( filters ).hasSize( 2 );
        assertThat( filters.get( 0 )[0] )
                .hasFieldOrPropertyWithValue( "propertyName", "a" )
                .hasFieldOrPropertyWithValue( "requiredValue", "b" );
        assertThat( filters.get( 1 )[0] )
                .hasFieldOrPropertyWithValue( "propertyName", "c" )
                .hasFieldOrPropertyWithValue( "requiredValue", "d" );
    }

    @Test
    public void testDisjunction() {
        List<ObjectFilter[]> filters;

        filters = FilterArg.valueOf( "a = b, c = d" ).getObjectFilters( mockVoService );
        assertThat( filters.get( 0 ) ).hasSize( 2 );
        assertThat( filters.get( 0 )[0] )
                .hasFieldOrPropertyWithValue( "propertyName", "a" )
                .hasFieldOrPropertyWithValue( "requiredValue", "b" );
        assertThat( filters.get( 0 )[1] )
                .hasFieldOrPropertyWithValue( "propertyName", "c" )
                .hasFieldOrPropertyWithValue( "requiredValue", "d" );

        FilterArg.valueOf( "a = b or c = d" ).getObjectFilters( mockVoService );
        assertThat( filters.get( 0 ) ).hasSize( 2 );
    }
}