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

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ubic.gemma.analysis.expression.coexpression.CoexpressionMetaValueObject;
import ubic.gemma.analysis.expression.coexpression.CoexpressionValueObjectExt;
import ubic.gemma.analysis.expression.coexpression.GeneCoexpressionService;
import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneService;

/**
 *Allows access to the gene co-expression analysis.  Given a gene, a taxon and a stringency this service will return all the related co-expression data.  
 *The stringency is the miniumum number of times we found a particular relationship. 
 *  Returns the coexpressed Gene, the support ( the number of times that coexpression was found )
 * and the experiments that co-expression was found in (since there should be more than 1 experiment this list will be returned as a space delimted string of EE Ids.
 * 
 * @author gavin, klc
 * @version$Id$
 */

public class GeneCoexpressionEndpoint extends AbstractGemmaEndpoint {

    private static Log log = LogFactory.getLog( GeneCoexpressionEndpoint.class );

    private TaxonService taxonService;

    private GeneService geneService;

    private GeneCoexpressionService geneCoexpressionService;

    /**
     * The local name of the expected request/response.
     */
    public static final String LOCAL_NAME = "geneCoexpression";

    // The maximum number of coexpression results to return; a value of zero will return all possible results (ie. max is infinity)
    public static final int MAX_RESULTS = 0;

    /**
     * Sets the "business service" to delegate to.
     */
    public void setTaxonService( TaxonService taxonService ) {
        this.taxonService = taxonService;
    }

    public void setGeneService( GeneService geneS ) {
        this.geneService = geneS;
    }

 
    public void setgeneCoexpressionService( GeneCoexpressionService geneCoexpressionService ) {
        this.geneCoexpressionService = geneCoexpressionService;
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
        StopWatch watch = new StopWatch();
        watch.start();
        
        setLocalName( LOCAL_NAME );

        Collection<String> geneInput = getArrayValues( requestElement, "gene_id" );        
        Collection<Long> geneIDLong = new HashSet<Long>();
        for ( String id : geneInput )
            geneIDLong.add( Long.parseLong( id ) );
        
        Collection<String> taxonInput = getNodeValues( requestElement, "taxon_id" );
        String taxonId = "";
        for ( String id : taxonInput ) {
            taxonId = id;
        }

        Collection<String> analysisInput = getNodeValues( requestElement, "expression_experiment_set_id" );
        String analysisId = "";
        for ( String id : analysisInput ) {
            analysisId = id;
        }
        
        Collection<String> stringencyInput = getNodeValues( requestElement, "stringency" );
        String string = "";
        for ( String id : stringencyInput ) {
            string = id;
        }
        
        Collection<String> queryGenesOnlyInput = getNodeValues( requestElement, "queryGenesOnly");
        String query = "";
        for (String id : queryGenesOnlyInput)
            query = id;
        boolean queryGenesOnly = false; 
        if (query.endsWith( "1" ))
            queryGenesOnly = true;
       
        
        log.info( "XML input read: "+geneInput.size()+" gene ids,  & taxon id, "+taxonId+" & stringency, "+string+ " & queryGenesOnly="+ query);
        
        Taxon taxon = taxonService.load( Long.parseLong( taxonId ) );
        if ( taxon == null ) {
            String msg = "No taxon with id, " + taxon + ", can be found.";
            return buildBadResponse( document, msg );
        }
        
        Collection<Gene> geneCol = geneService.loadMultiple( geneIDLong );
        if ( geneCol == null ) {
            String msg = "None of the gene id's can be found.";
            return buildBadResponse( document, msg );
        }
        
        geneService.thawLite( geneCol );

        int stringency = Integer.parseInt( string );

//        Collection<GeneCoexpressionAnalysis> analysisCol = geneCoexpressionAnalysisService.findByTaxon( taxon );
//        GeneCoexpressionAnalysis analysis2Use = null;
//
//        // use the 1st canned analysis that isn't virtual  for the given taxon (should be the all"Taxon" analysis)
//        for ( GeneCoexpressionAnalysis analysis : analysisCol ) {
//            if (analysis instanceof GeneCoexpressionVirtualAnalysis)                
//                continue;
//            else{
//                    analysis2Use = analysis;
//                    break;
//            }
//        }
              
        // get Gene2GeneCoexpressio objects canned analysis
        CoexpressionMetaValueObject coexpressedGenes = geneCoexpressionService.getCannedAnalysisResults( Long.parseLong(analysisId), geneCol, stringency, MAX_RESULTS, queryGenesOnly );

        if ( coexpressedGenes == null || coexpressedGenes.getKnownGeneResults().isEmpty() ) {
            String msg = "No coexpressed genes can be found.";
            return buildBadResponse( document, msg );
        }

        final String QUERY_GENE_NAME = "query_gene";
        final String FOUND_GENE_NAME = "found_gene";
        final String SUPPORT_NAME = "support";
        final String SIGN_NAME = "sign";
        final String EEID_NAME = "eeIdList";

        Element responseWrapper = document.createElementNS( NAMESPACE_URI, LOCAL_NAME );
        Element responseElement = document.createElementNS( NAMESPACE_URI, LOCAL_NAME + RESPONSE );
        responseWrapper.appendChild( responseElement );

        for ( CoexpressionValueObjectExt cvo : coexpressedGenes.getKnownGeneResults() ) {

            Element e1 = document.createElement( QUERY_GENE_NAME );
            e1.appendChild( document.createTextNode( cvo.getQueryGene().getOfficialSymbol() ) );
            responseElement.appendChild( e1 );
            
            Element e2 = document.createElement( FOUND_GENE_NAME );
            e2.appendChild( document.createTextNode( cvo.getFoundGene().getOfficialSymbol() ) );
            responseElement.appendChild( e2 );

            Integer support = 0;
            String sign = "";
            
            if (cvo.getPosLinks() > 0){
                support = cvo.getPosLinks();
                sign = "+";
            }
            else if (cvo.getNegLinks() > 0){
                support = cvo.getNegLinks();
                sign = "-";
            }
           
            //If it happens that a result has both neg and pos links, then the pos link and sign will be used
            //TODO: Handle cases where a result can have both neg and pos links 
            Element e3 = document.createElement( SUPPORT_NAME );
            e3.appendChild( document.createTextNode( support.toString() ) );
            responseElement.appendChild( e3 );

            Element e4 = document.createElement( SIGN_NAME );
            e4.appendChild( document.createTextNode( sign ) );
            responseElement.appendChild( e4 );
            
            Element e5 = document.createElement( EEID_NAME );
            e5.appendChild( document.createTextNode( encode(cvo.getSupportingExperiments().toArray())) );
            responseElement.appendChild( e5 );

        }
        watch.stop();
        Long time = watch.getTime();
        
        log.info( "XML response for coexpression canned result built in " + time + "ms." );
        return responseWrapper;

    }

}
