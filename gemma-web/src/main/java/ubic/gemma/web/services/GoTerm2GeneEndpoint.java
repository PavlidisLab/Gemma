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

import ubic.gemma.genome.taxon.service.TaxonService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.ontology.providers.GeneOntologyService;

/**
 * Given a Gene Ontology Term URI and a Taxon ID as input, will return a collection of gene IDs that match the GO Term
 * and Taxon.
 * 
 * @author gavin, klc
 * @version$Id$
 */

public class GoTerm2GeneEndpoint extends AbstractGemmaEndpoint {

    private static Log log = LogFactory.getLog( GoTerm2GeneEndpoint.class );

    private GeneOntologyService geneOntologyService;

    private TaxonService taxonService;

    /**
     * The local name of the expected request/response.
     */
    public static final String GO2Gene_LOCAL_NAME = "goTerm2Gene";

    /**
     * Sets the "business service" to delegate to.
     */
    public void setGeneOntologyService( GeneOntologyService goS ) {
        this.geneOntologyService = goS;
    }

    public void settaxonService( TaxonService taxS ) {
        this.taxonService = taxS;
    }

    /**
     * Reads the given <code>requestElement</code>, and sends a the response back.
     * 
     * @param requestElement the contents of the SOAP message as DOM elements
     * @param document a DOM document to be used for constructing <code>Node</code>s
     * @return the response element
     */
    @Override
    protected Element invokeInternal( Element requestElement, Document document ) throws Exception {
        StopWatch watch = new StopWatch();
        watch.start();

        setLocalName( GO2Gene_LOCAL_NAME );
        String goId = "";
        String taxonId = "";

        // get GO id from request
        Collection<String> goIdResult = getSingleNodeValue( requestElement, "go_id" );
        for ( String id : goIdResult ) {
            goId = id;
        }

        // get taxon id from request
        Collection<String> taxonIdResult = getSingleNodeValue( requestElement, "taxon_id" );
        for ( String id : taxonIdResult ) {
            taxonId = id;
        }

        log.debug( "XML input read: GO id, " + goId + " & taxon id, " + taxonId );

        // get gene from GO term
        Taxon taxon = taxonService.load( Long.parseLong( taxonId ) );
        if ( taxon == null ) {
            String msg = "No taxon with id, " + taxonId + " can be found.";
            return buildBadResponse( document, msg );
        }

        Collection<Gene> genes = geneOntologyService.getGenes( goId, taxon );
        if ( genes == null || genes.isEmpty() ) {
            return buildBadResponse( document, "No genes associated with goId = " + goId + " and taxon = "
                    + taxon.getCommonName() );
        }
        // build results in the form of a collection
        Collection<String> geneIds = new HashSet<String>();
        for ( Gene gene : genes ) {
            geneIds.add( gene.getId().toString() );
        }

        Element wrapper = buildWrapper( document, geneIds, "gene_id" );

        watch.stop();
        Long time = watch.getTime();
        log.debug( "XML response for gene id results built in " + time + "ms." );
        return wrapper;

    }

}
