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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;

import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.util.TaxonUtility;

/**
 * Manages 'links' between genes.
 * 
 * @see ubic.gemma.model.association.coexpression.Gene2GeneCoexpression
 * @version $Id$
 * @author klc
 * @author paul
 */
public abstract class Gene2GeneCoexpressionDaoImpl extends
        ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionDaoBase {

    private class SupportComparator implements Comparator<Gene2GeneCoexpression> {
        public int compare( Gene2GeneCoexpression o1, Gene2GeneCoexpression o2 ) {
            return -o1.getNumDataSets().compareTo( o2.getNumDataSets() );
        }
    }

    /**
     * Clear the cache of gene2gene objects. This should be run when gene2gene is updated. FIXME externalize this and
     * set it up so it can be done in a taxon-specific way?
     */
    protected void clearCache() {
        this.getGene2GeneCoexpressionCache().getCache().removeAll();
    }

    /*
     * Implementation note: we need the sourceAnalysis because although we normally have only one analysis per taxon,
     * when reanalyses are in progress there can be more than one temporarily. (non-Javadoc)
     * @see
     * ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionDaoBase#handleFindCoexpressionRelationships(java
     * .util.Collection, ubic.gemma.model.analysis.Analysis, int)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected java.util.Map<Gene, Collection<Gene2GeneCoexpression>> handleFindCoexpressionRelationships(
            Collection<Gene> genes, int stringency, int maxResults, GeneCoexpressionAnalysis sourceAnalysis ) {

        /*
         * Check cache and initialize the result data structure.
         */
        Collection<Gene> genesNeeded = new HashSet<Gene>();
        Map<Gene, Collection<Gene2GeneCoexpression>> result = new HashMap<Gene, Collection<Gene2GeneCoexpression>>();
        for ( Gene g : genes ) {
            result.put( g, new HashSet<Gene2GeneCoexpression>() );
            Element e = this.getGene2GeneCoexpressionCache().getCache().get( g.getId() );
            if ( e != null ) {
                result.put( g, ( Collection<Gene2GeneCoexpression> ) e.getValue() );
            } else {
                genesNeeded.add( g );
            }
        }
        int CHUNK_SIZE = 1000;

        // All genes were in the cache
        if ( genesNeeded.size() == 0 ) return result;

        // Less than batch size
        if ( genesNeeded.size() <= CHUNK_SIZE ) {
            getCoexpressionRelationshipsFromDB( result, genesNeeded, stringency, maxResults, sourceAnalysis );
            return result;
        }

        // Potentially too many genes to put in one hibernate query.
        // Batch it up!

        int count = 0;
        Collection<Gene> batch = new HashSet<Gene>();

        for ( Gene g : genesNeeded ) {
            batch.add( g );
            count++;
            if ( count % CHUNK_SIZE == 0 ) {
                getCoexpressionRelationshipsFromDB( result, batch, stringency, maxResults, sourceAnalysis );
                batch.clear();
            }
        }

        if ( batch.size() > 0 ) {
            getCoexpressionRelationshipsFromDB( result, batch, stringency, maxResults, sourceAnalysis );
        }

        return result;
    }

    /**
     * @see ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionDao#findCoexpressionRelationships(null,
     *      java.util.Collection)
     *      <p>
     *      Implementation note: we need the sourceAnalysis because although we normally have only one analysis per *
     *      taxon, when reanalyses are in progress there can be more than one temporarily. NOTE: this method is pretty
     *      redundant with the one that takes a collection of genes - possibly deprecate this.
     */
    @SuppressWarnings("unchecked")
    @Override
    protected java.util.Collection<Gene2GeneCoexpression> handleFindCoexpressionRelationships( Gene gene,
            int stringency, int maxResults, GeneCoexpressionAnalysis sourceAnalysis ) {

        Element element = this.getGene2GeneCoexpressionCache().getCache().get( gene.getId() );
        if ( element != null ) {
            return ( Collection<Gene2GeneCoexpression> ) element.getValue();
        }

        String g2gClassName;

        g2gClassName = getClassName( gene );

        final String queryStringFirstVector = "select distinct g2g from "
                + g2gClassName
                + " as g2g where g2g.firstGene = :gene and g2g.numDataSets >= :stringency and g2g.sourceAnalysis = :sourceAnalysis";

        final String queryStringSecondVector = "select distinct g2g from "
                + g2gClassName
                + " as g2g where g2g.secondGene = :gene and g2g.numDataSets >= :stringency and g2g.sourceAnalysis = :sourceAnalysis";

        Collection<Gene2GeneCoexpression> results = new HashSet<Gene2GeneCoexpression>();

        results.addAll( this.getHibernateTemplate().findByNamedParam( queryStringFirstVector,
                new String[] { "gene", "stringency", "sourceAnalysis" },
                new Object[] { gene, stringency, sourceAnalysis } ) );
        results.addAll( this.getHibernateTemplate().findByNamedParam( queryStringSecondVector,
                new String[] { "gene", "stringency", "sourceAnalysis" },
                new Object[] { gene, stringency, sourceAnalysis } ) );

        List<Gene2GeneCoexpression> lr = new ArrayList<Gene2GeneCoexpression>( results );
        Collections.sort( lr, new SupportComparator() );

        this.getGene2GeneCoexpressionCache().getCache().put( new Element( gene.getId(), lr ) );

        int count = 0;
        for ( Iterator<Gene2GeneCoexpression> it = lr.iterator(); it.hasNext(); ) {
            it.next();
            if ( maxResults > 0 && count > maxResults ) {
                it.remove();
            }
            count++;
        }

        return results;
    }

    /*
     * Implementation note: we need the sourceAnalysis because although we normally have only one analysis per taxon,
     * when reanalyses are in progress there can be more than one temporarily. (non-Javadoc)
     * @see
     * ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionDaoBase#handleFindInterCoexpressionRelationships
     * (java.util.Collection, ubic.gemma.model.analysis.Analysis, int)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected java.util.Map<Gene, Collection<Gene2GeneCoexpression>> handleFindInterCoexpressionRelationships(
            Collection<Gene> genes, int stringency, GeneCoexpressionAnalysis sourceAnalysis ) {

        if ( genes.size() == 0 ) return new HashMap<Gene, Collection<Gene2GeneCoexpression>>();

        // we assume the genes are from the same taxon.
        String g2gClassName = getClassName( genes.iterator().next() );

        final String queryString = "select g2g from "
                + g2gClassName
                + " as g2g where g2g.firstGene in (:genes) and g2g.secondGene in (:genes) and g2g.numDataSets >= :stringency and g2g.sourceAnalysis = :sourceAnalysis";

        Collection<Gene2GeneCoexpression> r = this.getHibernateTemplate().findByNamedParam( queryString,
                new String[] { "genes", "stringency", "sourceAnalysis" },
                new Object[] { genes, stringency, sourceAnalysis } );

        List<Gene2GeneCoexpression> lr = new ArrayList<Gene2GeneCoexpression>( r );
        Collections.sort( lr, new SupportComparator() );

        Map<Gene, Collection<Gene2GeneCoexpression>> result = new HashMap<Gene, Collection<Gene2GeneCoexpression>>();
        for ( Gene g : genes ) {
            result.put( g, new HashSet<Gene2GeneCoexpression>() );
        }
        int count = 0;
        for ( Gene2GeneCoexpression g2g : r ) {
            // all the genes are guaranteed to be in the query list. But we want them listed both ways so we count them
            // up right later.
            result.get( g2g.getFirstGene() ).add( g2g );
            result.get( g2g.getSecondGene() ).add( g2g );
            count++;
        }
        return result;

    }

    /*
     * (non-Javadoc)
     * @see org.springframework.dao.support.DaoSupport#initDao()
     */
    @Override
    protected void initDao() throws Exception {
        super.initDao();
        try {
            this.getGene2GeneCoexpressionCache().initializeCache();
        } catch ( CacheException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * @param object
     */
    protected void removeFromCache( Gene2GeneCoexpression object ) {
        this.getGene2GeneCoexpressionCache().getCache().remove( object.getFirstGene().getId() );
        this.getGene2GeneCoexpressionCache().getCache().remove( object.getSecondGene().getId() );
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
     * @param result2
     * @param genes
     * @param stringency
     * @param maxResults
     * @param sourceAnalysis
     * @return
     */
    @SuppressWarnings("unchecked")
    private Map<Gene, Collection<Gene2GeneCoexpression>> getCoexpressionRelationshipsFromDB(
            Map<Gene, Collection<Gene2GeneCoexpression>> result, Collection<Gene> genes, int stringency,
            int maxResults, GeneCoexpressionAnalysis sourceAnalysis ) {

        // WARNING we assume the genes are from the same taxon.
        String g2gClassName = getClassName( genes.iterator().next() );

        final String queryStringFirstVector = "select g2g from "
                + g2gClassName
                + " as g2g where g2g.firstGene in (:genes) and g2g.numDataSets >= :stringency and g2g.sourceAnalysis = :sourceAnalysis";

        final String queryStringSecondVector = "select g2g from "
                + g2gClassName
                + " as g2g where g2g.secondGene in (:genes) and g2g.numDataSets >= :stringency and g2g.sourceAnalysis = :sourceAnalysis";

        Collection<Gene2GeneCoexpression> r = new HashSet<Gene2GeneCoexpression>();

        r.addAll( this.getHibernateTemplate().findByNamedParam( queryStringFirstVector,
                new String[] { "genes", "stringency", "sourceAnalysis" },
                new Object[] { genes, stringency, sourceAnalysis } ) );
        r.addAll( this.getHibernateTemplate().findByNamedParam( queryStringSecondVector,
                new String[] { "genes", "stringency", "sourceAnalysis" },
                new Object[] { genes, stringency, sourceAnalysis } ) );

        List<Gene2GeneCoexpression> lr = new ArrayList<Gene2GeneCoexpression>( r );

        Collections.sort( lr, new SupportComparator() );

        int count = 0;

        for ( Gene2GeneCoexpression g2g : r ) {
            if ( maxResults > 0 && count == maxResults ) break;
            Gene firstGene = g2g.getFirstGene();
            Gene secondGene = g2g.getSecondGene();
            if ( genes.contains( firstGene ) ) {
                result.get( firstGene ).add( g2g );
            } else if ( genes.contains( secondGene ) ) {
                result.get( secondGene ).add( g2g );
            }
            count++;
        }

        for ( Gene g : genes ) {
            this.getGene2GeneCoexpressionCache().getCache().put( new Element( g.getId(), result.get( g ) ) );
        }

        return result;

    }

}