package ubic.gemma.model.expression.experiment;

import org.junit.Before;
import org.junit.Test;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.measurement.Measurement;
import ubic.gemma.model.common.measurement.MeasurementType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.expression.biomaterial.BioMaterial;

import java.util.Collection;
import java.util.HashSet;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ExperimentalDesignUtilsTest {

    private final Random random = new Random( 123L );

    @Before
    public void setUp() {
        random.setSeed( 123L );
    }

    @Test
    public void testGetFactorValueMap() {
        Collection<BioMaterial> samples = new HashSet<>();
        for ( int i = 0; i < 10; i++ ) {
            samples.add( BioMaterial.Factory.newInstance( "sample" + i ) );
        }
        ExperimentalDesign design = randomExperimentalDesign( samples, 3, 5, 2, 0.0 );
        // print out the factor values for each sample in the experimental
        assertThat( ExperimentalDesignUtils.getFactorValueMap( design, samples ) )
                .hasSize( 5 )
                .allSatisfy( ( k, v ) -> {
                    assertThat( v )
                            .hasSize( 10 )
                            .doesNotContainValue( null );
                } );
    }

    @Test
    public void testGetFactorValueMapWithUnassignedFactors() {
        Collection<BioMaterial> samples = new HashSet<>();
        for ( int i = 0; i < 10; i++ ) {
            samples.add( BioMaterial.Factory.newInstance( "sample" + i ) );
        }
        ExperimentalDesign design = randomExperimentalDesign( samples, 3, 5, 2, 0.5 );
        // print out the factor values for each sample in the experimental
        assertThat( ExperimentalDesignUtils.getFactorValueMap( design, samples ) )
                .hasSize( 5 )
                .allSatisfy( ( k, v ) -> {
                    assertThat( v )
                            .hasSize( 10 )
                            .containsValue( null );
                } );
    }

    @Test
    public void testGetFactorValueMapWithMultipleValues() {
        Collection<BioMaterial> samples = new HashSet<>();
        for ( int i = 0; i < 10; i++ ) {
            samples.add( BioMaterial.Factory.newInstance( "sample" + i ) );
        }
        ExperimentalDesign design = randomExperimentalDesign( samples, 3, 5, 2, 0.3 );
        BioMaterial sample = samples.iterator().next();
        FactorValue fv = sample.getFactorValues().iterator().next();
        ExperimentalFactor fact = fv.getExperimentalFactor();
        FactorValue duplicatedValue;
        if ( fact.getType() == FactorType.CATEGORICAL ) {
            // pick any other value
            duplicatedValue = fact.getFactorValues().stream()
                    .filter( v -> !v.equals( fv ) )
                    .findFirst()
                    .orElseThrow( () -> new IllegalStateException( "" ) );
        } else {
            // make up a new continuous value
            duplicatedValue = FactorValue.Factory.newInstance( fact, Measurement.Factory.newInstance( MeasurementType.ABSOLUTE, "12.0", PrimitiveType.DOUBLE ) );
        }
        sample.getFactorValues().add( duplicatedValue );
        assertThatThrownBy( () -> ExperimentalDesignUtils.getFactorValueMap( design, samples ) )
                .isInstanceOf( IllegalStateException.class );
    }

    /**
     *
     * @param numCategorical number of categorical factors to create
     * @param numValues      number of values for each categorical factor
     * @param numContinuous  number of continuous factors to create
     * @param fractionUnassigned fraction of samples that wll lack a value
     * @return
     */
    public ExperimentalDesign randomExperimentalDesign( Collection<BioMaterial> samples, int numCategorical, int numValues, int numContinuous, double fractionUnassigned ) {
        ExperimentalDesign design = new ExperimentalDesign();

        for ( int i = 0; i < numCategorical; i++ ) {
            ExperimentalFactor factor = ExperimentalFactor.Factory.newInstance( "factor" + i, FactorType.CATEGORICAL );
            for ( int j = 0; j < numValues; j++ ) {
                factor.getFactorValues().add( FactorValue.Factory.newInstance( factor, Characteristic.Factory.newInstance( "cat" + i, null, "val" + j, null ) ) );
            }
            design.getExperimentalFactors().add( factor );
        }
        for ( int k = 0; k < numContinuous; k++ ) {
            design.getExperimentalFactors().add( ExperimentalFactor.Factory.newInstance( "continuous" + k, FactorType.CONTINUOUS ) );
        }

        // assign factor values to samples
        int i = 0;
        for ( BioMaterial bm : samples ) {
            for ( ExperimentalFactor factor : design.getExperimentalFactors() ) {
                FactorValue value;
                if ( random.nextDouble() < fractionUnassigned ) {
                    continue;
                } else if ( factor.getType().equals( FactorType.CONTINUOUS ) ) {
                    value = FactorValue.Factory.newInstance( factor, Measurement.Factory.newInstance( MeasurementType.ABSOLUTE, String.valueOf( random.nextDouble() ), PrimitiveType.DOUBLE ) );
                } else {
                    // pick a random value
                    value = factor.getFactorValues().stream()
                            .skip( random.nextInt( factor.getFactorValues().size() ) )
                            .findFirst()
                            .orElseThrow( () -> new IllegalStateException( "No factor values found for " + factor.getName() ) );
                }
                bm.getFactorValues().add( value );
            }
            i++;
        }

        return design;
    }
}