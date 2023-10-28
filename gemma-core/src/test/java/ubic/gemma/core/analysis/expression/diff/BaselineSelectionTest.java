package ubic.gemma.core.analysis.expression.diff;

import org.junit.Test;
import ubic.gemma.model.common.measurement.Measurement;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.Statement;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BaselineSelectionTest {

    @Test
    public void testBaseline() {
        FactorValue fv = new FactorValue();
        assertFalse( BaselineSelection.isBaselineCondition( fv ) );

        fv = new FactorValue();
        fv.getCharacteristics().add( createStatement( "control", "http://www.ebi.ac.uk/efo/EFO_0001461" ) );
        assertTrue( BaselineSelection.isBaselineCondition( fv ) );

        fv = new FactorValue();
        fv.getCharacteristics().add( createStatement( "control", null ) );
        assertTrue( BaselineSelection.isBaselineCondition( fv ) );

        fv = new FactorValue();
        fv.getCharacteristics().add( createStatement( "CONTROL", null ) );
        assertTrue( BaselineSelection.isBaselineCondition( fv ) );

        fv = new FactorValue();
        fv.getCharacteristics().add( createStatement( "  control    ", null ) );
        assertTrue( BaselineSelection.isBaselineCondition( fv ) );

        fv = new FactorValue();
        fv.getCharacteristics().add( createStatement( "  initial  time point", null ) );
        assertTrue( BaselineSelection.isBaselineCondition( fv ) );

        fv = new FactorValue();
        fv.getCharacteristics().add( createStatement( "initial_time_point", null ) );
        assertTrue( BaselineSelection.isBaselineCondition( fv ) );

        // a "control" term is used, but it's not a control term URI
        fv = new FactorValue();
        fv.getCharacteristics().add( createStatement( "control", "http://www.ebi.ac.uk/efo/EFO_0001462" ) );
        assertFalse( BaselineSelection.isBaselineCondition( fv ) );

        fv = new FactorValue();
        fv.setMeasurement( new Measurement() );
        fv.getCharacteristics().add( createStatement( "control", "http://www.ebi.ac.uk/efo/EFO_0001461" ) );
        assertFalse( BaselineSelection.isBaselineCondition( fv ) );
    }

    @Test
    public void testForcedBaseline() {
        FactorValue fv = new FactorValue();
        fv.setIsBaseline( true );
        assertTrue( BaselineSelection.isBaselineCondition( fv ) );
        assertTrue( BaselineSelection.isForcedBaseline( fv ) );

        fv = new FactorValue();
        fv.getCharacteristics().add( createStatement( "control", "http://www.ebi.ac.uk/efo/EFO_0001461" ) );
        assertTrue( BaselineSelection.isBaselineCondition( fv ) );
        assertTrue( BaselineSelection.isForcedBaseline( fv ) );

        fv = new FactorValue();
        fv.getCharacteristics().add( createStatement( "control", "http://www.ebi.ac.uk/EfO/efo_0001461" ) );
        assertTrue( BaselineSelection.isBaselineCondition( fv ) );
        assertTrue( BaselineSelection.isForcedBaseline( fv ) );

        fv = new FactorValue();
        fv.setIsBaseline( false );
        fv.getCharacteristics().add( createStatement( "control", "http://www.ebi.ac.uk/efo/EFO_0001461" ) );
        assertFalse( BaselineSelection.isBaselineCondition( fv ) );
        assertFalse( BaselineSelection.isForcedBaseline( fv ) );
    }

    private Statement createStatement( String subject, String subjectUri ) {
        Statement s = new Statement();
        s.setSubject( subject );
        s.setSubjectUri( subjectUri );
        return s;
    }
}