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

import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;

/**
 * Used for getting the Short Name given an Expression Experiment ID  eg: 793 --> GSE10470
 * 
 * @author klc, gavin
 * @version$Id$
 */

public class ExperimentNameEndpoint extends AbstractGemmaEndpoint {

    private static Log log = LogFactory.getLog(ExperimentNameEndpoint.class);

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
    protected Element invokeInternal( Element requestElement, Document document ) throws Exception {
        StopWatch watch = new StopWatch();
        watch.start();
        
        setLocalName( EXPERIMENT_LOCAL_NAME );
        String eeId = "";

        Collection<String> eeResult = getNodeValues( requestElement, "ee_id" );
        // expect only one element in the collection
        for ( String id : eeResult )
            eeId = id;

        log.info( "XML input read: expression experiment id, " + eeId );
        
        ExpressionExperiment ee = expressionExperimentService.load( Long.parseLong( eeId ) );

        if ( ee == null ) {
            String msg = "No Expression Experiment with id, " + ee + " can be found.";
            return buildBadResponse( document, msg );
        }

        // build collection to pass to wrapper
        Collection<String> values = new HashSet<String>();
        values.add( ee.getShortName() );

        Element wrapper = buildWrapper( document, values, "ee_name" );
        
        watch.stop();
        Long time = watch.getTime();
        log.info( "XML response for Expression Experiment Name result built in " + time + "ms." );   
        
        return wrapper;

    }

}
