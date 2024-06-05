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
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.util.Collection;

/**
 * for a given Official Gene Symbol and Taxon ID will return all the probes IDs and their array design IDs that assay
 * for that given gene.
 *
 * @author gavin, klc
 */

public class Gene2ProbeEndpoint extends AbstractGemmaEndpoint {

    /**
     * The local name of the expected request/response.
     */
    private static final String PROBE_LOCAL_NAME = "gene2Probe";
    private static Log log = LogFactory.getLog( Gene2ProbeEndpoint.class );
    private TaxonService taxonService;
    private GeneService geneService;
    private CompositeSequenceService compositeSequenceService;

    public void setCompositeSequenceService( CompositeSequenceService compositeSequenceService ) {
        this.compositeSequenceService = compositeSequenceService;
    }

    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

    /**
     * Sets the "business service" to delegate to.
     */
    public void setTaxonService( TaxonService taxonService ) {
        this.taxonService = taxonService;
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

        this.setLocalName( Gene2ProbeEndpoint.PROBE_LOCAL_NAME );

        // OLDFIXME this should take gene_id
        String geneSymbol = "";
        Collection<String> geneResults = this.getSingleNodeValue( requestElement, "gene_official_symbol" );
        for ( String id : geneResults ) {
            geneSymbol = id;
        }

        String taxonid = "";
        Collection<String> taxonResults = this.getSingleNodeValue( requestElement, "taxon_id" );
        for ( String id : taxonResults ) {
            taxonid = id;
        }

        Gene2ProbeEndpoint.log.info( "XML iput read: Gene symbol, " + geneSymbol + " & taxon id, " + taxonid );
        // get the probe and array design info
        // get taxon
        Taxon taxon = taxonService.load( Long.parseLong( taxonid ) );
        if ( taxon == null ) {
            String msg = "No taxon with id, " + taxonid + ", can be found.";
            return this.buildBadResponse( document, msg );
        }

        // get gene, gven taxon and symbol
        Gene gene = geneService.findByOfficialSymbol( geneSymbol, taxon );
        if ( gene == null ) {
            String msg = "No gene with symbol, " + geneSymbol + ", can be found.";
            return this.buildBadResponse( document, msg );
        }

        // get probe
        Collection<CompositeSequence> csCol = compositeSequenceService.findByGene( gene );
        if ( csCol == null || csCol.isEmpty() ) {
            String msg = "No composite sequence can be found.";
            return this.buildBadResponse( document, msg );
        }

        // start building the wrapper
        // build xml manually rather than use buildWrapper inherited from
        // AbstractGemmeEndpoint
        String elementName1 = "probe_id";
        String elementName2 = "array_design_identifier";

        Element responseWrapper = document
                .createElementNS( AbstractGemmaEndpoint.NAMESPACE_URI, Gene2ProbeEndpoint.PROBE_LOCAL_NAME );
        Element responseElement = document.createElementNS( AbstractGemmaEndpoint.NAMESPACE_URI,
                Gene2ProbeEndpoint.PROBE_LOCAL_NAME + AbstractGemmaEndpoint.RESPONSE );
        responseWrapper.appendChild( responseElement );

        for ( CompositeSequence cs : csCol ) {
            // CompositeSequence id
            String elementString1 = cs.getId().toString();

            // corresponding ArrayDesign identifier
            String elementString2 = cs.getArrayDesign().getId().toString();

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
        Gene2ProbeEndpoint.log.info( "XML response for Probe result built in " + time + "ms." );
        return responseWrapper;
    }

}
