/*
 * The Gemma project
 *
 * Copyright (c) 2008 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.persistence.service.expression.bioAssayData;

import org.apache.commons.lang3.time.StopWatch;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.core.analysis.preprocess.convert.QuantitationTypeConversionException;
import ubic.gemma.core.analysis.preprocess.detect.QuantitationTypeDetectionException;
import ubic.gemma.core.analysis.preprocess.normalize.QuantileNormalizer;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssayData.*;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentDao;
import ubic.gemma.persistence.util.CommonQueries;

import java.util.*;

import static ubic.gemma.core.analysis.preprocess.convert.QuantitationTypeConversionUtils.ensureLog2Scale;
import static ubic.gemma.persistence.util.QueryUtils.batchIdentifiableParameterList;
import static ubic.gemma.persistence.util.QueryUtils.optimizeIdentifiableParameterList;

/**
 * @author Paul
 */
@Repository
public class ProcessedExpressionDataVectorDaoImpl extends AbstractDesignElementDataVectorDao<ProcessedExpressionDataVector>
        implements ProcessedExpressionDataVectorDao {

    /**
     * Don't attempt to renormalize data that is smaller than this. This avoids unnecessary normalization in tests, and
     * in data sets where normalization is more likely to harm than good.
     */
    private static final int MIN_SIZE_FOR_RENORMALIZATION = 4000;

    @Autowired
    private ExpressionExperimentDao expressionExperimentDao;

    @Autowired
    private ProcessedDataVectorByGeneCache processedDataVectorByGeneCache;

    @Autowired
    public ProcessedExpressionDataVectorDaoImpl( SessionFactory sessionFactory ) {
        super( ProcessedExpressionDataVector.class, sessionFactory );
    }

    @Override
    public int createProcessedDataVectors( ExpressionExperiment expressionExperiment, boolean ignoreQuantitationMismatch ) throws QuantitationTypeDetectionException, QuantitationTypeConversionException {
        log.info( "Removing processed expression vectors for " + expressionExperiment + "..." );
        expressionExperimentDao.removeProcessedDataVectors( expressionExperiment );

        log.info( "Computing processed expression vectors for " + expressionExperiment );

        boolean isTwoChannel = this.isTwoChannel( expressionExperiment );

        Collection<RawExpressionDataVector> missingValueVectors = new HashSet<>();
        if ( isTwoChannel ) {
            missingValueVectors = this.getMissingValueVectors( expressionExperiment );
        }

        Collection<RawExpressionDataVector> rawPreferredDataVectors = this
                .getPreferredDataVectors( expressionExperiment );
        if ( rawPreferredDataVectors.isEmpty() ) {
            throw new IllegalArgumentException( "No preferred data vectors for " + expressionExperiment );
        }

        removeDuplicateElements( rawPreferredDataVectors );

        RawExpressionDataVector preferredDataVectorExemplar = rawPreferredDataVectors.iterator().next();
        QuantitationType preferredMaskedDataQuantitationType = this
                .getPreferredMaskedDataQuantitationType( expressionExperiment, preferredDataVectorExemplar.getQuantitationType() );

        /* log-transform if necessary */
        // this will also consolidate sets of raw vectors that have multiple BADs
        Collection<RawExpressionDataVector> consolidatedRawVectors = consolidateRawVectors( rawPreferredDataVectors,
                preferredMaskedDataQuantitationType, ignoreQuantitationMismatch );

        BioAssayDimension preferredMaskedDataDimension = consolidatedRawVectors.iterator().next().getBioAssayDimension();
        Map<CompositeSequence, DoubleVectorValueObject> maskedVectorObjects = this
                .maskAndUnpack( consolidatedRawVectors, missingValueVectors );

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

        if ( !preferredMaskedDataQuantitationType.getIsRatio()
                && maskedVectorObjects.size() > ProcessedExpressionDataVectorDaoImpl.MIN_SIZE_FOR_RENORMALIZATION ) {
            log.info( "Normalizing the data" );
            this.renormalize( maskedVectorObjects );
        } else {
            log.info( "Normalization skipped for this data set (not suitable)" );
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

        this.getSessionFactory().getCurrentSession().update( expressionExperiment );
        assert expressionExperiment.getNumberOfDataVectors() != null;

        this.processedDataVectorByGeneCache.evict( expressionExperiment );

        return created;
    }

    @Override
    public Collection<ProcessedExpressionDataVector> getProcessedVectors( ExpressionExperiment ee ) {
        StopWatch timer = StopWatch.createStarted();
        //noinspection unchecked
        List<ProcessedExpressionDataVector> result = this.getSessionFactory().getCurrentSession().createQuery(
                        "select dedv from ProcessedExpressionDataVector dedv "
                                + "where dedv.expressionExperiment = :ee" )
                .setParameter( "ee", ee )
                .list();
        log.info( String.format( "Loading %d %s took %d ms", result.size(), getElementClass().getSimpleName(), timer.getTime() ) );
        return result;
    }

    @Override
    public List<ProcessedExpressionDataVector> getProcessedVectors( ExpressionExperiment ee, BioAssayDimension dimension, int offset, int limit ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                        "select dedv from ProcessedExpressionDataVector dedv "
                                + "where dedv.expressionExperiment = :ee and dedv.bioAssayDimension = :dimension" )
                .setParameter( "ee", ee )
                .setParameter( "dimension", dimension )
                .setFirstResult( offset )
                .setMaxResults( limit )
                .list();
    }

    @Override
    public List<CompositeSequence> getProcessedVectorsDesignElements( ExpressionExperiment ee, BioAssayDimension dimension, int offset, int limit ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                        "select dedv.designElement from ProcessedExpressionDataVector dedv "
                                + "where dedv.expressionExperiment = :ee and dedv.bioAssayDimension = :dimension" )
                .setParameter( "ee", ee )
                .setParameter( "dimension", dimension )
                .setFirstResult( offset )
                .setMaxResults( limit )
                .list();
    }

    @Override
    public Map<ExpressionExperiment, Map<Gene, Collection<Double>>> getRanks(
            Collection<ExpressionExperiment> expressionExperiments, Collection<Gene> genes, RankMethod method ) {

        Collection<ArrayDesign> arrayDesigns = CommonQueries.getArrayDesignsUsed( expressionExperiments, this.getSessionFactory().getCurrentSession() );

        // this could be further improved by getting probes specific to experiments in batches.
        Map<CompositeSequence, Collection<Gene>> cs2gene = CommonQueries
                .getCs2GeneMap( genes, arrayDesigns, this.getSessionFactory().getCurrentSession() );

        if ( cs2gene.isEmpty() ) {
            log.warn( "No composite sequences found for genes" );
            return new HashMap<>();
        }
        Map<ExpressionExperiment, Map<Gene, Collection<Double>>> result = new HashMap<>();

        for ( Collection<CompositeSequence> batch : batchIdentifiableParameterList( cs2gene.keySet(), 512 ) ) {

            //language=HQL
            //noinspection unchecked
            List<Object[]> qr = this.getSessionFactory().getCurrentSession().createQuery(
                            "select dedv.expressionExperiment, dedv.designElement, dedv.rankByMean, dedv.rankByMax from ProcessedExpressionDataVector dedv "
                                    + "where dedv.designElement in ( :cs ) and dedv.expressionExperiment in (:ees) "
                                    + "group by dedv.designElement, dedv.expressionExperiment" )
                    .setParameter( "cs", batch )
                    .setParameterList( "ees", optimizeIdentifiableParameterList( expressionExperiments ) )
                    .list();

            for ( Object[] o : qr ) {
                ExpressionExperiment e = ( ExpressionExperiment ) o[0];
                CompositeSequence d = ( CompositeSequence ) o[1];
                Double rMean = o[2] == null ? Double.NaN : ( Double ) o[2];
                Double rMax = o[3] == null ? Double.NaN : ( Double ) o[3];

                if ( !result.containsKey( e ) ) {
                    result.put( e, new HashMap<>() );
                }

                Map<Gene, Collection<Double>> rMap = result.get( e );

                Collection<Gene> genes4probe = cs2gene.get( d );

                this.addToGene( method, rMap, rMean, rMax, genes4probe );
            }
        }
        return result;
    }

    @Override
    public Map<Gene, Collection<Double>> getRanks( ExpressionExperiment expressionExperiment, Collection<Gene> genes,
            RankMethod method ) {
        Map<CompositeSequence, Collection<Gene>> cs2gene = CommonQueries
                .getCs2GeneMap( genes, this.getSessionFactory().getCurrentSession() );
        if ( cs2gene.isEmpty() ) {
            log.warn( "No composite sequences found for genes" );
            return new HashMap<>();
        }

        //language=HQL
        //noinspection unchecked
        List<Object[]> qr = this.getSessionFactory().getCurrentSession().createQuery(
                        "select dedv.designElement, dedv.rankByMean, dedv.rankByMax from ProcessedExpressionDataVector dedv "
                                + "where dedv.designElement in (:cs) and dedv.expressionExperiment = :ee "
                                + "group by dedv.designElement, dedv.expressionExperiment" )
                .setParameterList( "cs", optimizeIdentifiableParameterList( cs2gene.keySet() ) )
                .setParameter( "ee", expressionExperiment )
                .list();

        Map<Gene, Collection<Double>> result = new HashMap<>();
        for ( Object[] o : qr ) {
            CompositeSequence d = ( CompositeSequence ) o[0];
            Double rMean = o[1] == null ? Double.NaN : ( Double ) o[1];
            Double rMax = o[2] == null ? Double.NaN : ( Double ) o[2];

            Collection<Gene> genes4probe = cs2gene.get( d );

            this.addToGene( method, result, rMean, rMax, genes4probe );
        }
        return result;

    }

    @Override
    public Map<ProcessedExpressionDataVector, Collection<Long>> getGenes( Collection<ProcessedExpressionDataVector> vectors ) {
        Collection<Long> probes = new ArrayList<>();
        for ( ProcessedExpressionDataVector pedv : vectors ) {
            probes.add( pedv.getDesignElement().getId() );
        }

        Map<Long, Collection<Long>> cs2gene = CommonQueries
                .getCs2GeneMapForProbes( probes, this.getSessionFactory().getCurrentSession() );

        Map<ProcessedExpressionDataVector, Collection<Long>> vector2gene = new HashMap<>( cs2gene.size() );
        for ( ProcessedExpressionDataVector pedv : vectors ) {
            vector2gene.put( pedv, cs2gene.getOrDefault( pedv.getDesignElement().getId(), Collections.emptySet() ) );
        }

        return vector2gene;
    }

    /**
     * Consolidate raw vectors that have multiple BADs and log-transform them if necessary.
     *
     * @param  rawPreferredDataVectors             raw preferred data vectors
     * @param  preferredMaskedDataQuantitationType preferred masked data QT
     * @return collection containing the vectors
     */
    private Collection<RawExpressionDataVector> consolidateRawVectors(
            Collection<RawExpressionDataVector> rawPreferredDataVectors,
            QuantitationType preferredMaskedDataQuantitationType,
            boolean ignoreQuantitationMismatch ) throws QuantitationTypeDetectionException, QuantitationTypeConversionException {
        ExpressionDataDoubleMatrix matrix = ensureLog2Scale( new ExpressionDataDoubleMatrix( rawPreferredDataVectors ), ignoreQuantitationMismatch );
        preferredMaskedDataQuantitationType.setScale( ScaleType.LOG2 );
        this.getSessionFactory().getCurrentSession().update( preferredMaskedDataQuantitationType );
        return new HashSet<>( matrix.toRawDataVectors() );
    }

    private void addToGene( RankMethod method, Map<Gene, Collection<Double>> result, Double rMean, Double rMax,
            Collection<Gene> genes4probe ) {
        for ( Gene gene : genes4probe ) {
            if ( !result.containsKey( gene ) ) {
                result.put( gene, new ArrayList<>() );
            }
            switch ( method ) {
                case mean:
                    result.get( gene ).add( rMean );
                    break;
                case max:
                    result.get( gene ).add( rMax );
                    break;
                default:
                    break;
            }
        }
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

    private Collection<RawExpressionDataVector> getMissingValueVectors( ExpressionExperiment ee ) {
        //language=HQL
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                        "select dedv from RawExpressionDataVector dedv "
                                + "join dedv.quantitationType q "
                                + "where q.type = 'PRESENTABSENT' and dedv.expressionExperiment  = :ee " )
                .setParameter( "ee", ee )
                .list();
    }

    /**
     * @param  ee ee
     * @return Retrieve the RAW data for the preferred quantitation type.
     */
    private Collection<RawExpressionDataVector> getPreferredDataVectors( ExpressionExperiment ee ) {
        //language=HQL
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                        "select dedv from RawExpressionDataVector dedv "
                                + "join dedv.quantitationType q "
                                + "where q.isPreferred = true and dedv.expressionExperiment = :ee" )
                .setParameter( "ee", ee )
                .list();
    }

    /**
     * Make (or re-use) a quantitation type for attaching to the new processed data - always log2 transformed
     *
     * @param  ee          expression experiment we're dealing with
     * @param  preferredQt preferred QT
     * @return QT
     */
    private QuantitationType getPreferredMaskedDataQuantitationType( ExpressionExperiment ee, QuantitationType preferredQt ) {
        QuantitationType present = QuantitationType.Factory.newInstance();

        present.setName( preferredQt.getName() + " - Processed version" );
        present.setDescription(
                "Processed data (as per Gemma) for analysis, based on the preferred quantitation type raw data" );

        present.setGeneralType( preferredQt.getGeneralType() );
        present.setRepresentation( preferredQt.getRepresentation() ); // better be a number!
        present.setScale( preferredQt.getScale() );

        present.setIsBackground( false );
        present.setIsPreferred( false ); // This is the correct thing to do because it's not raw data.
        present.setIsMaskedPreferred( true );
        present.setIsBackgroundSubtracted( preferredQt.getIsBackgroundSubtracted() );

        present.setIsBatchCorrected( preferredQt.getIsBatchCorrected() );
        present.setIsRecomputedFromRawData(
                preferredQt.getIsRecomputedFromRawData() ); // By "RAW" we mean CEL files or Fastq etc.

        present.setIsNormalized( preferredQt.getIsNormalized() );

        present.setIsRatio( preferredQt.getIsRatio() );
        present.setType( preferredQt.getType() );

        // use existing QT if possible 
        for ( QuantitationType oldqt : ee.getQuantitationTypes() ) {
            if ( oldqt.getName() != null && oldqt.getName().equals( present.getName() ) ) { // FIXME make this a more stringent check for a match
                present = oldqt;
                break;
            }
        }

        if ( present.getId() == null ) {
            Long id = ( Long ) this.getSessionFactory().getCurrentSession().save( present );
            return ( QuantitationType ) this.getSessionFactory().getCurrentSession().load( QuantitationType.class, id );
        }
        return present;

    }


    /**
     * @param  expressionExperiment ee
     * @return true if any platform used by the ee is two-channel (including dual-mode)
     */
    private boolean isTwoChannel( ExpressionExperiment expressionExperiment ) {

        boolean isTwoChannel = false;
        Collection<ArrayDesign> arrayDesignsUsed = CommonQueries.getArrayDesignsUsed( expressionExperiment, this.getSessionFactory().getCurrentSession() );
        for ( ArrayDesign ad : arrayDesignsUsed ) {
            TechnologyType technologyType = ad.getTechnologyType();

            if ( technologyType == null ) {
                throw new IllegalStateException(
                        "Array designs must have a technology type assigned before processed vector computation" );
            }

            if ( technologyType.equals( TechnologyType.TWOCOLOR ) || technologyType
                    .equals( TechnologyType.DUALMODE ) ) {
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
            Collection<RawExpressionDataVector> preferredData, Collection<RawExpressionDataVector> missingValueData ) {
        Map<CompositeSequence, DoubleVectorValueObject> unpackedData = this.unpack( preferredData );

        if ( missingValueData.isEmpty() ) {
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
     */
    private void removeDuplicateElements( Collection<RawExpressionDataVector> rawPreferredDataVectors ) {
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
        if ( !toRemove.isEmpty() ) {
            rawPreferredDataVectors.removeAll( toRemove );
            log.info( "Removed " + toRemove.size() + " duplicate elements, " + rawPreferredDataVectors.size()
                    + " remain" );
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

    private Map<CompositeSequence, DoubleVectorValueObject> unpack(
            Collection<? extends BulkExpressionDataVector> data ) {
        Map<CompositeSequence, DoubleVectorValueObject> result = new HashMap<>();
        Map<BioAssayDimension, BioAssayDimensionValueObject> badVos = this.getBioAssayDimensionValueObjects( data );
        for ( BulkExpressionDataVector v : data ) {
            result.put( v.getDesignElement(),
                    new DoubleVectorValueObject( v, badVos.get( v.getBioAssayDimension() ) ) );
        }
        return result;
    }

    private Collection<BooleanVectorValueObject> unpackBooleans( Collection<? extends BulkExpressionDataVector> data ) {
        Collection<BooleanVectorValueObject> result = new HashSet<>();

        Map<BioAssayDimension, BioAssayDimensionValueObject> badVos = this.getBioAssayDimensionValueObjects( data );

        for ( BulkExpressionDataVector v : data ) {
            result.add( new BooleanVectorValueObject( v, badVos.get( v.getBioAssayDimension() ) ) );
        }
        return result;
    }

    /**
     * @param  data data
     * @return Pre-fetch and construct the BioAssayDimensionValueObjects. Used on the basis that the data probably
     *              just
     *              have one
     *              (or a few) BioAssayDimensionValueObjects needed, not a different one for each vector. See bug 3629
     *              for
     *              details.
     */
    private Map<BioAssayDimension, BioAssayDimensionValueObject> getBioAssayDimensionValueObjects(
            Collection<? extends BulkExpressionDataVector> data ) {
        Map<BioAssayDimension, BioAssayDimensionValueObject> badVos = new HashMap<>();
        for ( BulkExpressionDataVector v : data ) {
            BioAssayDimension bioAssayDimension = v.getBioAssayDimension();
            if ( !badVos.containsKey( bioAssayDimension ) ) {
                badVos.put( bioAssayDimension, new BioAssayDimensionValueObject( bioAssayDimension ) );
            }
        }
        return badVos;
    }
}
