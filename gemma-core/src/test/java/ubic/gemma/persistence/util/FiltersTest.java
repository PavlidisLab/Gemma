package ubic.gemma.persistence.util;

import org.apache.commons.collections4.IterableUtils;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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
        List<List<Filter>> clauses = IterableUtils.toList( filters );
        assertThat( clauses ).hasSize( 4 );
        assertThat( clauses.get( 0 ) ).extracting( "requiredValue" ).containsExactly( 3L );
        assertThat( clauses.get( 1 ) ).extracting( "requiredValue" ).containsExactly( 1L, 2L );
        assertThat( clauses.get( 2 ) ).isEmpty();
        assertThat( clauses.get( 3 ) ).extracting( "requiredValue" ).containsExactly( "%brain%", "%tumor%" );
        assertThat( filters ).hasToString( "ee.id = 3 and ee.id = 1 or ee.id = 2 and ee.shortName like %brain% or ee.description like %tumor%" );
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

}