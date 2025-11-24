package ubic.gemma.core.analysis.preprocess.filter;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.util.Assert;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Filter that removes samples that do not meet a minimum number of cells requirement.
 * <p>
 * DEA does not deal well with slicing, so we mask out the entire sample by setting all its values to {@link Double#NaN}.
 * <p>
 * The filter is triggered if at least one {@link BioAssay} has its {@link BioAssay#getNumberOfCells()} populated. Samples with no
 *
 * @author poirigui
 */
@CommonsLog
public class MinimumCellsFilter implements ExpressionDataFilter<ExpressionDataDoubleMatrix> {

    public static final int DEFAULT_MINIMUM_NUMBER_OF_CELLS = 100;

    private int minimumNumberOfCells = DEFAULT_MINIMUM_NUMBER_OF_CELLS;

    public MinimumCellsFilter() {

    }

    public MinimumCellsFilter( int minimumNumberOfCells ) {
        Assert.isTrue( minimumNumberOfCells >= 0, "Minimum number of cells must be zero or greater." );
        this.minimumNumberOfCells = minimumNumberOfCells;
    }

    @Override
    public ExpressionDataDoubleMatrix filter( ExpressionDataDoubleMatrix dataMatrix ) throws FilteringException {
        if ( minimumNumberOfCells == 0 ) {
            return dataMatrix;
        }

        // for multi-assay data, we need to sum the number of cells across all assays for a given sample
        Map<BioMaterial, Integer> numberOfCellsBySample = new HashMap<>();
        for ( int i = 0; i < dataMatrix.columns(); i++ ) {
            BioMaterial sample = dataMatrix.getBioMaterialForColumn( i );
            for ( BioAssay ba : dataMatrix.getBioAssaysForColumn( i ) ) {
                if ( ba.getNumberOfCells() != null ) {
                    int numberOfCells = ba.getNumberOfCells();
                    numberOfCellsBySample.compute( sample, ( v, prev ) -> prev != null ? prev + numberOfCells : numberOfCells );
                }
            }
        }

        if ( numberOfCellsBySample.isEmpty() ) {
            log.debug( "None of the assays have cell counts; skipping minimum cells filtering." );
            return dataMatrix;
        }

        // identify samples that do not meet the minimum number of cells requirement
        Set<BioMaterial> filteredSamples = new HashSet<>();
        for ( int i = 0; i < dataMatrix.columns(); i++ ) {
            BioMaterial sample = dataMatrix.getBioMaterialForColumn( i );
            Integer numberOfCells = numberOfCellsBySample.get( sample );
            if ( numberOfCells == null ) {
                throw new FilteringException( "Single-cell metadata are not populated for the assay(s) of " + sample + ": " + dataMatrix.getBioAssaysForColumn( i ) + "; cannot proceed with filtering." );
            }
            if ( numberOfCells < minimumNumberOfCells ) {
                log.warn( "Excluding " + sample + " from DEA because it has only " + numberOfCells + " cells (minimum required: " + minimumNumberOfCells + ")." );
                filteredSamples.add( sample );
            }
        }

        if ( filteredSamples.size() == dataMatrix.columns() ) {
            throw new InsufficientSamplesException( "All assays were filtered out because they did not meet the minimum number of cells requirement." );
        } else if ( !filteredSamples.isEmpty() ) {
            // FIXME: use a slice instead, but  the DiffEx analyzer needs to be changed to slice the design matrix too
            DoubleMatrix<CompositeSequence, BioMaterial> maskedMatrix = dataMatrix.getMatrix().copy();
            for ( int j = 0; j < dataMatrix.columns(); j++ ) {
                BioMaterial sample = dataMatrix.getBioMaterialForColumn( j );
                if ( filteredSamples.contains( sample ) ) {
                    for ( int i = 0; i < dataMatrix.rows(); i++ ) {
                        maskedMatrix.set( i, j, Double.NaN );
                    }
                }
            }
            return new ExpressionDataDoubleMatrix( dataMatrix, maskedMatrix );
        } else {
            log.info( "All samples meet the minimum number of cells requirement of " + minimumNumberOfCells + " ." );
            return dataMatrix;
        }
    }

    @Override
    public boolean appliesTo( ExpressionDataDoubleMatrix dataMatrix ) {
        if ( minimumNumberOfCells == 0 ) {
            return false;
        }
        // check if at least one BioAssay has its number of cells populated
        for ( int j = 0; j < dataMatrix.columns(); j++ ) {
            for ( BioAssay ba : dataMatrix.getBioAssaysForColumn( j ) ) {
                if ( ba.getNumberOfCells() != null ) {
                    return true;
                }
            }
        }
        return false;
    }

    public void setMinimumNumberOfCells( int minimumNumberOfCells ) {
        Assert.isTrue( minimumNumberOfCells >= 0, "The minimum number of cells must be zero or greater." );
        this.minimumNumberOfCells = minimumNumberOfCells;
    }

    @Override
    public String toString() {
        return String.format( "MinimumCellsFilter Minimum Number Of Cells=%d", minimumNumberOfCells );
    }
}
