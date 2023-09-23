package ubic.gemma.persistence.util;

import org.apache.commons.collections4.IterableUtils;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FiltersTest {

    @Test
    public void test() {
        Filters filters = Filters.empty()
                .and( "ee", "id", Long.class, Filter.Operator.eq, 3L )
                .and()
                    .or( "ee", "id", Long.class, Filter.Operator.eq, 1L )
                    .or( "ee", "id", Long.class, Filter.Operator.eq, 2L )
                .and()
                .and()
                    .or( "ee", "shortName", String.class, Filter.Operator.like, "%brain%" )
                    .or( "ee", "description", String.class, Filter.Operator.like, "%tumor%" )
                .build();
        assertThat( filters )
                .hasSize( 4 )
                .hasToString( "ee.description like %tumor% or ee.shortName like %brain% and ee.id = 1 or ee.id = 2 and ee.id = 3" )
                .flatExtracting( it -> {
                    List<Filter> f = new ArrayList<>();
                    it.forEach( f::add );
                    return f;
                } )
                .extracting( Filter::getRequiredValue )
                .containsExactly( "%tumor%", "%brain%", 1L, 2L, 3L );
    }

    @Test
    public void testEmptySubClause() {
        assertThat( Filters.empty().isEmpty() ).isTrue();
        Filters f = Filters.empty()
                .and( new Filter[0] );
        assertThat( f.isEmpty() ).isTrue();
    }

    @Test
    public void testEquality() {
        assertThat( Filters.by( "ee", "id", Long.class, Filter.Operator.lessThan, 10L ) )
                .isEqualTo( Filters.by( "ee", "id", Long.class, Filter.Operator.lessThan, 10L ) );
    }

    @Test
    public void testUseBuiltSubClause() {
        Filters.FiltersClauseBuilder subClause = Filters.empty().and();
        subClause
                .or( Filter.by( "ee", "id", Long.class, Filter.Operator.lessThan, 10L ) )
                .build();
        assertThatThrownBy( () -> subClause.or( Filter.by( "ee", "id", Long.class, Filter.Operator.lessThan, 10L ) ) )
                .isInstanceOf( IllegalStateException.class );
    }
}