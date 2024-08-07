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

import org.apache.commons.lang3.StringUtils;
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

import static ubic.gemma.core.ontology.providers.GeneOntologyUtils.asRegularGoId;

/**
 * Given a collection of Gene IDs, will return a collection of Gene Ontology URIs for each gene.
 * 
 * @author klc, gavin
 *
 */

public class Gene2GoTermEndpoint extends AbstractGemmaEndpoint {

    private static Log log = LogFactory.getLog( Gene2GoTermEndpoint.class );

    private GeneOntologyService geneOntologyService;

    private GeneService geneService;

    /**
     * The local name of the expected request/response.
     */
    public static final String GENE2GO_LOCAL_NAME = "gene2Go";

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
     * @param document a DOM document to be used for constructing <code>Node</code>s
     * @return the response element
     */
    @Override
    protected Element invokeInternal( Element requestElement, Document document ) {
        StopWatch watch = new StopWatch();
        watch.start();

        setLocalName( GENE2GO_LOCAL_NAME );

        Collection<String> geneResult = getArrayValues( requestElement, "gene_ids" );

        log.info( "XML input read: " + geneResult.size() + " gene ids" );
        // start building the wrapper
        // build xml manually for mapped result rather than use buildWrapper inherited from AbstractGemmeEndpoint
        // log.info( "Building " + GENE2GO_LOCAL_NAME + " XML response" );

        String elementName1 = "gene_id";
        String elementName2 = "goIdList";

        Element responseWrapper = document.createElementNS( NAMESPACE_URI, GENE2GO_LOCAL_NAME );
        Element responseElement = document.createElementNS( NAMESPACE_URI, GENE2GO_LOCAL_NAME + RESPONSE );
        responseWrapper.appendChild( responseElement );

        for ( String geneString : geneResult ) {

            Long geneId = Long.parseLong( geneString );
            Gene gene = geneService.load( geneId );
            if ( gene == null ) {
                String msg = "No gene with ids, " + geneId + " can be found.";
                return buildBadResponse( document, msg );
            }

            Collection<OntologyTerm> terms = geneOntologyService.getGOTerms( gene );

            // get the labels and store them
            Collection<String> goTerms = new HashSet<String>();
            if ( terms != null ) {
                for ( OntologyTerm ot : terms ) {
                    goTerms.add( asRegularGoId( ot ) );
                }
            } else
                goTerms.add( "NaN" );
            String elementString1 = geneId.toString();
            String elementString2 = encode( retainNumericIds( goTerms ).toArray() );

            Element e1 = document.createElement( elementName1 );
            e1.appendChild( document.createTextNode( elementString1 ) );
            responseElement.appendChild( e1 );

            Element e2 = document.createElement( elementName2 );
            e2.appendChild( document.createTextNode( elementString2 ) );
            responseElement.appendChild( e2 );
        }
        watch.stop();
        Long time = watch.getTime();
        // log.info( "Finished generating result. Sending response to client." );
        log.info( "XML response for GO Term results built in " + time + "ms." );

        return responseWrapper;

    }

    /**
     * @param regularIds - input collection will be unchanged
     * @return - collection of GO ids, without the "GO:" infront, only the numeric portion of the id is returned
     */
    private Collection<String> retainNumericIds( Collection<String> regularIds ) {
        Collection<String> numericIds = new ArrayList<String>();
        for ( String id : regularIds ) {
            numericIds.add( StringUtils.substring( id, 3 ) );
        }
        return numericIds;

    }

}
