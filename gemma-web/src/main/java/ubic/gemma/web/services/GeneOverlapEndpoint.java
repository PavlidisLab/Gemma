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

import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.core.ontology.providers.GeneOntologyService;
import ubic.gemma.core.ontology.providers.GeneOntologyServiceImpl;
import ubic.gemma.model.genome.Gene;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import static ubic.gemma.core.ontology.providers.GeneOntologyUtils.asRegularGoId;

/**
 * given a query gene id &amp; collection of target gene ids will determine the overlapping Go terms (intersection) between
 * each pair of Query Gene and Target Gene. The actual overlapping go terms will be returned as a single string
 * delimited by white space.
 *
 * @author gavin, klc
 */

public class GeneOverlapEndpoint extends AbstractGemmaEndpoint {

    private static final Log log = LogFactory.getLog( GeneOverlapEndpoint.class );
    /**
     * The local name of the expected request/response.
     */
    private static final String LOCAL_NAME = "geneOverlap";
    private GeneOntologyService geneOntologyService;
    private GeneService geneService;

    /**
     * Sets the "business service" to delegate to.
     */
    public void setGeneOntologyService( GeneOntologyService goS ) {
        this.geneOntologyService = goS;
    }

    public void setGeneService( GeneService geneS ) {
        this.geneService = geneS;
    }

    /**
     * Reads the given <code>requestElement</code>, and sends a the response back.
     *
     * @param requestElement the contents of the SOAP message as DOM elements
     * @param document       a DOM document to be used for constructing <code>Node</code>s
     * @return the response element
     */
    @Override
    protected Element invokeInternal( Element requestElement, Document document ) {
        StopWatch watch = new StopWatch();
        watch.start();

        setLocalName( LOCAL_NAME );

        String queryInput = "";
        Collection<String> query = getSingleNodeValue( requestElement, "query_gene_id" );
        for ( String gene_id : query ) {
            queryInput = gene_id;
        }

        Collection<String> geneResult = getArrayValues( requestElement, "gene_ids" );
        Collection<Long> geneIdLongs = new ArrayList<Long>();
        for ( String gene_id : geneResult ) {
            geneIdLongs.add( Long.parseLong( gene_id ) );
        }

        log.info( "XML input read: query gene id, " + queryInput + ", against " + geneResult.size() + " other genes" );

        // start building the wrapper
        // build xml manually for mapped result rather than use buildWrapper inherited from AbstractGemmeEndpoint
        // start building the wrapper
        // build xml manually for mapped result rather than use buildWrapper inherited from AbstractGemmeEndpoint

        String elementName1 = "gene";
        String elementName2 = "overlap_GO_terms";

        Element responseWrapper = document.createElementNS( NAMESPACE_URI, LOCAL_NAME );
        Element responseElement = document.createElementNS( NAMESPACE_URI, LOCAL_NAME + RESPONSE );
        responseWrapper.appendChild( responseElement );

        // get query gene object from query gene id
        Long queryId = Long.parseLong( queryInput );
        Gene queryGene = geneService.load( queryId );
        if ( queryGene == null ) {
            String msg = "No gene with ids, " + queryId + " can be found.";
            return buildBadResponse( document, msg );
        }

        Map<Long, Collection<OntologyTerm>> gene2Ot = geneOntologyService
                .calculateGoTermOverlap( queryGene, geneIdLongs );

        Collection<Long> geneCol = gene2Ot.keySet();

        // for each gene
        for ( Long geneId : geneCol ) {

            // get the labels and store them
            Collection<String> goTerms = new HashSet<String>();
            for ( OntologyTerm ot : gene2Ot.get( geneId ) ) {
                goTerms.add( asRegularGoId( ot ) );
            }

            String elementString1 = geneId.toString();
            String elementString2 = encode( goTerms.toArray() );

            Element e1 = document.createElement( elementName1 );
            e1.appendChild( document.createTextNode( elementString1 ) );
            responseElement.appendChild( e1 );

            Element e2 = document.createElement( elementName2 );
            e2.appendChild( document.createTextNode( elementString2 ) );
            responseElement.appendChild( e2 );
        }
        watch.stop();
        Long time = watch.getTime();

        log.info( "XML response for gene overlap results built in " + time + "ms." );
        return responseWrapper;

    }

}
