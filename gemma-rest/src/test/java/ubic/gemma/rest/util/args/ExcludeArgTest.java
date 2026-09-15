package ubic.gemma.rest.util.args;

import jakarta.ws.rs.BadRequestException;
import org.junit.jupiter.api.Test;
import ubic.gemma.rest.util.MalformedArgException;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ExcludeArgTest {

    @Test
    public void testValueOfSingle() {
        ExcludeArg<?> arg = ExcludeArg.valueOf( "name" );
        assertThat( arg.getValue() ).containsExactly( "name" );
    }

    @Test
    public void testValueOfMultiple() {
        ExcludeArg<?> arg = ExcludeArg.valueOf( "name,description,id" );
        assertThat( arg.getValue() ).containsExactly( "name", "description", "id" );
    }

    @Test
    public void testValueOfEmptyRaises() {
        assertThatThrownBy( () -> ExcludeArg.valueOf( "" ) )
                .isInstanceOf( MalformedArgException.class );
    }

    @Test
    public void testGetValueWithAllowedSetReturnsSubset() {
        ExcludeArg<?> arg = ExcludeArg.valueOf( "name,description" );
        Set<String> allowed = new HashSet<>();
        allowed.add( "name" );
        allowed.add( "description" );
        allowed.add( "id" );
        assertThat( arg.getValue( allowed ) ).containsExactlyInAnyOrder( "name", "description" );
    }

    @Test
    public void testGetValueWithDisallowedFieldRaises() {
        ExcludeArg<?> arg = ExcludeArg.valueOf( "name,bogus" );
        Set<String> allowed = new HashSet<>();
        allowed.add( "name" );
        allowed.add( "description" );
        assertThatThrownBy( () -> arg.getValue( allowed ) )
                .isInstanceOf( BadRequestException.class );
    }
}
