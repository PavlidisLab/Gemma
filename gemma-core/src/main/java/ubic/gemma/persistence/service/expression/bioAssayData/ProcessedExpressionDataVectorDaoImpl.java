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
import org.hibernate.FlushMode;
import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.core.analysis.preprocess.normalize.QuantileNormalizer;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrixUtil;
import ubic.gemma.core.datastructure.matrix.QuantitationMismatchException;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.bioAssayData.*;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.AbstractDao;
import ubic.gemma.persistence.util.CommonQueries;
import ubic.gemma.persistence.util.EntityUtils;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

import static ubic.gemma.persistence.util.QueryUtils.*;

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

    private final ProcessedDataVectorCache processedDataVectorCache;

    @Autowired
    public ProcessedExpressionDataVectorDaoImpl( SessionFactory sessionFactory,
            ProcessedDataVectorCache processedDataVectorCache ) {
        super( ProcessedExpressionDataVector.class, sessionFactory );
        this.processedDataVectorCache = processedDataVectorCache;
    }

    @Override
    public void clearCache() {
        processedDataVectorCache.clearCache();
    }

    @Override
    public Set<ProcessedExpressionDataVector> createProcessedDataVectors( ExpressionExperiment ee, boolean ignoreQuantitationMismatch ) throws QuantitationMismatchException {
        if ( ee == null ) {
            throw new IllegalStateException( "ExpressionExperiment cannot be null" );
        }

        /*
         * Initialize objects, get some parameters, locate the vectors we're going to use.
         */
        ExpressionExperiment expressionExperiment = ( ExpressionExperiment ) this.getSessionFactory()
                .getCurrentSession().get( ExpressionExperiment.class, ee.getId() );

        assert expressionExperiment != null;

        this.removeProcessedDataVectors( expressionExperiment );

        Hibernate.initialize( expressionExperiment );
        Hibernate.initialize( expressionExperiment.getQuantitationTypes() );
        Hibernate.initialize( expressionExperiment.getProcessedExpressionDataVectors() );

        expressionExperiment.getProcessedExpressionDataVectors().clear();

        AbstractDao.log.info( "Computing processed expression vectors for " + expressionExperiment );

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
        Collection<RawExpressionDataVector> preferredDataVectors = ensureLog2Scale( rawPreferredDataVectors,
                preferredMaskedDataQuantitationType, ignoreQuantitationMismatch );

        Map<CompositeSequence, DoubleVectorValueObject> maskedVectorObjects = this
                .maskAndUnpack( preferredDataVectors, missingValueVectors );

        /*
         * Note that we used to not normalize count data, but we've removed this restriction; and in any case we have
         * moved to using non-count summaries for the primary data type.
         */
        if ( preferredMaskedDataQuantitationType.getType().equals( StandardQuantitationType.COUNT ) ) {
            /*
             * Backfill target
             */
            AbstractDao.log.warn( "Preferred data are counts; please convert to log2cpm" );
        }

        if ( !preferredMaskedDataQuantitationType.getIsRatio()
                && maskedVectorObjects.size() > ProcessedExpressionDataVectorDaoImpl.MIN_SIZE_FOR_RENORMALIZATION ) {
            AbstractDao.log.info( "Normalizing the data" );
            this.renormalize( maskedVectorObjects );
        } else {
            AbstractDao.log.info( "Normalization skipped for this data set (not suitable)" );
        }

        /*
         * Done with processing, now build the vectors and persist; Do a sanity check that we don't have more than we
         * should
         */
        int i = 0;
        Collection<CompositeSequence> seenDes = new HashSet<>();
        for ( CompositeSequence cs : maskedVectorObjects.keySet() ) {

            DoubleVectorValueObject dvvo = maskedVectorObjects.get( cs );

            if ( seenDes.contains( cs ) ) {
                // defensive programming, this happens.
                throw new IllegalStateException( "Duplicated design element: " + cs
                        + "; make sure the experiment has only one 'preferred' quantitation type. "
                        + "Perhaps you need to run vector merging following an array design switch?" );
            }

            ProcessedExpressionDataVector vec = ( ProcessedExpressionDataVector ) dvvo
                    .toDesignElementDataVector( ee, cs, preferredMaskedDataQuantitationType );

            expressionExperiment.getProcessedExpressionDataVectors().add( vec );
            seenDes.add( cs );
            if ( ++i % 5000 == 0 ) {
                AbstractDao.log.info( i + " vectors built" );
            }
        }

        AbstractDao.log.info( "Persisting " + expressionExperiment.getProcessedExpressionDataVectors().size()
                + " processed data vectors" );

        expressionExperiment.getQuantitationTypes().add( preferredMaskedDataQuantitationType );
        expressionExperiment.setNumberOfDataVectors( expressionExperiment.getProcessedExpressionDataVectors().size() );

        this.getSessionFactory().getCurrentSession().update( expressionExperiment );
        assert expressionExperiment.getNumberOfDataVectors() != null;

        this.processedDataVectorCache.clearCache( expressionExperiment.getId() );

        return expressionExperiment.getProcessedExpressionDataVectors();
    }

    @Override
    public Collection<DoubleVectorValueObject> getProcessedDataArrays( BioAssaySet expressionExperiment ) {
        return this.getProcessedDataArrays( expressionExperiment, -1 );
    }

    @Override
    public Collection<DoubleVectorValueObject> getProcessedDataArrays( BioAssaySet expressionExperiment,
            Collection<Long> genes ) {
        Collection<BioAssaySet> expressionExperiments = new HashSet<>();
        expressionExperiments.add( expressionExperiment );
        return this.handleGetProcessedExpressionDataArrays( expressionExperiments, genes );
    }

    @Override
    public Collection<DoubleVectorValueObject> getProcessedDataArrays( BioAssaySet ee, int limit ) {

        Collection<ProcessedExpressionDataVector> pedvs = this.getProcessedVectors( this.getExperiment( ee ), limit );

        if ( pedvs.isEmpty() ) {
            AbstractDao.log.warn( "No processed vectors for experiment " + ee );
            return new HashSet<>();
        }

        Collection<Long> probes = new ArrayList<>();
        for ( ProcessedExpressionDataVector pedv : pedvs ) {
            probes.add( pedv.getDesignElement().getId() );
        }

        if ( probes.isEmpty() ) {
            return this.unpack( pedvs ).values();
        }

        Map<Long, Collection<Long>> cs2gene = CommonQueries
                .getCs2GeneMapForProbes( probes, this.getSessionFactory().getCurrentSession() );

        Collection<BioAssayDimension> bioAssayDimensions = this.getBioAssayDimensions( ee );

        if ( bioAssayDimensions.size() == 1 ) {
            return this.unpack( pedvs, cs2gene ).values();
        }

        /*
         * deal with 'misalignment problem'
         */

        BioAssayDimension longestBad = this.checkRagged( bioAssayDimensions );

        if ( longestBad != null ) {
            return this.unpack( pedvs, cs2gene, longestBad );
        }
        return this.unpack( pedvs, cs2gene ).values();
    }

    @Override
    public Collection<DoubleVectorValueObject> getProcessedDataArrays(
            Collection<? extends BioAssaySet> expressionExperiments, Collection<Long> genes ) {
        return this.handleGetProcessedExpressionDataArrays( expressionExperiments, genes );
    }

    @Override
    public Collection<DoubleVectorValueObject> getProcessedDataArraysByProbe( Collection<? extends BioAssaySet> ees,
            Collection<CompositeSequence> probes ) {

        if ( probes.isEmpty() )
            return new HashSet<>();

        Collection<Long> probeIds = EntityUtils.getIds( probes );

        return this.getProcessedDataArraysByProbeIds( ees, probeIds );

    }

    @Override
    public Collection<DoubleVectorValueObject> getProcessedDataArraysByProbeIds( BioAssaySet ee,
            Collection<Long> probes ) {
        return this.getProcessedDataArraysByProbeIds( Collections.singleton( ee ), probes );
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
        log.info( String.format( "Loading %d %s took %d ms", result.size(), elementClass.getSimpleName(), timer.getTime() ) );
        return result;
    }

    @Override
    public Map<ExpressionExperiment, Map<Gene, Collection<Double>>> getRanks(
            Collection<ExpressionExperiment> expressionExperiments, Collection<Gene> genes, RankMethod method ) {

        Collection<ArrayDesign> arrayDesigns = CommonQueries
                .getArrayDesignsUsed( EntityUtils.getIds( expressionExperiments ),
                        this.getSessionFactory().getCurrentSession() )
                .keySet();

        // this could be further improved by getting probes specific to experiments in batches.
        Map<CompositeSequence, Collection<Gene>> cs2gene = CommonQueries
                .getCs2GeneMap( genes, arrayDesigns, this.getSessionFactory().getCurrentSession() );

        if ( cs2gene.isEmpty() ) {
            AbstractDao.log.warn( "No composite sequences found for genes" );
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
        if ( cs2gene.keySet().size() == 0 ) {
            AbstractDao.log.warn( "No composite sequences found for genes" );
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
    public Map<CompositeSequence, Double> getRanks( ExpressionExperiment expressionExperiment, RankMethod method ) {
        //language=HQL
        //noinspection unchecked
        List<Object[]> qr = this.getSessionFactory().getCurrentSession().createQuery(
                        "select dedv.designElement, dedv.rankByMean, dedv.rankByMax from ProcessedExpressionDataVector dedv "
                                + "where dedv.expressionExperiment = :ee" )
                .setParameter( "ee", expressionExperiment )
                .list();
        Map<CompositeSequence, Double> result = new HashMap<>();
        for ( Object[] o : qr ) {
            CompositeSequence d = ( CompositeSequence ) o[0];
            Double rMean = o[1] == null ? Double.NaN : ( Double ) o[1];
            Double rMax = o[2] == null ? Double.NaN : ( Double ) o[2];
            switch ( method ) {
                case mean:
                    result.put( d, rMean );
                    break;
                case max:
                    result.put( d, rMax );
                    break;
                default:
                    break;
            }
        }
        return result;

    }

    @Override
    public Map<ExpressionExperiment, Map<Gene, Map<CompositeSequence, Double[]>>> getRanksByProbe(
            Collection<ExpressionExperiment> expressionExperiments, Collection<Gene> genes ) {
        Map<CompositeSequence, Collection<Gene>> cs2gene = CommonQueries
                .getCs2GeneMap( genes, this.getSessionFactory().getCurrentSession() );

        if ( cs2gene.keySet().size() == 0 ) {
            AbstractDao.log.warn( "No composite sequences found for genes" );
            return new HashMap<>();
        }

        //language=HQL
        //noinspection unchecked
        List<Object[]> qr = this.getSessionFactory().getCurrentSession().createQuery( "select dedv.expressionExperiment, dedv.designElement, dedv.rankByMean, dedv.rankByMax "
                        + "from ProcessedExpressionDataVector dedv "
                        + "where dedv.designElement in (:cs) and dedv.expressionExperiment in (:ees) "
                        + "group by dedv.designElement, dedv.expressionExperiment" )
                .setParameterList( "cs", optimizeIdentifiableParameterList( cs2gene.keySet() ) )
                .setParameterList( "ees", expressionExperiments )
                .list();

        Map<ExpressionExperiment, Map<Gene, Map<CompositeSequence, Double[]>>> resultNew = new HashMap<>();
        for ( Object[] o : qr ) {
            ExpressionExperiment e = ( ExpressionExperiment ) o[0];
            CompositeSequence d = ( CompositeSequence ) o[1];
            Double rMean = ( Double ) o[2];
            Double rMax = ( Double ) o[3];

            if ( !resultNew.containsKey( e ) ) {
                resultNew.put( e, new HashMap<>() );
            }

            Map<Gene, Map<CompositeSequence, Double[]>> rMapNew = resultNew.get( e );

            Collection<Gene> genes4probe = cs2gene.get( d );

            for ( Gene gene : genes4probe ) {
                if ( !rMapNew.containsKey( gene ) ) {
                    rMapNew.put( gene, new HashMap<>() );
                }

                // return BOTH mean and max

                if ( rMean == null || rMax == null ) {
                    continue;
                }
                Double[] MeanMax = new Double[] { rMean, rMax };
                rMapNew.get( gene ).put( d, MeanMax );
            }
        }
        return resultNew;
    }

    @Override
    public void removeProcessedDataVectors( ExpressionExperiment expressionExperiment ) {
        assert expressionExperiment != null;
        assert expressionExperiment.getId() != null;

        if ( !this.getSessionFactory().getCurrentSession().contains( expressionExperiment ) ) {
            expressionExperiment = ( ExpressionExperiment ) this.getSessionFactory().getCurrentSession()
                    .get( ExpressionExperiment.class, expressionExperiment.getId() );
        }

        /*
         * Get quantitation types that will be removed.
         */
        //noinspection unchecked
        List<QuantitationType> qtsToRemove = this.getSessionFactory().getCurrentSession().createQuery(
                        "select q from ExpressionExperiment e "
                                + "inner join e.processedExpressionDataVectors p "
                                + "inner join p.quantitationType q "
                                + "where e = :ee "
                                + "group by q" )
                .setParameter( "ee", expressionExperiment )
                .list();

        //        Collection<ProcessedExpressionDataVector> vectors = expressionExperiment.getProcessedExpressionDataVectors();
        //        Hibernate.initialize( vectors );
        //        expressionExperiment.setProcessedExpressionDataVectors( new HashSet<ProcessedExpressionDataVector>() );
        //        this.getSessionFactory().getCurrentSession().update( expressionExperiment );

        expressionExperiment.getProcessedExpressionDataVectors().clear();
        expressionExperiment.setNumberOfDataVectors( 0 );

        //        if ( !vectors.isEmpty() ) {
        //            this.getSessionFactory().getCurrentSession()
        //                    .createQuery( "delete from ProcessedExpressionDataVector p where p.id in (:ids)" )
        //                    .setParameterList( "ids", EntityUtils.getIds( vectors ) ).executeUpdate();
        //        }
        if ( !qtsToRemove.isEmpty() ) {
            AbstractDao.log.info( "Deleting " + qtsToRemove.size() + " old quantitation types" );
            qtsToRemove.forEach( expressionExperiment.getQuantitationTypes()::remove );
            this.getSessionFactory().getCurrentSession().update( expressionExperiment );
            this.getSessionFactory().getCurrentSession()
                    .createQuery( "delete from QuantitationType qt where qt in (:qts)" )
                    .setParameterList( "qts", optimizeIdentifiableParameterList( qtsToRemove ) );
        }
    }

    /**
     * Make sure the data are on a log2 scale
     *
     * @param  rawPreferredDataVectors             raw preferred data vectors
     * @param  preferredMaskedDataQuantitationType preferred masked data QT
     * @return collection containing the vectors
     */
    private Collection<RawExpressionDataVector> ensureLog2Scale(
            Collection<RawExpressionDataVector> rawPreferredDataVectors,
            QuantitationType preferredMaskedDataQuantitationType,
            boolean ignoreQuantitationMismatch ) throws QuantitationMismatchException {
        ExpressionDataDoubleMatrix matrix = ExpressionDataDoubleMatrixUtil
                .ensureLog2Scale( new ExpressionDataDoubleMatrix( rawPreferredDataVectors ), ignoreQuantitationMismatch );
        preferredMaskedDataQuantitationType.setScale( ScaleType.LOG2 );
        this.getSessionFactory().getCurrentSession().update( preferredMaskedDataQuantitationType );
        return new HashSet<>( matrix.toRawDataVectors() );
    }

    //
    //    @Override
    //    public ExpressionExperiment createProcessedDataVectors( ExpressionExperiment ee,
    //            Collection<ProcessedExpressionDataVector> data ) {
    //        if ( ee == null ) {
    //            throw new IllegalStateException( "ExpressionExperiment cannot be null" );
    //        }
    //
    //        ExpressionExperiment expressionExperiment = ( ExpressionExperiment ) this.getSessionFactory()
    //                .getCurrentSession().get( ExpressionExperiment.class, ee.getId() );
    //
    //        assert expressionExperiment != null;
    //
    //        this.removeProcessedDataVectors( expressionExperiment );
    //
    //        Hibernate.initialize( expressionExperiment );
    //        Hibernate.initialize( expressionExperiment.getQuantitationTypes() );
    //
    //        data = ExpressionDataDoubleMatrixUtil.ensureLog2Scale( data );
    //
    //        QuantitationType qt = data.iterator().next().getQuantitationType(); // assumes all are same.
    //        qt.setScale( ScaleType.LOG2 );
    //        this.getSession().update( qt );
    //
    //        expressionExperiment.getProcessedExpressionDataVectors().addAll( data );
    //
    //        this.getSessionFactory().getCurrentSession().saveOrUpdate( qt );
    //        expressionExperiment.getQuantitationTypes().add( data.iterator().next().getQuantitationType() );
    //        expressionExperiment.setNumberOfDataVectors( expressionExperiment.getProcessedExpressionDataVectors().size() );
    //
    //        this.getSessionFactory().getCurrentSession().update( expressionExperiment );
    //        assert expressionExperiment.getNumberOfDataVectors() != null;
    //
    //        this.processedDataVectorCache.clearCache( expressionExperiment.getId() );
    //
    //        return expressionExperiment;
    //
    //    }

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

    /**
     * @param newResults Always provide full vectors, not subsets.
     */
    private void cacheResults( Collection<DoubleVectorValueObject> newResults ) {
        /*
         * Break up by gene and EE to cache collections of vectors for EE-gene combos.
         */
        Map<Long, Map<Long, Collection<DoubleVectorValueObject>>> mapForCache = this.makeCacheMap( newResults );
        int i = 0;
        for ( Long eeid : mapForCache.keySet() ) {
            for ( Long g : mapForCache.get( eeid ).keySet() ) {
                i++;
                this.processedDataVectorCache.addToCache( eeid, g, mapForCache.get( eeid ).get( g ) );
            }
        }
        // WARNING cache size() can be slow, esp. terracotta.
        AbstractDao.log.debug( "Cached " + i + ", input " + newResults.size() + "; total cached: "
                /* + this.processedDataVectorCache.size() */ );
    }

    /**
     * We cache vectors at the experiment level. If we need subsets, we have to slice them out.
     *
     * @param bioAssaySets  that we exactly need the data for.
     * @param genes         that might have cached results
     * @param results       from the cache will be put here
     * @param needToSearch  experiments that need to be searched (not fully cached); this will be populated
     * @param genesToSearch that still need to be searched (not in cache)
     */
    private void checkCache( Collection<? extends BioAssaySet> bioAssaySets, Collection<Long> genes,
            Collection<DoubleVectorValueObject> results, Collection<ExpressionExperiment> needToSearch,
            Collection<Long> genesToSearch ) {

        for ( BioAssaySet ee : bioAssaySets ) {

            ExpressionExperiment experiment = null;
            boolean needSubSet = false;
            if ( ee instanceof ExpressionExperiment ) {
                experiment = ( ExpressionExperiment ) ee;
            } else if ( ee instanceof ExpressionExperimentSubSet ) {
                experiment = ( ( ExpressionExperimentSubSet ) ee ).getSourceExperiment();
                needSubSet = true;
            }

            assert experiment != null;

            for ( Long g : genes ) {
                Collection<DoubleVectorValueObject> obs = processedDataVectorCache.get( ee, g );
                if ( obs != null ) {
                    if ( needSubSet ) {
                        obs = this.sliceSubSet( ( ExpressionExperimentSubSet ) ee, obs );
                    }
                    results.addAll( obs );
                } else {
                    genesToSearch.add( g );
                }
            }
            /*
             * This experiment is not fully cached for the genes in question.
             */
            if ( genesToSearch.size() > 0 ) {
                needToSearch.add( experiment );
            }
        }
    }

    /**
     * @param  bioAssayDimensions See if anything is 'ragged' (fewer bioassays per biomaterial than in some other
     *                            sample)
     * @return bio assay dimension
     */
    private BioAssayDimension checkRagged( Collection<BioAssayDimension> bioAssayDimensions ) {
        int s = -1;
        int longest = -1;
        BioAssayDimension longestBad = null;
        for ( BioAssayDimension bad : bioAssayDimensions ) {
            Collection<BioAssay> assays = bad.getBioAssays();
            if ( s < 0 ) {
                s = assays.size();
            }

            if ( assays.size() > longest ) {
                longest = assays.size();
                longestBad = bad;
            }
        }
        return longestBad;
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

    private Collection<BioAssayDimension> getBioAssayDimensions( BioAssaySet ee ) {
        if ( ee instanceof ExpressionExperiment ) {
            StopWatch timer = new StopWatch();
            timer.start();
            //noinspection unchecked
            List<BioAssayDimension> r = this.getSessionFactory().getCurrentSession().createQuery(
                            // this does not look efficient.
                            "select bad from ExpressionExperiment e, BioAssayDimension bad "
                                    + "inner join e.bioAssays b "
                                    + "inner join bad.bioAssays badba "
                                    + "where e = :ee and b in (badba) "
                                    + "group by bad" )
                    .setParameter( "ee", ee )
                    .list();
            timer.stop();
            if ( timer.getTime() > 100 ) {
                AbstractDao.log.info( "Fetch " + r.size() + " bioassayDimensions for experiment id=" + ee.getId() + ": "
                        + timer.getTime() + "ms" );
            }
            return r;
        }

        // subset.
        return this.getBioAssayDimensions( this.getExperiment( ee ) );

    }

    private Map<BioAssaySet, Collection<BioAssayDimension>> getBioAssayDimensions(
            Collection<ExpressionExperiment> ees ) {
        Map<BioAssaySet, Collection<BioAssayDimension>> result = new HashMap<>();

        if ( ees.size() == 1 ) {
            ExpressionExperiment ee = ees.iterator().next();
            result.put( ee, this.getBioAssayDimensions( ee ) );
            return result;
        }

        StopWatch timer = new StopWatch();
        timer.start();
        //noinspection unchecked
        List<Object[]> r = this.getSessionFactory().getCurrentSession().createQuery(
                        "select e, bad from ExpressionExperiment e, BioAssayDimension bad "
                                + "inner join e.bioAssays b "
                                + "inner join bad.bioAssays badba "
                                + "where e in (:ees) and b in (badba) "
                                + "group by e, bad" )
                .setParameterList( "ees", optimizeIdentifiableParameterList( ees ) )
                .list();

        for ( Object[] o : r ) {
            BioAssaySet bas = ( BioAssaySet ) o[0];
            if ( !result.containsKey( bas ) )
                result.put( bas, new HashSet<>() );

            result.get( bas ).add( ( BioAssayDimension ) o[1] );
        }
        if ( timer.getTime() > 100 ) {
            AbstractDao.log
                    .info( "Fetch " + r.size() + " bioAssayDimensions for " + ees.size() + " experiment(s): " + timer
                            .getTime() + "ms" );
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
            Collection<? extends DesignElementDataVector> data ) {
        Map<BioAssayDimension, BioAssayDimensionValueObject> badVos = new HashMap<>();
        for ( DesignElementDataVector v : data ) {
            BioAssayDimension bioAssayDimension = v.getBioAssayDimension();
            if ( !badVos.containsKey( bioAssayDimension ) ) {
                badVos.put( bioAssayDimension, new BioAssayDimensionValueObject( bioAssayDimension ) );
            }
        }
        return badVos;
    }

    private ExpressionExperiment getExperiment( BioAssaySet bas ) {
        ExpressionExperiment e;
        if ( bas instanceof ExpressionExperiment ) {
            e = ( ExpressionExperiment ) bas;
        } else if ( bas instanceof ExpressionExperimentSubSet ) {
            e = ( ( ExpressionExperimentSubSet ) bas ).getSourceExperiment();
        } else {
            throw new UnsupportedOperationException( "Couldn't handle a " + bas.getClass() );
        }
        assert e != null;
        return e;
    }

    /**
     * Determine the experiments that bioAssaySets refer to.
     *
     * @param  bioAssaySets - either ExpressionExperiment or ExpressionExperimentSubSet (which has an associated
     *                      ExpressionExperiment, which is what we're after)
     * @return Note that this collection can be smaller than the input, if two bioAssaySets come from (or
     *                      are) the same
     *                      Experiment
     */
    private Collection<ExpressionExperiment> getExperiments( Collection<? extends BioAssaySet> bioAssaySets ) {
        Collection<ExpressionExperiment> result = new HashSet<>();

        for ( BioAssaySet bas : bioAssaySets ) {
            ExpressionExperiment e = this.getExperiment( bas );

            result.add( e );
        }
        return result;
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
            if ( oldqt.getName().equals( present.getName() ) ) { // FIXME make this a more stringent check for a match
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

    private Collection<DoubleVectorValueObject> getProcessedDataArraysByProbeIds( Collection<? extends BioAssaySet> ees,
            Collection<Long> probeIds ) {
        Collection<DoubleVectorValueObject> results = new HashSet<>();

        Map<Long, Collection<Long>> cs2gene = CommonQueries
                .getCs2GeneMapForProbes( probeIds, this.getSessionFactory().getCurrentSession() );

        Map<Long, Collection<Long>> noGeneProbes = new HashMap<>();
        for ( Long pid : probeIds ) {
            if ( !cs2gene.containsKey( pid ) || cs2gene.get( pid ).isEmpty() ) {
                noGeneProbes.put( pid, new HashSet<>() );
                cs2gene.remove( pid );
            }
        }

        AbstractDao.log.debug( cs2gene.size() + " probes associated with a gene; " + noGeneProbes.size() + " not" );

        /*
         * To Check the cache we need the list of genes 1st. Get from CS2Gene list then check the cache.
         */
        Collection<Long> genes = new HashSet<>();
        for ( Long cs : cs2gene.keySet() ) {
            genes.addAll( cs2gene.get( cs ) );
        }

        // this will be populated with experiments for which we don't have all the needed results cached
        Collection<ExpressionExperiment> needToSearch = new HashSet<>();
        // will contain IDs of genes that weren't covered by the cache
        Collection<Long> genesToSearch = new HashSet<>();
        this.checkCache( ees, genes, results, needToSearch, genesToSearch );

        if ( !results.isEmpty() )
            AbstractDao.log.debug( results.size() + " vectors fetched from cache" );

        /*
         * Get data that wasn't in the cache.
         *
         * Small problem: noGeneProbes are never really cached since we use the gene as part of that. So always need to get them.
         */
        Map<ProcessedExpressionDataVector, Collection<Long>> noncached = new HashMap<>();
        if ( !noGeneProbes.isEmpty() ) {
            Collection<ExpressionExperiment> eesForNoGeneProbes = new HashSet<>();
            for ( BioAssaySet ee : ees ) {
                if ( ee instanceof ExpressionExperiment ) {
                    eesForNoGeneProbes.add( ( ExpressionExperiment ) ee );
                } else {
                    eesForNoGeneProbes.add( ( ( ExpressionExperimentSubSet ) ee ).getSourceExperiment() );
                }
            }
            needToSearch.addAll( eesForNoGeneProbes );
            noncached.putAll( this.getProcessedVectorsAndGenes( eesForNoGeneProbes, noGeneProbes ) );
        }

        if ( !noncached.isEmpty() )
            AbstractDao.log.debug( noncached.size() + " vectors retrieved so far, for noGeneProbes" );

        /*
         * Non-cached items.
         */
        Map<ProcessedExpressionDataVector, Collection<Long>> moreNonCached = new HashMap<>();
        if ( !needToSearch.isEmpty() && !genesToSearch.isEmpty() ) {
            /*
             * cut cs2gene down, otherwise we're probably fetching everything again.
             */
            Map<Long, Collection<Long>> filteredcs2gene = cs2gene.entrySet().stream()
                    .filter( entry -> entry.getValue().stream().anyMatch( genesToSearch::contains ) )
                    .collect( Collectors.toMap( Map.Entry::getKey, Map.Entry::getValue ) );

            moreNonCached = this.getProcessedVectorsAndGenes( needToSearch, filteredcs2gene );
        }

        if ( !moreNonCached.isEmpty() )
            AbstractDao.log.debug( noncached.size() + " more fetched from db" );

        noncached.putAll( moreNonCached );

        /*
         * Deal with possibility of 'gaps' and unpack the vectors.
         */
        Collection<DoubleVectorValueObject> newResults = new HashSet<>();
        for ( ExpressionExperiment ee : needToSearch ) {

            Collection<BioAssayDimension> bioAssayDimensions = this.getBioAssayDimensions( ee );

            if ( bioAssayDimensions.size() == 1 ) {
                newResults.addAll( this.unpack( noncached ) );
            } else {
                /*
                 * See handleGetProcessedExpressionDataArrays(Collection<? extends BioAssaySet>, Collection<Gene>,
                 * boolean) and bug 1704.
                 */
                BioAssayDimension longestBad = this.checkRagged( bioAssayDimensions );
                assert longestBad != null;
                newResults.addAll( this.unpack( noncached, longestBad ) );
            }

            if ( !newResults.isEmpty() ) {
                this.cacheResults( newResults );

                newResults = this.sliceSubsets( ees, newResults );

                results.addAll( newResults );
            }
        }

        return results;
    }

    /**
     * Obtain processed expression vectors with their associated genes.
     *
     * @param  cs2gene Map of probe to genes.
     * @param  ees     ees
     * @return map of vectors to genes.
     */
    private Map<ProcessedExpressionDataVector, Collection<Long>> getProcessedVectorsAndGenes( @Nullable Collection<ExpressionExperiment> ees,
            Map<Long, Collection<Long>> cs2gene ) {
        if ( ( ees != null && ees.isEmpty() ) || cs2gene.isEmpty() ) {
            return Collections.emptyMap();
        }

        StopWatch timer = StopWatch.createStarted();

        // Do not do in clause for experiments, as it can't use the indices
        Query queryObject = this.getSessionFactory().getCurrentSession().createQuery(
                        "select dedv, dedv.designElement.id from ProcessedExpressionDataVector dedv fetch all properties"
                                + " where dedv.designElement.id in ( :cs ) "
                                + ( ees != null ? " and dedv.expressionExperiment in :ees" : "" ) )
                .setParameterList( "cs", optimizeParameterList( cs2gene.keySet() ) );
        if ( ees != null ) {
            queryObject.setParameterList( "ees", optimizeIdentifiableParameterList( ees ) );
        }
        Map<ProcessedExpressionDataVector, Collection<Long>> dedv2genes = new HashMap<>();
        //noinspection unchecked
        List<Object[]> results = queryObject
                .setFlushMode( FlushMode.MANUAL )
                .setReadOnly( true )
                .list();
        for ( Object[] row : results ) {
            ProcessedExpressionDataVector dedv = ( ProcessedExpressionDataVector ) row[0];
            Long cs = ( Long ) row[1];
            Collection<Long> associatedGenes = cs2gene.get( cs );
            if ( !dedv2genes.containsKey( dedv ) ) {
                dedv2genes.put( dedv, associatedGenes );
            } else {
                Collection<Long> mappedGenes = dedv2genes.get( dedv );
                mappedGenes.addAll( associatedGenes );
            }
        }

        if ( timer.getTime() > Math.max( 200, 20 * dedv2genes.size() ) ) {
            AbstractDao.log.warn( String.format( "Fetched %d vectors for %d probes in %dms",
                    dedv2genes.size(), cs2gene.size(), timer.getTime() ) );

        }

        return dedv2genes;
    }

    /**
     * @param  limit if non-null and positive, you will get a random set of vectors for the experiment
     * @param  ee    ee
     * @return processed data vectors
     */
    private Collection<ProcessedExpressionDataVector> getProcessedVectors( ExpressionExperiment ee, int limit ) {

        if ( limit <= 0 ) {
            return this.getProcessedVectors( ee );
        }

        StopWatch timer = new StopWatch();
        timer.start();
        List<ProcessedExpressionDataVector> result;

        Integer availableVectorCount = ee.getNumberOfDataVectors();
        if ( availableVectorCount == null || availableVectorCount == 0 ) {
            AbstractDao.log.info( "Experiment does not have vector count populated." );
            // cannot fix this here, because we're read-only.
        }

        Query q = this.getSessionFactory().getCurrentSession()
                .createQuery( " from ProcessedExpressionDataVector dedv "
                        + "where dedv.expressionExperiment = :ee" );
        q.setParameter( "ee", ee );
        q.setMaxResults( limit );
        if ( availableVectorCount != null && availableVectorCount > limit ) {
            q.setFirstResult( new Random().nextInt( availableVectorCount - limit ) );
        }

        // we should already be read-only, so this is probably pointless.
        q.setReadOnly( true );

        // and so this probably doesn't do anything useful.
        q.setFlushMode( FlushMode.MANUAL );

        //noinspection unchecked
        result = q.list();
        if ( timer.getTime() > 1000 )
            AbstractDao.log
                    .info( "Fetch " + limit + " vectors from " + ee.getShortName() + ": " + timer.getTime() + "ms" );

        if ( result.isEmpty() ) {
            AbstractDao.log.warn( "Experiment does not have any processed data vectors" );
            return result;
        }

        this.thaw( result ); // needed?
        return result;
    }

    /**
     * This is an important method for fetching vectors.
     *
     * @param  genes genes
     * @param  ees   ees
     * @return vectors, possibly subsetted.
     */
    private Collection<DoubleVectorValueObject> handleGetProcessedExpressionDataArrays(
            Collection<? extends BioAssaySet> ees, Collection<Long> genes ) {

        // ees must be thawed first as currently implemented (?)

        Collection<DoubleVectorValueObject> results = new HashSet<>();

        /*
         * Check the cache.
         */
        Collection<ExpressionExperiment> needToSearch = new HashSet<>();
        Collection<Long> genesToSearch = new HashSet<>();
        this.checkCache( ees, genes, results, needToSearch, genesToSearch );
        AbstractDao.log.info( "Using " + results.size() + " DoubleVectorValueObject(s) from cache" );

        if ( needToSearch.size() == 0 ) {
            return results;
        }

        /*
         * Get items not in the cache.
         */
        AbstractDao.log.info( "Searching for vectors for " + genes.size() + " genes from " + needToSearch.size()
                + " experiments not in cache" );

        Collection<ArrayDesign> arrays = CommonQueries
                .getArrayDesignsUsed( EntityUtils.getIds( this.getExperiments( ees ) ),
                        this.getSessionFactory().getCurrentSession() )
                .keySet();
        assert !arrays.isEmpty();
        Map<Long, Collection<Long>> cs2gene = CommonQueries
                .getCs2GeneIdMap( genesToSearch, EntityUtils.getIds( arrays ),
                        this.getSessionFactory().getCurrentSession() );

        if ( cs2gene.size() == 0 ) {
            if ( results.isEmpty() ) {
                AbstractDao.log.warn( "No composite sequences found for genes" );
                return new HashSet<>();
            }
            return results;
        }

        /*
         * Fill in the map, because we want to track information on the specificity of the probes used in the data
         * vectors.
         */
        cs2gene = CommonQueries
                .getCs2GeneMapForProbes( cs2gene.keySet(), this.getSessionFactory().getCurrentSession() );

        Map<ProcessedExpressionDataVector, Collection<Long>> processedDataVectors = this
                .getProcessedVectorsAndGenes( needToSearch, cs2gene );

        Map<BioAssaySet, Collection<BioAssayDimension>> bioAssayDimensions = this.getBioAssayDimensions( needToSearch );

        Collection<DoubleVectorValueObject> newResults = new HashSet<>();

        /*
         * This loop is to ensure that we don't get misaligned vectors for experiments that use more than one array
         * design. See bug 1704. This isn't that common, so we try to break out as soon as possible.
         */
        for ( BioAssaySet bas : needToSearch ) {

            Collection<BioAssayDimension> dims = bioAssayDimensions.get( bas );

            if ( dims == null || dims.isEmpty() ) {
                AbstractDao.log.warn( "BioAssayDimensions were null/empty unexpectedly." );
                continue;
            }

            /*
             * Get the vectors for just this experiment. This is made more efficient by removing things from the map
             * each time through.
             */
            Map<ProcessedExpressionDataVector, Collection<Long>> vecsForBas = new HashMap<>();
            if ( needToSearch.size() == 1 ) {
                vecsForBas = processedDataVectors;
            } else {
                // isolate the vectors for the current experiment.
                for ( Iterator<ProcessedExpressionDataVector> it = processedDataVectors.keySet().iterator(); it
                        .hasNext(); ) {
                    ProcessedExpressionDataVector v = it.next();
                    if ( v.getExpressionExperiment().equals( bas ) ) {
                        vecsForBas.put( v, processedDataVectors.get( v ) );
                        it.remove(); // since we're done with it.
                    }
                }
            }

            /*
             * Now see if anything is 'ragged' (fewer bioassays per biomaterial than in some other vector)
             */
            if ( dims.size() == 1 ) {
                newResults.addAll( this.unpack( vecsForBas ) );
            } else {
                BioAssayDimension longestBad = this.checkRagged( dims );
                if ( longestBad == null ) {
                    newResults.addAll( this.unpack( vecsForBas ) );
                } else {
                    newResults.addAll( this.unpack( vecsForBas, longestBad ) );
                }
            }
        }

        /*
         * Finally....
         */

        if ( !newResults.isEmpty() ) {
            this.cacheResults( newResults );
            newResults = this.sliceSubsets( ees, newResults );
            results.addAll( newResults );
        }

        return results;

    }

    /**
     * @param  expressionExperiment ee
     * @return true if any platform used by the ee is two-channel (including dual-mode)
     */
    private boolean isTwoChannel( ExpressionExperiment expressionExperiment ) {

        boolean isTwoChannel = false;
        Collection<ArrayDesign> arrayDesignsUsed = CommonQueries
                .getArrayDesignsUsed( expressionExperiment, this.getSessionFactory().getCurrentSession() );
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

    private Map<Long, Map<Long, Collection<DoubleVectorValueObject>>> makeCacheMap(
            Collection<DoubleVectorValueObject> newResults ) {
        Map<Long, Map<Long, Collection<DoubleVectorValueObject>>> mapForCache = new HashMap<>();
        for ( DoubleVectorValueObject v : newResults ) {
            ExpressionExperimentValueObject e = v.getExpressionExperiment();
            if ( !mapForCache.containsKey( e.getId() ) ) {
                mapForCache.put( e.getId(), new HashMap<>() );
            }
            Map<Long, Collection<DoubleVectorValueObject>> innerMap = mapForCache.get( e.getId() );
            for ( Long g : v.getGenes() ) {
                if ( !innerMap.containsKey( g ) ) {
                    innerMap.put( g, new HashSet<>() );
                }
                innerMap.get( g ).add( v );
            }
        }
        return mapForCache;
    }

    /**
     * Mask missing values. This is mostly for two-color (ratiometric) data.
     *
     */
    private Map<CompositeSequence, DoubleVectorValueObject> maskAndUnpack(
            Collection<RawExpressionDataVector> preferredData, Collection<RawExpressionDataVector> missingValueData ) {
        Map<CompositeSequence, DoubleVectorValueObject> unpackedData = this.unpack( preferredData );

        if ( missingValueData.size() == 0 ) {
            AbstractDao.log.debug( "There is no separate missing data information, simply using the data as is" );
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
                if ( !warned && AbstractDao.log.isWarnEnabled() )
                    AbstractDao.log.warn( "No mask vector for " + de
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

    /**
     * @param  ee  ee
     * @param  obs obs
     * @return Given an ExpressionExperimentSubset and vectors from the source experiment, give vectors that include
     *             just the
     *             data for the subset.
     */
    private Collection<DoubleVectorValueObject> sliceSubSet( ExpressionExperimentSubSet ee,
            Collection<DoubleVectorValueObject> obs ) {

        Collection<DoubleVectorValueObject> sliced = new HashSet<>();
        if ( obs == null || obs.isEmpty() )
            return sliced;

        Hibernate.initialize( ee.getBioAssays() );
        List<BioAssayValueObject> sliceBioAssays = new ArrayList<>();

        DoubleVectorValueObject exemplar = obs.iterator().next();

        BioAssayDimensionValueObject bad = new BioAssayDimensionValueObject( -1L );
        bad.setName( "Subset of :" + exemplar.getBioAssayDimension().getName() );
        bad.setDescription( "Subset slice" );
        bad.setSourceBioAssayDimension( exemplar.getBioAssayDimension() );
        bad.setIsSubset( true );
        Collection<Long> subsetBioAssayIds = EntityUtils.getIds( ee.getBioAssays() );

        for ( BioAssayValueObject ba : exemplar.getBioAssays() ) {
            if ( !subsetBioAssayIds.contains( ba.getId() ) ) {
                continue;
            }

            sliceBioAssays.add( ba );
        }

        bad.addBioAssays( sliceBioAssays );
        for ( DoubleVectorValueObject vec : obs ) {
            DoubleVectorValueObject s = new DoubleVectorValueObject( ee, vec, bad );
            sliced.add( s );
        }

        return sliced;
    }

    /**
     * @param  ees  Experiments and/or subsets required
     * @param  vecs vectors to select from and if necessary slice, obviously from the given ees.
     * @return vectors that are for the requested subset. If an ee is not a subset, vectors will be unchanged.
     *              Otherwise
     *              the data in a vector will be for the subset of samples in the ee subset.
     */
    private Collection<DoubleVectorValueObject> sliceSubsets( Collection<? extends BioAssaySet> ees,
            Collection<DoubleVectorValueObject> vecs ) {
        Collection<DoubleVectorValueObject> results = new HashSet<>();
        if ( vecs == null || vecs.isEmpty() )
            return results;

        for ( BioAssaySet bas : ees ) {
            if ( bas instanceof ExpressionExperimentSubSet ) {

                for ( DoubleVectorValueObject d : vecs ) {
                    if ( d.getExpressionExperiment().getId()
                            .equals( ( ( ExpressionExperimentSubSet ) bas ).getSourceExperiment().getId() ) ) {

                        Collection<DoubleVectorValueObject> ddvos = new HashSet<>();
                        ddvos.add( d );
                        results.addAll( this.sliceSubSet( ( ExpressionExperimentSubSet ) bas, ddvos ) );// coll

                    }
                }

            } else {
                for ( DoubleVectorValueObject d : vecs ) {
                    if ( d.getExpressionExperiment().getId().equals( bas.getId() ) ) {
                        results.add( d );
                    }
                }
            }

        }

        return results;
    }

    private Map<CompositeSequence, DoubleVectorValueObject> unpack(
            Collection<? extends DesignElementDataVector> data ) {
        Map<CompositeSequence, DoubleVectorValueObject> result = new HashMap<>();
        Map<BioAssayDimension, BioAssayDimensionValueObject> badVos = this.getBioAssayDimensionValueObjects( data );
        for ( DesignElementDataVector v : data ) {
            result.put( v.getDesignElement(),
                    new DoubleVectorValueObject( v, badVos.get( v.getBioAssayDimension() ) ) );
        }
        return result;
    }

    private Map<CompositeSequence, DoubleVectorValueObject> unpack( Collection<? extends DesignElementDataVector> data,
            Map<Long, Collection<Long>> cs2GeneMap ) {
        Map<CompositeSequence, DoubleVectorValueObject> result = new HashMap<>();
        Map<BioAssayDimension, BioAssayDimensionValueObject> badVos = this.getBioAssayDimensionValueObjects( data );
        for ( DesignElementDataVector v : data ) {
            result.put( v.getDesignElement(),
                    new DoubleVectorValueObject( v, cs2GeneMap.get( v.getDesignElement().getId() ),
                            badVos.get( v.getBioAssayDimension() ) ) );
        }
        return result;
    }

    private Collection<DoubleVectorValueObject> unpack( Collection<? extends DesignElementDataVector> data,
            Map<Long, Collection<Long>> cs2GeneMap, BioAssayDimension longestBad ) {
        Collection<DoubleVectorValueObject> result = new HashSet<>();
        Map<BioAssayDimension, BioAssayDimensionValueObject> badVos = this.getBioAssayDimensionValueObjects( data );
        for ( DesignElementDataVector v : data ) {
            result.add( new DoubleVectorValueObject( v, badVos.get( v.getBioAssayDimension() ),
                    cs2GeneMap.get( v.getDesignElement().getId() ), longestBad ) );
        }
        return result;
    }

    private Collection<DoubleVectorValueObject> unpack(
            Map<? extends DesignElementDataVector, Collection<Long>> data ) {
        Collection<DoubleVectorValueObject> result = new HashSet<>();
        Map<BioAssayDimension, BioAssayDimensionValueObject> badVos = this
                .getBioAssayDimensionValueObjects( data.keySet() );

        for ( DesignElementDataVector v : data.keySet() ) {
            result.add( new DoubleVectorValueObject( v, data.get( v ), badVos.get( v.getBioAssayDimension() ) ) );
        }
        return result;
    }

    private Collection<? extends DoubleVectorValueObject> unpack(
            Map<ProcessedExpressionDataVector, Collection<Long>> data, BioAssayDimension longestBad ) {
        Collection<DoubleVectorValueObject> result = new HashSet<>();
        Map<BioAssayDimension, BioAssayDimensionValueObject> badVos = this
                .getBioAssayDimensionValueObjects( data.keySet() );
        for ( ProcessedExpressionDataVector v : data.keySet() ) {
            result.add( new DoubleVectorValueObject( v, badVos.get( v.getBioAssayDimension() ), data.get( v ),
                    longestBad ) );
        }
        return result;
    }

    private Collection<BooleanVectorValueObject> unpackBooleans( Collection<? extends DesignElementDataVector> data ) {
        Collection<BooleanVectorValueObject> result = new HashSet<>();

        Map<BioAssayDimension, BioAssayDimensionValueObject> badVos = this.getBioAssayDimensionValueObjects( data );

        for ( DesignElementDataVector v : data ) {
            result.add( new BooleanVectorValueObject( v, badVos.get( v.getBioAssayDimension() ) ) );
        }
        return result;
    }
}
