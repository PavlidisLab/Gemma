package ubic.gemma.web.services.rest.util.args;

import org.junit.Test;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.util.Sort;
import ubic.gemma.web.services.rest.util.MalformedArgException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SortArgTest {

    @Test
    public void testValueOf() {
        SortArg.valueOf( "+id" ).getValueForClass( ExpressionExperiment.class );
    }

    @Test
    public void testEmptySortArg() {
        assertThatThrownBy( () -> SortArg.valueOf( "" ).getValue() )
                .isInstanceOf( MalformedArgException.class );
    }

    @Test
    public void testSortArgWithoutField() {
        assertThatThrownBy( () -> SortArg.valueOf( "+" ).getValue() )
                .isInstanceOf( MalformedArgException.class );
    }

    @Test
    public void testDirectionParsing() {
        assertThat( SortArg.valueOf( "+id" ).getValue() )
                .hasFieldOrPropertyWithValue( "orderBy", "id" )
                .hasFieldOrPropertyWithValue( "direction", Sort.Direction.ASC );

        assertThat( SortArg.valueOf( "-id" ).getValue() )
                .hasFieldOrPropertyWithValue( "orderBy", "id" )
                .hasFieldOrPropertyWithValue( "direction", Sort.Direction.DESC );

        assertThat( SortArg.valueOf( "id" ).getValue() )
                .hasFieldOrPropertyWithValue( "orderBy", "id" )
                .hasFieldOrPropertyWithValue( "direction", null );
    }
}