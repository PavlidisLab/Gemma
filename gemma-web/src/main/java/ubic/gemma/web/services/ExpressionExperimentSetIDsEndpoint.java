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

import ubic.gemma.expression.experiment.service.ExpressionExperimentSetService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSetValueObject;

/**
 * A service to return the full list of Expression Experiment Set IDs along with the corresponding name and experiment
 * ids it contains.
 * 
 * @author gavin
 * @version$Id$
 */

public class ExpressionExperimentSetIDsEndpoint extends AbstractGemmaEndpoint {

    private static Log log = LogFactory.getLog( ExpressionExperimentSetIDsEndpoint.class );

    private ExpressionExperimentSetService expressionExperimentSetService;

    /**
     * The local name of the expected Request/Response.
     */
    public static final String LOCAL_NAME = "expressionExperimentSetIDs";

    /**
     * Sets the "business service" to delegate to.
     */
    public void setExpressionExperimentSetService( ExpressionExperimentSetService expressionExperimentSetService ) {
        this.expressionExperimentSetService = expressionExperimentSetService;
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
        authenticate();
        StopWatch watch = new StopWatch();
        watch.start();

        setLocalName( LOCAL_NAME );

        Collection<ExpressionExperimentSetValueObject> eesCol = expressionExperimentSetService
                .loadAllExperimentSetValueObjects();

        // retain expression experiment sets that have a name assigned (probably not necessary?)
        Collection<ExpressionExperimentSetValueObject> eesColToUse = new HashSet<ExpressionExperimentSetValueObject>();
        for ( ExpressionExperimentSetValueObject ees : eesCol ) {
            if ( ees.getName() != null ) eesColToUse.add( ees );
        }

        // start building the wrapper
        // build xml manually for mapped result rather than use buildWrapper inherited from AbstractGemmeEndpoint

        Element responseWrapper = document.createElementNS( NAMESPACE_URI, LOCAL_NAME );
        Element responseElement = document.createElementNS( NAMESPACE_URI, LOCAL_NAME + RESPONSE );
        responseWrapper.appendChild( responseElement );

        for ( ExpressionExperimentSetValueObject ees : eesColToUse ) {

            Element e1 = document.createElement( "expression_experiment_set_id" );
            e1.appendChild( document.createTextNode( ees.getId().toString() ) );
            responseElement.appendChild( e1 );

            Element e2 = document.createElement( "ees_name" );
            e2.appendChild( document.createTextNode( ees.getName() ) );
            responseElement.appendChild( e2 );

            Collection<Long> eeIds = ees.getExpressionExperimentIds();
            Element e3 = document.createElement( "datasets" );
            e3.appendChild( document.createTextNode( encode( eeIds.toArray() ) ) );
            responseElement.appendChild( e3 );

            Element e4 = document.createElement( "taxon" );
            assert ees.getTaxonId() != null;
            e4.appendChild( document.createTextNode( ees.getTaxonId().toString() ) );
            responseElement.appendChild( e4 );

        }

        watch.stop();
        Long time = watch.getTime();
        // log.info( "Finished generating result. Sending response to client." );
        if ( time > 1000 ) {
            log.info( "XML response for Experiment Set IDs results built in " + time + "ms." );
        }
        return responseWrapper;

    }

}