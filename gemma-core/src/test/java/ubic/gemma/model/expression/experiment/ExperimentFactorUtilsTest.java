package ubic.gemma.model.expression.experiment;

import org.junit.Test;
import ubic.gemma.model.common.description.Categories;

import static org.junit.Assert.*;
import static ubic.gemma.model.expression.experiment.ExperimentFactorUtils.isBatchFactor;

public class ExperimentFactorUtilsTest {

    @Test
    public void testIsBatchFactor() {
        assertFalse( isBatchFactor( ExperimentalFactor.Factory.newInstance( "batch", FactorType.CONTINUOUS ) ) );
        assertTrue( isBatchFactor( ExperimentalFactor.Factory.newInstance( "BATCH", FactorType.CATEGORICAL ) ) );
        assertTrue( isBatchFactor( ExperimentalFactor.Factory.newInstance( "batch", FactorType.CATEGORICAL ) ) );
        assertTrue( isBatchFactor( ExperimentalFactor.Factory.newInstance( "batch", FactorType.CATEGORICAL, Categories.BLOCK ) ) );
        assertTrue( isBatchFactor( ExperimentalFactor.Factory.newInstance( "lane", FactorType.CATEGORICAL, Categories.BLOCK ) ) );
        ExperimentalFactor ef = ExperimentalFactor.Factory.newInstance( "block2", FactorType.CATEGORICAL, Categories.BLOCK );
        assertNotNull( ef.getCategory() );
        ef.getCategory().setCategory( "block2" );
        assertTrue( isBatchFactor( ef ) );
    }
}