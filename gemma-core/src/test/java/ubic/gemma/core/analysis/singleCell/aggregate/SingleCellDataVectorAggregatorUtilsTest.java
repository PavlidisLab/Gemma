package ubic.gemma.core.analysis.singleCell.aggregate;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.Test;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.*;
import ubic.gemma.persistence.service.expression.bioAssayData.RandomSingleCellDataUtils;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static ubic.gemma.core.analysis.singleCell.aggregate.SingleCellDataVectorAggregatorUtils.aggregate;

public class SingleCellDataVectorAggregatorUtilsTest {

    @Test
    public void testAggregate() {
        RandomSingleCellDataUtils.setSeed( 123L );
        List<SingleCellExpressionDataVector> vectors = RandomSingleCellDataUtils.randomSingleCellVectors();
        SingleCellDimension scd = vectors.iterator().next().getSingleCellDimension();
        CellTypeAssignment cta = RandomSingleCellDataUtils.randomCellTypeAssignment( scd, 10, 0.0 );
        Collection<RawExpressionDataVector> aggregatedVectors = aggregate( vectors, SingleCellDataVectorAggregatorUtils.SingleCellAggregationMethod.SUM, cta, false );
        assertThat( aggregatedVectors )
                .hasSize( 100 )
                .extracting( RawExpressionDataVector::getDataAsDoubles )
                .first()
                .asInstanceOf( InstanceOfAssertFactories.DOUBLE_ARRAY )
                .hasSize( 40 )
                .containsExactly( 49.0, 64.0, 99.0, 53.0, 38.0, 42.0, 48.0, 79.0, 68.0, 19.0, 69.0, 48.0, 69.0, 78.0, 96.0, 18.0, 67.0, 24.0, 94.0, 35.0, 73.0, 61.0, 44.0, 39.0, 38.0, 30.0, 34.0, 105.0, 67.0, 74.0, 67.0, 99.0, 38.0, 80.0, 49.0, 76.0, 69.0, 31.0, 30.0, 76.0 );

        double[] expectedCounts = new double[4 * 10];
        SingleCellExpressionDataVector firstVector = vectors.iterator().next();
        double[] data = firstVector.getDataAsDoubles();
        for ( int i = 0; i < data.length; i++ ) {
            int cellIndex = firstVector.getDataIndices()[i];
            int sampleIndex = scd.getBioAssays().indexOf( scd.getBioAssay( cellIndex ) );
            int cellTypeIndex = cta.getCellTypeIndices()[cellIndex];
            if ( cellTypeIndex != -1 ) {
                expectedCounts[10 * sampleIndex + cellTypeIndex] += data[i];
            }
        }
        assertThat( aggregatedVectors.iterator().next().getDataAsDoubles() )
                .containsExactly( expectedCounts );

        double[] expectedLibrarySizes = new double[scd.getBioAssays().size()];
        for ( SingleCellExpressionDataVector vector : vectors ) {
            for ( int i = 0; i < vector.getSingleCellDimension().getBioAssays().size(); i++ ) {
                double[] sampleData = SingleCellExpressionDataVectorUtils.getSampleDataAsDoubles( vector, i );
                for ( double d : sampleData ) {
                    expectedLibrarySizes[i] += d;
                }
            }
        }
        double[] actualLibrarySizes = new double[scd.getBioAssays().size()];
        for ( RawExpressionDataVector vector : aggregatedVectors ) {
            for ( int i = 0; i < vector.getBioAssayDimension().getBioAssays().size(); i++ ) {
                double[] sampleData = vector.getDataAsDoubles();
                BioAssay sourceBioAssay = vector.getBioAssayDimension().getBioAssays().get( i ).getSampleUsed().getSourceBioMaterial().getBioAssaysUsedIn().iterator().next();
                actualLibrarySizes[scd.getBioAssays().indexOf( sourceBioAssay )] += sampleData[i];
            }
        }
        assertThat( actualLibrarySizes ).isEqualTo( expectedLibrarySizes );
    }

    @Test
    public void testAggregateWithUnknown() {
        RandomSingleCellDataUtils.setSeed( 123L );
        List<SingleCellExpressionDataVector> vectors = RandomSingleCellDataUtils.randomSingleCellVectors();
        SingleCellDimension scd = vectors.iterator().next().getSingleCellDimension();
        CellTypeAssignment cta = RandomSingleCellDataUtils.randomCellTypeAssignment( scd, 10, 0.1 );
        Collection<RawExpressionDataVector> aggregatedVectors = aggregate( vectors, SingleCellDataVectorAggregatorUtils.SingleCellAggregationMethod.SUM, cta, true );
        assertThat( aggregatedVectors )
                .hasSize( 100 )
                .extracting( RawExpressionDataVector::getDataAsDoubles )
                .first()
                .asInstanceOf( InstanceOfAssertFactories.DOUBLE_ARRAY )
                .hasSize( 44 )
                .containsExactly( 49.0, 38.0, 47.0, 41.0, 22.0, 104.0, 57.0, 46.0, 40.0, 54.0, 61.0, 25.0, 102.0, 66.0, 60.0, 82.0, 43.0, 85.0, 47.0, 31.0, 42.0, 15.0, 86.0, 28.0, 40.0, 30.0, 51.0, 52.0, 39.0, 43.0, 43.0, 88.0, 65.0, 40.0, 42.0, 90.0, 21.0, 64.0, 44.0, 49.0, 95.0, 53.0, 59.0, 58.0 );

        double[] expectedCounts = new double[4 * 11];
        SingleCellExpressionDataVector firstVector = vectors.iterator().next();
        double[] data = firstVector.getDataAsDoubles();
        for ( int i = 0; i < data.length; i++ ) {
            int cellIndex = firstVector.getDataIndices()[i];
            int sampleIndex = scd.getBioAssays().indexOf( scd.getBioAssay( cellIndex ) );
            int cellTypeIndex = cta.getCellTypeIndices()[cellIndex];
            if ( cellTypeIndex != -1 ) {
                expectedCounts[11 * sampleIndex + cellTypeIndex] += data[i];
            } else {
                expectedCounts[11 * sampleIndex + 10] += data[i]; // unknown
            }
        }
        assertThat( aggregatedVectors.iterator().next().getDataAsDoubles() )
                .containsExactly( expectedCounts );
    }
}