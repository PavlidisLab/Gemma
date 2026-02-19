package ubic.gemma.model.expression.experiment;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ExpressionExperimentSubSetUtilsTest {

    @Test
    public void testGetUnprefixedName() {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setName( "Molecular and spatial heterogeneity of microglia in Rasmussen encephalitis" );
        ExpressionExperimentSubSet subset = new ExpressionExperimentSubSet();
        subset.setSourceExperiment( ee );
        subset.setName( "Molecular and spatial heterogeneity of microglia in Rasmussen encephalitis - astrocyte" );
        assertEquals( "astrocyte", ExpressionExperimentSubSetUtils.getUnprefixedName( subset ) );
    }

    @Test
    public void testGetUnprefixedNameFromOldDeaName() {
        ExpressionExperimentSubSet subset = new ExpressionExperimentSubSet();
        subset.setName( "Subset for FactorValue 130192: organism part:inferior parietal cortex | posterior orientation | " );
        assertEquals( "FactorValue 130192: organism part:inferior parietal cortex | posterior orientation | ", ExpressionExperimentSubSetUtils.getUnprefixedName( subset ) );
    }
}