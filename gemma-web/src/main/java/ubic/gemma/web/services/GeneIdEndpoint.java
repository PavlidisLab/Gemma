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
 * Given the official symbol and taxon of a gene, will return the matching gene ID.
 *
 * @author klc, gavin
 *
 */
public class GeneIdEndpoint extends AbstractGemmaEndpoint {

    private static Log log = LogFactory.getLog( GeneIdEndpoint.class );

    private GeneService geneService;
    private TaxonService taxonService;

    /**
     * The local name of the expected Request/Response.
     */
    public static final String GENE_LOCAL_NAME = "geneId";

    /**
     * Sets the "business service" to delegate to.
     */
    public void setGeneService( GeneService geneS ) {
        this.geneService = geneS;
    }

    public void setTaxonService( TaxonService taxonService ) {
        this.taxonService = taxonService;
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
        String geneName = "";
        String taxString = "";
        Collection<String> geneResults = getSingleNodeValue( requestElement, "gene_official_symbol" );

        for ( String name : geneResults ) {
            geneName = name;
        }
        Collection<String> taxonResults = getSingleNodeValue( requestElement, "taxon_id" );

        for ( String tax : taxonResults ) {
            taxString = tax;
        }

        log.debug( "XML input read: gene symbol, " + geneName + " & taxon id, " + taxString );
        // Collection<Gene> genes = geneService.findByOfficialSymbolInexact( geneName );
        Taxon taxon = taxonService.loadOrFail( Long.parseLong( taxString ) );
        Gene gene = geneService.findByOfficialSymbol( geneName, taxon );

        if ( gene == null ) {
            String msg = "No gene with official symbol, " + geneName + ", and taxon, " + taxString + ", can be found.";
            return buildBadResponse( document, msg );
        }

        // build results in the form of a collection
        Collection<String> gIDs = new HashSet<String>();
        gIDs.add( gene.getId().toString() );

        Element wrapper = buildWrapper( document, gIDs, "gene_id" );

        watch.stop();
        Long time = watch.getTime();
        if ( time > 1000 ) {
            log.info( "XML response for gene id result (from gene symbol) built in " + time + "ms." );
        }
        return wrapper;

    }

}
