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
 * Used for determining the number of samples (biomaterials) associated with a given expression experiment
 * 
 * @author klc, gavin
 * @version$Id$
 */

public class ExperimentNumSamplesEndpoint extends AbstractGemmaEndpoint {

    private static Log log = LogFactory.getLog( ExperimentNumSamplesEndpoint.class );

    private ExpressionExperimentService expressionExperimentService;

    /**
     * The local name of the expected request.
     */
    public static final String EXPERIMENT_LOCAL_NAME = "experimentNumSamples";

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
        String eeId = "";

        Collection<String> eeResult = getSingleNodeValue( requestElement, "ee_id" );
        // expect only one element in the collection since only one input value
        for ( String id : eeResult )
            eeId = id;

        log.debug( "XML input read: expression experiment id, " + eeId );

        ExpressionExperiment ee = expressionExperimentService.load( Long.parseLong( eeId ) );
        if ( ee == null ) {
            String msg = "No Expression Experiment with id, " + eeId + " can be found.";
            return buildBadResponse( document, msg );
        }

        Integer bmCount = expressionExperimentService.getBioMaterialCount( ee );

        // build collection to pass to wrapper
        Collection<String> values = new HashSet<String>();
        values.add( bmCount.toString() );

        Element wrapper = buildWrapper( document, values, "eeNumSample_id" );

        watch.stop();
        Long time = watch.getTime();
        log.debug( "XML response for Expression Experiment Sample Number result built in " + time + "ms." );

        return wrapper;

    }

}
