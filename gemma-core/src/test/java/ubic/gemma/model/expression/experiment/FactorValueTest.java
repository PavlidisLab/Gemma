package ubic.gemma.model.expression.experiment;

import org.junit.Test;
import ubic.gemma.model.common.measurement.Measurement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class FactorValueTest {

    @Test
    public void testEquals() {
        FactorValue fv1 = new FactorValue();
        fv1.setMeasurement( new Measurement() );
        FactorValue fv2 = new FactorValue();
        fv2.setMeasurement( new Measurement() );
        assertEquals( fv1, fv2 );
    }

    @Test
    public void testEqualsWithDifferentCharacteristics() {
        FactorValue fv1 = new FactorValue();
        fv1.getCharacteristics().add( createStatement( "bob", "is", "a farmer" ) );
        FactorValue fv2 = new FactorValue();
        fv2.getCharacteristics().add( createStatement( "bob", "is", "an insurance broken" ) );
        assertNotEquals( fv1, fv2 );
    }

    @Test
    public void testEqualsWithDifferentValues() {
        FactorValue fv1 = new FactorValue();
        fv1.setValue( "bob is a farmer" );
        FactorValue fv2 = new FactorValue();
        fv2.setValue( "bob is an insurance broken" );
        assertNotEquals( fv1, fv2 );
    }

    private Statement createStatement( String subject, String predicate, String object ) {
        Statement s = new Statement();
        s.setSubject( subject );
        s.setPredicate( predicate );
        s.setObject( object );
        return s;
    }
}