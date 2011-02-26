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

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ubic.gemma.analysis.service.ExpressionDataMatrixService;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;

/**
 * Allows access to all the Design Element Data Vectors (DEDV's) for a given Expression Experiment. Returns the DEDV's
 * and the genes that might be responsible for that DEDV
 * 
 * @author klc, gavin
 * @version$Id$
 */

public class ExperimentDEDVEndpoint extends AbstractGemmaEndpoint {

    private static final String DEFAULT_FILENAME = "DEDVforEE-";
    private static final String DEFAULT_EXTENSION = ".xml";

    private static Log log = LogFactory.getLog( ExperimentDEDVEndpoint.class );

    private ExpressionExperimentService expressionExperimentService;
    private ExpressionDataMatrixService expressionDataMatrixService;
    private CompositeSequenceService compositeSequenceService;

    /**
     * The local name of the expected request/response.
     */
    private static final String EXPERIMENT_LOCAL_NAME = "experimentDEDV";

    public void setCompositeSequenceService( CompositeSequenceService compositeSequenceService ) {
        this.compositeSequenceService = compositeSequenceService;
    }

    public void setExpressionDataMatrixService( ExpressionDataMatrixService expressionDataMatrixService ) {
        this.expressionDataMatrixService = expressionDataMatrixService;
    }

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
        String eeid = "";

        Collection<String> eeResults = getSingleNodeValue( requestElement, "ee_id" );

        for ( String id : eeResults ) {
            eeid = id;
        }

        // Check to make sure we haven't already generated this EE report.
        Document doc = readReport( DEFAULT_FILENAME + eeid + DEFAULT_EXTENSION );

        if ( doc != null ) {
            // Successfully got report from disk
            watch.stop();
            Long time = watch.getTime();
            log.info( "XML response for ee" + eeid + " retrieved from disk in " + time + "ms." );

            return doc.getDocumentElement();

        }

        // Build the matrix
        ExpressionExperiment ee = expressionExperimentService.load( Long.parseLong( eeid ) );
        ee = expressionExperimentService.thawLite( ee );

        ExpressionDataDoubleMatrix dmatrix = expressionDataMatrixService.getProcessedExpressionDataMatrix( ee );

        // start building the wrapper
        // build xml manually rather than use buildWrapper inherited from AbstractGemmeEndpoint
        String elementName1 = "dedv";
        String elementName2 = "geneIdist";

        // log.info( "Building " + EXPERIMENT_LOCAL_NAME + " XML response" );

        Element responseWrapper = document.createElementNS( NAMESPACE_URI, EXPERIMENT_LOCAL_NAME );
        Element responseElement = document.createElementNS( NAMESPACE_URI, EXPERIMENT_LOCAL_NAME + RESPONSE );
        responseWrapper.appendChild( responseElement );

        if ( dmatrix == null || ( dmatrix.rows() == 0 ) )
            responseElement.appendChild( document.createTextNode( "No " + elementName1 + " result" ) );
        else {

            for ( int rowNum = 0; rowNum < dmatrix.rows(); rowNum++ ) {
                String elementString1 = encode( dmatrix.getRow( rowNum ) ); // data vector string for output
                String elementString2 = "";

                CompositeSequence de = dmatrix.getDesignElementForRow( rowNum );
                Collection<Gene> geneCol = compositeSequenceService.getGenes( de );
                for ( Gene gene : geneCol ) {
                    if ( elementString2.equals( "" ) )
                        elementString2 = elementString2.concat( gene.getId().toString() );
                    else
                        elementString2 = elementString2.concat( DELIMITER + gene.getId().toString() );
                }

                Element e1 = document.createElement( elementName1 );
                e1.appendChild( document.createTextNode( elementString1 ) );
                responseElement.appendChild( e1 );

                Element e2 = document.createElement( elementName2 );
                e2.appendChild( document.createTextNode( elementString2 ) );
                responseElement.appendChild( e2 );
            }
        }

        watch.stop();
        Long time = watch.getTime();
        log.info( "XML response for ee:" + eeid + " created from scratch in " + time + "ms." );
        writeReport( responseWrapper, document, DEFAULT_FILENAME + eeid );
        return responseWrapper;

    }

}
