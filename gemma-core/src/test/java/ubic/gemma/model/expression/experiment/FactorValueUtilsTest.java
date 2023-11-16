package ubic.gemma.model.expression.experiment;

import org.junit.Test;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.measurement.Measurement;
import ubic.gemma.model.common.measurement.MeasurementType;
import ubic.gemma.model.common.measurement.Unit;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;

import static org.junit.Assert.assertEquals;

public class FactorValueUtilsTest {

    @Test
    public void test() {
        FactorValue fv;

        fv = new FactorValue();
        fv.getCharacteristics().add( createStatement( "methoxyacetic acid", "delivered at dose", "650 mg/kg" ) );
        assertEquals( "methoxyacetic acid delivered at dose 650 mg/kg", FactorValueUtils.getSummaryString( fv ) );

        fv = new FactorValue();
        fv.getCharacteristics().add( createStatement( "methoxyacetic acid", "delivered at dose", "650 mg/kg", "for duration", "4h" ) );
        assertEquals( "methoxyacetic acid delivered at dose 650 mg/kg and for duration 4h", FactorValueUtils.getSummaryString( fv ) );

        fv = new FactorValue();
        fv.getCharacteristics().add( createStatement( "methoxyacetic acid", null, null, "for duration", "4h" ) );
        assertEquals( "methoxyacetic acid for duration 4h", FactorValueUtils.getSummaryString( fv ) );
    }

    @Test
    public void testMeasurement() {
        FactorValue fv = new FactorValue();
        fv.setExperimentalFactor( new ExperimentalFactor() );
        Measurement m = Measurement.Factory.newInstance( MeasurementType.ABSOLUTE, "5.0", PrimitiveType.DOUBLE );
        m.setUnit( Unit.Factory.newInstance( "mg" ) );
        fv.setMeasurement( m );
        assertEquals( "5.0 mg", FactorValueUtils.getSummaryString( fv ) );
    }

    @Test
    public void testMeasurementWithCategory() {
        ExperimentalFactor age = new ExperimentalFactor();
        Characteristic ageC = new Characteristic();
        ageC.setCategory( "age" );
        age.setCategory( ageC );
        FactorValue fv = new FactorValue();
        fv.setExperimentalFactor( age );
        Measurement m = Measurement.Factory.newInstance( MeasurementType.ABSOLUTE, "5.0", PrimitiveType.DOUBLE );
        m.setUnit( Unit.Factory.newInstance( "years" ) );
        fv.setMeasurement( m );
        assertEquals( "age: 5.0 years", FactorValueUtils.getSummaryString( fv ) );
    }

    private Statement createStatement( String subject, String predicate, String object, String secondPredicate, String secondObject ) {
        Statement s = new Statement();
        s.setSubject( subject );
        s.setPredicate( predicate );
        s.setObject( object );
        s.setSecondPredicate( secondPredicate );
        s.setSecondObject( secondObject );
        return s;
    }

    private Statement createStatement( String subject, String predicate, String object ) {
        return createStatement( subject, predicate, object, null, null );
    }

    private Measurement createMeasurement( String value ) {
        Measurement m = new Measurement();
        m.setValue( value );
        return m;
    }
}