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
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

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
public class ProcessedExpressionDataVectorDaoImpl extends DesignElementDataVectorDaoImpl implements
        ProcessedExpressionDataVectorDao {

    private static final String PROCESSED_DATA_VECTOR_CACHE_NAME = "ProcessedDataVectorCache";
    private static Log log = LogFactory.getLog( ProcessedExpressionDataVectorDaoImpl.class.getName() );
    private Cache cache;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorDao#createProcessedDataVectors(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    @SuppressWarnings("unchecked")
    public Collection<ProcessedExpressionDataVector> createProcessedDataVectors(
            final ExpressionExperiment expressionExperiment ) {
        /*
         * If the experiment already has them, delete them.
         */
        Collection<ProcessedExpressionDataVector> oldVectors = this.getProcessedVectors( expressionExperiment );
        if ( oldVectors.size() > 0 ) {
            log.info( "Removing old processed vectors" );
            this.remove( oldVectors );
        }
        expressionExperiment.setProcessedExpressionDataVectors( null );
        this.getHibernateTemplate().update( expressionExperiment );

        log.info( "Computing processed expression vectors for " + expressionExperiment );

        /*
         * Figure out if it is two-channel
         */
        boolean isTwoChannel = false;
        Collection<ArrayDesign> arrayDesignsUsed = CommonQueries.getArrayDesignsUsed( expressionExperiment, this
                .getSession() );
        for ( ArrayDesign ad : arrayDesignsUsed ) {
            if ( !ad.getTechnologyType().equals( TechnologyType.ONECOLOR ) ) {
                isTwoChannel = true;
            }
        }

        Collection<DesignElementDataVector> missingValueVectors = new HashSet<DesignElementDataVector>();
        if ( isTwoChannel ) {
            missingValueVectors = super.getMissingValueVectors( expressionExperiment );
        }

        Collection<DesignElementDataVector> preferredDataVectors = super.getPreferredDataVectors( expressionExperiment );
        if ( preferredDataVectors.isEmpty() ) {
            throw new IllegalArgumentException( "No preferred data vectors for " + expressionExperiment );
        }

        Collection<DoubleVectorValueObject> maskedVectorObjects = maskAndUnpack( preferredDataVectors,
                missingValueVectors );

        /*
         * Create the vectors.
         */
        QuantitationType preferredMaskedDataQuantitationType = getPreferredMaskedDataQuantitationType( preferredDataVectors
                .iterator().next().getQuantitationType() );
        Collection<ProcessedExpressionDataVector> result = new ArrayList<ProcessedExpressionDataVector>();
        for ( DoubleVectorValueObject dvvo : maskedVectorObjects ) {
            result.add( ( ProcessedExpressionDataVector ) dvvo
                    .toDesignElementDataVector( preferredMaskedDataQuantitationType ) );
        }

        Collection<ProcessedExpressionDataVector> results = this.create( result );
        log.info( "Creating " + results.size() + " processed data vectors" );
        this.getHibernateTemplate().lock( expressionExperiment, LockMode.READ );
        Hibernate.initialize( expressionExperiment.getProcessedExpressionDataVectors() );
        expressionExperiment.setProcessedExpressionDataVectors( new HashSet<ProcessedExpressionDataVector>( results ) );

        this.getHibernateTemplate().update( expressionExperiment );
        return expressionExperiment.getProcessedExpressionDataVectors();

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorDao#getProcessedDataMatrices(java.util.Collection)
     */
    public Map<ExpressionExperiment, Collection<DoubleVectorValueObject>> getProcessedDataArrays(
            Collection<ExpressionExperiment> expressionExperiments ) {
        // FIXME implement
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorDao#getProcessedDataMatrices(java.util.Collection,
     *      java.util.Collection)
     */
    public Collection<DoubleVectorValueObject> getProcessedDataArrays(
            Collection<ExpressionExperiment> expressionExperiments, Collection<Gene> genes ) {
        return this.handleGetProcessedExpressionDataArrays( expressionExperiments, genes );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorDao#getProcessedDataMatrix(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    public Collection<DoubleVectorValueObject> getProcessedDataArrays( ExpressionExperiment expressionExperiment ) {
        // FIXME implement
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorDao#getProcessedDataMatrix(ubic.gemma.model.expression.experiment.ExpressionExperiment,
     *      java.util.Collection)
     */
    public Collection<DoubleVectorValueObject> getProcessedDataArrays( ExpressionExperiment expressionExperiment,
            Collection<Gene> genes ) {
        Collection<ExpressionExperiment> expressionExperiments = new HashSet<ExpressionExperiment>();
        expressionExperiments.add( expressionExperiment );
        return this.handleGetProcessedExpressionDataArrays( expressionExperiments, genes );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorDao#getProcessedVectors(ubic.gemma.model.expression.experiment.ExpressionExperiment)
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
     * 
     * @see ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorDao#getRanks(java.util.Collection,
     *      java.util.Collection, ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorDao.RankMethod)
     */
    public Map<ExpressionExperiment, Map<Gene, Collection<Double>>> getRanks(
            Collection<ExpressionExperiment> expressionExperiments, Collection<Gene> genes, RankMethod method ) {
        Map<CompositeSequence, Collection<Gene>> cs2gene = CommonQueries.getCs2GeneMap( genes, this.getSession() );
        if ( cs2gene.keySet().size() == 0 ) {
            log.warn( "No composite sequences found for genes" );
            return new HashMap<ExpressionExperiment, Map<Gene, Collection<Double>>>();
        }

        final String queryString = "select distinct ded.expressionExperiment, dedv.designElement, dedv.rankFromMean, dedv.rankFromMax from ProcessedExpressionDataVectorImpl dedv "
                + " inner join fetch dedv.bioAssayDimension bd "
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

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorDao#getRanks(ubic.gemma.model.expression.experiment.ExpressionExperiment,
     *      java.util.Collection, ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorDao.RankMethod)
     */
    public Map<Gene, Collection<Double>> getRanks( ExpressionExperiment expressionExperiment, Collection<Gene> genes,
            RankMethod method ) {
        Map<CompositeSequence, Collection<Gene>> cs2gene = CommonQueries.getCs2GeneMap( genes, this.getSession() );
        if ( cs2gene.keySet().size() == 0 ) {
            log.warn( "No composite sequences found for genes" );
            return new HashMap<Gene, Collection<Double>>();
        }

        final String queryString = "select distinct dedv.designElement, dedv.rankFromMean, dedv.rankFromMax from ProcessedExpressionDataVectorImpl dedv "
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
     * 
     * @see ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorDao#getRanks(ubic.gemma.model.expression.experiment.ExpressionExperiment,
     *      ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorDao.RankMethod)
     */
    public Map<DesignElement, Double> getRanks( ExpressionExperiment expressionExperiment, RankMethod method ) {
        final String queryString = "select dedv.designElement, dedv.rankFromMean, dedv.rankFromMax from ProcessedExpressionDataVectorImpl dedv where dedv.expressionExperiment = :ee";
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

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.dao.support.DaoSupport#initDao()
     */
    @Override
    protected void initDao() throws Exception {
        super.initDao();
        try {
            /*
             * Create a cache for the probe data.s
             */
            CacheManager manager = CacheManager.getInstance();

            if ( manager.cacheExists( PROCESSED_DATA_VECTOR_CACHE_NAME ) ) {
                return;
            }

            /*
             * FIXME configure this somewhere else.
             */
            this.cache = new Cache( PROCESSED_DATA_VECTOR_CACHE_NAME, 10000, MemoryStoreEvictionPolicy.LFU, false, "",
                    false, 100, 1000, false, 500, null );

            manager.addCache( this.cache );
            this.cache = manager.getCache( PROCESSED_DATA_VECTOR_CACHE_NAME );

        } catch ( CacheException e ) {
            throw new RuntimeException( e );
        }

    }

    /**
     * for testing purposes.
     * 
     * @return
     */
    public Cache getCache() {
        return this.cache;
    }

    /**
     * @param newResults
     */
    private void cacheResults( Collection<DoubleVectorValueObject> newResults ) {
        /*
         * Break up by gene and EE to cache collections of vectors for EE-gene combos.
         */
        Map<ExpressionExperiment, Map<Gene, Collection<DoubleVectorValueObject>>> mapForCache = new HashMap<ExpressionExperiment, Map<Gene, Collection<DoubleVectorValueObject>>>();
        for ( DoubleVectorValueObject v : newResults ) {
            ExpressionExperiment e = v.getExpressionExperiment();
            if ( !mapForCache.containsKey( e ) ) {
                mapForCache.put( e, new HashMap<Gene, Collection<DoubleVectorValueObject>>() );
            }
            Map<Gene, Collection<DoubleVectorValueObject>> innerMap = mapForCache.get( e );
            for ( Gene g : v.getGenes() ) {
                if ( !innerMap.containsKey( g ) ) {
                    innerMap.put( g, new HashSet<DoubleVectorValueObject>() );
                }
                innerMap.get( g ).add( v );
            }
        }

        for ( ExpressionExperiment e : mapForCache.keySet() ) {
            for ( Gene g : mapForCache.get( e ).keySet() ) {
                VectorKey k = new VectorKey( e, g );
                cache.put( new Element( k, mapForCache.get( e ).get( g ) ) );
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
     * @param cs2gene
     * @return
     */
    private Map<DesignElementDataVector, Collection<Gene>> getProcessedVectors( Collection ees,
            Map<CompositeSequence, Collection<Gene>> cs2gene ) {

        final String queryString;
        if ( ees == null || ees.size() == 0 ) {
            queryString = "select distinct dedv, dedv.designElement from ProcessedExpressionDataVectorImpl dedv "
                    + " inner join fetch dedv.bioAssayDimension bd inner join dedv.designElement de  "
                    + " where dedv.designElement in ( :cs )  ";
        } else {
            queryString = "select distinct dedv, dedv.designElement from ProcessedExpressionDataVectorImpl dedv"
                    + " inner join fetch dedv.bioAssayDimension bd " + " inner join dedv.designElement de "
                    + " where dedv.designElement in (:cs )  and dedv.expressionExperiment in ( :ees )";
        }
        return getVectorsForProbesInExperiments( ees, cs2gene, queryString );
    }

    /**
     * @param ees
     * @param genes
     * @return
     */
    @SuppressWarnings("unchecked")
    private Collection<DoubleVectorValueObject> handleGetProcessedExpressionDataArrays(
            Collection<ExpressionExperiment> ees, Collection<Gene> genes ) {

        // ees must be thawed first as currently implemented (?)

        Collection<DoubleVectorValueObject> results = new HashSet<DoubleVectorValueObject>();

        /*
         * Check the cache.
         */
        Collection<ExpressionExperiment> needToSearch = new HashSet<ExpressionExperiment>();
        Collection<Gene> genesToSearch = new HashSet<Gene>();
        for ( ExpressionExperiment ee : ees ) {
            for ( Gene g : genes ) {
                VectorKey k = new VectorKey( ee, g );
                Element element = cache.get( k );
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

        if ( needToSearch.size() != 0 ) {
            Map<CompositeSequence, Collection<Gene>> cs2gene = CommonQueries.getCs2GeneMap( genesToSearch, this
                    .getSession() );
            if ( cs2gene.keySet().size() == 0 ) {
                log.warn( "No composite sequences found for genes" );
                return new HashSet<DoubleVectorValueObject>();
            }

            Map<DesignElementDataVector, Collection<Gene>> processedDataVectors = getProcessedVectors( needToSearch,
                    cs2gene );
            Collection<DoubleVectorValueObject> newResults = unpack( processedDataVectors );
            cacheResults( newResults );
            results.addAll( newResults );
        }

        return results;

    }

    /**
     * @param preferredData
     * @param missingValueData
     * @return
     */
    private Collection<DoubleVectorValueObject> maskAndUnpack( Collection<DesignElementDataVector> preferredData,
            Collection<DesignElementDataVector> missingValueData ) {
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

        for ( DoubleVectorValueObject rv : unpackedData ) {
            double[] data = rv.getData();
            DesignElement de = rv.getDesignElement();
            BooleanVectorValueObject mv = missingValueMap.get( de );
            if ( mv == null ) {
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

    private Collection<DoubleVectorValueObject> unpack( Collection<DesignElementDataVector> data ) {
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
    private Collection<DoubleVectorValueObject> unpack( Map<DesignElementDataVector, Collection<Gene>> data ) {
        Collection<DoubleVectorValueObject> result = new HashSet<DoubleVectorValueObject>();
        for ( DesignElementDataVector v : data.keySet() ) {
            result.add( new DoubleVectorValueObject( v, data.get( v ) ) );
        }
        return result;
    }

    private Collection<BooleanVectorValueObject> unpackBooleans( Collection<DesignElementDataVector> data ) {
        Collection<BooleanVectorValueObject> result = new HashSet<BooleanVectorValueObject>();

        for ( DesignElementDataVector v : data ) {
            result.add( new BooleanVectorValueObject( v ) );
        }
        return result;
    }

    private class VectorKey {
        Long eeId;
        Long geneId;

        public VectorKey( ExpressionExperiment ee, Gene g ) {
            super();
            this.eeId = ee.getId();
            this.geneId = g.getId();
        }

        public VectorKey( Long eeId, Long geneId ) {
            super();
            this.eeId = eeId;
            this.geneId = geneId;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if ( this == obj ) return true;
            if ( obj == null ) return false;
            if ( getClass() != obj.getClass() ) return false;
            final VectorKey other = ( VectorKey ) obj;
            if ( eeId == null ) {
                if ( other.eeId != null ) return false;
            } else if ( !eeId.equals( other.eeId ) ) return false;
            if ( geneId == null ) {
                if ( other.geneId != null ) return false;
            } else if ( !geneId.equals( other.geneId ) ) return false;
            return true;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            // FIXME: this must be unique.
            final int prime = 31;
            int result = 1;
            result = prime * result + ( ( eeId == null ) ? 0 : eeId.hashCode() );
            result = prime * result + ( ( geneId == null ) ? 0 : geneId.hashCode() );
            return result;
        }
    }

}
