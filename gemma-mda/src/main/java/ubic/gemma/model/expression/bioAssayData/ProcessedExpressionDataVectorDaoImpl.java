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
package ubic.gemma.model.expression.bioAssayData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.FlushMode;
import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.type.LongType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeImpl;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentImpl;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.util.CommonQueries;
import ubic.gemma.util.EntityUtils;

/**
 * @author Paul
 * @version $Id$
 */
@Repository
public class ProcessedExpressionDataVectorDaoImpl extends DesignElementDataVectorDaoImpl<ProcessedExpressionDataVector>
        implements ProcessedExpressionDataVectorDao {

    private static Log log = LogFactory.getLog( ProcessedExpressionDataVectorDaoImpl.class.getName() );

    @Autowired
    private ProcessedDataVectorCache processedDataVectorCache;

    @Autowired
    public ProcessedExpressionDataVectorDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    @Override
    public void clearCache() {
        processedDataVectorCache.clearCache();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorDao#createProcessedDataVectors(ubic.gemma
     * .model.expression.experiment.ExpressionExperiment)
     */
    @Override
    public ExpressionExperiment createProcessedDataVectors( ExpressionExperiment ee ) {
        if ( ee == null ) {
            throw new IllegalStateException( "ExpressionExperiment cannot be null" );
        }

        ExpressionExperiment expressionExperiment = getHibernateTemplate().get( ExpressionExperimentImpl.class,
                ee.getId() );

        assert expressionExperiment != null;

        removeProcessedDataVectors( expressionExperiment );

        Hibernate.initialize( expressionExperiment );
        Hibernate.initialize( expressionExperiment.getQuantitationTypes() );
        Hibernate.initialize( expressionExperiment.getProcessedExpressionDataVectors() );

        expressionExperiment.getProcessedExpressionDataVectors().clear();

        log.info( "Computing processed expression vectors for " + expressionExperiment );

        boolean isTwoChannel = isTwoChannel( expressionExperiment );

        Collection<RawExpressionDataVector> missingValueVectors = new HashSet<RawExpressionDataVector>();
        if ( isTwoChannel ) {
            missingValueVectors = this.getMissingValueVectors( expressionExperiment );
        }

        Collection<RawExpressionDataVector> preferredDataVectors = this.getPreferredDataVectors( expressionExperiment );
        if ( preferredDataVectors.isEmpty() ) {
            throw new IllegalArgumentException( "No preferred data vectors for " + expressionExperiment );
        }

        Map<CompositeSequence, DoubleVectorValueObject> maskedVectorObjects = maskAndUnpack( preferredDataVectors,
                missingValueVectors );

        /*
         * Create the vectors. Do a sanity check that we don't have more than we should
         */
        Collection<CompositeSequence> seenDes = new HashSet<CompositeSequence>();
        QuantitationType preferredMaskedDataQuantitationType = getPreferredMaskedDataQuantitationType( preferredDataVectors
                .iterator().next().getQuantitationType() );

        int i = 0;
        for ( CompositeSequence cs : maskedVectorObjects.keySet() ) {

            DoubleVectorValueObject dvvo = maskedVectorObjects.get( cs );

            if ( seenDes.contains( cs ) ) {
                // defensive programming, this happens.
                throw new IllegalStateException( "Duplicated design element: " + cs
                        + "; make sure the experiment has only one 'preferred' quantitation type. "
                        + "Perhaps you need to run vector merging following an array desing switch?" );
            }

            ProcessedExpressionDataVector vec = ( ProcessedExpressionDataVector ) dvvo.toDesignElementDataVector( ee,
                    cs, preferredMaskedDataQuantitationType );

            expressionExperiment.getProcessedExpressionDataVectors().add( vec );
            seenDes.add( cs );
            if ( ++i % 5000 == 0 ) {
                log.info( i + " vectors built" );
            }
        }

        log.info( "Persisting " + expressionExperiment.getProcessedExpressionDataVectors().size()
                + " processed data vectors" );

        expressionExperiment.getQuantitationTypes().add( preferredMaskedDataQuantitationType );
        expressionExperiment.setNumberOfDataVectors( expressionExperiment.getProcessedExpressionDataVectors().size() );

        this.getHibernateTemplate().update( expressionExperiment );

        this.processedDataVectorCache.clearCache( expressionExperiment.getId() );

        return expressionExperiment;

    }

    private boolean isTwoChannel( ExpressionExperiment expressionExperiment ) {
        /*
         * Figure out if it is two-channel
         */
        boolean isTwoChannel = false;
        Collection<ArrayDesign> arrayDesignsUsed = CommonQueries.getArrayDesignsUsed( expressionExperiment,
                this.getSession() );
        for ( ArrayDesign ad : arrayDesignsUsed ) {
            TechnologyType technologyType = ad.getTechnologyType();

            if ( technologyType == null ) {
                throw new IllegalStateException(
                        "Array designs must have a technology type assigned before processed vector computation" );
            }

            if ( !technologyType.equals( TechnologyType.ONECOLOR ) && !technologyType.equals( TechnologyType.NONE ) ) {
                isTwoChannel = true;
            }
        }
        return isTwoChannel;
    }

    @Override
    public Collection<? extends DesignElementDataVector> find( ArrayDesign arrayDesign,
            QuantitationType quantitationType ) {
        final String queryString = "select dev from ProcessedExpressionDataVectorImpl dev  inner join fetch dev.bioAssayDimension bd "
                + " inner join fetch dev.designElement de inner join fetch dev.quantitationType where dev.designElement in (:desEls) "
                + "and dev.quantitationType = :quantitationType ";
        try {
            org.hibernate.Query queryObject = super.getSession().createQuery( queryString );
            queryObject.setParameter( "quantitationType", quantitationType );

            Collection<CompositeSequence> batch = new HashSet<CompositeSequence>();
            Collection<RawExpressionDataVector> result = new HashSet<RawExpressionDataVector>();
            int batchSize = 2000;
            for ( CompositeSequence cs : arrayDesign.getCompositeSequences() ) {
                batch.add( cs );

                if ( batch.size() >= batchSize ) {
                    queryObject.setParameterList( "desEls", batch );
                    result.addAll( queryObject.list() );
                    batch.clear();
                }
            }

            if ( batch.size() > 0 ) {
                queryObject.setParameterList( "desEls", batch );
                result.addAll( queryObject.list() );
            }

            return result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    @Override
    public Collection<ProcessedExpressionDataVector> find( Collection<QuantitationType> quantitationTypes ) {
        final String queryString = "select dev from ProcessedExpressionDataVectorImpl dev   where  "
                + "  dev.quantitationType in ( :quantitationTypes) ";
        return this.getHibernateTemplate().findByNamedParam( queryString, "quantitationTypes", quantitationTypes );

    }

    @Override
    public Collection<ProcessedExpressionDataVector> find( QuantitationType quantitationType ) {
        final String queryString = "select dev from ProcessedExpressionDataVectorImpl dev   where  "
                + "  dev.quantitationType = :quantitationType ";
        return this.getHibernateTemplate().findByNamedParam( queryString, "quantitationType", quantitationType );

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorDao#getProcessedDataArrays(ubic.gemma.model
     * .expression.experiment.BioAssaySet)
     */
    @Override
    public Collection<DoubleVectorValueObject> getProcessedDataArrays( BioAssaySet expressionExperiment ) {
        return getProcessedDataArrays( expressionExperiment, -1 );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorDao#getProcessedDataMatrix(ubic.gemma.model
     * .expression.experiment.ExpressionExperiment, java.util.Collection)
     */
    @Override
    public Collection<DoubleVectorValueObject> getProcessedDataArrays( BioAssaySet expressionExperiment,
            Collection<Long> genes ) {
        Collection<BioAssaySet> expressionExperiments = new HashSet<BioAssaySet>();
        expressionExperiments.add( expressionExperiment );
        return this.handleGetProcessedExpressionDataArrays( expressionExperiments, genes );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorDao#getProcessedDataArrays(ubic.gemma.model
     * .expression.experiment.BioAssaySet, int, boolean)
     */
    @Override
    public Collection<DoubleVectorValueObject> getProcessedDataArrays( BioAssaySet ee, int limit ) {

        Collection<ProcessedExpressionDataVector> pedvs = this.getProcessedVectors( getExperiment( ee ), limit );

        Collection<Long> probes = new ArrayList<Long>();
        for ( ProcessedExpressionDataVector pedv : pedvs ) {
            probes.add( pedv.getDesignElement().getId() );
        }

        if ( probes.isEmpty() ) {
            return unpack( pedvs ).values();
        }

        Map<Long, Collection<Long>> cs2gene = CommonQueries.getCs2GeneMapForProbes( probes, this.getSession() );

        Collection<BioAssayDimension> bioAssayDimensions = this.getBioAssayDimensions( ee );

        if ( bioAssayDimensions.size() == 1 ) {
            return unpack( pedvs, cs2gene ).values();
        }

        /*
         * deal with 'misalignment problem'
         */

        BioAssayDimension longestBad = checkRagged( bioAssayDimensions );

        if ( longestBad != null ) {
            return unpack( pedvs, cs2gene, longestBad );
        }
        return unpack( pedvs, cs2gene ).values();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorDao#getProcessedDataArrays(java.util.Collection
     * , java.util.Collection, boolean)
     */
    @Override
    public Collection<DoubleVectorValueObject> getProcessedDataArrays(
            Collection<? extends BioAssaySet> expressionExperiments, Collection<Long> genes ) {
        return this.handleGetProcessedExpressionDataArrays( expressionExperiments, genes );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorDao#getProcessedDataArraysByProbe(java.
     * util.Collection, java.util.Collection, boolean)
     */
    @Override
    @SuppressWarnings("unchecked")
    public Collection<DoubleVectorValueObject> getProcessedDataArraysByProbe( Collection<? extends BioAssaySet> ees,
            Collection<CompositeSequence> probes ) {

        Collection<DoubleVectorValueObject> results = new HashSet<DoubleVectorValueObject>();

        if ( probes.isEmpty() ) return results;

        Map<Long, Collection<Long>> cs2gene = CommonQueries.getCs2GeneMapForProbes( EntityUtils.getIds( probes ),
                this.getSession() );

        Map<Long, Collection<Long>> noGeneProbes = new HashMap<Long, Collection<Long>>();

        for ( CompositeSequence p : probes ) {
            Long pid = p.getId();
            if ( !cs2gene.containsKey( pid ) || cs2gene.get( pid ).isEmpty() ) {
                noGeneProbes.put( pid, new HashSet<Long>() );
                cs2gene.remove( pid );
            }
        }

        /*
         * To Check the cache we need the list of genes 1st. Get from CS2Gene list then check the cache.
         */
        Collection<Long> genes = new HashSet<Long>();
        for ( Long cs : cs2gene.keySet() ) {
            genes.addAll( cs2gene.get( cs ) );
        }

        Collection<ExpressionExperiment> needToSearch = new HashSet<ExpressionExperiment>();
        Collection<Long> genesToSearch = new HashSet<Long>();
        checkCache( ees, genes, results, needToSearch, genesToSearch );

        Map<ProcessedExpressionDataVector, Collection<Long>> rawResults = new HashMap<ProcessedExpressionDataVector, Collection<Long>>();

        /*
         * Small problem: noGeneProbes are never really cached since we use the gene as part of that.
         */
        if ( !noGeneProbes.isEmpty() ) {
            Collection<ExpressionExperiment> eesForNoGeneProbes = new HashSet<ExpressionExperiment>();
            eesForNoGeneProbes.addAll( ( Collection<? extends ExpressionExperiment> ) ees );
            rawResults.putAll( getProcessedVectors( EntityUtils.getIds( eesForNoGeneProbes ), noGeneProbes ) );
        }

        /*
         * Non-cached items.
         */
        if ( !needToSearch.isEmpty() ) {
            rawResults.putAll( getProcessedVectors( EntityUtils.getIds( needToSearch ), cs2gene ) );
        }

        /*
         * Deal with possibility of 'gaps' and unpack the vectors.
         */
        Collection<DoubleVectorValueObject> newResults = new HashSet<DoubleVectorValueObject>();
        for ( ExpressionExperiment ee : needToSearch ) {

            Collection<BioAssayDimension> bioAssayDimensions = this.getBioAssayDimensions( ee );

            if ( bioAssayDimensions.size() == 1 ) {
                newResults.addAll( unpack( rawResults ) );
            } else {
                /*
                 * See handleGetProcessedExpressionDataArrays(Collection<? extends BioAssaySet>, Collection<Gene>,
                 * boolean) and bug 1704.
                 */
                BioAssayDimension longestBad = checkRagged( bioAssayDimensions );
                if ( longestBad != null ) {
                    newResults.addAll( unpack( rawResults, longestBad ) );
                }
            }

            cacheResults( newResults );

            newResults = sliceSubsets( ees, newResults );

            results.addAll( newResults );
        }

        return results;

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorDao#getProcessedVectors(ubic.gemma.model
     * .expression.experiment.ExpressionExperiment)
     */
    @Override
    public Collection<ProcessedExpressionDataVector> getProcessedVectors( ExpressionExperiment ee ) {
        final String queryString = " from ProcessedExpressionDataVectorImpl dedv where dedv.expressionExperiment.id = :ee";
        return this.getHibernateTemplate().findByNamedParam( queryString, "ee", ee.getId() );
    }

    /**
     * @param ee
     * @param limit if non-null and positive, you will get a random set of vectors for the experiment
     * @return
     */
    @Override
    public Collection<ProcessedExpressionDataVector> getProcessedVectors( ExpressionExperiment ee, Integer limit ) {

        if ( limit == null || limit < 0 ) {
            return this.getProcessedVectors( ee );
        }

        StopWatch timer = new StopWatch();
        timer.start();
        List<ProcessedExpressionDataVector> result = new ArrayList<ProcessedExpressionDataVector>();

        Integer numvecsavailable = ee.getNumberOfDataVectors();
        if ( numvecsavailable == null || numvecsavailable == 0 ) {
            log.info( "Experiment does not have any processed vectors" );
            return result;
        }

        Query q = this.getSession().createQuery(
                " from ProcessedExpressionDataVectorImpl dedv where dedv.expressionExperiment.id = :ee" );
        q.setReadOnly( true );
        q.setFlushMode( FlushMode.MANUAL );
        q.setParameter( "ee", ee.getId(), LongType.INSTANCE );
        q.setMaxResults( limit );
        if ( numvecsavailable > limit ) {
            q.setFirstResult( RandomUtils.nextInt( numvecsavailable - limit ) );
        }
        result = q.list();
        if ( timer.getTime() > 1000 )
            log.info( "Fetch " + limit + " vectors from " + ee.getShortName() + ": " + timer.getTime() + "ms" );
        this.thaw( result ); // needed?
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorDao#getRanks(java.util.Collection,
     * java.util.Collection, ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorDao.RankMethod)
     */
    @Override
    public Map<ExpressionExperiment, Map<Gene, Collection<Double>>> getRanks(
            Collection<ExpressionExperiment> expressionExperiments, Collection<Gene> genes, RankMethod method ) {
        Map<CompositeSequence, Collection<Gene>> cs2gene = CommonQueries.getCs2GeneMap( genes, this.getSession() );
        if ( cs2gene.keySet().size() == 0 ) {
            log.warn( "No composite sequences found for genes" );
            return new HashMap<ExpressionExperiment, Map<Gene, Collection<Double>>>();
        }

        final String queryString = "select distinct dedv.expressionExperiment, dedv.designElement, dedv.rankByMean, dedv.rankByMax from ProcessedExpressionDataVectorImpl dedv "
                + " where dedv.designElement in ( :cs ) and dedv.expressionExperiment in (:ees) ";

        List<?> qr = this.getHibernateTemplate().findByNamedParam( queryString, new String[] { "cs", "ees" },
                new Object[] { cs2gene.keySet(), expressionExperiments } );

        Map<ExpressionExperiment, Map<Gene, Collection<Double>>> result = new HashMap<ExpressionExperiment, Map<Gene, Collection<Double>>>();
        for ( Object o : qr ) {
            Object[] oa = ( Object[] ) o;
            ExpressionExperiment e = ( ExpressionExperiment ) oa[0];
            CompositeSequence d = ( CompositeSequence ) oa[1];
            Double rMean = ( Double ) oa[2];
            Double rMax = ( Double ) oa[3];

            if ( !result.containsKey( e ) ) {
                result.put( e, new HashMap<Gene, Collection<Double>>() );
            }

            Map<Gene, Collection<Double>> rmap = result.get( e );

            Collection<Gene> genes4probe = cs2gene.get( d );

            for ( Gene gene : genes4probe ) {
                if ( !rmap.containsKey( gene ) ) {
                    rmap.put( gene, new ArrayList<Double>() );
                }
                switch ( method ) {
                    case mean:
                        rmap.get( gene ).add( rMean );
                        break;
                    case max:
                        rmap.get( gene ).add( rMax );
                        break;
                    default:
                        break;
                }
            }
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorDao#getRanks(ubic.gemma.model.expression
     * .experiment.ExpressionExperiment, java.util.Collection,
     * ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorDao.RankMethod)
     */
    @Override
    public Map<Gene, Collection<Double>> getRanks( ExpressionExperiment expressionExperiment, Collection<Gene> genes,
            RankMethod method ) {
        Map<CompositeSequence, Collection<Gene>> cs2gene = CommonQueries.getCs2GeneMap( genes, this.getSession() );
        if ( cs2gene.keySet().size() == 0 ) {
            log.warn( "No composite sequences found for genes" );
            return new HashMap<Gene, Collection<Double>>();
        }

        final String queryString = "select distinct dedv.designElement, dedv.rankByMean, dedv.rankByMax from ProcessedExpressionDataVectorImpl dedv "
                + " inner join fetch dedv.bioAssayDimension bd "
                + " inner join dedv.designElement de  "
                + " where dedv.designElement in ( :cs ) and dedv.expressionExperiment.id = :eeid ";

        List<?> qr = this.getHibernateTemplate().findByNamedParam( queryString, new String[] { "cs", "eeid" },
                new Object[] { cs2gene.keySet(), expressionExperiment.getId() } );

        Map<Gene, Collection<Double>> result = new HashMap<Gene, Collection<Double>>();
        for ( Object o : qr ) {
            Object[] oa = ( Object[] ) o;
            CompositeSequence d = ( CompositeSequence ) oa[0];
            Double rMean = ( Double ) oa[1];
            Double rMax = ( Double ) oa[2];

            Collection<Gene> genes4probe = cs2gene.get( d );

            for ( Gene gene : genes4probe ) {
                if ( !result.containsKey( gene ) ) {
                    result.put( gene, new ArrayList<Double>() );
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
        return result;

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorDao#getRanks(ubic.gemma.model.expression
     * .experiment.ExpressionExperiment,
     * ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorDao.RankMethod)
     */
    @Override
    public Map<CompositeSequence, Double> getRanks( ExpressionExperiment expressionExperiment, RankMethod method ) {
        final String queryString = "select dedv.designElement, dedv.rankByMean, dedv.rankByMax from ProcessedExpressionDataVectorImpl dedv where dedv.expressionExperiment.id = :ee";
        List<?> qr = this.getHibernateTemplate().findByNamedParam( queryString, "ee", expressionExperiment.getId() );
        Map<CompositeSequence, Double> result = new HashMap<CompositeSequence, Double>();
        for ( Object o : qr ) {
            Object[] oa = ( Object[] ) o;
            CompositeSequence d = ( CompositeSequence ) oa[0];
            Double rMean = ( Double ) oa[1];
            Double rMax = ( Double ) oa[2];
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
        Map<CompositeSequence, Collection<Gene>> cs2gene = CommonQueries.getCs2GeneMap( genes, this.getSession() );
        if ( cs2gene.keySet().size() == 0 ) {
            log.warn( "No composite sequences found for genes" );
            return new HashMap<ExpressionExperiment, Map<Gene, Map<CompositeSequence, Double[]>>>();
        }

        final String queryString = "select distinct dedv.expressionExperiment, dedv.designElement, dedv.rankByMean, dedv.rankByMax from ProcessedExpressionDataVectorImpl dedv "
                + " inner join dedv.bioAssayDimension bd "
                + " inner join dedv.designElement de  "
                + " where dedv.designElement.id in ( :cs ) and dedv.expressionExperiment.id in (:ees) ";

        List<?> qr = this.getHibernateTemplate().findByNamedParam( queryString, new String[] { "cs", "ees" },
                new Object[] { EntityUtils.getIds( cs2gene.keySet() ), EntityUtils.getIds( expressionExperiments ) } );

        Map<ExpressionExperiment, Map<Gene, Map<CompositeSequence, Double[]>>> resultnew = new HashMap<ExpressionExperiment, Map<Gene, Map<CompositeSequence, Double[]>>>();
        for ( Object o : qr ) {
            Object[] oa = ( Object[] ) o;
            ExpressionExperiment e = ( ExpressionExperiment ) oa[0];
            CompositeSequence d = ( CompositeSequence ) oa[1];
            Double rMean = ( Double ) oa[2];
            Double rMax = ( Double ) oa[3];

            if ( !resultnew.containsKey( e ) ) {
                resultnew.put( e, new HashMap<Gene, Map<CompositeSequence, Double[]>>() );
            }

            Map<Gene, Map<CompositeSequence, Double[]>> rmapnew = resultnew.get( e );

            Collection<Gene> genes4probe = cs2gene.get( d );

            for ( Gene gene : genes4probe ) {
                if ( !rmapnew.containsKey( gene ) ) {
                    rmapnew.put( gene, new HashMap<CompositeSequence, Double[]>() );
                }

                // return BOTH mean and max

                if ( rMean == null || rMax == null ) {
                    continue;
                }
                Double[] MeanMax = new Double[] { rMean, rMax };
                rmapnew.get( gene ).put( d, MeanMax );
            }
        }
        return resultnew;
    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorDao#load(int, java.lang.Long)
     */

    @Override
    public ProcessedExpressionDataVector load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "ProcessedExpressionDataVector.load - 'id' can not be null" );
        }
        return this.getHibernateTemplate().get( ProcessedExpressionDataVectorImpl.class, id );

    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorDao#loadAll()
     */
    @Override
    public java.util.Collection<? extends ProcessedExpressionDataVector> loadAll() {
        return this.getHibernateTemplate().loadAll( ProcessedExpressionDataVectorImpl.class );
    }

    @Override
    public void remove( ProcessedExpressionDataVector designElementDataVector ) {
        this.getHibernateTemplate().delete( designElementDataVector );

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorDao#removeProcessedDataVectors(ubic.gemma
     * .model.expression.experiment.ExpressionExperiment)
     */
    @Override
    public void removeProcessedDataVectors( final ExpressionExperiment expressionExperiment ) {
        assert expressionExperiment != null;

        /*
         * Get quantitation types that will be removed.
         */
        List<QuantitationType> qtsToRemove = this.getHibernateTemplate().findByNamedParam(
                "select distinct p.quantitationType from ExpressionExperimentImpl e "
                        + "inner join e.processedExpressionDataVectors p where e.id = :id", "id",
                expressionExperiment.getId() );

        this.getHibernateTemplate().bulkUpdate(
                "delete from ProcessedExpressionDataVectorImpl p where p.expressionExperiment = ?",
                expressionExperiment );

        if ( !qtsToRemove.isEmpty() ) {
            expressionExperiment.getQuantitationTypes().removeAll( qtsToRemove );
            this.getHibernateTemplate().update( expressionExperiment );
            this.getHibernateTemplate().deleteAll( qtsToRemove );
        }
    }

    /**
     * @param processedDataVectorCache the processedDataVectorCache to set
     */
    public void setProcessedDataVectorCache( ProcessedDataVectorCache processedDataVectorCache ) {
        this.processedDataVectorCache = processedDataVectorCache;
    }

    /**
     * @param ee
     * @return
     */
    protected Collection<RawExpressionDataVector> getMissingValueVectors( ExpressionExperiment ee ) {
        final String queryString = "select dedv from RawExpressionDataVectorImpl dedv "
                + "inner join dedv.quantitationType q where q.type = 'PRESENTABSENT'"
                + " and dedv.expressionExperiment  = :ee ";
        return this.getHibernateTemplate().findByNamedParam( queryString, "ee", ee );
    }

    /**
     * @param ee
     * @return
     */
    protected Collection<RawExpressionDataVector> getPreferredDataVectors( ExpressionExperiment ee ) {
        final String queryString = "select dedv from RawExpressionDataVectorImpl dedv inner join dedv.quantitationType q "
                + " where q.isPreferred = true  and dedv.expressionExperiment.id = :ee";
        return this.getHibernateTemplate().findByNamedParam( queryString, "ee", ee.getId() );
    }

    /**
     * @return the processedDataVectorCache
     */
    protected ProcessedDataVectorCache getProcessedDataVectorCache() {
        return processedDataVectorCache;
    }

    @Override
    protected Integer handleCountAll() {
        final String query = "select count(*) from ProcessedExpressionDataVectorImpl";
        try {
            org.hibernate.Query queryObject = super.getSession().createQuery( query );

            return ( Integer ) queryObject.iterate().next();
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /**
     * @param newResults
     */
    private void cacheResults( Collection<DoubleVectorValueObject> newResults ) {
        /*
         * Break up by gene and EE to cache collections of vectors for EE-gene combos.
         */
        Map<Long, Map<Long, Collection<DoubleVectorValueObject>>> mapForCache = makeCacheMap( newResults );

        for ( Long eeid : mapForCache.keySet() ) {
            for ( Long g : mapForCache.get( eeid ).keySet() ) {
                this.processedDataVectorCache.addToCache( eeid, g, mapForCache.get( eeid ).get( g ) );
            }
        }
    }

    /**
     * We cache vectors at the experiment level. If we need subsets, we have to slice them out.
     * 
     * @param bioAssaySets that we exactly need the data for.
     * @param genes that might have cached results
     * @param results from the cache will be put here
     * @param needToSearch experiments that need to be searched (not fully cached); this will be populated
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
                        obs = sliceSubSet( ( ExpressionExperimentSubSet ) ee, obs );
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
     * See if anything is 'ragged' (fewer bioassays per biomaterial than in some other sample)
     * 
     * @param bioAssayDimensions
     * @return
     */
    private BioAssayDimension checkRagged( Collection<BioAssayDimension> bioAssayDimensions ) {
        int s = -1;
        int longest = -1;
        BioAssayDimension longestBad = null;
        boolean ragged = false;
        for ( BioAssayDimension bad : bioAssayDimensions ) {
            Collection<BioAssay> assays = bad.getBioAssays();
            if ( s < 0 ) {
                s = assays.size();
            } else if ( s != assays.size() ) {
                ragged = true;
            }

            if ( assays.size() > longest ) {
                longest = assays.size();
                longestBad = bad;
            }
        }
        if ( ragged ) return longestBad;
        return null;
    }

    /**
     * @param ees
     * @return
     */
    @SuppressWarnings("unchecked")
    private Collection<BioAssayDimension> getBioAssayDimensions( BioAssaySet ee ) {
        if ( ee instanceof ExpressionExperiment ) {
            StopWatch timer = new StopWatch();
            timer.start();
            List<?> r = this
                    .getHibernateTemplate()
                    .findByNamedParam(
                            // this does not look efficient.
                            "select distinct bad from ExpressionExperimentImpl e, BioAssayDimensionImpl bad"
                                    + " inner join e.bioAssays b inner join bad.bioAssays badba where e = :ee and b in (badba) ",
                            "ee", ee );
            timer.stop();
            if ( timer.getTime() > 100 ) {
                log.info( "Fetch " + r.size() + " bioassaydimensions for experiment id=" + ee.getId() + ": "
                        + timer.getTime() + "ms" );
            }
            return ( Collection<BioAssayDimension> ) r;
        }

        return getBioAssayDimensions( getExperiment( ee ) );

    }

    /**
     * @param ees
     * @return
     */
    private Map<BioAssaySet, Collection<BioAssayDimension>> getBioAssayDimensions( Collection<ExpressionExperiment> ees ) {
        Map<BioAssaySet, Collection<BioAssayDimension>> result = new HashMap<BioAssaySet, Collection<BioAssayDimension>>();

        if ( ees.size() == 1 ) {
            ExpressionExperiment ee = ees.iterator().next();
            result.put( ee, getBioAssayDimensions( ee ) );
            return result;
        }

        StopWatch timer = new StopWatch();
        timer.start();
        List<?> r = this
                .getHibernateTemplate()
                .findByNamedParam(
                        "select distinct e, bad from ExpressionExperimentImpl e, BioAssayDimensionImpl bad"
                                + " inner join e.bioAssays b inner join bad.bioAssays badba where e in (:ees) and b in (badba) ",
                        "ees", ees );

        for ( Object o : r ) {
            Object[] tup = ( Object[] ) o;
            if ( !result.containsKey( tup[0] ) )
                result.put( ( BioAssaySet ) tup[0], new HashSet<BioAssayDimension>() );

            result.get( tup[0] ).add( ( BioAssayDimension ) tup[1] );
        }
        if ( timer.getTime() > 100 ) {
            log.info( "Fetch " + r.size() + " bioassaydimensions for " + ees.size() + " experiment(s): "
                    + timer.getTime() + "ms" );
        }

        return result;

    }

    /**
     * @param bas
     * @return
     */
    private ExpressionExperiment getExperiment( BioAssaySet bas ) {
        ExpressionExperiment e = null;
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
     * @param bioAssaySets - either ExpressionExperiment or ExpressionExperimentSubSet (which has an associated
     *        ExpressionExperiment, which is what we're after)
     * @return Note that this collection can be smaller than the input, if two bioAssaySets come from (or are) the same
     *         Experiment
     */
    private Collection<ExpressionExperiment> getExperiments( Collection<? extends BioAssaySet> bioAssaySets ) {
        Collection<ExpressionExperiment> result = new HashSet<ExpressionExperiment>();

        for ( BioAssaySet bas : bioAssaySets ) {
            ExpressionExperiment e = getExperiment( bas );

            result.add( e );
        }
        return result;
    }

    private QuantitationType getPreferredMaskedDataQuantitationType( QuantitationType preferredQt ) {
        QuantitationType present = QuantitationType.Factory.newInstance();
        present.setName( preferredQt.getName() + " - Masked " );
        present.setDescription( "Data masked with missing values (Computed by Gemma)" );
        present.setGeneralType( preferredQt.getGeneralType() );
        present.setIsBackground( preferredQt.getIsBackground() );
        present.setRepresentation( preferredQt.getRepresentation() );
        present.setScale( preferredQt.getScale() );
        present.setIsPreferred( false ); // I think this is the right thing to do.
        present.setIsMaskedPreferred( true );
        present.setIsBackgroundSubtracted( preferredQt.getIsBackgroundSubtracted() );
        present.setIsNormalized( preferredQt.getIsNormalized() );
        present.setIsRatio( preferredQt.getIsRatio() );
        present.setType( preferredQt.getType() );
        Long id = ( Long ) this.getHibernateTemplate().save( present );
        return this.getHibernateTemplate().load( QuantitationTypeImpl.class, id );
    }

    /**
     * @param ees
     * @param cs2gene Map of probe to genes.
     * @return
     */
    private Map<ProcessedExpressionDataVector, Collection<Long>> getProcessedVectors( Collection<Long> ees,
            Map<Long, Collection<Long>> cs2gene ) {

        final String queryString;
        if ( ees == null || ees.size() == 0 ) {
            queryString = "select dedv, dedv.designElement.id from ProcessedExpressionDataVectorImpl dedv fetch all properties"
                    + " where dedv.designElement.id in ( :cs ) ";
            return getVectorsForProbesInExperiments( cs2gene, queryString );
        }

        // Do not do in clause for experiments, as it can't use the indices
        queryString = "select dedv, dedv.designElement.id from ProcessedExpressionDataVectorImpl dedv fetch all properties"
                + " where dedv.designElement.id in ( :cs ) and dedv.expressionExperiment.id  = :eeid ";
        Map<ProcessedExpressionDataVector, Collection<Long>> result = new HashMap<ProcessedExpressionDataVector, Collection<Long>>();
        for ( Long ee : ees ) {
            result.putAll( getVectorsForProbesInExperiments( ee, cs2gene, queryString ) );
        }
        return result;

    }

    /**
     * This is an important method for fetching vectors.
     * 
     * @param ees
     * @param genes
     * @return vectors, possibly subsetted.
     */
    private Collection<DoubleVectorValueObject> handleGetProcessedExpressionDataArrays(
            Collection<? extends BioAssaySet> ees, Collection<Long> genes ) {

        // ees must be thawed first as currently implemented (?)

        Collection<DoubleVectorValueObject> results = new HashSet<DoubleVectorValueObject>();

        /*
         * Check the cache.
         */
        Collection<ExpressionExperiment> needToSearch = new HashSet<ExpressionExperiment>();
        Collection<Long> genesToSearch = new HashSet<Long>();
        checkCache( ees, genes, results, needToSearch, genesToSearch );
        log.info( "Using " + results.size() + " DoubleVectorValueObject(s) from cache; need to search for vectors for "
                + genes.size() + " genes from " + needToSearch.size() + " experiments" );

        if ( needToSearch.size() != 0 ) {

            Collection<ArrayDesign> arrays = CommonQueries.getArrayDesignsUsed(
                    EntityUtils.getIds( getExperiments( ees ) ), this.getSession() ).keySet();
            Map<Long, Collection<Long>> cs2gene = CommonQueries.getCs2GeneIdMap( genesToSearch,
                    EntityUtils.getIds( arrays ), this.getSession() );

            if ( cs2gene.size() == 0 ) {
                if ( results.isEmpty() ) {
                    log.warn( "No composite sequences found for genes" );
                    return new HashSet<DoubleVectorValueObject>();
                }
                return results;

            }

            /*
             * Fill in the map, because we want to track information on the specificity of the probes used in the data
             * vectors.
             */
            cs2gene = CommonQueries.getCs2GeneMapForProbes( cs2gene.keySet(), this.getSession() );

            Map<ProcessedExpressionDataVector, Collection<Long>> processedDataVectors = getProcessedVectors(
                    EntityUtils.getIds( needToSearch ), cs2gene );

            Map<BioAssaySet, Collection<BioAssayDimension>> bioAssayDimensions = this
                    .getBioAssayDimensions( needToSearch );

            Collection<DoubleVectorValueObject> newResults = new HashSet<DoubleVectorValueObject>();

            /*
             * This loop is to ensure that we don't get misaligned vectors for experiments that use more than one array
             * design. See bug 1704. This isn't that common, so we try to break out as soon as possible.
             */
            for ( BioAssaySet bas : needToSearch ) {

                Collection<BioAssayDimension> dims = bioAssayDimensions.get( bas );

                if ( dims == null || dims.isEmpty() ) {
                    log.warn( "BioAssayDimensions were null/empty unexpectedly." );
                    continue;
                }

                if ( dims.size() == 1 ) {
                    if ( needToSearch.size() == 1 ) {
                        // simple case.
                        newResults.addAll( unpack( processedDataVectors ) );
                        cacheResults( newResults );
                        return newResults;
                    }
                } // might have more than one dim, but might just be one

                /*
                 * Get the vectors for just this experiment. This is made more efficient by removing things from the map
                 * each time through.
                 */
                Map<ProcessedExpressionDataVector, Collection<Long>> vecsForBas = new HashMap<ProcessedExpressionDataVector, Collection<Long>>();
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
                    newResults.addAll( unpack( vecsForBas ) );
                } else {
                    BioAssayDimension longestBad = checkRagged( dims );
                    if ( longestBad == null ) {
                        newResults.addAll( unpack( vecsForBas ) );
                    } else {
                        newResults.addAll( unpack( vecsForBas, longestBad ) );
                    }
                }
            }

            /*
             * Finally....
             */

            cacheResults( newResults );

            newResults = sliceSubsets( ees, newResults );
            results.addAll( newResults );
        }

        return results;

    }

    /**
     * @param newResults
     * @return
     */
    private Map<Long, Map<Long, Collection<DoubleVectorValueObject>>> makeCacheMap(
            Collection<DoubleVectorValueObject> newResults ) {
        Map<Long, Map<Long, Collection<DoubleVectorValueObject>>> mapForCache = new HashMap<Long, Map<Long, Collection<DoubleVectorValueObject>>>();
        for ( DoubleVectorValueObject v : newResults ) {
            ExpressionExperimentValueObject e = v.getExpressionExperiment();
            if ( !mapForCache.containsKey( e.getId() ) ) {
                mapForCache.put( e.getId(), new HashMap<Long, Collection<DoubleVectorValueObject>>() );
            }
            Map<Long, Collection<DoubleVectorValueObject>> innerMap = mapForCache.get( e.getId() );
            for ( Long g : v.getGenes() ) {
                if ( !innerMap.containsKey( g ) ) {
                    innerMap.put( g, new HashSet<DoubleVectorValueObject>() );
                }
                innerMap.get( g ).add( v );
            }
        }
        return mapForCache;
    }

    /**
     * @param preferredData
     * @param missingValueData
     * @return
     */
    private Map<CompositeSequence, DoubleVectorValueObject> maskAndUnpack(
            Collection<RawExpressionDataVector> preferredData, Collection<RawExpressionDataVector> missingValueData ) {
        Map<CompositeSequence, DoubleVectorValueObject> unpackedData = unpack( preferredData );

        if ( missingValueData.size() == 0 ) {
            log.info( "There is no seprate missing data information, simply using the data as is" );
            for ( DoubleVectorValueObject rv : unpackedData.values() ) {
                rv.setMasked( true );
            }
            return unpackedData;
        }

        Collection<BooleanVectorValueObject> unpackedMissingValueData = unpackBooleans( missingValueData );
        Map<CompositeSequenceValueObject, BooleanVectorValueObject> missingValueMap = new HashMap<CompositeSequenceValueObject, BooleanVectorValueObject>();
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

            boolean[] mvdata = mv.getData();

            if ( mvdata.length != data.length ) {
                throw new IllegalStateException( "Missing value data didn't match data length" );
            }
            for ( int i = 0; i < data.length; i++ ) {
                if ( !mvdata[i] ) {
                    data[i] = Double.NaN;
                }
            }
            rv.setMasked( true );
        }

        return unpackedData;
    }

    /**
     * Given an ExpressionExperimentSubset and vectors from the source experiment, give vectors that include just the
     * data for the subset.
     * 
     * @param ee
     * @param obs
     * @return
     */
    private Collection<DoubleVectorValueObject> sliceSubSet( ExpressionExperimentSubSet ee,
            Collection<DoubleVectorValueObject> obs ) {

        Collection<DoubleVectorValueObject> sliced = new HashSet<DoubleVectorValueObject>();
        if ( obs == null || obs.isEmpty() ) return sliced;

        this.getHibernateTemplate().lock( ee, LockMode.NONE );
        Hibernate.initialize( ee.getBioAssays() );
        List<BioAssay> sliceBioAssays = new ArrayList<BioAssay>();

        BioAssayDimension bad = BioAssayDimension.Factory.newInstance();
        bad.setId( null ); // because it isn't a real bioassaydimension

        BioAssayDimension bioAssayDimension = obs.iterator().next().getBioAssayDimension();
        this.getHibernateTemplate().lock( bioAssayDimension, LockMode.NONE );
        Hibernate.initialize( bioAssayDimension );
        sliceBioAssays.clear();
        Hibernate.initialize( bioAssayDimension.getBioAssays() );
        for ( BioAssay ba : bioAssayDimension.getBioAssays() ) {
            if ( !ee.getBioAssays().contains( ba ) ) {
                continue;
            }
            Hibernate.initialize( ba.getSampleUsed() );
            BioMaterial bm = ba.getSampleUsed();
            Hibernate.initialize( bm.getBioAssaysUsedIn() );
            Hibernate.initialize( bm.getFactorValues() );

            sliceBioAssays.add( ba );
        }
        bad.setBioAssays( sliceBioAssays );

        bad.setName( "Subset of " + bioAssayDimension );

        for ( DoubleVectorValueObject vec : obs ) {
            DoubleVectorValueObject s = new DoubleVectorValueObject( ee, vec, bad );
            sliced.add( s );
        }

        return sliced;
    }

    /**
     * @param ees
     * @param vecs
     * @return
     */
    private Collection<DoubleVectorValueObject> sliceSubsets( Collection<? extends BioAssaySet> ees,
            Collection<DoubleVectorValueObject> vecs ) {
        Collection<DoubleVectorValueObject> results = new HashSet<DoubleVectorValueObject>();
        if ( vecs == null || vecs.isEmpty() ) return results;

        /*
         * FIXME nested loops; this is probably quite inefficient once the number of data sets & vectors grows beyond a
         * few (if both are small, no big deal)
         */
        for ( BioAssaySet bas : ees ) {
            if ( bas instanceof ExpressionExperimentSubSet ) {

                for ( DoubleVectorValueObject d : vecs ) {
                    if ( d.getExpressionExperiment().getId()
                            .equals( ( ( ExpressionExperimentSubSet ) bas ).getSourceExperiment().getId() ) ) {

                        Collection<DoubleVectorValueObject> ddvos = new HashSet<DoubleVectorValueObject>();
                        ddvos.add( d );
                        results.addAll( sliceSubSet( ( ExpressionExperimentSubSet ) bas, ddvos ) );// coll

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

    /**
     * @param data
     * @return
     */
    private Map<CompositeSequence, DoubleVectorValueObject> unpack( Collection<? extends DesignElementDataVector> data ) {
        Map<CompositeSequence, DoubleVectorValueObject> result = new HashMap<CompositeSequence, DoubleVectorValueObject>();

        for ( DesignElementDataVector v : data ) {
            result.put( v.getDesignElement(), new DoubleVectorValueObject( v ) );
        }
        return result;
    }

    /**
     * @param data
     * @return
     */
    private Map<CompositeSequence, DoubleVectorValueObject> unpack( Collection<? extends DesignElementDataVector> data,
            Map<Long, Collection<Long>> cs2GeneMap ) {
        Map<CompositeSequence, DoubleVectorValueObject> result = new HashMap<CompositeSequence, DoubleVectorValueObject>();

        for ( DesignElementDataVector v : data ) {
            result.put( v.getDesignElement(),
                    new DoubleVectorValueObject( v, cs2GeneMap.get( v.getDesignElement().getId() ) ) );
        }
        return result;
    }

    /**
     * @param data
     * @param cs2GeneMap
     * @param longestBad
     * @return
     */
    private Collection<DoubleVectorValueObject> unpack( Collection<? extends DesignElementDataVector> data,
            Map<Long, Collection<Long>> cs2GeneMap, BioAssayDimension longestBad ) {
        Collection<DoubleVectorValueObject> result = new HashSet<DoubleVectorValueObject>();

        for ( DesignElementDataVector v : data ) {
            result.add( new DoubleVectorValueObject( v, cs2GeneMap.get( v.getDesignElement().getId() ), longestBad ) );
        }
        return result;
    }

    /**
     * @param data
     * @return
     */
    private Collection<DoubleVectorValueObject> unpack( Map<? extends DesignElementDataVector, Collection<Long>> data ) {
        Collection<DoubleVectorValueObject> result = new HashSet<DoubleVectorValueObject>();
        for ( DesignElementDataVector v : data.keySet() ) {
            result.add( new DoubleVectorValueObject( v, data.get( v ) ) );
        }
        return result;
    }

    /**
     * @param data
     * @param longestBad
     * @return
     */
    private Collection<? extends DoubleVectorValueObject> unpack(
            Map<ProcessedExpressionDataVector, Collection<Long>> data, BioAssayDimension longestBad ) {
        Collection<DoubleVectorValueObject> result = new HashSet<DoubleVectorValueObject>();
        for ( DesignElementDataVector v : data.keySet() ) {
            result.add( new DoubleVectorValueObject( v, data.get( v ), longestBad ) );
        }
        return result;
    }

    /**
     * @param data
     * @return
     */
    private Collection<BooleanVectorValueObject> unpackBooleans( Collection<? extends DesignElementDataVector> data ) {
        Collection<BooleanVectorValueObject> result = new HashSet<BooleanVectorValueObject>();

        for ( DesignElementDataVector v : data ) {
            result.add( new BooleanVectorValueObject( v ) );
        }
        return result;
    }
}
