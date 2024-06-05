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
 * Given a collection of Gemma gene IDs, will return the matching NCBI Ids. The result is a 2D array mapping the gene
 * IDs to the NCBI IDs.
 * 
 * @author gavin
 */

public class GeneDetailsByGeneIDEndpoint extends AbstractGemmaEndpoint {

    private static final Log log = LogFactory.getLog( GeneDetailsByGeneIDEndpoint.class );

    private GeneService geneService;

    /**
     * The local name of the expected Request/Response.
     */
    private static final String GENE_LOCAL_NAME = "geneDetailsByGeneID";

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

        Collection<String> geneInput = getArrayValues( requestElement, "gene_ids" );
        Collection<Long> geneLongInput = new ArrayList<Long>( geneInput.size() );
        for ( String gene : geneInput )
            geneLongInput.add( Long.parseLong( gene ) );

        log.debug( "XML input read: " + geneInput.size() + " gene ids read" );

        Collection<Gene> geneCol = geneService.loadThawed( geneLongInput );

        if ( geneCol == null || geneCol.isEmpty() ) {
            String msg = "No genes can be found.";
            return buildBadResponse( document, msg );
        }

        Element responseWrapper = document.createElementNS( NAMESPACE_URI, GENE_LOCAL_NAME );
        Element responseElement = document.createElementNS( NAMESPACE_URI, GENE_LOCAL_NAME + RESPONSE );
        responseWrapper.appendChild( responseElement );

        for ( Gene gene : geneCol ) {

            Element e1 = document.createElement( "gene_id" );
            e1.appendChild( document.createTextNode( gene.getId().toString() ) );
            responseElement.appendChild( e1 );

            Integer ncbiId = gene.getNcbiGeneId();
            if ( ncbiId == null ) ncbiId = -1;

            Element e2 = document.createElement( "ncbi_id" );
            e2.appendChild( document.createTextNode( ncbiId.toString() ) );
            responseElement.appendChild( e2 );

            Element e3 = document.createElement( "official_name" );
            e3.appendChild( document.createTextNode( gene.getOfficialName() ) );
            responseElement.appendChild( e3 );

            Element e4 = document.createElement( "official_symbol" );
            e4.appendChild( document.createTextNode( gene.getOfficialSymbol() ) );
            responseElement.appendChild( e4 );

        }
        watch.stop();
        Long time = watch.getTime();

        log.debug( "XML response for gene details id result built in " + time + "ms." );
        return responseWrapper;

    }

}
