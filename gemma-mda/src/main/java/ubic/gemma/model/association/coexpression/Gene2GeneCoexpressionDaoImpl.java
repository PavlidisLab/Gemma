/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2008 University of British Columbia
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
package ubic.gemma.model.association.coexpression;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.ehcache.Element;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import edu.emory.mathcs.backport.java.util.Arrays;

import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.util.ConfigUtils;
import ubic.gemma.util.TaxonUtility;

/**
 * Manages 'links' between genes.
 * 
 * @see ubic.gemma.model.association.coexpression.Gene2GeneCoexpression
 * @version $Id$
 * @author klc
 * @author paul
 */
@Repository
public class Gene2GeneCoexpressionDaoImpl extends Gene2GeneCoexpressionDaoBase {

    @Autowired
    public Gene2GeneCoexpressionDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    private static boolean SINGLE_QUERY_FOR_LINKS = ConfigUtils.getBoolean( "store.gene.coexpression.bothways", true );

    /**
     * For storing information about gene results that are cached.
     */
    private static class GeneCached implements Serializable {
        /**
         * 
         */
        private static final long serialVersionUID = 915877171652447101L;
        long analysisId;
        long geneId;

        public GeneCached( long geneId, long analysisId ) {
            super();
            this.geneId = geneId;
            this.analysisId = analysisId;
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
            GeneCached other = ( GeneCached ) obj;
            if ( analysisId != other.analysisId ) return false;
            if ( geneId != other.geneId ) return false;
            return true;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ( int ) ( analysisId ^ ( analysisId >>> 32 ) );
            result = prime * result + ( int ) ( geneId ^ ( geneId >>> 32 ) );
            return result;
        }

    }

    static private class SupportComparator implements Comparator<Gene2GeneCoexpression> {
        @Override
        public int compare( Gene2GeneCoexpression o1, Gene2GeneCoexpression o2 ) {
            return -o1.getNumDataSets().compareTo( o2.getNumDataSets() );
        }
    }

    private static Log log = LogFactory.getLog( Gene2GeneCoexpressionDaoImpl.class );

    /**
     * Clear the cache of gene2gene objects. This should be run when gene2gene is updated.
     * <p>
     * FIXME externalize this and set it up so it can be done in a taxon-specific way?
     */
    protected void clearCache() {
        this.getGene2GeneCoexpressionCache().getCache().removeAll();
    }

    /*
     * Implementation note: we need the sourceAnalysis because although we normally have only one analysis per taxon,
     * when reanalyses are in progress there can be more than one temporarily. (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionDaoBase#handleFindCoexpressionRelationships(java
     * .util.Collection, ubic.gemma.model.analysis.Analysis, int)
     */
    @Override
    protected java.util.Map<Gene, Collection<Gene2GeneCoexpression>> handleFindCoexpressionRelationships(
            Collection<Gene> genes, int stringency, int maxResults, GeneCoexpressionAnalysis sourceAnalysis ) {

        /*
         * Check cache and initialize the result data structure.
         */
        Collection<Gene> genesNeeded = new HashSet<Gene>();

        List<Gene2GeneCoexpression> rawResults = new ArrayList<Gene2GeneCoexpression>();

        Map<Gene, Collection<Gene2GeneCoexpression>> finalResult = new HashMap<Gene, Collection<Gene2GeneCoexpression>>();
        for ( Gene g : genes ) {
            Element e = this.getGene2GeneCoexpressionCache().getCache()
                    .get( new GeneCached( g.getId(), sourceAnalysis.getId() ) );
            if ( e != null ) {
                rawResults.addAll( ( List<Gene2GeneCoexpression> ) e.getValue() );
            } else {
                genesNeeded.add( g );
            }
        }

        int CHUNK_SIZE = 10;

        if ( genesNeeded.size() > 0 ) {

            // Potentially too many genes to put in one hibernate query.
            // Batch it up!

            int count = 0;
            Collection<Gene> batch = new HashSet<Gene>();

            for ( Gene g : genesNeeded ) {
                batch.add( g );
                count++;
                if ( count % CHUNK_SIZE == 0 ) {
                	StopWatch timer = new StopWatch();
                	timer.start();                	
                    rawResults.addAll( getCoexpressionRelationshipsFromDB( batch, sourceAnalysis ) );
                    
                    if ( timer.getTime() > 100 ) {
                        log.info( "getCoexpressionRelationshipsFromDB for genes:"+Arrays.toString(batch.toArray())+" and sourceAnalysis"+sourceAnalysis.getId()+" took " + timer.getTime() + "ms" );
                    }
                    batch.clear();
                }
            }

            if ( batch.size() > 0 ) {
                rawResults.addAll( getCoexpressionRelationshipsFromDB( batch, sourceAnalysis ) );
            }
        }

        /*
         * Filter
         */
        Collection<GeneLink> seen = new HashSet<GeneLink>();
        Collections.sort( rawResults, new SupportComparator() );
        for ( Gene2GeneCoexpression g2g : rawResults ) {

            if ( g2g.getNumDataSets() < stringency ) continue;

            GeneLink link = new GeneLink( g2g );
            if ( seen.contains( link ) ) {
                continue;
            }
            seen.add( link );

            Gene firstGene = g2g.getFirstGene();
            Gene secondGene = g2g.getSecondGene();

            if ( genes.contains( firstGene ) ) {

                if ( !finalResult.containsKey( firstGene ) ) {
                    finalResult.put( firstGene, new HashSet<Gene2GeneCoexpression>() );
                } else if ( maxResults > 0 && finalResult.get( firstGene ).size() >= maxResults ) {
                    continue;
                }
                finalResult.get( firstGene ).add( g2g );

            } else if ( genes.contains( secondGene ) ) {

                if ( !finalResult.containsKey( secondGene ) ) {
                    finalResult.put( secondGene, new HashSet<Gene2GeneCoexpression>() );
                } else if ( maxResults > 0 && finalResult.get( secondGene ).size() >= maxResults ) {
                    continue;
                }
                finalResult.get( secondGene ).add( g2g );
            }
        }

        return finalResult;
    }

    /**
     * @see ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionDao#findCoexpressionRelationships(null,
     *      java.util.Collection) <p>
     *      Implementation note: we need the sourceAnalysis because although we normally have only one analysis per *
     *      taxon, when reanalyses are in progress there can be more than one temporarily.
     *      <p>
     */
    @Override
    protected java.util.Collection<Gene2GeneCoexpression> handleFindCoexpressionRelationships( Gene gene,
            int stringency, int maxResults, GeneCoexpressionAnalysis sourceAnalysis ) {

        Collection<Gene> genes = new HashSet<Gene>();
        genes.add( gene );
        return this.handleFindCoexpressionRelationships( genes, stringency, maxResults, sourceAnalysis ).get( gene );
    }

    /*
     * Implementation note: we need the sourceAnalysis because although we normally have only one analysis per taxon,
     * when reanalyses are in progress there can be more than one temporarily. (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionDaoBase#handleFindInterCoexpressionRelationships
     * (java.util.Collection, ubic.gemma.model.analysis.Analysis, int)
     */
    @Override
    protected java.util.Map<Gene, Collection<Gene2GeneCoexpression>> handleFindInterCoexpressionRelationships(
            Collection<Gene> genes, int stringency, GeneCoexpressionAnalysis sourceAnalysis ) {

        if ( genes.size() == 0 ) return new HashMap<Gene, Collection<Gene2GeneCoexpression>>();

        // we assume the genes are from the same taxon.
        String g2gClassName = getClassName( genes.iterator().next() );

        Collection<Gene> unseen = new HashSet<Gene>();
        unseen.addAll( genes );

        List<Gene2GeneCoexpression> rawResults = new ArrayList<Gene2GeneCoexpression>();
        Collection<Gene> genesNeeded = new HashSet<Gene>();
        for ( Gene g : genes ) {
            Element e = this.getGene2GeneCoexpressionCache().getCache()
                    .get( new GeneCached( g.getId(), sourceAnalysis.getId() ) );
            if ( e != null ) {
                /*
                 * find results for the cached result that include the second gene at the appropriate stringency.
                 */
                for ( Gene2GeneCoexpression g2g : ( List<Gene2GeneCoexpression> ) e.getValue() ) {
                    if ( genes.contains( g2g.getFirstGene() ) && genes.contains( g2g.getSecondGene() )
                            && g2g.getNumDataSets() >= stringency ) rawResults.add( g2g );
                }
            } else {
                genesNeeded.add( g );
            }
        }

        Map<Gene, Collection<Gene2GeneCoexpression>> result = new HashMap<Gene, Collection<Gene2GeneCoexpression>>();

        for ( Gene g : genes ) {
            result.put( g, new HashSet<Gene2GeneCoexpression>() );
        }

        /*
         * Filter the raw results
         */
        if ( rawResults.size() > 0 ) {
            Collections.sort( rawResults, new SupportComparator() );
            for ( Gene2GeneCoexpression g2g : rawResults ) {
                if ( g2g.getNumDataSets() < stringency ) continue;
                Gene firstGene = g2g.getFirstGene();
                Gene secondGene = g2g.getSecondGene();

                if ( genes.contains( firstGene ) && genes.contains( secondGene ) ) {
                    result.get( secondGene ).add( g2g );
                }
            }
        }

        /*
         * Note: doing this using two 'in' clauses with many genes is very slow -- it can take minutes to complete a
         * query! Doing it the dumb way is shockingly fast. For just a few genes it probably doesn't matter.
         */
        final String firstQueryString = "select g2g from " + g2gClassName
                + " as g2g where g2g.firstGene = :qgene and g2g.secondGene in (:genes) "
                + "and g2g.numDataSets >= :stringency and g2g.sourceAnalysis = :sourceAnalysis";

        // we only use this if not SINGLE_QUERY_FOR_LINKS
        final String secondQueryString = "select g2g from " + g2gClassName
                + " as g2g where g2g.secondGene = :qgene and g2g.firstGene in (:genes) "
                + "and g2g.numDataSets >= :stringency and g2g.sourceAnalysis = :sourceAnalysis";

        for ( Gene queryGene : genesNeeded ) {

            log.debug( queryGene );

            StopWatch timer = new StopWatch();
    		timer.start();
    		log.info("Starting: "+firstQueryString +" for genes "+Arrays.toString(genes.toArray()) +" and sourceAnalysis"+sourceAnalysis.getId());
            Collection<Gene2GeneCoexpression> r = this.getHibernateTemplate().findByNamedParam( firstQueryString,
                    new String[] { "qgene", "genes", "stringency", "sourceAnalysis" },
                    new Object[] { queryGene, unseen, stringency, sourceAnalysis } );
            if ( timer.getTime() > 1000 ) {
                log.info(firstQueryString +" for genes "+Arrays.toString(genes.toArray()) +" and sourceAnalysis"+sourceAnalysis.getId()+" took " + timer.getTime() + "ms" );
            }
            
            if ( !SINGLE_QUERY_FOR_LINKS ) {
            	timer.reset();
            	timer.start();
            	
            	log.info("Starting: "+secondQueryString +" for genes "+Arrays.toString(genes.toArray()) +" and sourceAnalysis"+sourceAnalysis.getId());
            	
                r.addAll( this.getHibernateTemplate().findByNamedParam( secondQueryString,
                        new String[] { "qgene", "genes", "stringency", "sourceAnalysis" },
                        new Object[] { queryGene, unseen, stringency, sourceAnalysis } ) );
                
                if ( timer.getTime() > 1000 ) {
                    log.info("!SINGLE_QUERY_FOR_LINKS "+secondQueryString +" for genes "+Arrays.toString(genes.toArray()) +" and sourceAnalysis"+sourceAnalysis.getId()+" took " + timer.getTime() + "ms" );
                }
            }

            List<Gene2GeneCoexpression> lr = new ArrayList<Gene2GeneCoexpression>( r );
            Collections.sort( lr, new SupportComparator() );

            Collection<GeneLink> seen = new HashSet<GeneLink>();

            for ( Gene2GeneCoexpression g2g : r ) {

                GeneLink link = new GeneLink( g2g );
                if ( seen.contains( link ) ) {
                    continue;
                }

                /*
                 * all the genes are guaranteed to be in the query list. But we want them listed both ways so we count
                 * them up right later.
                 */
                result.get( g2g.getFirstGene() ).add( g2g );
                result.get( g2g.getSecondGene() ).add( g2g );

                seen.add( link );
            }

            /*
             * DO NOT populate the cache with these results, as they are limited by stringency and the second gene
             * choice.
             */
        }

        return result;

    }

    /**
     * @param object
     */
    protected void removeFromCache( Gene2GeneCoexpression object ) {
        this.getGene2GeneCoexpressionCache().getCache()
                .remove( new GeneCached( object.getFirstGene().getId(), object.getSourceAnalysis().getId() ) );
        this.getGene2GeneCoexpressionCache().getCache()
                .remove( new GeneCached( object.getFirstGene().getId(), object.getSourceAnalysis().getId() ) );
    }

    /**
     * @param gene
     * @return
     */
    private String getClassName( Gene gene ) {
        String g2gClassName;
        if ( TaxonUtility.isHuman( gene.getTaxon() ) )
            g2gClassName = "HumanGeneCoExpressionImpl";
        else if ( TaxonUtility.isMouse( gene.getTaxon() ) )
            g2gClassName = "MouseGeneCoExpressionImpl";
        else if ( TaxonUtility.isRat( gene.getTaxon() ) )
            g2gClassName = "RatGeneCoExpressionImpl";
        else
            // must be other
            g2gClassName = "OtherGeneCoExpressionImpl";
        return g2gClassName;
    }

    /**
     * @param result map to add to
     * @param genes
     * @param stringency
     * @param maxResults
     * @param sourceAnalysis
     */
    private List<Gene2GeneCoexpression> getCoexpressionRelationshipsFromDB( Collection<Gene> genes,
            GeneCoexpressionAnalysis sourceAnalysis ) {

        // WARNING we assume the genes are from the same taxon.
        String g2gClassName = getClassName( genes.iterator().next() );

        final String queryStringFirstVector = "select g2g from " + g2gClassName
                + " as g2g where g2g.firstGene in (:genes) and g2g.sourceAnalysis = :sourceAnalysis";

        final String queryStringSecondVector = "select g2g from " + g2gClassName
                + " as g2g where g2g.secondGene in (:genes) and g2g.sourceAnalysis = :sourceAnalysis";

        List<Gene2GeneCoexpression> r = new ArrayList<Gene2GeneCoexpression>();
		StopWatch timer = new StopWatch();
		timer.start();
		
		log.info("Starting "+queryStringFirstVector +" for genes "+Arrays.toString(genes.toArray()) +" and sourceAnalysis"+sourceAnalysis.getId());
        r.addAll( this.getHibernateTemplate().findByNamedParam( queryStringFirstVector,
                new String[] { "genes", "sourceAnalysis" }, new Object[] { genes, sourceAnalysis } ) );
        if ( timer.getTime() > 1000 ) {
            log.info(queryStringFirstVector +" for genes "+Arrays.toString(genes.toArray()) +" and sourceAnalysis"+sourceAnalysis.getId()+" took " + timer.getTime() + "ms" );
        }
        if ( !SINGLE_QUERY_FOR_LINKS ) {
        	timer.reset();
        	timer.start();
        	
        	log.info("Starting "+queryStringSecondVector +" for genes "+Arrays.toString(genes.toArray()) +" and sourceAnalysis"+sourceAnalysis.getId());
            r.addAll( this.getHibernateTemplate().findByNamedParam( queryStringSecondVector,
                    new String[] { "genes", "sourceAnalysis" }, new Object[] { genes, sourceAnalysis } ) );
            if ( timer.getTime() > 1000 ) {
            	log.info("!SINGLE_QUERY_FOR_LINKS "+queryStringFirstVector +" for genes "+Arrays.toString(genes.toArray()) +" and sourceAnalysis"+sourceAnalysis.getId()+" took " + timer.getTime() + "ms" );
            }
        } else {
            if ( r.isEmpty() ) return r;

            /*
             * remove duplicates, since each link can be here twice. FIXME maybe could do at same time as caching.
             */
            log.info("SINGLE_QUERY_FOR_LINKS=false: starting to remove duplicates for "+r.size()+ "results " );
            timer.reset();
        	timer.start();
            int removed = 0;
            Set<GeneLink> allSeen = new HashSet<GeneLink>();
            for ( Iterator<Gene2GeneCoexpression> iterator = r.iterator(); iterator.hasNext(); ) {
                Gene2GeneCoexpression g2g = iterator.next();

                GeneLink seen = new GeneLink( g2g );

                if ( allSeen.contains( seen ) ) {
                    iterator.remove();
                    ++removed;
                    continue;
                }

                allSeen.add( seen );
            }
            
            if ( timer.getTime() > 1000 ) {
            	log.info("Removing duplicates took: "+timer.getTime()+"ms");
            }
            if ( removed > 0 ) log.info( "Removed " + removed + " duplicate links" );

            if ( r.isEmpty() ) throw new IllegalStateException( "Removed everything!" );
        }

        timer.reset();
    	timer.start();
        Collections.sort( r, new SupportComparator() );
        if ( timer.getTime() > 1000 ) {
        	log.info("Sorting "+ r.size()+" results took: "+timer.getTime()+"ms");
        }

        timer.reset();
    	timer.start();
        cacheCoexpression( genes, sourceAnalysis, r );
        
        if ( timer.getTime() > 1000 ) {
        	log.info("caching "+ r.size()+" results took: "+timer.getTime()+"ms");
        }

        return r;

    }

    private void cacheCoexpression( Collection<Gene> genes, GeneCoexpressionAnalysis sourceAnalysis,
            List<Gene2GeneCoexpression> r ) {
        /*
         * Cache all values.
         */
        Map<Gene, List<Gene2GeneCoexpression>> forCache = new HashMap<Gene, List<Gene2GeneCoexpression>>();
        for ( Gene2GeneCoexpression g2g : r ) {

            Gene firstGene = g2g.getFirstGene();
            if ( genes.contains( firstGene ) ) {
                if ( !forCache.containsKey( firstGene ) ) {
                    forCache.put( firstGene, new ArrayList<Gene2GeneCoexpression>() );
                }

                forCache.get( firstGene ).add( g2g );
            }

            Gene secondGene = g2g.getSecondGene();
            if ( genes.contains( secondGene ) ) {
                if ( !forCache.containsKey( secondGene ) ) {
                    forCache.put( secondGene, new ArrayList<Gene2GeneCoexpression>() );
                }

                forCache.get( secondGene ).add( g2g );
            }

        }

        log.info("Begin caching results for "+forCache.keySet().size()+" genes ");
        int totalResultsCached=0;
        for ( Gene gene : forCache.keySet() ) {
        	
        	List<Gene2GeneCoexpression> resultsToCache = forCache.get( gene );
        	
        	log.info("Caching "+resultsToCache.size()+" results for gene "+gene.getOfficialSymbol()+" and sourceAnalysis "+sourceAnalysis.getId());
        	
            this.getGene2GeneCoexpressionCache().getCache()
                    .put( new Element( new GeneCached( gene.getId(), sourceAnalysis.getId() ), resultsToCache ) );
            
            totalResultsCached = totalResultsCached+resultsToCache.size();
        }
        log.info("Done caching "+totalResultsCached+" results for "+forCache.keySet().size()+" genes ");
        
    }

    @Override
    public Integer getNumberOfLinks( Gene gene, GeneCoexpressionAnalysis analysis ) {
        // Assume SINGLE_QUERY_FOR_LINKS.
        final String q = "select count (g2g) from " + getClassName( gene ) + " as g2g where g2g.firstGene = :qgene "
                + " and g2g.sourceAnalysis = :sourceAnalysis";
        List<?> r = this.getHibernateTemplate().findByNamedParam( q, new String[] { "qgene", "sourceAnalysis" },
                new Object[] { gene, analysis } );

        return ( ( Long ) r.iterator().next() ).intValue();

    }
}

class GeneLink {

    private Gene g1;
    private Gene g2;
    private boolean positive = true;

    public GeneLink( Gene2GeneCoexpression link ) {
        this( link.getFirstGene(), link.getSecondGene(), link.getEffect() > 0 );
    }

    public GeneLink( Gene g1, Gene g2, boolean positive ) {
        if ( g1.getId() < g2.getId() ) {
            this.g1 = g1;
            this.g2 = g2;
        } else {
            this.g1 = g2;
            this.g2 = g1;
        }
        this.positive = positive;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( g1 == null ) ? 0 : g1.hashCode() );
        result = prime * result + ( ( g2 == null ) ? 0 : g2.hashCode() );
        result = prime * result + ( positive ? 1231 : 1237 );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        GeneLink other = ( GeneLink ) obj;
        if ( g1 == null ) {
            if ( other.g1 != null ) return false;
        } else if ( !g1.equals( other.g1 ) ) return false;
        if ( g2 == null ) {
            if ( other.g2 != null ) return false;
        } else if ( !g2.equals( other.g2 ) ) return false;
        if ( positive != other.positive ) return false;
        return true;
    }

}
