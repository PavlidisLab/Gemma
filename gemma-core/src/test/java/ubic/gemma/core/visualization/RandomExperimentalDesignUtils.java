package ubic.gemma.core.visualization;

import ubic.gemma.model.common.measurement.Measurement;
import ubic.gemma.model.common.measurement.MeasurementType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.experiment.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generate random experimental design for a given experiment.
 */
public class RandomExperimentalDesignUtils {

    private static final Random random = new Random();

    public static void setSeed( long seed ) {
        random.setSeed( seed );
    }

    public static ExperimentalDesign randomExperimentalDesign( ExpressionExperiment ee, int numFactors ) {
        ExperimentalDesign design = new ExperimentalDesign();
        for ( int i = 0; i < numFactors; i++ ) {
            if ( random.nextBoolean() ) {
                design.getExperimentalFactors().add( randomCategoricalFactor( ee, random.nextInt( 5 ) + 2 ) );
            } else {
                design.getExperimentalFactors().add( randomContinuousFactor( ee ) );
            }
        }
        return new ExperimentalDesign();
    }

    public static ExperimentalDesign randomSingleCellExperimentalDesign( ExpressionExperiment ee, int numFactors ) {
        return new ExperimentalDesign();
    }

    public static ExperimentalFactor randomCategoricalFactor( ExpressionExperiment ee, int numValues ) {
        ExperimentalFactor factor = ExperimentalFactor.Factory.newInstance();
        List<FactorValue> fvs = new ArrayList<>( numValues );
        for ( int i = 0; i < numValues; i++ ) {
            fvs.add( FactorValue.Factory.newInstance( factor ) );
        }
        factor.getFactorValues().addAll( fvs );
        for ( BioAssay ba : ee.getBioAssays() ) {
            ba.getSampleUsed().getFactorValues().add( fvs.get( random.nextInt( fvs.size() ) ) );
        }
        return factor;
    }

    public static ExperimentalFactor randomContinuousFactor( ExpressionExperiment ee ) {
        ExperimentalFactor factor = ExperimentalFactor.Factory.newInstance( "factor", FactorType.CONTINUOUS );
        for ( BioAssay ba : ee.getBioAssays() ) {
            FactorValue fv = FactorValue.Factory.newInstance( factor, randomMeasurement() );
            factor.getFactorValues().add( fv );
            ba.getSampleUsed().getFactorValues().add( fv );
        }
        return factor;
    }

    private static Measurement randomMeasurement() {
        Measurement measurement = new Measurement();
        measurement.setType( MeasurementType.ABSOLUTE );
        measurement.setRepresentation( PrimitiveType.DOUBLE );
        measurement.setValueAsDouble( random.nextDouble() );
        return measurement;
    }

    public static ExperimentalFactor randomCellTypeFactor( CellTypeAssignment cellTypeAssignment ) {
        return ExperimentalFactor.Factory.newInstance( "cell type", FactorType.CATEGORICAL );
    }
}
