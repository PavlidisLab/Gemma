package ubic.gemma.model.expression.experiment;

import org.junit.Test;

import javax.annotation.Nullable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class StatementTest {

    @Test
    public void testEqual() {
        assertEquals( createStatement( null, "foo", null, "bar", null, "foo" ),
                createStatement( null, "foo", null, "bar", null, "foo" ) );
        // two statements with the same predicate label but differing URIs
        assertNotEquals( createStatement( "", "", "http://foo", "foo", "", "" ),
                createStatement( "", "", "http://bar", "foo", "", "" ) );
        // two statements with the same predicate URI but different labels
        assertEquals( createStatement( "", "", "http://foo", "foo", "", "" ),
                createStatement( "", "", "http://foo", "bar", "", "" ) );
    }

    private Statement createStatement( @Nullable String valueUri, String value, @Nullable String predicateUri, String predicate, @Nullable String objectUri, String object ) {
        Statement s = new Statement();
        s.setValueUri( valueUri );
        s.setValue( value );
        s.setPredicateUri( predicateUri );
        s.setPredicate( predicate );
        s.setObjectUri( objectUri );
        s.setObject( object );
        return s;
    }
}