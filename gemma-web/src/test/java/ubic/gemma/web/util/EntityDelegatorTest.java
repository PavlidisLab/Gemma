package ubic.gemma.web.util;

import org.junit.Test;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import static org.junit.Assert.*;

public class EntityDelegatorTest {

    @Test
    public void test() {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 1L );
        EntityDelegator<ExpressionExperiment> ed = new EntityDelegator<>( ee );
        assertEquals( ee.getId(), ed.getId() );
        assertEquals( ExpressionExperiment.class.getName(), ed.getClassDelegatingFor() );
        assertTrue( ed.holds( ExpressionExperiment.class ) );
        assertFalse( ed.holds( ExperimentalDesign.class ) );
    }

    @Test
    public void testWhenClassUsesSimpleName() {
        EntityDelegator<ExpressionExperiment> ed = new EntityDelegator<>();
        ed.setId( 1L );
        ed.setClassDelegatingFor( "ExpressionExperiment" );
        assertTrue( ed.holds( ExpressionExperiment.class ) );
        assertFalse( ed.holds( ExperimentalDesign.class ) );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEntityDelegatorWithNullId() {
        new EntityDelegator<>( new ExpressionExperiment() );
    }
}