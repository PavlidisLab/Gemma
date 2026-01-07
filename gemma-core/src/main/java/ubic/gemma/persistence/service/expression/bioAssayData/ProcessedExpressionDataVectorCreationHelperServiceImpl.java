package ubic.gemma.persistence.service.expression.bioAssayData;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.core.analysis.preprocess.convert.QuantitationTypeConversionException;
import ubic.gemma.core.analysis.preprocess.detect.QuantitationTypeDetectionException;
import ubic.gemma.core.analysis.preprocess.normalize.QuantileNormalizer;
import ubic.gemma.core.analysis.preprocess.slice.BulkDataSlicerUtils;
import ubic.gemma.core.datastructure.matrix.BulkExpressionDataMatrixUtils;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.*;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

import static ubic.gemma.core.analysis.preprocess.convert.QuantitationTypeConversionUtils.ensureLog2Scale;

@CommonsLog
@Service
class ProcessedExpressionDataVectorCreationHelperServiceImpl implements ProcessedExpressionDataVectorCreationHelperService {

    /**
     * Suffix added to the name of the quantitation type used for processed data vectors.
     */
    private static final String PROCESSED_DATA_NAME_SUFFIX = " - Processed version";

    private static final String PROCESSED_DATA_DESCRIPTION = "Processed data (as per Gemma) for analysis, based on the preferred quantitation type raw data";

    /**
     * Don't attempt to renormalize data that is smaller than this. This avoids unnecessary normalization in tests, and
     * in data sets where normalization is more likely to harm than good.
     */
    private static final int MIN_SIZE_FOR_RENORMALIZATION = 4000;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private QuantitationTypeService quantitationTypeService;

    @Override
    @Transactional(rollbackFor = { QuantitationTypeDetectionException.class, QuantitationTypeConversionException.class })
    public QuantitationType createProcessedDataVectors( ExpressionExperiment expressionExperiment, boolean ignoreQuantitationMismatch, ProcessedExpressionDataVectorCreationSummary summary ) throws QuantitationTypeDetectionException, QuantitationTypeConversionException {
        log.info( "Removing processed expression vectors for " + expressionExperiment + "..." );
        expressionExperimentService.removeProcessedDataVectors( expressionExperiment );

        log.info( "Computing processed expression vectors for " + expressionExperiment );

        Collection<RawExpressionDataVector> rawPreferredDataVectors = expressionExperimentService.getPreferredRawDataVectors( expressionExperiment );
        if ( rawPreferredDataVectors.isEmpty() ) {
            throw new IllegalArgumentException( "No preferred data vectors for " + expressionExperiment );
        }

        rawPreferredDataVectors = removeDuplicateElements( rawPreferredDataVectors );

        /* log-transform if necessary */
        // this will also consolidate sets of raw vectors that have multiple BADs
        rawPreferredDataVectors = consolidateAndLogTransformVectors( expressionExperiment, rawPreferredDataVectors, ignoreQuantitationMismatch );

        // once the vectors have been consolidated, we can recover the dimension
        // no that if multiple BADs were consolidated, this will return a new BAD, otherwise the same BAD that was used
        // for the raw vectors will be re-used
        // create a masked QT based on the preferred raw vectors once all the necessary transformation have been done
        RawExpressionDataVector preferredDataVectorExemplar = rawPreferredDataVectors.iterator().next();
        QuantitationType preferredQt = preferredDataVectorExemplar.getQuantitationType();
        BioAssayDimension dimension = preferredDataVectorExemplar.getBioAssayDimension();

        summary.setRawQuantitationType( preferredQt );

        QuantitationType processedQt = createPreferredMaskedDataQuantitationType( preferredQt );

        Map<CompositeSequence, int[]> numberOfCells = rawPreferredDataVectors.stream()
                .filter( v -> v.getNumberOfCells() != null )
                .collect( Collectors.toMap(
                        DesignElementDataVector::getDesignElement,
                        RawExpressionDataVector::getNumberOfCells ) );

        Map<CompositeSequence, double[]> preferredData = unpackData( rawPreferredDataVectors, dimension );

        boolean isTwoChannel = expressionExperimentService.isTwoChannel( expressionExperiment );
        Collection<RawExpressionDataVector> missingValueVectors = getMissingValueVectors( expressionExperiment );
        if ( isTwoChannel && missingValueVectors != null ) {
            maskMissingValues( preferredData, missingValueVectors, dimension, summary );
        }

        maskOutliers( preferredData, dimension, summary );

        /*
         * Note that we used to not normalize count data, but we've removed this restriction; and in any case we have
         * moved to using non-count summaries for the primary data type.
         */
        if ( processedQt.getType().equals( StandardQuantitationType.COUNT ) ) {
            /*
             * Backfill target
             */
            log.warn( "Preferred data are counts; please convert to log2cpm" );
        }

        if ( processedQt.getIsRatio() ) {
            String m = "Data is on a ratio scale, skipping normalization step.";
            log.info( m );
            summary.addComment( m );
        } else if ( preferredData.size() < MIN_SIZE_FOR_RENORMALIZATION ) {
            String m = "Not enough data vectors (" + preferredData.size() + ") to perform normalization.";
            log.info( m );
            summary.addComment( m );
        } else {
            log.info( "Normalizing the data" );
            quantileNormalize( preferredData );
            processedQt.setIsNormalized( true );
            summary.setQuantileNormalized( true );
        }

        processedQt = quantitationTypeService.create( processedQt, ProcessedExpressionDataVector.class );

        /*
         * Done with processing, now build the vectors and persist; Do a sanity check that we don't have more than we
         * should
         */
        Collection<ProcessedExpressionDataVector> newVectors = new HashSet<>( preferredData.size() );
        for ( Map.Entry<CompositeSequence, double[]> e : preferredData.entrySet() ) {
            ProcessedExpressionDataVector vec = ProcessedExpressionDataVector.Factory.newInstance();
            vec.setExpressionExperiment( expressionExperiment );
            vec.setQuantitationType( processedQt );
            vec.setBioAssayDimension( dimension );
            vec.setDesignElement( e.getKey() );
            vec.setDataAsDoubles( e.getValue() );
            vec.setNumberOfCells( numberOfCells.get( e.getKey() ) );
            newVectors.add( vec );
        }

        log.info( String.format( "Persisting %d processed data vectors...",
                newVectors.size() ) );

        int createdVectors = expressionExperimentService.createProcessedDataVectors( expressionExperiment, newVectors );

        summary.setNumberOfDataVectors( createdVectors );
        log.info( String.format( "Persisted %d processed data vectors.", createdVectors ) );

        return processedQt;
    }

    /**
     * Consolidate raw vectors that have multiple BADs and log-transform them if necessary.
     * <p>
     * The consolidation is done by passing the vectors through {@link ExpressionDataDoubleMatrix} which handle multiple
     * assays per sample and then recover them on the other side.
     */
    private Collection<RawExpressionDataVector> consolidateAndLogTransformVectors(
            ExpressionExperiment ee,
            Collection<RawExpressionDataVector> rawPreferredDataVectors,
            boolean ignoreQuantitationMismatch ) throws QuantitationTypeDetectionException, QuantitationTypeConversionException {
        ExpressionDataDoubleMatrix matrix = new ExpressionDataDoubleMatrix( ee, rawPreferredDataVectors );
        matrix = ensureLog2Scale( matrix, ignoreQuantitationMismatch );
        return BulkExpressionDataMatrixUtils.toVectors( matrix, RawExpressionDataVector.class );
    }

    @Nullable
    private Collection<RawExpressionDataVector> getMissingValueVectors( ExpressionExperiment ee ) {
        Map<QuantitationType, Collection<RawExpressionDataVector>> mvv = expressionExperimentService.getMissingValuesVectors( ee );
        if ( mvv.isEmpty() ) {
            return null;
        } else if ( mvv.size() > 1 ) {
            throw new IllegalStateException( ee + " has more than one set of missing value vectors: " + mvv.keySet() );
        } else {
            return mvv.values().iterator().next();
        }
    }

    /**
     * Create a quantitation type for attaching to the new processed data.
     * <p>
     * The {@code normalized} flag on the original QT is ignored because there is no guarantee that the data has been
     * quantile-normalized. The description is set to a constant value. All remaining properties are copied as per
     * {@link QuantitationType.Factory#newInstance(QuantitationType)}.
     */
    private QuantitationType createPreferredMaskedDataQuantitationType( QuantitationType preferredQt ) {
        QuantitationType processedQt = QuantitationType.Factory.newInstance( preferredQt );
        processedQt.setName( preferredQt.getName() + PROCESSED_DATA_NAME_SUFFIX );
        processedQt.setDescription( PROCESSED_DATA_DESCRIPTION );
        // we do not copy the normalized flag from raw QTs because it does not guarantee to mean it is quantile-normalized
        processedQt.setIsNormalized( false );
        processedQt.setIsMaskedPreferred( true );
        return processedQt;
    }

    /**
     * Remove duplicate elements from a collection of raw vectors.
     * <p>
     * If no duplicate elements are found, the original collection is returned.
     */
    private Collection<RawExpressionDataVector> removeDuplicateElements( Collection<RawExpressionDataVector> rawPreferredDataVectors ) {
        /*
         * Remove rows that are duplicates for the same design element. This can happen for data sets that were merged.
         * We arbitrarily throw one out.
         */
        int maxWarn = 10;
        int warned = 0;
        Set<CompositeSequence> seenDes = new HashSet<>();
        Collection<RawExpressionDataVector> toRemove = new HashSet<>();
        for ( RawExpressionDataVector rdv : rawPreferredDataVectors ) {
            CompositeSequence de = rdv.getDesignElement();

            if ( seenDes.contains( de ) ) {
                if ( warned <= maxWarn ) {
                    log.info( "Duplicate vector for: " + de );
                    warned++;
                }
                if ( warned == maxWarn ) {
                    log.info( "Further warnings skipped" );
                }
                toRemove.add( rdv );
            }
            seenDes.add( de );
        }

        if ( toRemove.isEmpty() ) {
            log.info( "No duplicate elements found, returning all raw vectors." );
            return rawPreferredDataVectors;
        } else {
            Set<RawExpressionDataVector> result = new HashSet<>( rawPreferredDataVectors );
            result.removeAll( toRemove );
            log.info( String.format( "Removed %d duplicate elements, %d remain.", toRemove.size(), rawPreferredDataVectors.size() ) );
            return result;
        }
    }

    /**
     * Mask missing values. This is mostly for two-color (ratiometric) data.
     *
     * @param dimension dimension to target when unpacking data
     */
    private Map<CompositeSequence, double[]> unpackData( Collection<RawExpressionDataVector> preferredData, BioAssayDimension dimension ) {
        ArrayList<BulkExpressionDataVector> orderedVectors = new ArrayList<>( preferredData );
        List<double[]> sliced = BulkDataSlicerUtils.sliceDoubles( orderedVectors, dimension.getBioAssays(), false );
        Map<CompositeSequence, double[]> result = new HashMap<>( preferredData.size() );
        for ( int i = 0; i < orderedVectors.size(); i++ ) {
            result.put( orderedVectors.get( i ).getDesignElement(), sliced.get( i ) );
        }
        return result;
    }

    private void maskMissingValues( Map<CompositeSequence, double[]> unpackedData, Collection<RawExpressionDataVector> missingValueData, BioAssayDimension dimension, ProcessedExpressionDataVectorCreationSummary summary ) {
        Assert.isTrue( !missingValueData.isEmpty(), "At least one missing values vector is required." );
        int maskedValues = 0;
        Map<CompositeSequence, boolean[]> missingValueMap = unpackBooleans( missingValueData, dimension );
        boolean warned = false;
        for ( Map.Entry<CompositeSequence, double[]> rv : unpackedData.entrySet() ) {
            CompositeSequence de = rv.getKey();
            double[] data = rv.getValue();
            boolean[] mvData = missingValueMap.get( de );
            if ( mvData == null ) {
                if ( !warned && log.isWarnEnabled() )
                    log.warn( "No mask vector for " + de
                            + ", additional warnings for missing masks for this job will be skipped" );
                // we're missing a mask vector for it for some reason, but still flag it as effectively masked.
                warned = true;
                continue;
            }

            if ( mvData.length != data.length ) {
                throw new IllegalStateException( "Missing value data didn't match data length" );
            }
            for ( int i = 0; i < data.length; i++ ) {
                if ( !mvData[i] ) {
                    data[i] = Double.NaN;
                    maskedValues++;
                }
            }
        }
        summary.setNumberOfMaskedMissingValues( maskedValues );
    }

    private Map<CompositeSequence, boolean[]> unpackBooleans( Collection<? extends BulkExpressionDataVector> vectors, BioAssayDimension dimension ) {
        ArrayList<BulkExpressionDataVector> orderedVectors = new ArrayList<>( vectors );
        List<boolean[]> sliced = BulkDataSlicerUtils.sliceBooleans( orderedVectors, dimension.getBioAssays(), false );
        Map<CompositeSequence, boolean[]> result = new HashMap<>( vectors.size() );
        for ( int i = 0; i < orderedVectors.size(); i++ ) {
            result.put( orderedVectors.get( i ).getDesignElement(), sliced.get( i ) );
        }
        return result;
    }

    private void maskOutliers( Map<CompositeSequence, double[]> data, BioAssayDimension dimension, ProcessedExpressionDataVectorCreationSummary summary ) {
        List<Integer> outliers = new ArrayList<>();
        List<BioAssay> bioAssays = dimension.getBioAssays();
        for ( int i = 0; i < bioAssays.size(); i++ ) {
            BioAssay ba = bioAssays.get( i );
            if ( ba.getIsOutlier() ) {
                outliers.add( i );
            }
        }
        if ( outliers.isEmpty() ) {
            log.info( "There are no outlier assays to mask." );
            return;
        }
        int[] outliersI = ArrayUtils.toPrimitive( outliers.toArray( new Integer[0] ) );
        log.info( "There are " + outliers.size() + " outlier assays; masking their values in processed data." );
        for ( double[] vector : data.values() ) {
            for ( int k : outliersI ) {
                vector[k] = Double.NaN;
            }
        }
        summary.setNumberOfMaskedOutliers( outliers.size() );
    }

    /**
     * Quantile normalize data. This should be one of the last steps in processing before persisting
     */
    private void quantileNormalize( Map<CompositeSequence, double[]> vectors ) {
        Assert.isTrue( vectors.size() >= MIN_SIZE_FOR_RENORMALIZATION,
                "At least " + MIN_SIZE_FOR_RENORMALIZATION + " vector are required for renormalization." );

        int cols = vectors.values().iterator().next().length;
        int rows = vectors.size();
        DoubleMatrix<CompositeSequence, Integer> mat = new DenseDoubleMatrix<>( rows, cols );
        for ( int i = 0; i < cols; i++ ) {
            mat.setColumnName( i, i );
        }

        int i = 0;
        for ( Map.Entry<CompositeSequence, double[]> c : vectors.entrySet() ) {
            CompositeSequence designElement = c.getKey();
            double[] data = c.getValue();
            if ( data.length != cols ) {
                throw new IllegalStateException( "Unexpected vector length for design element " + designElement + "." );
            }
            for ( int j = 0; j < cols; j++ ) {
                mat.set( i, j, data[j] );
            }
            mat.setRowName( designElement, i );
            i++;
        }

        assert mat.columns() == cols;
        assert mat.rows() == rows;

        DoubleMatrix<CompositeSequence, Integer> normalizedMat = new QuantileNormalizer<CompositeSequence, Integer>()
                .normalize( mat );

        assert normalizedMat.columns() == cols;
        assert normalizedMat.rows() == rows;

        // rewrite the vectors with normalized data
        for ( i = 0; i < rows; i++ ) {
            CompositeSequence c = normalizedMat.getRowName( i );
            double[] vector = vectors.get( c );
            for ( int j = 0; j < cols; j++ ) {
                vector[j] = normalizedMat.get( i, j );
            }
        }
    }
}
