package ubic.gemma.model.expression.experiment;

import org.junit.jupiter.api.Test;
import ubic.gemma.model.common.description.Categories;
import ubic.gemma.model.common.description.Characteristic;

import static org.junit.jupiter.api.Assertions.*;
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

    @Test
    public void testIsDeIncludeExcludeFactor() {
        ExperimentalFactor ef = ExperimentalFactor.Factory.newInstance( "collection of material", FactorType.CATEGORICAL );
        assertFalse( ExperimentFactorUtils.isDeIncludeExcludeFactor( ef ) );

        // a marker factor is recognized from either of its two values, by URI
        ef.getFactorValues().add( markerValue( ef, "DE_Include", FactorValueUtils.DE_INCLUDE_URI ) );
        assertTrue( ExperimentFactorUtils.isDeIncludeExcludeFactor( ef ) );

        ExperimentalFactor excludeOnly = ExperimentalFactor.Factory.newInstance( "collection of material", FactorType.CATEGORICAL );
        excludeOnly.getFactorValues().add( markerValue( excludeOnly, "DE_Exclude", FactorValueUtils.DE_EXCLUDE_URI ) );
        assertTrue( ExperimentFactorUtils.isDeIncludeExcludeFactor( excludeOnly ) );

        // the value string alone is not enough: matching is by URI
        ExperimentalFactor stringOnly = ExperimentalFactor.Factory.newInstance( "collection of material", FactorType.CATEGORICAL );
        stringOnly.getFactorValues().add( markerValue( stringOnly, "DE_Exclude", null ) );
        assertFalse( ExperimentFactorUtils.isDeIncludeExcludeFactor( stringOnly ) );
    }

    private FactorValue markerValue( ExperimentalFactor ef, String value, String valueUri ) {
        Characteristic c = Characteristic.Factory.newInstance();
        c.setCategory( "collection of material" );
        c.setCategoryUri( "http://www.ebi.ac.uk/efo/EFO_0005066" );
        c.setValue( value );
        c.setValueUri( valueUri );
        FactorValue fv = FactorValue.Factory.newInstance( ef );
        fv.getCharacteristics().add( Statement.Factory.newInstance( c ) );
        return fv;
    }
}