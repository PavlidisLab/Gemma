package ubic.gemma.model.analysis.expression.diff;

import org.junit.Test;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.model.expression.experiment.FactorValue;

import static org.junit.Assert.*;

public class ContrastTest {

    @Test
    public void test() {
        ExperimentalFactor ef = new ExperimentalFactor();
        ef.setType( FactorType.CATEGORICAL );
        FactorValue fv1 = new FactorValue();
        fv1.setExperimentalFactor( ef );
        fv1.setId( 1L );
        assertEquals( Contrast.categorical( fv1 ), Contrast.categorical( fv1 ) );
        assertFalse( Contrast.categorical( fv1 ).isInteraction() );
        assertFalse( Contrast.categorical( fv1 ).isContinuous() );
    }


    @Test
    public void testInteraction() {
        ExperimentalFactor ef = new ExperimentalFactor();
        ef.setType( FactorType.CATEGORICAL );
        FactorValue fv1 = new FactorValue();
        fv1.setExperimentalFactor( ef );
        fv1.setId( 1L );
        ExperimentalFactor ef2 = new ExperimentalFactor();
        ef2.setType( FactorType.CATEGORICAL );
        FactorValue fv2 = new FactorValue();
        fv2.setId( 2L );
        fv2.setExperimentalFactor( ef2 );
        assertEquals( Contrast.categorical( fv1 ), Contrast.categorical( fv1 ) );
        assertFalse( Contrast.categorical( fv1 ).isInteraction() );
        assertFalse( Contrast.categorical( fv1 ).isContinuous() );
    }

    @Test
    public void testContinuous() {
        ExperimentalFactor ef = new ExperimentalFactor();
        ef.setType( FactorType.CONTINUOUS );
        assertTrue( Contrast.continuous( ef ).isContinuous() );
    }
}