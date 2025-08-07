package ubic.gemma.persistence.service.expression.bioAssayData;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.core.analysis.preprocess.convert.QuantitationTypeConversionException;
import ubic.gemma.core.analysis.preprocess.detect.QuantitationTypeDetectionException;
import ubic.gemma.core.analysis.preprocess.normalize.QuantileNormalizer;
import ubic.gemma.core.datastructure.matrix.BulkExpressionDataMatrixUtils;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeValueObject;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssayData.*;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeDao;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentDao;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;

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
        rawPreferredDataVectors = consolidateAndLogTransformVectors( rawPreferredDataVectors, ignoreQuantitationMismatch );

        // create a masked QT based on the preferred raw vectors once all the necessary transformation have been done
        RawExpressionDataVector preferredDataVectorExemplar = rawPreferredDataVectors.iterator().next();
        QuantitationType preferredMaskedDataQuantitationType = this
                .createPreferredMaskedDataQuantitationType( preferredDataVectorExemplar.getQuantitationType() );

        // once the vectors have been consolidated, we can recover the dimension
        // no that if multiple BADs were consolidated, this will return a new BAD, otherwise the same BAD that was used
        // for the raw vectors will be re-used
        BioAssayDimension preferredMaskedDataDimension = rawPreferredDataVectors.iterator().next().getBioAssayDimension();

        // mask raw vectors
        Collection<RawExpressionDataVector> missingValueVectors = new HashSet<>();
        boolean isTwoChannel = this.isTwoChannel( expressionExperiment );
        if ( isTwoChannel ) {
            missingValueVectors = this.getMissingValueVectors( expressionExperiment );
        }
        Map<CompositeSequence, DoubleVectorValueObject> maskedVectorObjects = this
                .maskAndUnpack( rawPreferredDataVectors, missingValueVectors );

        /*
         * Note that we used to not normalize count data, but we've removed this restriction; and in any case we have
         * moved to using non-count summaries for the primary data type.
         */
        if ( preferredMaskedDataQuantitationType.getType().equals( StandardQuantitationType.COUNT ) ) {
            /*
             * Backfill target
             */
            log.warn( "Preferred data are counts; please convert to log2cpm" );
        }

        if ( preferredMaskedDataQuantitationType.getIsRatio() ) {
            log.info( "Data is on a ratio scale, skipping normalization step." );
        } else if ( maskedVectorObjects.size() < MIN_SIZE_FOR_RENORMALIZATION ) {
            log.info( "Not enough data vectors (" + maskedVectorObjects.size() + ") to perform normalization." );
        } else {
            log.info( "Normalizing the data" );
            this.renormalize( maskedVectorObjects );
            preferredMaskedDataQuantitationType.setIsNormalized( true );
            quantitationTypeDao.update( preferredMaskedDataQuantitationType );
        }

        /*
         * Done with processing, now build the vectors and persist; Do a sanity check that we don't have more than we
         * should
         */
        int i = 0;
        Collection<CompositeSequence> seenDes = new HashSet<>();
        Collection<ProcessedExpressionDataVector> newVectors = new HashSet<>();
        for ( CompositeSequence cs : maskedVectorObjects.keySet() ) {

            DoubleVectorValueObject dvvo = maskedVectorObjects.get( cs );

            if ( seenDes.contains( cs ) ) {
                // defensive programming, this happens.
                throw new IllegalStateException( "Duplicated design element: " + cs
                        + "; make sure the experiment has only one 'preferred' quantitation type. "
                        + "Perhaps you need to run vector merging following an array design switch?" );
            }

            ProcessedExpressionDataVector vec = ProcessedExpressionDataVector.Factory.newInstance();
            vec.setExpressionExperiment( expressionExperiment );
            // assert this.getBioAssays().size() > 0;
            vec.setQuantitationType( preferredMaskedDataQuantitationType );
            vec.setBioAssayDimension( preferredMaskedDataDimension );
            vec.setDesignElement( cs );
            // assert this.getBioAssays().size() > 0;
            vec.setDataAsDoubles( dvvo.getData() );
            vec.setRankByMax( dvvo.getRankByMax() );
            vec.setRankByMean( dvvo.getRankByMean() );

            newVectors.add( vec );
            seenDes.add( cs );
            if ( ++i % 5000 == 0 ) {
                log.info( i + " vectors built" );
            }
        }

        log.info( String.format( "Persisting %d processed data vectors...",
                newVectors.size() ) );

        int created = expressionExperimentDao.createProcessedDataVectors( expressionExperiment, newVectors );
        assert expressionExperiment.getNumberOfDataVectors() == created;
        return created;
    }

    /**
     * Consolidate raw vectors that have multiple BADs and log-transform them if necessary.
     * <p>
     * The consolidation is done by passing the vectors through {@link ExpressionDataDoubleMatrix} which handle multiple
     * assays per sample and then recover them on the other side.
     */
    private Collection<RawExpressionDataVector> consolidateAndLogTransformVectors(
            Collection<RawExpressionDataVector> rawPreferredDataVectors,
            boolean ignoreQuantitationMismatch ) throws QuantitationTypeDetectionException, QuantitationTypeConversionException {
        ExpressionDataDoubleMatrix matrix = new ExpressionDataDoubleMatrix( rawPreferredDataVectors );
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
        QuantitationType present = QuantitationType.Factory.newInstance( preferredQt );
        present.setName( preferredQt.getName() + PROCESSED_DATA_NAME_SUFFIX );
        present.setDescription( PROCESSED_DATA_DESCRIPTION );
        // we do not copy the normalized flag from raw QTs because it does not guarantee to mean it is quantile-normalized
        present.setIsNormalized( false );
        present.setIsMaskedPreferred( true );
        return quantitationTypeDao.create( present, ProcessedExpressionDataVector.class );
    }

    /**
     * @param  expressionExperiment ee
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
     * Mask missing values. This is mostly for two-color (ratiometric) data.
     *
     */
    private Map<CompositeSequence, DoubleVectorValueObject> maskAndUnpack(
            Collection<RawExpressionDataVector> preferredData, @Nullable Collection<RawExpressionDataVector> missingValueData ) {
        Map<CompositeSequence, DoubleVectorValueObject> unpackedData = this.unpack( preferredData );

        if ( missingValueData == null || missingValueData.isEmpty() ) {
            log.debug( "There is no separate missing data information, simply using the data as is" );
            for ( DoubleVectorValueObject rv : unpackedData.values() ) {
                rv.setMasked( true );
            }
            return unpackedData;
        }

        Collection<BooleanVectorValueObject> unpackedMissingValueData = this.unpackBooleans( missingValueData );
        Map<CompositeSequenceValueObject, BooleanVectorValueObject> missingValueMap = new HashMap<>();
        for ( BooleanVectorValueObject bv : unpackedMissingValueData ) {
            missingValueMap.put( bv.getDesignElement(), bv );
        }

        boolean warned = false;
        for ( DoubleVectorValueObject rv : unpackedData.values() ) {
            double[] data = rv.getData();
            CompositeSequenceValueObject de = rv.getDesignElement();
            BooleanVectorValueObject mv = missingValueMap.get( de );
            if ( mv == null ) {
                if ( !warned && log.isWarnEnabled() )
                    log.warn( "No mask vector for " + de
                            + ", additional warnings for missing masks for this job will be skipped" );
                // we're missing a mask vector for it for some reason, but still flag it as effectively masked.
                rv.setMasked( true );
                warned = true;
                continue;
            }

            boolean[] mvData = mv.getData();

            if ( mvData.length != data.length ) {
                throw new IllegalStateException( "Missing value data didn't match data length" );
            }
            for ( int i = 0; i < data.length; i++ ) {
                if ( !mvData[i] ) {
                    data[i] = Double.NaN;
                }
            }
            rv.setMasked( true );
        }

        return unpackedData;
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
     * Quantile normalize data. This should be one of the last steps in processing before persisting
     *
     * @param vectors vectors
     */
    private void renormalize( Map<CompositeSequence, DoubleVectorValueObject> vectors ) {

        int cols = vectors.values().iterator().next().getBioAssayDimension().getBioAssays().size();
        DoubleMatrix<CompositeSequence, Integer> mat = new DenseDoubleMatrix<>( vectors.size(), cols );
        for ( int i = 0; i < cols; i++ ) {
            mat.setColumnName( i, i );
        }

        int i = 0;
        for ( CompositeSequence c : vectors.keySet() ) {
            DoubleVectorValueObject v = vectors.get( c );
            double[] data = v.getData();

            if ( data.length != cols ) {
                throw new IllegalStateException(
                        "Normalization failed: perhaps vector merge needs to be run on this experiment? (vector length="
                                + data.length + "; " + cols + " bioAssays in bioassaydimension ID=" + v
                                .getBioAssayDimension().getId() );
            }
            for ( int j = 0; j < cols; j++ ) {
                mat.set( i, j, data[j] );
            }
            mat.setRowName( c, i );
            i++;
        }

        this.doQuantileNormalization( mat, vectors );

        assert mat.rows() == vectors.size();

    }

    private void doQuantileNormalization( DoubleMatrix<CompositeSequence, Integer> matrix,
            Map<CompositeSequence, DoubleVectorValueObject> vectors ) {

        QuantileNormalizer<CompositeSequence, Integer> normalizer = new QuantileNormalizer<>();

        DoubleMatrix<CompositeSequence, Integer> normalized = normalizer.normalize( matrix );

        for ( int i = 0; i < normalized.rows(); i++ ) {
            double[] row = normalized.getRow( i );
            CompositeSequence cs = normalized.getRowName( i );
            DoubleVectorValueObject vec = vectors.get( cs );
            double[] data = vec.getData();
            System.arraycopy( row, 0, data, 0, row.length );
        }

    }

    private Map<CompositeSequence, DoubleVectorValueObject> unpack(
            Collection<? extends BulkExpressionDataVector> data ) {
        Map<CompositeSequence, DoubleVectorValueObject> result = new HashMap<>();
        Map<ExpressionExperiment, ExpressionExperimentValueObject> eeVos = createValueObjectCache( data,
                BulkExpressionDataVector::getExpressionExperiment, ExpressionExperimentValueObject::new );
        Map<QuantitationType, QuantitationTypeValueObject> qtVos = createValueObjectCache( data,
                BulkExpressionDataVector::getQuantitationType, QuantitationTypeValueObject::new );
        Map<BioAssayDimension, BioAssayDimensionValueObject> badVos = createValueObjectCache( data,
                BulkExpressionDataVector::getBioAssayDimension, BioAssayDimensionValueObject::new );
        Map<ArrayDesign, ArrayDesignValueObject> adVos = createValueObjectCache( data,
                vec -> vec.getDesignElement().getArrayDesign(), ArrayDesignValueObject::new );
        for ( BulkExpressionDataVector v : data ) {
            result.put( v.getDesignElement(),
                    new DoubleVectorValueObject( v, eeVos.get( v.getExpressionExperiment() ),
                            qtVos.get( v.getQuantitationType() ), badVos.get( v.getBioAssayDimension() ),
                            adVos.get( v.getDesignElement().getArrayDesign() ), null ) );
        }
        return result;
    }

    private Collection<BooleanVectorValueObject> unpackBooleans( Collection<? extends BulkExpressionDataVector> data ) {
        Collection<BooleanVectorValueObject> result = new HashSet<>();
        Map<ExpressionExperiment, ExpressionExperimentValueObject> eeVos = createValueObjectCache( data,
                BulkExpressionDataVector::getExpressionExperiment, ExpressionExperimentValueObject::new );
        Map<QuantitationType, QuantitationTypeValueObject> qtVos = createValueObjectCache( data,
                BulkExpressionDataVector::getQuantitationType, QuantitationTypeValueObject::new );
        Map<BioAssayDimension, BioAssayDimensionValueObject> badVos = createValueObjectCache( data,
                BulkExpressionDataVector::getBioAssayDimension, BioAssayDimensionValueObject::new );
        Map<ArrayDesign, ArrayDesignValueObject> adVos = createValueObjectCache( data,
                v -> v.getDesignElement().getArrayDesign(), ArrayDesignValueObject::new );
        for ( BulkExpressionDataVector v : data ) {
            result.add( new BooleanVectorValueObject( v, eeVos.get( v.getExpressionExperiment() ),
                    qtVos.get( v.getQuantitationType() ), badVos.get( v.getBioAssayDimension() ),
                    adVos.get( v.getDesignElement().getArrayDesign() ) ) );
        }
        return result;
    }

    /**
     * @return Pre-fetch and construct the BioAssayDimensionValueObjects. Used on the basis that the data probably
     *              just
     *              have one
     *              (or a few) BioAssayDimensionValueObjects needed, not a different one for each vector. See bug 3629
     *              for
     *              details.
     */
    private <S, T> Map<S, T> createValueObjectCache( Collection<? extends BulkExpressionDataVector> vectors,
            Function<BulkExpressionDataVector, S> keyExtractor, Function<S, T> valueExtractor ) {
        Map<S, T> result = new HashMap<>();
        for ( BulkExpressionDataVector v : vectors ) {
            result.computeIfAbsent( keyExtractor.apply( v ), valueExtractor );
        }
        return result;
    }
}
