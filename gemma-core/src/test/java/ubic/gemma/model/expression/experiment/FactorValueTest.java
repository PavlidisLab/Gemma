package ubic.gemma.model.expression.experiment;

import org.junit.Test;
import ubic.gemma.model.common.measurement.Measurement;
import ubic.gemma.model.common.measurement.MeasurementKind;
import ubic.gemma.model.common.measurement.MeasurementType;
import ubic.gemma.model.common.measurement.Unit;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Test
    public void testCompareCategoricalFactor() {
        ExperimentalFactor factor = new ExperimentalFactor();
        factor.setType( FactorType.CATEGORICAL );
        assertThat( FactorValue.COMPARATOR.compare( FactorValue.Factory.newInstance( factor ), FactorValue.Factory.newInstance( factor ) ) ).isZero();
        assertThat( FactorValue.COMPARATOR.compare(
                FactorValue.Factory.newInstance( factor, createStatement( "a", "b", "c" ) ),
                FactorValue.Factory.newInstance( factor, createStatement( "a", "b", "c" ) ) ) )
                .isZero();
        assertThat( FactorValue.COMPARATOR.compare(
                FactorValue.Factory.newInstance( factor, createStatement( "a", "b", "c" ) ),
                FactorValue.Factory.newInstance( factor, createStatement( "b", "b", "c" ) ) ) )
                .isNegative();
        assertThat( FactorValue.COMPARATOR.compare(
                FactorValue.Factory.newInstance( factor, createStatement( "b", "b", "c" ) ),
                FactorValue.Factory.newInstance( factor, createStatement( "a", "b", "c" ) ) ) )
                .isPositive();
        assertThat( FactorValue.COMPARATOR.compare(
                FactorValue.Factory.newInstance( factor, createStatement( "a", "b", "a" ) ),
                FactorValue.Factory.newInstance( factor, createStatement( "a", "b", "b" ) ) ) )
                .isNegative();
    }

    @Test
    public void testCompareContinuousFactor() {
        ExperimentalFactor factor = new ExperimentalFactor();
        factor.setType( FactorType.CONTINUOUS );
        assertThat( FactorValue.COMPARATOR.compare( FactorValue.Factory.newInstance( factor, createMeasurement( 12 ) ),
                FactorValue.Factory.newInstance( factor, createMeasurement( 12 ) ) ) ).isZero();
        assertThat( FactorValue.COMPARATOR.compare( FactorValue.Factory.newInstance( factor, createMeasurement( 12 ) ),
                FactorValue.Factory.newInstance( factor, createMeasurement( 13 ) ) ) ).isNegative();
        assertThat( FactorValue.COMPARATOR.compare( FactorValue.Factory.newInstance( factor, createMeasurement( 13 ) ),
                FactorValue.Factory.newInstance( factor, createMeasurement( 12 ) ) ) ).isPositive();
        assertThatThrownBy( () -> FactorValue.COMPARATOR.compare(
                FactorValue.Factory.newInstance( factor, createMeasurement( 13, "kg" ) ),
                FactorValue.Factory.newInstance( factor, createMeasurement( 12, "mg" ) ) ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessage( "Cannot compare measurements with different units." );
    }

    @Test
    @Deprecated
    public void testCompareWithValue() {
        ExperimentalFactor factor = new ExperimentalFactor();
        assertThat( FactorValue.COMPARATOR.compare( FactorValue.Factory.newInstance( factor, "test" ), new FactorValue() ) ).isNegative();
        assertThat( FactorValue.COMPARATOR.compare( FactorValue.Factory.newInstance( factor, "test" ), FactorValue.Factory.newInstance( factor, "test" ) ) ).isZero();
    }

    private Statement createStatement( String subject, String predicate, String object ) {
        Statement s = new Statement();
        s.setSubject( subject );
        s.setPredicate( predicate );
        s.setObject( object );
        return s;
    }

    private Measurement createMeasurement( double value ) {
        return Measurement.Factory.newInstance( MeasurementType.ABSOLUTE, String.valueOf( value ), PrimitiveType.DOUBLE );
    }

    private Measurement createMeasurement( double value, String unit ) {
        return Measurement.Factory.newInstance( MeasurementType.ABSOLUTE, String.valueOf( value ), MeasurementKind.MASS, null, PrimitiveType.DOUBLE, Unit.Factory.newInstance( unit ) );
    }
}