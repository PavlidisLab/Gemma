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

package ubic.gemma.web.services;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis;
import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisService;
import ubic.gemma.model.association.coexpression.Gene2GeneCoexpression;
import ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneService;

/**
 * Gene (symbol + taxon #, like GRIN1 + 1 (human)) --> gene coexpression (using the'canned' analyses; default would be
 * the 'all' analysis for the taxon)
 * 
 * @author gavin, klc
 * @version$Id$
 */

public class GeneCoexpressionEndpoint extends AbstractGemmaEndpoint {

    private static Log log = LogFactory.getLog( Gene2GoTermEndpoint.class );

    private TaxonService taxonService;

    private GeneService geneService;

    private Gene2GeneCoexpressionService gene2GeneCoexpressionService;

    private GeneCoexpressionAnalysisService geneCoexpressionAnalysisService;

    /**
     * The local name of the expected request/response.
     */
    public static final String LOCAL_NAME = "geneCoexpression";

    /**
     * Sets the "business service" to delegate to.
     */
    public void setTaxonService( TaxonService taxonService ) {
        this.taxonService = taxonService;
    }

    public void setGeneService( GeneService geneS ) {
        this.geneService = geneS;
    }

    public void setgeneCoexpressionAnalysisService( GeneCoexpressionAnalysisService geneCoexpressionAnalysisService ) {
        this.geneCoexpressionAnalysisService = geneCoexpressionAnalysisService;
    }

    public void setGene2GeneCoexpressionService( Gene2GeneCoexpressionService gene2GeneCoexpressionService ) {
        this.gene2GeneCoexpressionService = gene2GeneCoexpressionService;
    }

    /**
     * Reads the given <code>requestElement</code>, and sends a the response back.
     * 
     * @param requestElement the contents of the SOAP message as DOM elements
     * @param document a DOM document to be used for constructing <code>Node</code>s
     * @return the response element
     */
    @SuppressWarnings("unchecked")
    protected Element invokeInternal( Element requestElement, Document document ) throws Exception {
        setLocalName( LOCAL_NAME );

        Collection<String> geneResults = getNodeValues( requestElement, "gene_id" );
        String geneId = "";
        for ( String id : geneResults ) {
            geneId = id;
        }

        Collection<String> taxonResults = getNodeValues( requestElement, "taxon_id" );
        String taxonId = "";
        for ( String id : taxonResults ) {
            taxonId = id;
        }

        Collection<String> stringencyResults = getNodeValues( requestElement, "stringency" );
        String string = "";
        for ( String id : stringencyResults ) {
            string = id;
        }
        // 

        Taxon taxon = taxonService.load( Long.parseLong( taxonId ) );
        if ( taxon == null ) {
            String msg = "No taxon with id, " + taxon + ", can be found.";
            return buildBadResponse( document, msg );
        }

        Gene gene = geneService.load( Long.parseLong( geneId ) );
        if ( gene == null ) {
            String msg = "No gene with id, " + geneId + ", can be found.";
            return buildBadResponse( document, msg );
        }
        int stringency = Integer.parseInt( string );

        Collection<GeneCoexpressionAnalysis> analysisCol = geneCoexpressionAnalysisService.findByTaxon( taxon );
        GeneCoexpressionAnalysis analysis = null;
        // expect one Analysis object
        for ( GeneCoexpressionAnalysis gca : analysisCol )
            analysis = gca;

        // get Gene2GeneCoexpressio objects canned analysis
        Collection<Gene2GeneCoexpression> coexpressedGenes = gene2GeneCoexpressionService
                .findCoexpressionRelationships( gene, analysis, stringency );

        if ( coexpressedGenes == null || coexpressedGenes.isEmpty() ) {
            String msg = "No coexpressed genes can be found.";
            return buildBadResponse( document, msg );
        }
        // build results in the form of a collection
        Collection<String> coexpressedGenesResults = new HashSet<String>();
        Long coexpressedGeneId = null;
        for ( Gene2GeneCoexpression coexpressedGeneRelation : coexpressedGenes ) {
            if ( coexpressedGeneRelation.getFirstGene().getId().compareTo( gene.getId() ) == 0 )
                coexpressedGeneId = coexpressedGeneRelation.getSecondGene().getId();
            else
                coexpressedGeneId = coexpressedGeneRelation.getFirstGene().getId();
            if ( coexpressedGeneId != Long.parseLong( geneId ) )
                coexpressedGenesResults.add( coexpressedGeneId.toString() );
        }

        return buildWrapper( document, coexpressedGenesResults, "coexpressed_genes" );

    }

}
