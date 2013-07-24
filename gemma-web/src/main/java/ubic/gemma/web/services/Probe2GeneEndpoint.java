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

import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.genome.Gene;

/**
 * Used for determining what genes a probe assays for. Given a probe name and an array Design Id will return a list of
 * gene ids
 * 
 * @author gavin, klc
 * @version$Id$
 */

public class Probe2GeneEndpoint extends AbstractGemmaEndpoint {

    private static Log log = LogFactory.getLog( Probe2GeneEndpoint.class );

    private CompositeSequenceService compositeSequenceService;

    private ArrayDesignService arrayDesignService;

    /**
     * The local name of the expected request/response.
     */
    private static final String PROBE_LOCAL_NAME = "probe2Gene";

    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

    /**
     * Sets the "business service" to delegate to.
     */
    public void setCompositeSequenceService( CompositeSequenceService compositeSequenceService ) {
        this.compositeSequenceService = compositeSequenceService;
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

        setLocalName( PROBE_LOCAL_NAME );

        String probeName = "";
        Collection<String> probeResults = getSingleNodeValue( requestElement, "probe_name" );
        for ( String name : probeResults ) {
            probeName = name;
        }

        String adid = "";
        Collection<String> adResults = getSingleNodeValue( requestElement, "array_design_identifier" );
        for ( String id : adResults ) {
            adid = id;
        }
        ArrayDesign ad = arrayDesignService.load( Long.parseLong( adid ) );

        // get genes, given probe (which is unique to an Array Design
        // therefore, the Array Design Identifier input is not really necessary
        log.info( "XML input read: probe name, " + probeName + " & array design, " + ad );

        CompositeSequence cs = compositeSequenceService.findByName( ad, probeName );
        Collection<Gene> geneCol = compositeSequenceService.getGenes( cs );

        // start building the wrapper
        // build xml manually rather than use buildWrapper inherited from AbstractGemmeEndpoint
        String elementName1 = "geneIdList";

        // build results in the form of a collection
        Collection<String> geneIds = new HashSet<String>();
        for ( Gene gene : geneCol ) {
            geneIds.add( gene.getId().toString() );
        }

        Element wrapper = buildWrapper( document, geneIds, elementName1 );
        watch.stop();
        Long time = watch.getTime();
        log.info( "XML response for gene id result (from gene symbol) built in " + time + "ms." );
        return wrapper;
    }

}