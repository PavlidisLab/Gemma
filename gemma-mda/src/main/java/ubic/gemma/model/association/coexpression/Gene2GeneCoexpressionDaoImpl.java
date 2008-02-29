/*
 * The Gemma project.
 * 
 * Copyright (c) 2006 University of British Columbia
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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import ubic.gemma.model.analysis.Analysis;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.util.TaxonUtility;

/**
 * @see ubic.gemma.model.association.coexpression.Gene2GeneCoexpression
 * @version $Id$
 * @author klc
 */
public class Gene2GeneCoexpressionDaoImpl extends
        ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionDaoBase {
    /**
     * @see ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionDao#findCoexpressionRelationships(null,
     *      java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected java.util.Collection handleFindCoexpressionRelationships( Gene gene, Analysis analysis, int stringency ) {
        String g2gClassName;

        g2gClassName = getClassName( gene );

        final String queryStringFirstVector = "select distinct g2g from "
                + g2gClassName
                + " as g2g where g2g.sourceAnalysis = :analysis and g2g.firstGene = :gene and g2g.numDataSets >= :stringency";

        final String queryStringSecondVector = "select distinct g2g from "
                + g2gClassName
                + " as g2g where g2g.sourceAnalysis = :analysis and g2g.secondGene = :gene and g2g.numDataSets >= :stringency";

        Collection<Gene2GeneCoexpression> results = new HashSet<Gene2GeneCoexpression>();

        results.addAll( this.getHibernateTemplate().findByNamedParam( queryStringFirstVector,
                new String[] { "analysis", "gene", "stringency" }, new Object[] { analysis, gene, stringency } ) );
        results.addAll( this.getHibernateTemplate().findByNamedParam( queryStringSecondVector,
                new String[] { "analysis", "gene", "stringency" }, new Object[] { analysis, gene, stringency } ) );

        return results;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionDaoBase#handleFindCoexpressionRelationships(java.util.Collection,
     *      ubic.gemma.model.analysis.Analysis, int)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected java.util.Map /* <Gene, Collection<Gene2GeneCoexpression>> */handleFindCoexpressionRelationships(
            Collection genes, Analysis analysis, int stringency ) {

        if ( genes.size() == 0 ) return new HashMap<Gene, Collection<Gene2GeneCoexpression>>();

        // we assume the genes are from the same taxon.
        String g2gClassName = getClassName( ( Gene ) genes.iterator().next() );

        final String queryStringFirstVector = "select g2g from "
                + g2gClassName
                + " as g2g where g2g.sourceAnalysis = :analysis and g2g.firstGene in (:genes) and g2g.numDataSets >= :stringency";

        final String queryStringSecondVector = "select g2g from "
                + g2gClassName
                + " as g2g where g2g.sourceAnalysis = :analysis and g2g.secondGene in (:genes) and g2g.numDataSets >= :stringency";

        Collection<Gene2GeneCoexpression> r = new HashSet<Gene2GeneCoexpression>();

        r.addAll( this.getHibernateTemplate().findByNamedParam( queryStringFirstVector,
                new String[] { "analysis", "genes", "stringency" }, new Object[] { analysis, genes, stringency } ) );
        r.addAll( this.getHibernateTemplate().findByNamedParam( queryStringSecondVector,
                new String[] { "analysis", "genes", "stringency" }, new Object[] { analysis, genes, stringency } ) );

        Map<Gene, Collection<Gene2GeneCoexpression>> result = new HashMap<Gene, Collection<Gene2GeneCoexpression>>();
        for ( Gene g : ( Collection<Gene> ) genes ) {
            result.put( g, new HashSet<Gene2GeneCoexpression>() );
        }

        for ( Gene2GeneCoexpression g2g : r ) {
            Gene firstGene = g2g.getFirstGene();
            Gene secondGene = g2g.getSecondGene();
            if ( genes.contains( firstGene ) ) {
                result.get( firstGene ).add( g2g );
            } else if ( genes.contains( secondGene ) ) {
                result.get( secondGene ).add( g2g );
            }
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionDaoBase#handleFindInterCoexpressionRelationships(java.util.Collection,
     *      ubic.gemma.model.analysis.Analysis, int)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected java.util.Map /* <Gene, Collection<Gene2GeneCoexpression> */handleFindInterCoexpressionRelationships(
            Collection genes, Analysis analysis, int stringency ) {

        if ( genes.size() == 0 ) return new HashMap<Gene, Collection<Gene2GeneCoexpression>>();

        // we assume the genes are from the same taxon.
        String g2gClassName = getClassName( ( Gene ) genes.iterator().next() );

        final String queryString = "select g2g from "
                + g2gClassName
                + " as g2g where g2g.sourceAnalysis = :analysis and g2g.firstGene in (:genes) and g2g.secondGene in (:genes) and g2g.numDataSets >= :stringency";

        Collection<Gene2GeneCoexpression> r = this.getHibernateTemplate().findByNamedParam( queryString,
                new String[] { "analysis", "genes", "stringency" }, new Object[] { analysis, genes, stringency } );

        Map<Gene, Collection<Gene2GeneCoexpression>> result = new HashMap<Gene, Collection<Gene2GeneCoexpression>>();
        for ( Gene g : ( Collection<Gene> ) genes ) {
            result.put( g, new HashSet<Gene2GeneCoexpression>() );
        }
        for ( Gene2GeneCoexpression g2g : r ) {
            // all the genes are guaranteed to be in the query list. But we want them listed both ways so we count them
            // up right later.
            result.get( g2g.getFirstGene() ).add( g2g );
            result.get( g2g.getSecondGene() ).add( g2g );
        }
        return result;

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

}