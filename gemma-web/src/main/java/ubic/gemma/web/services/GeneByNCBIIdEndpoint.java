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

import java.util.ArrayList;
import java.util.Collection;

/**
 * Given an NCBI ID, will return the matching Gemma gene id. The result is a 2D array mapping the NCBI IDs to the Gene
 * IDs.
 * 
 * @author gavin
 *
 */

public class GeneByNCBIIdEndpoint extends AbstractGemmaEndpoint {

    private static Log log = LogFactory.getLog( GeneByNCBIIdEndpoint.class );

    private GeneService geneService;

    /**
     * The local name of the expected Request/Response.
     */
    public static final String GENE_LOCAL_NAME = "geneByNCBIId";

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
     * @param document a DOM document to be used for constructing <code>Node</code>s
     * @return the response element
     */
    @Override
    protected Element invokeInternal( Element requestElement, Document document ) {
        StopWatch watch = new StopWatch();
        watch.start();

        setLocalName( GENE_LOCAL_NAME );

        Collection<String> ncbiInput = getArrayValues( requestElement, "ncbi_ids" );
        Collection<Long> ncbiLongInput = new ArrayList<Long>( ncbiInput.size() );
        for ( String gene : ncbiInput )
            ncbiLongInput.add( Long.parseLong( gene ) );

        log.info( "XML input read: " + ncbiInput.size() + " ncbi ids read" );

        Element responseWrapper = document.createElementNS( NAMESPACE_URI, GENE_LOCAL_NAME );
        Element responseElement = document.createElementNS( NAMESPACE_URI, GENE_LOCAL_NAME + RESPONSE );
        responseWrapper.appendChild( responseElement );

        for ( String ncbi : ncbiInput ) {

            String geneId;
            Gene gene = null;
            try {
                gene = geneService.findByNCBIId( Integer.parseInt( ncbi ) );
            } catch ( NumberFormatException e ) {
                //
            }
            if ( gene == null )
                geneId = "NaN";
            else
                geneId = gene.getId().toString();

            Element e1 = document.createElement( "gene_id" );
            e1.appendChild( document.createTextNode( geneId ) );
            responseElement.appendChild( e1 );

            Element e2 = document.createElement( "ncbi_id" );
            e2.appendChild( document.createTextNode( ncbi ) );
            responseElement.appendChild( e2 );

        }
        watch.stop();
        Long time = watch.getTime();

        log.info( "XML response for NCBI id result built in " + time + "ms." );
        return responseWrapper;

    }

}
