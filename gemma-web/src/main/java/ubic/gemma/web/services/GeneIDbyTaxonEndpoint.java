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
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.util.Collection;
import java.util.HashSet;

/**
 * Given the Taxon (eg. "1" for Homo Sapiens), will return all the Gene IDs that match the taxon.
 * 
 * @author klc, gavin
 *
 */

public class GeneIDbyTaxonEndpoint extends AbstractGemmaEndpoint {

    private static Log log = LogFactory.getLog( GeneIDbyTaxonEndpoint.class );

    private GeneService geneService;
    private TaxonService taxonService;

    /**
     * The local name of the expected request/response.
     */
    private static final String EXPERIMENT_LOCAL_NAME = "geneIDbyTaxon";

    /**
     * Sets the "business service" to delegate to.
     */
    public void setGeneService( GeneService gs ) {
        this.geneService = gs;
    }

    public void setTaxonService( TaxonService taxonService ) {
        this.taxonService = taxonService;
    }

    /**
     * Reads the given <code>requestElement</code>, and sends the response back.
     * 
     * @param requestElement the contents of the SOAP message as DOM elements
     * @param document a DOM document to be used for constructing <code>Node</code>s
     * @return the response element
     */
    @Override
    protected Element invokeInternal( Element requestElement, Document document ) {
        StopWatch watch = new StopWatch();
        watch.start();

        setLocalName( EXPERIMENT_LOCAL_NAME );
        Collection<String> taxonResults = getSingleNodeValue( requestElement, "taxon_id" );
        String taxonId = "";

        for ( String id : taxonResults ) {
            taxonId = id;
        }

        log.info( "XML input read: taxon id, " + taxonId );

        // Get Gene matched with Taxon
        Taxon tax = taxonService.load( Long.parseLong( taxonId ) );

        if ( tax == null ) {
            String msg = "No taxon with id, " + taxonId + " can be found.";
            return buildBadResponse( document, msg );
        }

        Collection<Gene> geneCollection = geneService.loadAll( tax );

        // build results in the form of a collection
        Collection<String> geneIds = new HashSet<String>();
        for ( Gene gene : geneCollection ) {
            geneIds.add( gene.getId().toString() );
        }
        Element wrapper = buildWrapper( document, geneIds, "gene_ids" );
        watch.stop();
        Long time = watch.getTime();
        log.info( "XML response for gene id results built in " + time + "ms." );
        return wrapper;

    }

}
