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

import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Given the short name of an Expression Experiment, will return the matching Expression Experiment ID
 * 
 * @author klc, gavin
 * @version$Id$
 */

public class ExperimentIdEndpoint extends AbstractGemmaEndpoint {

    private static Log log = LogFactory.getLog( ExperimentIdEndpoint.class );

    private ExpressionExperimentService expressionExperimentService;

    /**
     * The local name of the request/response.
     */
    public static final String EXPERIMENT_LOCAL_NAME = "experimentId";

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
    protected Element invokeInternal( Element requestElement, Document document ) {
        StopWatch watch = new StopWatch();
        watch.start();

        setLocalName( EXPERIMENT_LOCAL_NAME );
        String eeName = "";

        Collection<String> eeResults = getSingleNodeValue( requestElement, "ee_short_name" );

        for ( String id : eeResults ) {
            eeName = id;
        }
        log.debug( "XML input read: expression experiment shortname, " + eeName );

        ExpressionExperiment ee = expressionExperimentService.findByShortName( eeName );

        if ( ee == null ) {
            String msg = "No Expression Experiment with short name, " + eeName + " can be found.";
            return buildBadResponse( document, msg );
        }

        // get Array Design ID and build results in the form of a collection
        Collection<String> eeId = new HashSet<String>();
        eeId.add( ee.getId().toString() );

        Element wrapper = buildWrapper( document, eeId, "ee_id" );

        watch.stop();
        Long time = watch.getTime();
        log.debug( "XML response for Expression Experiment Id result built in " + time + "ms." );

        return wrapper;

    }

}
