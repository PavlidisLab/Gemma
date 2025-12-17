package ubic.gemma.core.analysis.preprocess.filter;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.util.Assert;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Filter that removes samples that do not meet a minimum number of cells requirement.
 * <p>
 * The requirement can be set at both sample and gene-level. When set at sample-level, we consider the total number of
 * cells across all assays.
 * <p>
 * DEA does not deal well with slicing, so we mask out the entire sample by setting all its values to {@link Double#NaN}.
 * <p>
 * The filter is triggered if {@link ExpressionDataDoubleMatrix#getNumberOfCellsForRow(int)} is populated or if at least
 * one {@link BioAssay} has its {@link BioAssay#getNumberOfCells()} populated or if
 *
 * @author poirigui
 */
@CommonsLog
public class MinimumCellsFilter implements ExpressionDataFilter<ExpressionDataDoubleMatrix> {

    public static final int
            DEFAULT_MINIMUM_NUMBER_OF_CELLS_PER_SAMPLE = 100,
            DEFAULT_MINIMUM_NUMBER_OF_CELLS_PER_GENE = 3;

    private int minimumNumberOfCellsPerSample = DEFAULT_MINIMUM_NUMBER_OF_CELLS_PER_SAMPLE;

    private int minimumNumberOfCellsPerGene = DEFAULT_MINIMUM_NUMBER_OF_CELLS_PER_GENE;

    /**
     * Set this to true to slice columns instead of masking them.
     */
    private boolean allowSlicingColumns = false;

    public MinimumCellsFilter() {

    }

    @Override
    public ExpressionDataDoubleMatrix filter( ExpressionDataDoubleMatrix dataMatrix ) throws FilteringException {
        dataMatrix = filterBySample( dataMatrix );
        dataMatrix = filterByGene( dataMatrix );
        return dataMatrix;
    }

    private ExpressionDataDoubleMatrix filterBySample( ExpressionDataDoubleMatrix dataMatrix ) throws FilteringException {
        if ( minimumNumberOfCellsPerSample == 0 ) {
            return dataMatrix;
        }

        Map<BioMaterial, Integer> numberOfCellsBySample = new HashMap<>();

        // preferably, use what's declared in the data matrix
        int[][] numberOfCells = dataMatrix.getNumberOfCells();
        if ( numberOfCells != null ) {
            log.info( "Using the number of cells defined in the data matrix." );
            int[] noc = new int[dataMatrix.columns()];
            for ( int i = 0; i < dataMatrix.rows(); i++ ) {
                for ( int j = 0; j < dataMatrix.columns(); j++ ) {
                    noc[j] += numberOfCells[i][j];
                }
            }
            for ( int j = 0; j < dataMatrix.columns(); j++ ) {
                BioMaterial bm = dataMatrix.getBioMaterialForColumn( j );
                numberOfCellsBySample.put( bm, noc[j] );
            }
        }

        // for preferred data, the values are stored at the BioAssay-level
        else if ( dataMatrix.getQuantitationType().isPreferred( RawExpressionDataVector.class )
                || dataMatrix.getQuantitationType().isPreferred( ProcessedExpressionDataVector.class ) ) {
            for ( int j = 0; j < dataMatrix.columns(); j++ ) {
                BioMaterial sample = dataMatrix.getBioMaterialForColumn( j );
                // for multi-assay data, we need to sum the number of cells across all assays for a given sample
                for ( BioAssay ba : dataMatrix.getBioAssaysForColumn( j ) ) {
                    if ( ba.getNumberOfCells() != null ) {
                        int noc = ba.getNumberOfCells();
                        numberOfCellsBySample.compute( sample, ( v, prev ) -> prev != null ? prev + noc : noc );
                    }
                }
            }
            if ( !numberOfCellsBySample.isEmpty() ) {
                log.info( "Using the number of cells stored at the assay-level." );
            } else {
                log.debug( "None of the samples have assay(s) with cell counts; skipping minimum cells filtering by sample." );
                return dataMatrix;
            }
        }

        // non-preferred data; cannot get cell counts at the BioAssay-level
        else {
            log.debug( "None of the samples have assay(s) with cell counts; skipping minimum cells filtering by sample." );
            return dataMatrix;
        }

        // identify samples that do not meet the minimum number of cells requirement
        List<BioMaterial> keptSamples = new ArrayList<>( dataMatrix.columns() );
        for ( int i = 0; i < dataMatrix.columns(); i++ ) {
            BioMaterial sample = dataMatrix.getBioMaterialForColumn( i );
            Integer noc = numberOfCellsBySample.get( sample );
            if ( noc == null ) {
                throw new FilteringException( "Single-cell metadata are not populated for the assay(s) of " + sample + ": " + dataMatrix.getBioAssaysForColumn( i ) + "; cannot proceed with filtering." );
            }
            if ( noc >= minimumNumberOfCellsPerSample ) {
                keptSamples.add( sample );
            } else {
                log.warn( "Excluding " + sample + " because it has only " + noc + " cells (minimum required: " + minimumNumberOfCellsPerSample + ")." );
            }
        }

        if ( keptSamples.isEmpty() ) {
            throw new NoSamplesException( "All samples were filtered out because they did not meet the minimum number of cells requirement." );
        } else if ( keptSamples.size() < dataMatrix.columns() ) {
            if ( allowSlicingColumns ) {
                return dataMatrix.sliceColumns( keptSamples );
            } else {
                DoubleMatrix<CompositeSequence, BioMaterial> maskedMatrix = dataMatrix.asDoubleMatrix();
                for ( int j = 0; j < dataMatrix.columns(); j++ ) {
                    BioMaterial sample = dataMatrix.getBioMaterialForColumn( j );
                    if ( !keptSamples.contains( sample ) ) {
                        for ( int i = 0; i < dataMatrix.rows(); i++ ) {
                            maskedMatrix.set( i, j, Double.NaN );
                        }
                    }
                }
                // this transformation does not alter QTs
                return dataMatrix.withMatrix( maskedMatrix );
            }
        } else {
            log.info( "All samples meet the minimum number of cells requirement of " + minimumNumberOfCellsPerSample + " ." );
            return dataMatrix;
        }
    }

    private ExpressionDataDoubleMatrix filterByGene( ExpressionDataDoubleMatrix dataMatrix ) throws FilteringException {
        if ( minimumNumberOfCellsPerGene == 0 ) {
            return dataMatrix;
        }

        if ( dataMatrix.getNumberOfCells() == null ) {
            log.debug( "Data matrix does not have cell counts; skipping minimum cells filtering by gene." );
            return dataMatrix;
        }

        List<CompositeSequence> keptDesignElements = new ArrayList<>( dataMatrix.rows() );

        int numWarns = 0;
        int[][] numberOfCells = dataMatrix.getNumberOfCells();
        for ( int i = 0; i < dataMatrix.rows(); i++ ) {
            CompositeSequence designElement = dataMatrix.getDesignElementForRow( i );
            if ( numberOfCells[i] == null ) {
                throw new FilteringException( designElement + " does not have cell counts populated." );
            }
            int numberOfCellsForGene = 0;
            for ( int j = 0; j < numberOfCells[i].length; j++ ) {
                if ( Double.isNaN( dataMatrix.getAsDouble( i, j ) ) ) {
                    // do not count cells from masked values (i.e. if the sample has too few cells or if the OutliersFilter has been previously applied)
                    continue;
                }
                numberOfCellsForGene += numberOfCells[i][j];
            }
            if ( numberOfCellsForGene >= minimumNumberOfCellsPerGene ) {
                keptDesignElements.add( designElement );
            } else {
                if ( log.isTraceEnabled() ) {
                    log.trace( "Excluding " + designElement + " because it has only " + numberOfCellsForGene + " cells (minimum required: " + minimumNumberOfCellsPerGene + ")." );
                } else if ( numWarns < 5 ) {
                    log.warn( "Excluding " + designElement + " because it has only " + numberOfCellsForGene + " cells (minimum required: " + minimumNumberOfCellsPerGene + ")." );
                    numWarns++;
                } else if ( numWarns == 5 ) {
                    log.warn( "Further warnings about excluded design elements will be suppressed, enable TRACE logs for " + MinimumCellsFilter.class.getName() + " to see everything." );
                    numWarns++;
                }
            }
        }

        if ( keptDesignElements.isEmpty() ) {
            throw new NoDesignElementsException( "All design elements were filtered out because they did not meet the minimum number of cells requirement." );
        } else if ( keptDesignElements.size() < dataMatrix.rows() ) {
            return dataMatrix.sliceRows( keptDesignElements );
        } else {
            log.info( " All design elements meet the minimum number of cells requirement of " + minimumNumberOfCellsPerGene + " ." );
            return dataMatrix;
        }
    }

    @Override
    public boolean appliesTo( ExpressionDataDoubleMatrix dataMatrix ) {
        return ( minimumNumberOfCellsPerSample > 0 && ( dataMatrix.getNumberOfCells() != null || hasOneOrMoreSamplesWithCellCounts( dataMatrix ) ) ) ||
                ( minimumNumberOfCellsPerGene > 0 && dataMatrix.getNumberOfCells() != null );
    }

    private boolean hasOneOrMoreSamplesWithCellCounts( ExpressionDataDoubleMatrix dataMatrix ) {
        if ( dataMatrix.getQuantitationType().isPreferred( RawExpressionDataVector.class )
                || dataMatrix.getQuantitationType().isPreferred( ProcessedExpressionDataVector.class ) ) {
            // check if at least one BioAssay has its number of cells populated
            for ( int j = 0; j < dataMatrix.columns(); j++ ) {
                for ( BioAssay ba : dataMatrix.getBioAssaysForColumn( j ) ) {
                    if ( ba.getNumberOfCells() != null ) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Sets the minimum number of cells required for a sample to be retained.
     */
    public void setMinimumNumberOfCellsPerSample( int minimumNumberOfCellsPerSample ) {
        Assert.isTrue( minimumNumberOfCellsPerSample >= 0, "The minimum number of cells must be zero or greater." );
        this.minimumNumberOfCellsPerSample = minimumNumberOfCellsPerSample;
    }

    /**
     * Sets the minimum number of cells required for a gene to be retained.
     * <p>
     * Only cells from unmasked samples are counted.
     */
    public void setMinimumNumberOfCellsPerGene( int minimumNumberOfCellsPerGene ) {
        Assert.isTrue( minimumNumberOfCellsPerGene >= 0, "The minimum number of cells must be zero or greater." );
        this.minimumNumberOfCellsPerGene = minimumNumberOfCellsPerGene;
    }

    /**
     * Sets whether to slice columns instead of masking them.
     * <p>
     * The default is to mask them.
     */
    public void setAllowSlicingColumns( boolean allowSlicingColumns ) {
        this.allowSlicingColumns = allowSlicingColumns;
    }

    @Override
    public String toString() {
        return String.format( "MinimumCellsFilter Minimum Number Of Cells Per Sample=%d Minimum Number Of Cells Per Gene=%d",
                minimumNumberOfCellsPerSample, minimumNumberOfCellsPerGene );
    }
}
