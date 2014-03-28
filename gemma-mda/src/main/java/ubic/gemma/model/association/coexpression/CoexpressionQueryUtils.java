/*
 * The gemma-mda project
 * 
 * Copyright (c) 2014 University of British Columbia
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
import java.util.Set;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.util.TaxonUtility;

/**
 * Methods to determine class and table names for coexpression queries.
 * 
 * @author Paul
 * @version $Id$
 */
public class CoexpressionQueryUtils {

    static String getExperimentLinkClassName( Gene gene ) {
        Taxon taxon = gene.getTaxon();
        assert taxon != null;
        return getExperimentLinkClassName( taxon );
    }

    /**
     * @param taxon
     * @return
     */
    static String getExperimentLinkClassName( Taxon taxon ) {
        String g2gClassName;
        if ( TaxonUtility.isHuman( taxon ) )
            g2gClassName = "HumanExperimentCoexpressionLinkImpl";
        else if ( TaxonUtility.isMouse( taxon ) )
            g2gClassName = "MouseExperimentCoexpressionLinkImpl";
        else if ( TaxonUtility.isRat( taxon ) )
            g2gClassName = "RatExperimentCoexpressionLinkImpl";
        else
            g2gClassName = "OtherExperimentCoexpressionLinkImpl";
        return g2gClassName;
    }

    static String getExperimentLinkTableName( Taxon taxon ) {
        String g2gClassName;
        if ( TaxonUtility.isHuman( taxon ) )
            g2gClassName = "HUMAN_EXPERIMENT_COEXPRESSION";
        else if ( TaxonUtility.isMouse( taxon ) )
            g2gClassName = "MOUSE_EXPERIMENT_COEXPRESSION";
        else if ( TaxonUtility.isRat( taxon ) )
            g2gClassName = "RAT_EXPERIMENT_COEXPRESSION";
        else
            // must be other
            g2gClassName = "OTHER_EXPERIMENT_COEXPRESSION";
        return g2gClassName;
    }

    /**
     * @param gene
     * @return the implementation class name for the GeneCoExpression entity for the taxon of the given gene
     */
    static String getGeneLinkClassName( Gene gene ) {
        Taxon taxon = gene.getTaxon();
        assert taxon != null;
        return getGeneLinkClassName( taxon );
    }

    /**
     * @param taxon
     * @return the implementation class name for the GeneCoExpression entity for that taxon.
     */
    static String getGeneLinkClassName( Taxon taxon ) {
        String g2gClassName;
        if ( TaxonUtility.isHuman( taxon ) )
            g2gClassName = "HumanGeneCoExpressionImpl";
        else if ( TaxonUtility.isMouse( taxon ) )
            g2gClassName = "MouseGeneCoExpressionImpl";
        else if ( TaxonUtility.isRat( taxon ) )
            g2gClassName = "RatGeneCoExpressionImpl";
        else
            // must be other
            g2gClassName = "OtherGeneCoExpressionImpl";
        return g2gClassName;
    }

    /**
     * @param taxon
     * @return the name of the SQL table that has the results for that taxon.
     */
    static String getGeneLinkTableName( Taxon taxon ) {
        String g2gClassName;
        if ( TaxonUtility.isHuman( taxon ) )
            g2gClassName = "HUMAN_GENE_COEXPRESSION";
        else if ( TaxonUtility.isMouse( taxon ) )
            g2gClassName = "MOUSE_GENE_COEXPRESSION";
        else if ( TaxonUtility.isRat( taxon ) )
            g2gClassName = "RAT_GENE_COEXPRESSION";
        else
            // must be other
            g2gClassName = "OTHER_GENE_COEXPRESSION";
        return g2gClassName;
    }

    /**
     * @param links
     * @return map of gene IDs to genes it is coexpressed with,
     */
    static Map<Long, Set<Long>> linksToMap( Collection<NonPersistentNonOrderedCoexpLink> links ) {
        Map<Long, Set<Long>> tr = new HashMap<>();

        for ( NonPersistentNonOrderedCoexpLink li : links ) {
            Long g1 = li.getFirstGene().getId();
            Long g2 = li.getSecondGene().getId();
            if ( !tr.containsKey( g1 ) ) tr.put( g1, new HashSet<Long>() );
            if ( !tr.containsKey( g2 ) ) tr.put( g2, new HashSet<Long>() );
            tr.get( g1 ).add( g2 );
            tr.get( g2 ).add( g1 );
        }
        return tr;
    }

}
