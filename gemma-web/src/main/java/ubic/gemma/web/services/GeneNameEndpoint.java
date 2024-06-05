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
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.model.genome.Gene;

import java.util.Collection;
import java.util.HashSet;

/**
 * Given a gene ID, will return the matching gene official symbol eg) 938103--&gt; Grin1
 *
 * @author klc, gavin
 */
public class GeneNameEndpoint extends AbstractGemmaEndpoint {

    /**
     * The local name of the expected Request/Response.
     */
    public static final String LOCAL_NAME = "geneName";
    private static final Log log = LogFactory.getLog( GeneNameEndpoint.class );
    private GeneService geneService;

    /**
     * Sets the "business service" to delegate to.
     */
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
        String geneId = "";

        Collection<String> geneInput = getArrayValues( requestElement, "gene_ids" );
        Collection<Long> geneIDs = new HashSet<Long>();
        for ( String id : geneInput )
            geneIDs.add( Long.parseLong( id ) );

        log.debug( "XML input read: " + geneInput.size() + " gene ids" );

        Collection<Gene> geneCol = geneService.load( geneIDs );

        if ( geneCol == null || geneCol.isEmpty() ) {
            String msg = "No gene with id '" + geneId + "' can be found.";
            return buildBadResponse( document, msg );
        }

        Element responseWrapper = document.createElementNS( NAMESPACE_URI, LOCAL_NAME );
        Element responseElement = document.createElementNS( NAMESPACE_URI, LOCAL_NAME + RESPONSE );
        responseWrapper.appendChild( responseElement );

        for ( Gene gene : geneCol ) {

            Element e1 = document.createElement( "gene_id" );
            e1.appendChild( document.createTextNode( gene.getId().toString() ) );
            responseElement.appendChild( e1 );

            Element e2 = document.createElement( "gene_official_symbol" );
            e2.appendChild( document.createTextNode( gene.getOfficialSymbol() ) );
            responseElement.appendChild( e2 );
        }

        watch.stop();
        Long time = watch.getTime();
        if ( time > 1000 ) {
            log.info( "XML response for Gene name results built in " + time + "ms." );
        }

        return responseWrapper;
    }

}
