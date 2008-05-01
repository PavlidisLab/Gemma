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

import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneService;

/**
 *Given a gene ID, will return the matching gene official symbol eg) 938103--> Grin1 
 * 
 * @author klc, gavin
 * @version$Id$
 */

public class GeneNameEndpoint extends AbstractGemmaEndpoint {

    private static Log log = LogFactory.getLog(GeneNameEndpoint.class);

    private GeneService geneService;

    /**
     * The local name of the expected Request/Response.
     */
    public static final String GENE_LOCAL_NAME = "geneName";

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
    protected Element invokeInternal( Element requestElement, Document document ) throws Exception {
        StopWatch watch = new StopWatch();
        watch.start();
        
        setLocalName( GENE_LOCAL_NAME );
        String geneId = "";

        Collection<String> geneResults = getNodeValues( requestElement, "gene_id" );

        for ( String id : geneResults ) {
            geneId = id;
        }

        log.info( "XML input read: gene id, "+geneId );
        
        Gene geneName = geneService.load( Long.parseLong( geneId ) );

        if ( geneName == null ) {
            String msg = "No gene with id, " + geneId + " can be found.";
            return buildBadResponse( document, msg );
        }

        // get Array Design ID and build results in the form of a collection
        Collection<String> gName = new HashSet<String>();
        gName.add( geneName.getOfficialSymbol());

        Element wrapper = buildWrapper( document, gName, "gene_name" );
        
        watch.stop();
        Long time = watch.getTime();
        log.info( "XML response for gene name result built in " + time + "ms." );   
        return wrapper;

    }

}
