package ubic.gemma.persistence.service.expression.bioAssayData;

import lombok.extern.apachecommons.CommonsLog;
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
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssayData.*;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeDao;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentDao;

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
    private ExpressionExperimentDao expressionExperimentDao;

    @Autowired
    private QuantitationTypeDao quantitationTypeDao;

    @Override
    @Transactional(rollbackFor = { QuantitationTypeDetectionException.class, QuantitationTypeConversionException.class })
    public int createProcessedDataVectors( ExpressionExperiment expressionExperiment, boolean ignoreQuantitationMismatch ) throws QuantitationTypeDetectionException, QuantitationTypeConversionException {
        log.info( "Removing processed expression vectors for " + expressionExperiment + "..." );
        expressionExperimentDao.removeProcessedDataVectors( expressionExperiment );

        log.info( "Computing processed expression vectors for " + expressionExperiment );

        Collection<RawExpressionDataVector> rawPreferredDataVectors = expressionExperimentDao.getPreferredRawDataVectors( expressionExperiment );
        if ( rawPreferredDataVectors.isEmpty() ) {
            throw new IllegalArgumentException( "No preferred data vectors for " + expressionExperiment );
        }

        rawPreferredDataVectors = removeDuplicateElements( rawPreferredDataVectors );

        /* log-transform if necessary */
        // this will also consolidate sets of raw vectors that have multiple BADs
        rawPreferredDataVectors = consolidateAndLogTransformVectors( expressionExperiment, rawPreferredDataVectors, ignoreQuantitationMismatch );

        // create a masked QT based on the preferred raw vectors once all the necessary transformation have been done
        RawExpressionDataVector preferredDataVectorExemplar = rawPreferredDataVectors.iterator().next();
        QuantitationType preferredQt = this
                .createPreferredMaskedDataQuantitationType( preferredDataVectorExemplar.getQuantitationType() );

        // once the vectors have been consolidated, we can recover the dimension
        // no that if multiple BADs were consolidated, this will return a new BAD, otherwise the same BAD that was used
        // for the raw vectors will be re-used
        BioAssayDimension dimension = rawPreferredDataVectors.iterator().next().getBioAssayDimension();

        Map<CompositeSequence, int[]> numberOfCells = rawPreferredDataVectors.stream()
                .filter( v -> v.getNumberOfCells() != null )
                .collect( Collectors.toMap(
                        DesignElementDataVector::getDesignElement,
                        RawExpressionDataVector::getNumberOfCells ) );

        // mask raw vectors
        Collection<RawExpressionDataVector> missingValueVectors = new HashSet<>();
        boolean isTwoChannel = this.isTwoChannel( expressionExperiment );
        if ( isTwoChannel ) {
            missingValueVectors = this.getMissingValueVectors( expressionExperiment );
        }
        Map<CompositeSequence, double[]> preferredData = unpackAndMask( rawPreferredDataVectors, missingValueVectors, dimension );

        /*
         * Note that we used to not normalize count data, but we've removed this restriction; and in any case we have
         * moved to using non-count summaries for the primary data type.
         */
        if ( preferredQt.getType().equals( StandardQuantitationType.COUNT ) ) {
            /*
             * Backfill target
             */
            log.warn( "Preferred data are counts; please convert to log2cpm" );
        }

        if ( preferredQt.getIsRatio() ) {
            log.info( "Data is on a ratio scale, skipping normalization step." );
        } else if ( preferredData.size() < MIN_SIZE_FOR_RENORMALIZATION ) {
            log.info( "Not enough data vectors (" + preferredData.size() + ") to perform normalization." );
        } else {
            log.info( "Normalizing the data" );
            preferredData = renormalize( preferredData );
            preferredQt.setIsNormalized( true );
        }

        preferredQt = quantitationTypeDao.create( preferredQt, ProcessedExpressionDataVector.class );

        /*
         * Done with processing, now build the vectors and persist; Do a sanity check that we don't have more than we
         * should
         */
        Collection<ProcessedExpressionDataVector> newVectors = new HashSet<>( preferredData.size() );
        for ( Map.Entry<CompositeSequence, double[]> e : preferredData.entrySet() ) {
            ProcessedExpressionDataVector vec = ProcessedExpressionDataVector.Factory.newInstance();
            vec.setExpressionExperiment( expressionExperiment );
            // assert this.getBioAssays().size() > 0;
            vec.setQuantitationType( preferredQt );
            vec.setBioAssayDimension( dimension );
            vec.setDesignElement( e.getKey() );
            // assert this.getBioAssays().size() > 0;
            vec.setDataAsDoubles( e.getValue() );
            vec.setNumberOfCells( numberOfCells.get( e.getKey() ) );
            newVectors.add( vec );
        }

        log.info( String.format( "Persisting %d processed data vectors...",
                newVectors.size() ) );

        return expressionExperimentDao.createProcessedDataVectors( expressionExperiment, newVectors );
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
        Map<QuantitationType, Collection<RawExpressionDataVector>> mvv = expressionExperimentDao.getMissingValueVectors( ee );
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
     * @param expressionExperiment ee
     * @return true if any platform used by the ee is two-channel (including dual-mode)
     */
    private boolean isTwoChannel( ExpressionExperiment expressionExperiment ) {
        boolean isTwoChannel = false;
        Collection<ArrayDesign> arrayDesignsUsed = expressionExperimentDao.getArrayDesignsUsed( expressionExperiment );
        for ( ArrayDesign ad : arrayDesignsUsed ) {
            TechnologyType technologyType = ad.getTechnologyType();
            if ( technologyType == null ) {
                throw new IllegalStateException(
                        "Array designs must have a technology type assigned before processed vector computation" );
            }
            if ( technologyType.equals( TechnologyType.TWOCOLOR ) || technologyType.equals( TechnologyType.DUALMODE ) ) {
                isTwoChannel = true;
            }
        }
        return isTwoChannel;
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
    private Map<CompositeSequence, double[]> unpackAndMask( Collection<RawExpressionDataVector> preferredData,
            @Nullable Collection<RawExpressionDataVector> missingValueData,
            BioAssayDimension dimension ) {
        Map<CompositeSequence, double[]> unpackedData = unpackDoubles( preferredData, dimension );

        if ( missingValueData == null || missingValueData.isEmpty() ) {
            log.debug( "There is no separate missing data information, simply using the data as is" );
            return unpackedData;
        }

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
                }
            }
        }

        return unpackedData;
    }

    private Map<CompositeSequence, double[]> unpackDoubles( Collection<? extends BulkExpressionDataVector> vectors, BioAssayDimension dimension ) {
        ArrayList<BulkExpressionDataVector> orderedVectors = new ArrayList<>( vectors );
        List<double[]> sliced = BulkDataSlicerUtils.sliceDoubles( orderedVectors, dimension.getBioAssays() );
        Map<CompositeSequence, double[]> result = new HashMap<>( vectors.size() );
        for ( int i = 0; i < orderedVectors.size(); i++ ) {
            result.put( orderedVectors.get( i ).getDesignElement(), sliced.get( i ) );
        }
        return result;
    }

    private Map<CompositeSequence, boolean[]> unpackBooleans( Collection<? extends BulkExpressionDataVector> vectors, BioAssayDimension dimension ) {
        ArrayList<BulkExpressionDataVector> orderedVectors = new ArrayList<>( vectors );
        List<boolean[]> sliced = BulkDataSlicerUtils.sliceBooleans( orderedVectors, dimension.getBioAssays() );
        Map<CompositeSequence, boolean[]> result = new HashMap<>( vectors.size() );
        for ( int i = 0; i < orderedVectors.size(); i++ ) {
            result.put( orderedVectors.get( i ).getDesignElement(), sliced.get( i ) );
        }
        return result;
    }

    /**
     * Quantile normalize data. This should be one of the last steps in processing before persisting
     *
     * @param vectors vectors
     */
    private Map<CompositeSequence, double[]> renormalize( Map<CompositeSequence, double[]> vectors ) {
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

        Map<CompositeSequence, double[]> normalizedVectors = new HashMap<>( vectors.size() );
        for ( i = 0; i < rows; i++ ) {
            CompositeSequence c = normalizedMat.getRowName( i );
            double[] normalizedData = new double[cols];
            for ( int j = 0; j < cols; j++ ) {
                normalizedData[j] = normalizedMat.get( i, j );
            }
            normalizedVectors.put( c, normalizedData );
        }
        return normalizedVectors;
    }
}
