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
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.core.ontology.providers.GeneOntologyService;
import ubic.gemma.core.ontology.providers.GeneOntologyServiceImpl;
import ubic.gemma.model.genome.Gene;

import java.util.ArrayList;
import java.util.Collection;

import static ubic.gemma.core.ontology.providers.GeneOntologyUtils.asRegularGoId;

/**
 * Given a collection of Gene ID, will return a collection of Gene Ontology IDs (ie. GO:0039392) and their corresponding
 * descriptions.
 * 
 * @author klc, gavin
 *
 */

public class Gene2GOdescriptionEndpoint extends AbstractGemmaEndpoint {

    private static Log log = LogFactory.getLog( Gene2GOdescriptionEndpoint.class );

    private GeneOntologyService geneOntologyService;

    private GeneService geneService;

    /**
     * The local name of the expected request/response.
     */
    public static final String LOCAL_NAME = "gene2GOdescription";

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

        setLocalName( LOCAL_NAME );

        Collection<String> geneInput = getArrayValues( requestElement, "gene_id" );
        // String geneString = geneInput.iterator().next();

        log.info( "XML input read: " + geneInput.size() + " gene ids " );

        Element responseWrapper = document.createElementNS( NAMESPACE_URI, LOCAL_NAME );
        Element responseElement = document.createElementNS( NAMESPACE_URI, LOCAL_NAME + RESPONSE );
        responseWrapper.appendChild( responseElement );

        Collection<Long> geneIds = parseLongCollection( geneInput );
        Collection<Gene> geneCol = geneService.load( geneIds );
        if ( geneCol == null || geneCol.isEmpty() ) {
            String msg = "No gene with the given ids can be found.";
            return buildBadResponse( document, msg );
        }
        for ( Gene gene : geneCol ) {
            Collection<OntologyTerm> terms = geneOntologyService.getGOTerms( gene );

            // see if there the gene is annotated with any go terms else, print NaN for the 2 output fields
            // get the labels for the gene and store them
            if ( terms != null ) {
                for ( OntologyTerm ot : terms ) {

                    // gene id output
                    Element e1 = document.createElement( "gene_id" );
                    e1.appendChild( document.createTextNode( gene.getId().toString() ) );
                    responseElement.appendChild( e1 );

                    String goTerm = asRegularGoId( ot );
                    Element e2 = document.createElement( "go_id" );
                    e2.appendChild( document.createTextNode( goTerm ) );
                    responseElement.appendChild( e2 );

                    Element e3 = document.createElement( "go_description" );
                    e3.appendChild( document.createTextNode( ot.getLabel() ) );
                    responseElement.appendChild( e3 );
                }
            } else {
                // gene id output
                Element e1 = document.createElement( "gene_id" );
                e1.appendChild( document.createTextNode( gene.getId().toString() ) );
                responseElement.appendChild( e1 );

                Element e2 = document.createElement( "go_id" );
                e2.appendChild( document.createTextNode( "NaN" ) );
                responseElement.appendChild( e2 );

                Element e3 = document.createElement( "go_description" );
                e3.appendChild( document.createTextNode( "NaN" ) );
                responseElement.appendChild( e3 );
            }
        }

        watch.stop();
        Long time = watch.getTime();
        log.info( "XML response for GO Term results built in " + time + "ms." );

        return responseWrapper;

    }

    private Collection<Long> parseLongCollection( Collection<String> stringCol ) {
        Collection<Long> longCol = new ArrayList<Long>( stringCol.size() );
        for ( String e : stringCol )
            longCol.add( Long.parseLong( e ) );
        return longCol;
    }

}
