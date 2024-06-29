package ubic.gemma.model.expression.experiment;

import org.junit.Test;
import ubic.gemma.core.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

public class StatementTest {

    @Test
    public void testEqual() {
        Statement s = createStatement( null, "foo", null, "bar", null, "foo" );
        assertThat( createStatement( null, "foo", null, "bar", null, "foo" ) )
                .isEqualByComparingTo( s )
                .hasSameHashCodeAs( s )
                .isEqualTo( s );
        Statement sWithDifferentCase = createStatement( null, "Foo", null, "baR", null, "fOo" );
        assertThat( createStatement( null, "foo", null, "bar", null, "foo" ) )
                .isEqualByComparingTo( sWithDifferentCase )
                .hasSameHashCodeAs( sWithDifferentCase )
                .isEqualTo( sWithDifferentCase );
        // two statements with the same predicate label but differing URIs
        assertThat( createStatement( "", "", "http://foo", "foo", "", "" ) )
                .isNotEqualTo( createStatement( "", "", "http://bar", "foo", "", "" ) )
                .isNotEqualByComparingTo( createStatement( "", "", "http://bar", "foo", "", "" ) );
        // two statements with the same predicate URI but different labels
        assertThat( createStatement( "", "", "http://foo", "foo", "", "" ) )
                .isEqualTo( createStatement( "", "", "http://foo", "bar", "", "" ) )
                .isEqualByComparingTo( createStatement( "", "", "http://foo", "bar", "", "" ) )
                .hasSameHashCodeAs( createStatement( "", "", "http://foo", "bar", "", "" ) );
    }

    private Statement createStatement( @Nullable String valueUri, String value, @Nullable String predicateUri, String predicate, @Nullable String objectUri, String object ) {
        Statement s = new Statement();
        s.setSubjectUri( valueUri );
        s.setSubject( value );
        s.setPredicateUri( predicateUri );
        s.setPredicate( predicate );
        s.setObjectUri( objectUri );
        s.setObject( object );
        return s;
    }
}