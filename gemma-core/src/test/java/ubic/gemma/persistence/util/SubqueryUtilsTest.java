package ubic.gemma.persistence.util;

import org.junit.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static ubic.gemma.persistence.util.SubqueryUtils.guessAliases;

public class SubqueryUtilsTest {

    @Test
    public void test() {
        assertThat( guessAliases( "experimentalDesign.characteristics.", "c" ) )
                .containsExactly(
                        new Subquery.Alias( null, "experimentalDesign", "alias1" ),
                        new Subquery.Alias( "alias1", "characteristics", "c" ) );
    }

    @Test
    public void testEmptyPath() {
        assertThat( guessAliases( "", "c" ) )
                .isEmpty();
    }

    @Test
    public void testSubqueryWithGuessedAliases() {
        Subquery s = new Subquery( "ExpressionExperiment", "id", guessAliases( "experimentalDesign.characteristics.", "c" ),
                Filter.by( "c", "valueUri", String.class, Filter.Operator.in, Collections.singletonList( "http://example.com" ) ) );
        assertThat( s )
                .hasToString( "select e.id from ExpressionExperiment e join e.experimentalDesign alias1 join alias1.characteristics c where c.valueUri in (http://example.com)" );
        Filters f = Filters.by( "ee", "id", Long.class, Filter.Operator.inSubquery, s );
        assertThat( FilterQueryUtils.formRestrictionClause( f ) )
                .isEqualTo( " and (ee.id in (select e.id from ExpressionExperiment e join e.experimentalDesign alias1 join alias1.characteristics c where c.valueUri in (:c_valueUri1)))" );
    }

    @Test
    public void testSubqueryWithRootAlias() {
        Subquery s = new Subquery( "ExpressionExperiment", "id",
                Collections.singletonList( new Subquery.Alias( null, "", "ee" ) ),
                Filter.by( "ee", "id", Long.class, Filter.Operator.eq, 1L ) );
        assertThat( s.getRootAlias() ).isEqualTo( "ee" );
        assertThat( s ).hasToString( "select ee.id from ExpressionExperiment ee where ee.id = 1" );
    }
}