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
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Used for getting the Short Name given an Expression Experiment ID eg: 793 --> GSE10470
 * 
 * @author klc, gavin
 * @version$Id$
 */

public class ExperimentNameEndpoint extends AbstractGemmaEndpoint {

    private static Log log = LogFactory.getLog( ExperimentNameEndpoint.class );

    private ExpressionExperimentService expressionExperimentService;

    /**
     * The local name of the expected request/response.
     */
    public static final String EXPERIMENT_LOCAL_NAME = "experimentName";

    /**
     * Sets the "business service" to delegate to.
     */
    public void setExpressionExperimentService( ExpressionExperimentService ees ) {
        this.expressionExperimentService = ees;
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

        setLocalName( EXPERIMENT_LOCAL_NAME );
        Collection<String> eeInput = getArrayValues( requestElement, "ee_ids" );
        Collection<Long> eeLongs = new HashSet<Long>();
        for ( String ee : eeInput )
            eeLongs.add( Long.parseLong( ee ) );
        log.debug( "XML input read: expression experiment id, " + eeInput );

        Collection<ExpressionExperiment> eeCol = expressionExperimentService.loadMultiple( eeLongs );

        if ( eeCol == null || eeCol.isEmpty() ) {
            String msg = "No input Expression Experiments can be found.";
            return buildBadResponse( document, msg );
        }

        Element responseWrapper = document.createElementNS( NAMESPACE_URI, EXPERIMENT_LOCAL_NAME );
        Element responseElement = document.createElementNS( NAMESPACE_URI, EXPERIMENT_LOCAL_NAME + RESPONSE );
        responseWrapper.appendChild( responseElement );

        for ( ExpressionExperiment ee : eeCol ) {
            Element e1 = document.createElement( "ee_id" );
            e1.appendChild( document.createTextNode( ee.getId().toString() ) );
            responseElement.appendChild( e1 );

            Element e2 = document.createElement( "ee_shortName" );
            e2.appendChild( document.createTextNode( ee.getShortName() ) );
            responseElement.appendChild( e2 );

            Element e3 = document.createElement( "ee_name" );
            e3.appendChild( document.createTextNode( ee.getName() ) );
            responseElement.appendChild( e3 );
        }

        watch.stop();
        Long time = watch.getTime();
        log.debug( "XML response for Expression Experiment Names result built in " + time + "ms." );

        return responseWrapper;
    }

}
