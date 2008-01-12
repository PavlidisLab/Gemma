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
import java.util.HashSet;

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

        if ( TaxonUtility.isHuman( gene.getTaxon() ) )
            g2gClassName = "HumanGeneCoExpressionImpl";
        else if ( TaxonUtility.isMouse( gene.getTaxon() ) )
            g2gClassName = "MouseGeneCoExpressionImpl";
        else if ( TaxonUtility.isRat( gene.getTaxon() ) )
            g2gClassName = "RatGeneCoExpressionImpl";
        else
            // must be other
            g2gClassName = "OtherGeneCoExpressionImpl";

        final String queryStringFirstVector = "select distinct g2g from "
                + g2gClassName
                + " as g2g where g2g.sourceAnalysis.id = :analysisID and g2g.firstGene.id = :geneID and g2g.numDataSets >= :stringency";

        final String queryStringSecondVector = "select distinct g2g from "
                + g2gClassName
                + " as g2g where g2g.sourceAnalysis.id = :analysisID and g2g.secondGene.id = :geneID and g2g.numDataSets >= :stringency";

        Collection<Gene2GeneCoexpression> results = new HashSet<Gene2GeneCoexpression>();

        results.addAll( this.getHibernateTemplate().findByNamedParam( queryStringFirstVector,
                new String[] { "analysisID", "geneID", "stringency" }, new Object[] { gene, analysis, stringency } ) );
        results.addAll( this.getHibernateTemplate().findByNamedParam( queryStringSecondVector,
                new String[] { "analysisID", "geneID", "stringency" }, new Object[] { gene, analysis, stringency } ) );

        return results;
    }

}