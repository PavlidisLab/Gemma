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

import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeImpl;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.util.CommonQueries;

/**
 * @author Paul
 * @version $Id$
 */
public class ProcessedExpressionDataVectorDaoImpl extends DesignElementDataVectorDaoImpl<ProcessedExpressionDataVector>
        implements ProcessedExpressionDataVectorDao {

    private static Log log = LogFactory.getLog( ProcessedExpressionDataVectorDaoImpl.class.getName() );

    private ProcessedDataVectorCache processedDataVectorCache;

    @Override
    protected Integer handleCountAll() throws Exception {
        final String query = "select count(*) from ProcessedExpressionDataVectorImpl";
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( query );

            return ( Integer ) queryObject.iterate().next();
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorDao#createProcessedDataVectors(ubic.gemma
     * .model.expression.experiment.ExpressionExperiment)
     */
    public Collection<ProcessedExpressionDataVector> createProcessedDataVectors(
            final ExpressionExperiment expressionExperiment ) {

        if ( expressionExperiment == null ) {
            throw new IllegalStateException( "ExpressionExperiment cannot be null" );
        }

        /*
         * If the experiment already has them, delete them.
         */
        Collection<ProcessedExpressionDataVector> oldVectors = this.getProcessedVectors( expressionExperiment );
        if ( oldVectors.size() > 0 ) {
            log.info( "Removing old processed vectors" );
            this.remove( oldVectors );
        }

        // We need to commit the remove transaction, or we can end up with 'object exists in session'
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
        Collection<DesignElement> seenDes = new HashSet<DesignElement>();
        QuantitationType preferredMaskedDataQuantitationType = getPreferredMaskedDataQuantitationType( preferredDataVectors
                .iterator().next().getQuantitationType() );
        Collection<ProcessedExpressionDataVector> result = new ArrayList<ProcessedExpressionDataVector>();
        for ( DoubleVectorValueObject dvvo : maskedVectorObjects ) {

            DesignElement designElement = dvvo.getDesignElement();

            if ( seenDes.contains( designElement ) ) {
                // defensive programming, this happens.
                throw new IllegalStateException( "Duplicated design element: " + designElement
                        + "; make sure the experiment has only one 'preferred' quantitation type." );
            }

            result.add( ( ProcessedExpressionDataVector ) dvvo
                    .toDesignElementDataVector( preferredMaskedDataQuantitationType ) );
            seenDes.add( designElement );
        }

        Collection<ProcessedExpressionDataVector> results = this.create( result );
        log.info( "Creating " + results.size() + " processed data vectors" );
        this.getHibernateTemplate().lock( expressionExperiment, LockMode.READ );
        Hibernate.initialize( expressionExperiment.getProcessedExpressionDataVectors() );
        expressionExperiment.setProcessedExpressionDataVectors( new HashSet<ProcessedExpressionDataVector>( results ) );

        this.getHibernateTemplate().update( expressionExperiment );

        this.processedDataVectorCache.clearCache( expressionExperiment.getId() );

        return expressionExperiment.getProcessedExpressionDataVectors();

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
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorDao#load(int, java.lang.Long)
     */

    public ProcessedExpressionDataVector load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "ProcessedExpressionDataVector.load - 'id' can not be null" );
        }
        return ( ProcessedExpressionDataVector ) this.getHibernateTemplate().get(
                ProcessedExpressionDataVectorImpl.class, id );

    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorDao#loadAll()
     */

    @SuppressWarnings( { "unchecked" })
    public java.util.Collection<ProcessedExpressionDataVector> loadAll() {
        return this.getHibernateTemplate().loadAll( ProcessedExpressionDataVectorImpl.class );
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorDao#getProcessedDataMatrix(ubic.gemma.model
     * .expression.experiment.ExpressionExperiment, java.util.Collection)
     */
    public Collection<DoubleVectorValueObject> getProcessedDataArrays( ExpressionExperiment expressionExperiment,
            Collection<Gene> genes ) {
        Collection<ExpressionExperiment> expressionExperiments = new HashSet<ExpressionExperiment>();
        expressionExperiments.add( expressionExperiment );
        return this.handleGetProcessedExpressionDataArrays( expressionExperiments, genes );
    }

    /*
     * (non-Javadoc)
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

    /*
     * (non-Javadoc)
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
            DesignElement d = ( DesignElement ) oa[1];
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

    @SuppressWarnings("unchecked")
    public Map<ExpressionExperiment, Map<Gene, Map<DesignElement, Double[]>>> getRanksByProbe(
            Collection<ExpressionExperiment> expressionExperiments, Collection<Gene> genes ) {
        Map<CompositeSequence, Collection<Gene>> cs2gene = CommonQueries.getCs2GeneMap( genes, this.getSession() );
        if ( cs2gene.keySet().size() == 0 ) {
            log.warn( "No composite sequences found for genes" );
            return new HashMap<ExpressionExperiment, Map<Gene, Map<DesignElement, Double[]>>>();
        }

        final String queryString = "select distinct dedv.expressionExperiment, dedv.designElement, dedv.rankByMean, dedv.rankByMax from ProcessedExpressionDataVectorImpl dedv "
                + " inner join dedv.bioAssayDimension bd "
                + " inner join dedv.designElement de  "
                + " where dedv.designElement in ( :cs ) and dedv.expressionExperiment in (:ees) ";

        List qr = this.getHibernateTemplate().findByNamedParam( queryString, new String[] { "cs", "ees" },
                new Object[] { cs2gene.keySet(), expressionExperiments } );

        Map<ExpressionExperiment, Map<Gene, Map<DesignElement, Double[]>>> resultnew = new HashMap<ExpressionExperiment, Map<Gene, Map<DesignElement, Double[]>>>();
        for ( Object o : qr ) {
            Object[] oa = ( Object[] ) o;
            ExpressionExperiment e = ( ExpressionExperiment ) oa[0];
            DesignElement d = ( DesignElement ) oa[1];
            Double rMean = ( Double ) oa[2];
            Double rMax = ( Double ) oa[3];

            if ( !resultnew.containsKey( e ) ) {
                resultnew.put( e, new HashMap<Gene, Map<DesignElement, Double[]>>() );
            }

            Map<Gene, Map<DesignElement, Double[]>> rmapnew = resultnew.get( e );

            Collection<Gene> genes4probe = cs2gene.get( d );

            for ( Gene gene : genes4probe ) {
                if ( !rmapnew.containsKey( gene ) ) {
                    rmapnew.put( gene, new HashMap<DesignElement, Double[]>() );
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

    /*
     * (non-Javadoc)
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
            DesignElement d = ( DesignElement ) oa[0];
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
     * @see
     * ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorDao#getRanks(ubic.gemma.model.expression
     * .experiment.ExpressionExperiment,
     * ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorDao.RankMethod)
     */
    @SuppressWarnings("unchecked")
    public Map<DesignElement, Double> getRanks( ExpressionExperiment expressionExperiment, RankMethod method ) {
        final String queryString = "select dedv.designElement, dedv.rankByMean, dedv.rankByMax from ProcessedExpressionDataVectorImpl dedv where dedv.expressionExperiment = :ee";
        List qr = this.getHibernateTemplate().findByNamedParam( queryString, "ee", expressionExperiment );
        Map<DesignElement, Double> result = new HashMap<DesignElement, Double>();
        for ( Object o : qr ) {
            Object[] oa = ( Object[] ) o;
            DesignElement d = ( DesignElement ) oa[0];
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

    /**
     * @param processedDataVectorCache the processedDataVectorCache to set
     */
    public void setProcessedDataVectorCache( ProcessedDataVectorCache processedDataVectorCache ) {
        this.processedDataVectorCache = processedDataVectorCache;
    }

    /**
     * @return the processedDataVectorCache
     */
    protected ProcessedDataVectorCache getProcessedDataVectorCache() {
        return processedDataVectorCache;
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
     * @param ees
     * @param genes
     * @param results
     * @param needToSearch
     * @param genesToSearch
     */
    @SuppressWarnings("unchecked")
    private void checkCache( Collection<ExpressionExperiment> ees, Collection<Gene> genes,
            Collection<DoubleVectorValueObject> results, Collection<ExpressionExperiment> needToSearch,
            Collection<Gene> genesToSearch ) {
        for ( ExpressionExperiment ee : ees ) {
            Cache cache = processedDataVectorCache.getCache( ee.getId() );
            for ( Gene g : genes ) {
                Element element = cache.get( g );
                if ( element != null ) {
                    Collection<DoubleVectorValueObject> obs = ( Collection<DoubleVectorValueObject> ) element
                            .getObjectValue();
                    results.addAll( obs );
                } else {
                    genesToSearch.add( g );
                }
            }
            if ( genesToSearch.size() > 0 ) {
                needToSearch.add( ee );
            }
        }
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
        return ( QuantitationType ) this.getHibernateTemplate().load( QuantitationTypeImpl.class, id );
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
                    + " inner join dedv.designElement de where dedv.designElement in ( :cs )  ";
        } else {
            queryString = "select distinct dedv, dedv.designElement from ProcessedExpressionDataVectorImpl dedv fetch all properties"
                    + " inner join dedv.designElement de "
                    + " where dedv.designElement in (:cs ) and dedv.expressionExperiment in ( :ees )";
        }
        return getVectorsForProbesInExperiments( ees, cs2gene, queryString );
    }

    /**
     * @param ees
     * @param genes
     * @return
     */

    private Collection<DoubleVectorValueObject> handleGetProcessedExpressionDataArrays(
            Collection<ExpressionExperiment> ees, Collection<Gene> genes ) {

        // ees must be thawed first as currently implemented (?)

        Collection<DoubleVectorValueObject> results = new HashSet<DoubleVectorValueObject>();

        /*
         * Check the cache.
         */
        Collection<ExpressionExperiment> needToSearch = new HashSet<ExpressionExperiment>();
        Collection<Gene> genesToSearch = new HashSet<Gene>();
        checkCache( ees, genes, results, needToSearch, genesToSearch );

        if ( needToSearch.size() != 0 ) {

            Map<CompositeSequence, Collection<Gene>> cs2gene = CommonQueries.getCs2GeneMap( genesToSearch, this
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
            cs2gene = CommonQueries.getFullCs2AllGeneMap( cs2gene.keySet(), this.getSession() );

            Map<ProcessedExpressionDataVector, Collection<Gene>> processedDataVectors = getProcessedVectors(
                    needToSearch, cs2gene );
            log.info( processedDataVectors.size() );
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
        Map<DesignElement, BooleanVectorValueObject> missingValueMap = new HashMap<DesignElement, BooleanVectorValueObject>();
        for ( BooleanVectorValueObject bv : unpackedMissingValueData ) {
            missingValueMap.put( bv.getDesignElement(), bv );
        }

        boolean warned = false;
        for ( DoubleVectorValueObject rv : unpackedData ) {
            double[] data = rv.getData();
            DesignElement de = rv.getDesignElement();
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

    public Map<ExpressionExperiment, Collection<DoubleVectorValueObject>> getProcessedDataArrays(
            Collection<ExpressionExperiment> expressionExperiments ) {
        throw new UnsupportedOperationException();
    }

    public Collection<DoubleVectorValueObject> getProcessedDataArrays(
            Collection<ExpressionExperiment> expressionExperiments, Collection<Gene> genes ) {
        return this.handleGetProcessedExpressionDataArrays( expressionExperiments, genes );
    }

    @Override
    public void remove( ProcessedExpressionDataVector designElementDataVector ) {
        this.getHibernateTemplate().delete( designElementDataVector );

    }

    public Collection<DoubleVectorValueObject> getProcessedDataArrays( ExpressionExperiment expressionExperiment ) {
        throw new UnsupportedOperationException();
    }

}
