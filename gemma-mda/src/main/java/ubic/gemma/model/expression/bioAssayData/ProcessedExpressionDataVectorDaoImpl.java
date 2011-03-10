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
import java.util.List;
import java.util.Map;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeImpl;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.util.CommonQueries;

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

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorDao#createProcessedDataVectors(ubic.gemma
     * .model.expression.experiment.ExpressionExperiment)
     */
    public Collection<ProcessedExpressionDataVector> createProcessedDataVectors(
            final ExpressionExperiment expressionExperiment ) {

        if ( expressionExperiment == null ) {
            throw new IllegalStateException( "ExpressionExperiment cannot be null" );
        }

        removeProcessedDataVectors( expressionExperiment );

        // We need to commit or we can end up with 'object exists in session'
        this.getHibernateTemplate().flush();
        this.getHibernateTemplate().clear();

        expressionExperiment.setProcessedExpressionDataVectors( null );
        this.getHibernateTemplate().update( expressionExperiment );
        this.getHibernateTemplate().flush();
        this.getHibernateTemplate().clear();

        assert this.getProcessedVectors( expressionExperiment ).size() == 0;

        log.info( "Computing processed expression vectors for " + expressionExperiment );

        /*
         * Figure out if it is two-channel
         */
        boolean isTwoChannel = false;
        Collection<ArrayDesign> arrayDesignsUsed = CommonQueries.getArrayDesignsUsed( expressionExperiment, this
                .getSession() );
        for ( ArrayDesign ad : arrayDesignsUsed ) {
            TechnologyType technologyType = ad.getTechnologyType();

            if ( technologyType == null ) {
                throw new IllegalStateException(
                        "Array designs must have a technology type assigned before processed vector computation" );
            }

            if ( !technologyType.equals( TechnologyType.ONECOLOR ) ) {
                isTwoChannel = true;
            }
        }

        Collection<RawExpressionDataVector> missingValueVectors = new HashSet<RawExpressionDataVector>();
        if ( isTwoChannel ) {
            missingValueVectors = this.getMissingValueVectors( expressionExperiment );
        }

        log.info( missingValueVectors.size() + " missing value vectors" );

        Collection<RawExpressionDataVector> preferredDataVectors = this.getPreferredDataVectors( expressionExperiment );
        if ( preferredDataVectors.isEmpty() ) {
            throw new IllegalArgumentException( "No preferred data vectors for " + expressionExperiment );
        }

        log.info( preferredDataVectors.size() + " preferred data vectors" );

        Collection<DoubleVectorValueObject> maskedVectorObjects = maskAndUnpack( preferredDataVectors,
                missingValueVectors );

        log.info( maskedVectorObjects.size() + " masked vectors" );

        /*
         * Create the vectors. Do a sanity check that we don't have more than we should
         */
        Collection<CompositeSequence> seenDes = new HashSet<CompositeSequence>();
        QuantitationType preferredMaskedDataQuantitationType = getPreferredMaskedDataQuantitationType( preferredDataVectors
                .iterator().next().getQuantitationType() );

        Collection<ProcessedExpressionDataVector> result = new ArrayList<ProcessedExpressionDataVector>();
        for ( DoubleVectorValueObject dvvo : maskedVectorObjects ) {

            CompositeSequence designElement = dvvo.getDesignElement();

            if ( seenDes.contains( designElement ) ) {
                // defensive programming, this happens.
                throw new IllegalStateException( "Duplicated design element: " + designElement
                        + "; make sure the experiment has only one 'preferred' quantitation type. "
                        + "Perhaps you need to run vector merging following an array desing switch?" );
            }

            result.add( ( ProcessedExpressionDataVector ) dvvo
                    .toDesignElementDataVector( preferredMaskedDataQuantitationType ) );
            seenDes.add( designElement );
        }

        Collection<ProcessedExpressionDataVector> results = this.create( result );
        log.info( "Creating " + results.size() + " processed data vectors" );

        this.getHibernateTemplate().clear();
        this.getHibernateTemplate().lock( expressionExperiment, LockMode.READ );
        Hibernate.initialize( expressionExperiment.getProcessedExpressionDataVectors() );
        expressionExperiment.setProcessedExpressionDataVectors( new HashSet<ProcessedExpressionDataVector>( results ) );
        expressionExperiment.getQuantitationTypes().add( preferredMaskedDataQuantitationType );

        this.getHibernateTemplate().update( expressionExperiment );

        this.processedDataVectorCache.clearCache( expressionExperiment.getId() );

        return expressionExperiment.getProcessedExpressionDataVectors();

    }

    @SuppressWarnings("unchecked")
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

    @SuppressWarnings("unchecked")
    public Collection<ProcessedExpressionDataVector> find( Collection<QuantitationType> quantitationTypes ) {
        final String queryString = "select dev from ProcessedExpressionDataVectorImpl dev   where  "
                + "  dev.quantitationType in ( :quantitationTypes) ";
        return this.getHibernateTemplate().findByNamedParam( queryString, "quantitationTypes", quantitationTypes );

    }

    @SuppressWarnings("unchecked")
    public Collection<ProcessedExpressionDataVector> find( QuantitationType quantitationType ) {
        final String queryString = "select dev from ProcessedExpressionDataVectorImpl dev   where  "
                + "  dev.quantitationType = :quantitationType ";
        return this.getHibernateTemplate().findByNamedParam( queryString, "quantitationType", quantitationType );

    }

    public Collection<DoubleVectorValueObject> getProcessedDataArrays( BioAssaySet expressionExperiment ) {
        return getProcessedDataArrays( expressionExperiment, 50, false );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorDao#getProcessedDataMatrix(ubic.gemma.model
     * .expression.experiment.ExpressionExperiment, java.util.Collection)
     */
    public Collection<DoubleVectorValueObject> getProcessedDataArrays( BioAssaySet expressionExperiment,
            Collection<Gene> genes ) {
        Collection<BioAssaySet> expressionExperiments = new HashSet<BioAssaySet>();
        expressionExperiments.add( expressionExperiment );
        return this.handleGetProcessedExpressionDataArrays( expressionExperiments, genes, true );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorDao#getProcessedDataArrays(ubic.gemma.model
     * .expression.experiment.BioAssaySet, int, boolean)
     */
    public Collection<DoubleVectorValueObject> getProcessedDataArrays( BioAssaySet ee, int limit, boolean fullMap ) {

        Collection<ProcessedExpressionDataVector> pedvs = this.getProcessedVectors( getExperiment( ee ), limit );

        Collection<CompositeSequence> probes = new ArrayList<CompositeSequence>();
        for ( ProcessedExpressionDataVector pedv : pedvs ) {
            probes.add( pedv.getDesignElement() );
        }

        if ( probes.isEmpty() ) {
            return unpack( pedvs );
        }

        Map<CompositeSequence, Collection<Gene>> cs2gene = null;
        if ( fullMap ) {
            cs2gene = CommonQueries.getFullCs2AllGeneMap( probes, this.getSession() );
        } else {
            cs2gene = CommonQueries.getFullCs2GeneMap( probes, this.getSession() );
        }
        return unpack( pedvs, cs2gene );
    }

    public Collection<DoubleVectorValueObject> getProcessedDataArrays(
            Collection<? extends BioAssaySet> expressionExperiments, Collection<Gene> genes, boolean fullMap ) {
        return this.handleGetProcessedExpressionDataArrays( expressionExperiments, genes, fullMap );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorDao#getProcessedDataArrays(java.util.Collection
     * , java.util.Collection)
     */
    public Collection<DoubleVectorValueObject> getProcessedDataArrays(
            Collection<ExpressionExperiment> expressionExperiments, Collection<Gene> genes ) {
        return this.handleGetProcessedExpressionDataArrays( expressionExperiments, genes, true );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorDao#getProcessedDataArraysByProbe(java.
     * util.Collection, java.util.Collection, boolean)
     */
    public Collection<DoubleVectorValueObject> getProcessedDataArraysByProbe( Collection<? extends BioAssaySet> ees,
            Collection<CompositeSequence> probes, boolean fullMap ) {

        Collection<DoubleVectorValueObject> results = new HashSet<DoubleVectorValueObject>();

        if ( probes.isEmpty() ) return results;

        Map<CompositeSequence, Collection<Gene>> cs2gene = null;
        if ( fullMap ) {
            cs2gene = CommonQueries.getFullCs2AllGeneMap( probes, this.getSession() );
        } else {
            cs2gene = CommonQueries.getFullCs2GeneMap( probes, this.getSession() );
        }

        Map<CompositeSequence, Collection<Gene>> noGeneProbes = new HashMap<CompositeSequence, Collection<Gene>>();

        for ( CompositeSequence p : probes ) {
            if ( !cs2gene.containsKey( p ) || cs2gene.get( p ).isEmpty() ) {
                noGeneProbes.put( p, new HashSet<Gene>() );
            }
        }

        /*
         * To Check the cached we need the list of genes 1st. Get from CS2Gene list then check the cache.
         */
        Collection<Gene> genes = new HashSet<Gene>();
        for ( CompositeSequence cs : cs2gene.keySet() ) {
            genes.addAll( cs2gene.get( cs ) );
        }

        Collection<ExpressionExperiment> needToSearch = new HashSet<ExpressionExperiment>();
        Collection<Gene> genesToSearch = new HashSet<Gene>();
        checkCache( ees, genes, results, needToSearch, genesToSearch );

        /*
         * FIXME noGeneProbes are never cached.
         */
        if ( !noGeneProbes.isEmpty() ) {
            Collection<ExpressionExperiment> eesForNoGeneProbes = new HashSet<ExpressionExperiment>();
            eesForNoGeneProbes.addAll( ( Collection<? extends ExpressionExperiment> ) ees );

            Map<ProcessedExpressionDataVector, Collection<Gene>> processedDataVectors = getProcessedVectors(
                    eesForNoGeneProbes, noGeneProbes );

            Collection<DoubleVectorValueObject> newResults = unpack( processedDataVectors );
            cacheResults( newResults );

            newResults = sliceSubsets( ees, newResults );

            results.addAll( newResults );
        }

        /*
         * Non-cached items.
         */
        if ( !needToSearch.isEmpty() ) {
            Map<ProcessedExpressionDataVector, Collection<Gene>> processedDataVectors = getProcessedVectors(
                    needToSearch, cs2gene );

            Collection<DoubleVectorValueObject> newResults = unpack( processedDataVectors );
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
    @SuppressWarnings("unchecked")
    public Collection<ProcessedExpressionDataVector> getProcessedVectors( ExpressionExperiment ee ) {
        final String queryString = " from ProcessedExpressionDataVectorImpl dedv where dedv.expressionExperiment = :ee";
        Collection<ProcessedExpressionDataVector> result = this.getHibernateTemplate().findByNamedParam( queryString,
                "ee", ee );
        this.thaw( result );
        return result;
    }

    /**
     * @param ee
     * @param limit
     * @return
     */
    @SuppressWarnings("unchecked")
    public Collection<ProcessedExpressionDataVector> getProcessedVectors( ExpressionExperiment ee, Integer limit ) {
        final String queryString = " from ProcessedExpressionDataVectorImpl dedv where dedv.expressionExperiment = :ee";

        if ( limit == null ) {
            return this.getProcessedVectors( ee );
        }

        int oldmax = getHibernateTemplate().getMaxResults();
        getHibernateTemplate().setMaxResults( limit );
        Collection<ProcessedExpressionDataVector> result = this.getHibernateTemplate().findByNamedParam( queryString,
                "ee", ee );

        getHibernateTemplate().setMaxResults( oldmax );
        this.thaw( result );
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorDao#getRanks(java.util.Collection,
     * java.util.Collection, ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorDao.RankMethod)
     */
    @SuppressWarnings("unchecked")
    public Map<ExpressionExperiment, Map<Gene, Collection<Double>>> getRanks(
            Collection<ExpressionExperiment> expressionExperiments, Collection<Gene> genes, RankMethod method ) {
        Map<CompositeSequence, Collection<Gene>> cs2gene = CommonQueries.getCs2GeneMap( genes, this.getSession() );
        if ( cs2gene.keySet().size() == 0 ) {
            log.warn( "No composite sequences found for genes" );
            return new HashMap<ExpressionExperiment, Map<Gene, Collection<Double>>>();
        }

        final String queryString = "select distinct dedv.expressionExperiment, dedv.designElement, dedv.rankByMean, dedv.rankByMax from ProcessedExpressionDataVectorImpl dedv "
                + " inner join dedv.bioAssayDimension bd "
                + " inner join dedv.designElement de  "
                + " where dedv.designElement in ( :cs ) and dedv.expressionExperiment in (:ees) ";

        List qr = this.getHibernateTemplate().findByNamedParam( queryString, new String[] { "cs", "ees" },
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
    @SuppressWarnings("unchecked")
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
                + " where dedv.designElement in ( :cs ) and dedv.expressionExperiment = ee ";

        List qr = this.getHibernateTemplate().findByNamedParam( queryString, new String[] { "cs", "ee" },
                new Object[] { cs2gene.keySet(), expressionExperiment } );

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
    @SuppressWarnings("unchecked")
    public Map<CompositeSequence, Double> getRanks( ExpressionExperiment expressionExperiment, RankMethod method ) {
        final String queryString = "select dedv.designElement, dedv.rankByMean, dedv.rankByMax from ProcessedExpressionDataVectorImpl dedv where dedv.expressionExperiment = :ee";
        List qr = this.getHibernateTemplate().findByNamedParam( queryString, "ee", expressionExperiment );
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

    @SuppressWarnings("unchecked")
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
                + " where dedv.designElement in ( :cs ) and dedv.expressionExperiment in (:ees) ";

        List qr = this.getHibernateTemplate().findByNamedParam( queryString, new String[] { "cs", "ees" },
                new Object[] { cs2gene.keySet(), expressionExperiments } );

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

    public ProcessedExpressionDataVector load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "ProcessedExpressionDataVector.load - 'id' can not be null" );
        }
        return this.getHibernateTemplate().get( ProcessedExpressionDataVectorImpl.class, id );

    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorDao#loadAll()
     */
    public java.util.Collection<? extends ProcessedExpressionDataVector> loadAll() {
        return this.getHibernateTemplate().loadAll( ProcessedExpressionDataVectorImpl.class );
    }

    @Override
    public void remove( ProcessedExpressionDataVector designElementDataVector ) {
        this.getHibernateTemplate().delete( designElementDataVector );

    }

    public void removeProcessedDataVectors( final ExpressionExperiment expressionExperiment ) {
        /*
         * If the experiment already has them, delete them.
         */
        Collection<ProcessedExpressionDataVector> oldVectors = this.getProcessedVectors( expressionExperiment );
        if ( oldVectors.size() > 0 ) {
            log.info( "Removing old processed vectors" );
            this.remove( oldVectors );
        }
    }

    /**
     * @param processedDataVectorCache the processedDataVectorCache to set
     */
    public void setProcessedDataVectorCache( ProcessedDataVectorCache processedDataVectorCache ) {
        this.processedDataVectorCache = processedDataVectorCache;
    }

    /**
     * @param newResults
     */
    private void cacheResults( Collection<DoubleVectorValueObject> newResults ) {
        /*
         * Break up by gene and EE to cache collections of vectors for EE-gene combos.
         */
        Map<Long, Map<Gene, Collection<DoubleVectorValueObject>>> mapForCache = makeCacheMap( newResults );

        for ( Long eeid : mapForCache.keySet() ) {
            Cache cache = this.processedDataVectorCache.getCache( eeid );
            for ( Gene g : mapForCache.get( eeid ).keySet() ) {
                cache.put( new Element( g, mapForCache.get( eeid ).get( g ) ) );
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
    @SuppressWarnings("unchecked")
    private void checkCache( Collection<? extends BioAssaySet> bioAssaySets, Collection<Gene> genes,
            Collection<DoubleVectorValueObject> results, Collection<ExpressionExperiment> needToSearch,
            Collection<Gene> genesToSearch ) {

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
            Cache cache = processedDataVectorCache.getCache( ee.getId() );
            for ( Gene g : genes ) {
                Element element = cache.get( g );
                if ( element != null ) {
                    Collection<DoubleVectorValueObject> obs = ( Collection<DoubleVectorValueObject> ) element
                            .getObjectValue();

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
    private Map<ProcessedExpressionDataVector, Collection<Gene>> getProcessedVectors(
            Collection<ExpressionExperiment> ees, Map<CompositeSequence, Collection<Gene>> cs2gene ) {

        final String queryString;
        if ( ees == null || ees.size() == 0 ) {
            queryString = "select distinct dedv, dedv.designElement from ProcessedExpressionDataVectorImpl dedv fetch all properties"
                    + " join dedv.designElement de join fetch dedv.expressionExperiment ee where de in ( :cs )  ";
        } else {
            queryString = "select distinct dedv, dedv.designElement from ProcessedExpressionDataVectorImpl dedv fetch all properties"
                    + " join dedv.designElement de join fetch dedv.expressionExperiment ee "
                    + " where de in (:cs ) and dedv.expressionExperiment in ( :ees )";
        }
        return getVectorsForProbesInExperiments( ees, cs2gene, queryString );
    }

    /**
     * This is an important method for fetching vectors.
     * 
     * @param ees
     * @param genes
     * @return
     */
    private Collection<DoubleVectorValueObject> handleGetProcessedExpressionDataArrays(
            Collection<? extends BioAssaySet> ees, Collection<Gene> genes, boolean fullMap ) {

        // ees must be thawed first as currently implemented (?)

        Collection<DoubleVectorValueObject> results = new HashSet<DoubleVectorValueObject>();

        /*
         * Check the cache.
         */
        Collection<ExpressionExperiment> needToSearch = new HashSet<ExpressionExperiment>();
        Collection<Gene> genesToSearch = new HashSet<Gene>();
        checkCache( ees, genes, results, needToSearch, genesToSearch );

        if ( needToSearch.size() != 0 ) {

            Collection<ArrayDesign> arrays = CommonQueries.getArrayDesignsUsed( getExperiments( ees ),
                    this.getSession() ).keySet();
            Map<CompositeSequence, Collection<Gene>> cs2gene = CommonQueries.getCs2GeneMap( genesToSearch, arrays, this
                    .getSession() );

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

            if ( fullMap ) {
                cs2gene = CommonQueries.getFullCs2AllGeneMap( cs2gene.keySet(), this.getSession() );
            } else {
                cs2gene = CommonQueries.getFullCs2GeneMap( cs2gene.keySet(), this.getSession() );
            }

            Map<ProcessedExpressionDataVector, Collection<Gene>> processedDataVectors = getProcessedVectors(
                    needToSearch, cs2gene );

            Collection<DoubleVectorValueObject> newResults = unpack( processedDataVectors );
            cacheResults( newResults );
            results.addAll( newResults );
        }

        return results;

    }

    /**
     * @param newResults
     * @return
     */
    private Map<Long, Map<Gene, Collection<DoubleVectorValueObject>>> makeCacheMap(
            Collection<DoubleVectorValueObject> newResults ) {
        Map<Long, Map<Gene, Collection<DoubleVectorValueObject>>> mapForCache = new HashMap<Long, Map<Gene, Collection<DoubleVectorValueObject>>>();
        for ( DoubleVectorValueObject v : newResults ) {
            ExpressionExperiment e = v.getExpressionExperiment();
            if ( !mapForCache.containsKey( e.getId() ) ) {
                mapForCache.put( e.getId(), new HashMap<Gene, Collection<DoubleVectorValueObject>>() );
            }
            Map<Gene, Collection<DoubleVectorValueObject>> innerMap = mapForCache.get( e.getId() );
            for ( Gene g : v.getGenes() ) {
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
    private Collection<DoubleVectorValueObject> maskAndUnpack( Collection<RawExpressionDataVector> preferredData,
            Collection<RawExpressionDataVector> missingValueData ) {
        Collection<DoubleVectorValueObject> unpackedData = unpack( preferredData );

        if ( missingValueData.size() == 0 ) {
            for ( DoubleVectorValueObject rv : unpackedData ) {
                rv.setMasked( true );
            }
            return unpackedData;
        }

        Collection<BooleanVectorValueObject> unpackedMissingValueData = unpackBooleans( missingValueData );
        Map<CompositeSequence, BooleanVectorValueObject> missingValueMap = new HashMap<CompositeSequence, BooleanVectorValueObject>();
        for ( BooleanVectorValueObject bv : unpackedMissingValueData ) {
            missingValueMap.put( bv.getDesignElement(), bv );
        }

        boolean warned = false;
        for ( DoubleVectorValueObject rv : unpackedData ) {
            double[] data = rv.getData();
            CompositeSequence de = rv.getDesignElement();
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
            Hibernate.initialize( ba.getSamplesUsed() );
            for ( BioMaterial bm : ba.getSamplesUsed() ) {
                Hibernate.initialize( bm.getBioAssaysUsedIn() );
                Hibernate.initialize( bm.getFactorValues() );
            }
            sliceBioAssays.add( ba );
        }
        bad.setBioAssays( sliceBioAssays );

        bad.setName( "Subset of " + bioAssayDimension );

        for ( DoubleVectorValueObject vec : obs ) {
            DoubleVectorValueObject s = new DoubleVectorValueObject( vec, bad );
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
                    if ( d.getExpressionExperiment().equals(
                            ( ( ExpressionExperimentSubSet ) bas ).getSourceExperiment() ) ) {

                        Collection<DoubleVectorValueObject> ddvos = new HashSet<DoubleVectorValueObject>();
                        ddvos.add( d );
                        results.addAll( sliceSubSet( ( ExpressionExperimentSubSet ) bas, ddvos ) );// coll

                    }
                }

            } else {
                for ( DoubleVectorValueObject d : vecs ) {
                    if ( d.getExpressionExperiment().equals( bas ) ) {
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
    private Collection<DoubleVectorValueObject> unpack( Collection<? extends DesignElementDataVector> data ) {
        Collection<DoubleVectorValueObject> result = new HashSet<DoubleVectorValueObject>();

        for ( DesignElementDataVector v : data ) {
            result.add( new DoubleVectorValueObject( v ) );
        }
        return result;
    }

    /**
     * @param data
     * @return
     */
    private Collection<DoubleVectorValueObject> unpack( Collection<? extends DesignElementDataVector> data,
            Map<? extends CompositeSequence, Collection<Gene>> cs2GeneMap ) {
        Collection<DoubleVectorValueObject> result = new HashSet<DoubleVectorValueObject>();

        for ( DesignElementDataVector v : data ) {
            result.add( new DoubleVectorValueObject( v, cs2GeneMap.get( v.getDesignElement() ) ) );
        }
        return result;
    }

    /**
     * @param data
     * @return
     */
    private Collection<DoubleVectorValueObject> unpack( Map<? extends DesignElementDataVector, Collection<Gene>> data ) {
        Collection<DoubleVectorValueObject> result = new HashSet<DoubleVectorValueObject>();
        for ( DesignElementDataVector v : data.keySet() ) {
            result.add( new DoubleVectorValueObject( v, data.get( v ) ) );
        }
        return result;
    }

    private Collection<BooleanVectorValueObject> unpackBooleans( Collection<? extends DesignElementDataVector> data ) {
        Collection<BooleanVectorValueObject> result = new HashSet<BooleanVectorValueObject>();

        for ( DesignElementDataVector v : data ) {
            result.add( new BooleanVectorValueObject( v ) );
        }
        return result;
    }

    /**
     * @param ee
     * @return
     */
    @SuppressWarnings("unchecked")
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
    @SuppressWarnings("unchecked")
    protected Collection<RawExpressionDataVector> getPreferredDataVectors( ExpressionExperiment ee ) {
        final String queryString = "select dedv from RawExpressionDataVectorImpl dedv inner join dedv.quantitationType q "
                + " where q.isPreferred = true  and dedv.expressionExperiment = :ee ";
        return this.getHibernateTemplate().findByNamedParam( queryString, "ee", ee );
    }

    /**
     * @return the processedDataVectorCache
     */
    protected ProcessedDataVectorCache getProcessedDataVectorCache() {
        return processedDataVectorCache;
    }

    @Override
    protected Integer handleCountAll() throws Exception {
        final String query = "select count(*) from ProcessedExpressionDataVectorImpl";
        try {
            org.hibernate.Query queryObject = super.getSession().createQuery( query );

            return ( Integer ) queryObject.iterate().next();
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }
}
