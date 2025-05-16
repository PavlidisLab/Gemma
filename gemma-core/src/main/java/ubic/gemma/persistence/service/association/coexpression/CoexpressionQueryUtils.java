/*
 * The gemma project
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

package ubic.gemma.persistence.service.association.coexpression;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.genome.taxon.TaxonUtils;

import java.util.*;

/**
 * Methods to determine class and table names for coexpression queries.
 *
 * @author Paul
 */
class CoexpressionQueryUtils {

    static String getExperimentLinkClassName( Taxon taxon ) {
        String g2gClassName;
        if ( TaxonUtils.isHuman( taxon ) )
            g2gClassName = "HumanExperimentCoexpressionLink";
        else if ( TaxonUtils.isMouse( taxon ) )
            g2gClassName = "MouseExperimentCoexpressionLink";
        else if ( TaxonUtils.isRat( taxon ) )
            g2gClassName = "RatExperimentCoexpressionLink";
        else
            g2gClassName = "OtherExperimentCoexpressionLink";
        return g2gClassName;
    }

    static String getExperimentLinkTableName( Taxon taxon ) {
        String g2gClassName;
        if ( TaxonUtils.isHuman( taxon ) )
            g2gClassName = "HUMAN_EXPERIMENT_COEXPRESSION";
        else if ( TaxonUtils.isMouse( taxon ) )
            g2gClassName = "MOUSE_EXPERIMENT_COEXPRESSION";
        else if ( TaxonUtils.isRat( taxon ) )
            g2gClassName = "RAT_EXPERIMENT_COEXPRESSION";
        else
            // must be other
            g2gClassName = "OTHER_EXPERIMENT_COEXPRESSION";
        return g2gClassName;
    }

    /**
     * @param gene gene
     * @return the implementation class name for the GeneCoExpression entity for the taxon of the given gene
     */
    static String getGeneLinkClassName( Gene gene ) {
        Taxon taxon = gene.getTaxon();
        assert taxon != null;
        return CoexpressionQueryUtils.getGeneLinkClassName( taxon );
    }

    /**
     * @param taxon taxon
     * @return the implementation class name for the GeneCoExpression entity for that taxon.
     */
    static String getGeneLinkClassName( Taxon taxon ) {
        String g2gClassName;
        if ( TaxonUtils.isHuman( taxon ) )
            g2gClassName = "HumanGeneCoExpression";
        else if ( TaxonUtils.isMouse( taxon ) )
            g2gClassName = "MouseGeneCoExpression";
        else if ( TaxonUtils.isRat( taxon ) )
            g2gClassName = "RatGeneCoExpression";
        else
            // must be other
            g2gClassName = "OtherGeneCoExpression";
        return g2gClassName;
    }

    /**
     * @param taxon taxon
     * @return the name of the SQL table that has the results for that taxon.
     */
    static String getGeneLinkTableName( Taxon taxon ) {
        String g2gClassName;
        if ( TaxonUtils.isHuman( taxon ) )
            g2gClassName = "HUMAN_GENE_COEXPRESSION";
        else if ( TaxonUtils.isMouse( taxon ) )
            g2gClassName = "MOUSE_GENE_COEXPRESSION";
        else if ( TaxonUtils.isRat( taxon ) )
            g2gClassName = "RAT_GENE_COEXPRESSION";
        else
            // must be other
            g2gClassName = "OTHER_GENE_COEXPRESSION";
        return g2gClassName;
    }

    /**
     * @param taxon taxon
     * @return the name of the SQL Tabel for the support details for that taxon.
     */
    static String getSupportDetailsTableName( Taxon taxon ) {
        String g2gClassName;
        if ( TaxonUtils.isHuman( taxon ) )
            g2gClassName = "HUMAN_LINK_SUPPORT_DETAILS";
        else if ( TaxonUtils.isMouse( taxon ) )
            g2gClassName = "MOUSE_LINK_SUPPORT_DETAILS";
        else if ( TaxonUtils.isRat( taxon ) )
            g2gClassName = "RAT_LINK_SUPPORT_DETAILS";
        else
            // must be other
            g2gClassName = "OTHER_LINK_SUPPORT_DETAILS";
        return g2gClassName;
    }

    /**
     * @param links links
     * @return map of gene IDs to genes it is coexpressed with,
     */
    static Map<Long, Set<Long>> linksToMap( Collection<NonPersistentNonOrderedCoexpLink> links ) {
        Map<Long, Set<Long>> tr = new HashMap<>();

        for ( NonPersistentNonOrderedCoexpLink li : links ) {
            Long g1 = li.getFirstGene();
            Long g2 = li.getSecondGene();
            if ( !tr.containsKey( g1 ) )
                tr.put( g1, new HashSet<Long>() );
            if ( !tr.containsKey( g2 ) )
                tr.put( g2, new HashSet<Long>() );
            tr.get( g1 ).add( g2 );
            tr.get( g2 ).add( g1 );
        }
        return tr;
    }

}
