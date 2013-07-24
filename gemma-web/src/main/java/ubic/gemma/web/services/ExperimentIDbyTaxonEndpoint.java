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

import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.genome.taxon.service.TaxonService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;

/**
 * Given a Taxon (eg. "1" for Homo Sapien), will return all the Expression Experiment IDs that match the Taxon.
 * 
 * @author klc, gavin
 * @version$Id$
 */

public class ExperimentIDbyTaxonEndpoint extends AbstractGemmaEndpoint {

    private static Log log = LogFactory.getLog( ExperimentIDbyTaxonEndpoint.class );

    private ExpressionExperimentService expressionExperimentService;
    private TaxonService taxonService;

    /**
     * The local name of the expected request/response.
     */
    private static final String EXPERIMENT_LOCAL_NAME = "experimentIDbyTaxon";

    /**
     * Sets the "business service" to delegate to.
     */
    public void setExpressionExperimentService( ExpressionExperimentService ees ) {
        this.expressionExperimentService = ees;
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
    protected Element invokeInternal( Element requestElement, Document document ) throws Exception {
        StopWatch watch = new StopWatch();
        watch.start();

        setLocalName( EXPERIMENT_LOCAL_NAME );
        Collection<String> taxonResults = getSingleNodeValue( requestElement, "taxon_id" );
        String taxonId = "";

        for ( String id : taxonResults ) {
            taxonId = id;
        }

        log.debug( "XML input read: taxon id, " + taxonId );

        // Get EE matched with Taxon
        Taxon tax = taxonService.load( Long.parseLong( taxonId ) );
        if ( tax == null ) {
            String msg = "No taxon with id, " + taxonId + " can be found.";
            return buildBadResponse( document, msg );
        }

        Collection<ExpressionExperiment> eeCollection = expressionExperimentService.findByTaxon( tax );

        // build results in the form of a collection
        Collection<String> eeIds = new HashSet<String>();
        for ( ExpressionExperiment ee : eeCollection ) {
            eeIds.add( ee.getId().toString() );
        }

        Element wrapper = buildWrapper( document, eeIds, "ee_ids" );
        watch.stop();
        Long time = watch.getTime();
        log.debug( "XML response for Expression Experiment Id results built in " + time + "ms." );
        return wrapper;

    }

}
