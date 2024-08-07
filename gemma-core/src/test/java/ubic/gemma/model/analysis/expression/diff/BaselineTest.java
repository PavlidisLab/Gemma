package ubic.gemma.model.analysis.expression.diff;

import org.junit.Test;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.model.expression.experiment.FactorValue;

import static org.junit.Assert.*;

public class BaselineTest {

    @Test
    public void test() {
        ExperimentalFactor ef = new ExperimentalFactor();
        ef.setType( FactorType.CATEGORICAL );
        FactorValue fv = new FactorValue();
        fv.setExperimentalFactor( ef );
        Baseline b = Baseline.categorical( fv );
        assertFalse( b.isInteraction() );
        assertEquals( "Baseline for FactorValue", b.toString() );
    }

    @Test
    public void testInteractionBaseline() {
        ExperimentalFactor ef = new ExperimentalFactor();
        ef.setId( 1L );
        ef.setType( FactorType.CATEGORICAL );
        FactorValue fv1 = new FactorValue();
        fv1.setId( 1L );
        fv1.setExperimentalFactor( ef );
        ExperimentalFactor ef2 = new ExperimentalFactor();
        ef2.setId( 2L );
        ef2.setType( FactorType.CATEGORICAL );
        FactorValue fv2 = new FactorValue();
        fv2.setId( 2L );
        fv2.setExperimentalFactor( ef2 );
        Baseline b = Baseline.interaction( fv1, fv2 );
        assertTrue( b.isInteraction() );
        assertEquals( "Baseline for FactorValue Id=1:FactorValue Id=2", b.toString() );
    }
}