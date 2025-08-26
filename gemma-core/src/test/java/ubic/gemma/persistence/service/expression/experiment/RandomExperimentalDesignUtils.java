package ubic.gemma.persistence.service.expression.experiment;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.util.Assert;
import ubic.gemma.model.common.description.Categories;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.measurement.Measurement;
import ubic.gemma.model.common.measurement.MeasurementType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Generate random experimental design for a given experiment.
 */
public class RandomExperimentalDesignUtils {

    /**
     * This ensures we use the same random as {@link RandomStringUtils}.
     */
    private static Random random() {
        return ThreadLocalRandom.current();
    }

    public static void setSeed( long seed ) {
        random().setSeed( seed );
    }

    /**
     * Create and assign a random experimental design to the given experiment.
     */
    public ExperimentalDesign randomExperimentalDesign( ExpressionExperiment ee, int numCategoricalFactors, int numContinuousFactors ) {
        Assert.notNull( ee.getExperimentalDesign(), "Experimental design must be initialized" );
        ExperimentalDesign design = randomExperimentalDesign( getSampleUsed( ee ), numCategoricalFactors, numContinuousFactors );
        ee.setExperimentalDesign( design );
        return design;
    }

    /**
     * Create a random experimental design with the given number of categorical and continuous factors.
     */
    public ExperimentalDesign randomExperimentalDesign( Collection<BioMaterial> samples, int numCategoricalFactors, int numContinuousFactors ) {
        ExperimentalDesign design = new ExperimentalDesign();
        for ( int i = 0; i < numCategoricalFactors; i++ ) {
            randomCategoricalFactor( design, samples, RandomStringUtils.randomAlphanumeric( 12 ), random().nextInt( 5 ) + 2 );
        }
        for ( int i = 0; i < numContinuousFactors; i++ ) {
            randomContinuousFactor( design, samples, RandomStringUtils.randomAlphanumeric( 12 ) );
        }
        return design;
    }

    /**
     * Create and assign a categorical factor to all the samples of an experiment.
     * @see #randomCategoricalFactor(ExperimentalDesign, Collection, String, int)
     */
    public static ExperimentalFactor randomCategoricalFactor( ExpressionExperiment ee, String name, int numValues ) {
        Assert.notNull( ee.getExperimentalDesign() );
        return randomCategoricalFactor( ee.getExperimentalDesign(), getSampleUsed( ee ),
                name, numValues );
    }

    /**
     * Create and assign a categorical factor.
     * @param design    design to which the factor will be added
     * @param samples   samples to which the factor values will be assigned
     * @param name      name to use for the factor
     * @param numValues number of factor values to create
     */
    public static ExperimentalFactor randomCategoricalFactor( ExperimentalDesign design, Collection<BioMaterial> samples, String name, int numValues ) {
        ExperimentalFactor factor = ExperimentalFactor.Factory.newInstance( name, FactorType.CATEGORICAL );
        List<FactorValue> fvs = new ArrayList<>( numValues );
        for ( int i = 0; i < numValues; i++ ) {
            fvs.add( FactorValue.Factory.newInstance( factor, Characteristic.Factory.newInstance( Categories.UNCATEGORIZED, RandomStringUtils.randomAlphanumeric( 12 ), null ) ) );
        }
        factor.getFactorValues().addAll( fvs );
        for ( BioMaterial sample : samples ) {
            sample.getFactorValues().add( fvs.get( random().nextInt( fvs.size() ) ) );
        }
        factor.setExperimentalDesign( design );
        design.getExperimentalFactors().add( factor );
        return factor;
    }

    /**
     * Create and assign a continuous factor to all the samples of an experiment.
     */
    public static ExperimentalFactor randomContinuousFactor( ExpressionExperiment ee, String name ) {
        Assert.notNull( ee.getExperimentalDesign() );
        return randomContinuousFactor( ee.getExperimentalDesign(), getSampleUsed( ee ), name );
    }

    /**
     * Create and assign a continuous factor.
     */
    public static ExperimentalFactor randomContinuousFactor( ExperimentalDesign design, Collection<BioMaterial> samples, String name ) {
        ExperimentalFactor factor = ExperimentalFactor.Factory.newInstance( name, FactorType.CONTINUOUS );
        for ( BioMaterial sample : samples ) {
            FactorValue fv = FactorValue.Factory.newInstance( factor, randomMeasurement() );
            factor.getFactorValues().add( fv );
            sample.getFactorValues().add( fv );
        }
        factor.setExperimentalDesign( design );
        design.getExperimentalFactors().add( factor );
        return factor;
    }

    private static Measurement randomMeasurement() {
        Measurement measurement = new Measurement();
        measurement.setType( MeasurementType.ABSOLUTE );
        measurement.setRepresentation( PrimitiveType.DOUBLE );
        measurement.setValueAsDouble( random().nextDouble() );
        return measurement;
    }

    private static Collection<BioMaterial> getSampleUsed( ExpressionExperiment ee ) {
        return ee.getBioAssays().stream().map( BioAssay::getSampleUsed ).collect( Collectors.toList() );
    }
}
