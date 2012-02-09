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

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ubic.gemma.genome.gene.service.GeneService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocation;

/**
 * Given a gene id, will return physical location (chromosome #, nucleotide range: start and end) Note: There may be >1
 * gene products for a particular gene id and hence >1 physical locations associated with gene id. Therefore, the
 * nucleotide range will be the global max and min nucleotides for all the gene products. The max and min do not
 * necessarily correspond to start and end because each gene product can be transcribed on either the +ve strand or the
 * -ve strand (ie. opposite directions of transcription).
 * 
 * @author gavin, klc package ubic.gemma.web.services;
 */

public class PhysicalLocationEndpoint extends AbstractGemmaEndpoint {

    private static Log log = LogFactory.getLog( PhysicalLocationEndpoint.class );

    private GeneService geneService;

    /**
     * The local name of the expected Request/Response.
     */
    public static final String PLOC_LOCAL_NAME = "physicalLocation";

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

        setLocalName( PLOC_LOCAL_NAME );
        Collection<String> geneResults = getSingleNodeValue( requestElement, "gene_id" );
        String geneId = "";

        for ( String id : geneResults ) {
            geneId = id;
        }

        log.debug( "XML input read: gene id, " + geneId );

        Gene gene = geneService.load( Long.parseLong( geneId ) );

        gene = geneService.thaw( gene );
        PhysicalLocation physicalLocation = geneService.getMaxPhysicalLength( gene );
        log.info( "Webservice - Phyisical location for gene: " + gene.getOfficialSymbol() + "physicallocation is: "
                + physicalLocation );

        Element wrapper = buildLocationWrapper( document, physicalLocation.getChromosome().getName(), physicalLocation
                .getNucleotide(), physicalLocation.getNucleotideLength() );

        watch.stop();
        Long time = watch.getTime();
        log.debug( "XML response for physical location result built in " + time + "ms." );
        return wrapper;
    }

    private Element buildLocationWrapper( Document document, String chrom, Long min, Integer length ) {

        Element responseWrapper = document.createElementNS( NAMESPACE_URI, PLOC_LOCAL_NAME );
        Element responseElement = document.createElementNS( NAMESPACE_URI, PLOC_LOCAL_NAME + RESPONSE );
        responseWrapper.appendChild( responseElement );

        Element e1 = document.createElement( "chromId" );
        e1.appendChild( document.createTextNode( chrom ) );
        responseElement.appendChild( e1 );

        Element e2 = document.createElement( "minNT" );
        e2.appendChild( document.createTextNode( min.toString() ) );
        responseElement.appendChild( e2 );

        Element e3 = document.createElement( "maxNT" );
        Long maxNt = min + length;
        e3.appendChild( document.createTextNode( maxNt.toString() ) );
        responseElement.appendChild( e3 );

        return responseWrapper;
    }

}
