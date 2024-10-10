package ubic.gemma.rest.util;

import org.junit.Test;
import ubic.gemma.persistence.util.Sort;

import static org.assertj.core.api.Assertions.assertThat;

public class SortValueObjectTest {

    @Test
    public void test() {
        assertThat( new SortValueObject( Sort.by( "ee", "id", Sort.Direction.DESC, Sort.NullMode.DEFAULT ) ) )
                .hasFieldOrPropertyWithValue( "orderBy", "ee.id" )
                .hasFieldOrPropertyWithValue( "direction", "-" );
        assertThat( new SortValueObject( Sort.by( "ee", "id", null, Sort.NullMode.DEFAULT ) ) )
                .hasFieldOrPropertyWithValue( "orderBy", "ee.id" )
                .hasFieldOrPropertyWithValue( "direction", null );
        assertThat( new SortValueObject( Sort.by( null, "id", null, Sort.NullMode.DEFAULT ) ) )
                .hasFieldOrPropertyWithValue( "orderBy", "id" )
                .hasFieldOrPropertyWithValue( "direction", null );
    }

}