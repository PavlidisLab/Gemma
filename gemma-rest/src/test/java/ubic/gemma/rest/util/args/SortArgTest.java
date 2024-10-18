package ubic.gemma.rest.util.args;

import org.junit.Test;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.service.FilteringService;
import ubic.gemma.persistence.util.Sort;
import ubic.gemma.rest.util.MalformedArgException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SortArgTest {

    @Test
    public void testValueOf() {
        assertThat( SortArg.valueOf( "+id" ).getValue() )
                .hasFieldOrPropertyWithValue( "orderBy", "id" )
                .hasFieldOrPropertyWithValue( "direction", SortArg.Sort.Direction.ASC );
    }

    @Test
    public void testGetSort() {
        FilteringService<Identifiable> filteringService = mock( FilteringService.class );
        when( filteringService.getSort( "id", Sort.Direction.ASC, Sort.NullMode.LAST ) ).thenReturn( Sort.by( "entity", "id", Sort.Direction.ASC, Sort.NullMode.LAST, "id" ) );
        assertThat( SortArg.valueOf( "+id" ).getSort( filteringService ) )
                .hasFieldOrPropertyWithValue( "objectAlias", "entity" )
                .hasFieldOrPropertyWithValue( "propertyName", "id" )
                .hasFieldOrPropertyWithValue( "direction", Sort.Direction.ASC );
    }

    @Test(expected = MalformedArgException.class)
    public void testGetSortWhenFieldDoesNotExistThenRaiseMalformedArgumentException() {
        FilteringService<Identifiable> filteringService = mock( FilteringService.class );
        when( filteringService.getSort( "wut", Sort.Direction.ASC, Sort.NullMode.LAST ) ).thenThrow( IllegalArgumentException.class );
        SortArg.valueOf( "+wut" ).getSort( filteringService );
    }

    @Test
    public void testEmptySortArg() {
        assertThatThrownBy( () -> SortArg.valueOf( "" ) )
                .isInstanceOf( MalformedArgException.class );
    }

    @Test
    public void testSortArgWithoutField() {
        assertThatThrownBy( () -> SortArg.valueOf( "+" ) )
                .isInstanceOf( MalformedArgException.class );
    }

    @Test
    public void testDirectionParsing() {
        assertThat( SortArg.valueOf( "+id" ).getValue() )
                .hasFieldOrPropertyWithValue( "orderBy", "id" )
                .hasFieldOrPropertyWithValue( "direction", SortArg.Sort.Direction.ASC );

        assertThat( SortArg.valueOf( "-id" ).getValue() )
                .hasFieldOrPropertyWithValue( "orderBy", "id" )
                .hasFieldOrPropertyWithValue( "direction", SortArg.Sort.Direction.DESC );

        assertThat( SortArg.valueOf( "id" ).getValue() )
                .hasFieldOrPropertyWithValue( "orderBy", "id" )
                .hasFieldOrPropertyWithValue( "direction", null );
    }
}