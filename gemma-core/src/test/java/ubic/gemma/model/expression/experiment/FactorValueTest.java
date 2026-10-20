package ubic.gemma.model.expression.experiment;

import org.junit.jupiter.api.Test;
import ubic.gemma.model.common.measurement.Measurement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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

    /**
     * Two unsaved factor values that differ only in their (saved) factor are different factor values.
     * {@code IdentifiableUtils.equals} compared the second factor's id with itself, so any factor with an id matched any
     * other, and such factor values collapsed into one in a set.
     */
    @Test
    public void testEqualsWithDifferentFactors() {
        ExperimentalFactor genotype = ExperimentalFactor.Factory.newInstance();
        genotype.setId( 1L );
        ExperimentalFactor treatment = ExperimentalFactor.Factory.newInstance();
        treatment.setId( 2L );
        FactorValue fv1 = new FactorValue();
        fv1.setExperimentalFactor( genotype );
        fv1.setValue( "control" );
        FactorValue fv2 = new FactorValue();
        fv2.setExperimentalFactor( treatment );
        fv2.setValue( "control" );
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