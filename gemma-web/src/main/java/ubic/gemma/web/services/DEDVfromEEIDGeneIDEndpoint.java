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

import ubic.gemma.genome.gene.service.GeneService;
import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;

/**
 * Given a list Experiment IDs and a list gene IDs will return design element data vectors (DEDV), all the genes that
 * could have been responsible for that DEDV (only needs to contain 1 of the given genes) and the Expression Experiment
 * that the data came from. The DEDV's will be a list of white space seperated doubles that might contain NaN's for
 * missing data. The gene's will also be a list of white space separated gene Ids. This query can be time consuming and
 * cause a tcp/ip server timeout . The service outputs the results to a file which can be found at (file wil be name
 * something like dedv-"gene IDs"-"Number of Experiments".xml http://chibi.ubc.ca/Gemma/ws/xml/
 * 
 * @author gavin, klc
 * @version$Id$
 */
public class DEDVfromEEIDGeneIDEndpoint extends AbstractGemmaEndpoint {

    private static Log log = LogFactory.getLog( DEDVfromEEIDGeneIDEndpoint.class );

    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;
    private GeneService geneService;
    private ExpressionExperimentService expressionExperimentService;

    /**
     * The local name of the expected request/response.
     */
    private static final String EXPERIMENT_LOCAL_NAME = "dEDVfromEEIDGeneID";

    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    /**
     * Sets the "business service" to delegate to.
     */
    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

    public void setProcessedExpressionDataVectorService(
            ProcessedExpressionDataVectorService processedExpressionDataVectorService ) {
        this.processedExpressionDataVectorService = processedExpressionDataVectorService;
    }

    // /**
    // * overloaded method for buildWrapper
    // */
    // protected Element buildWrapper(String localName, Document document,
    // Map<Integer, String> values1, Map<Integer, String> values2, String
    // elementName1, String elementName2){
    // Element responseWrapper = document.createElementNS(NAMESPACE_URI,
    // localName);
    // Element responseElement = document.createElementNS(NAMESPACE_URI,
    // localName + RESPONSE);
    // responseWrapper.appendChild(responseElement);
    //
    // if (values1 == null || values1.isEmpty())
    // responseElement.appendChild(document
    // .createTextNode("No "+elementName1 +" result"));
    // else {
    // // Need to create a list (array) of the geneIds
    // for (int i=0; i<values1.keySet().size(); i++){
    // Element e1 = document.createElement(elementName1);
    // e1.appendChild(document.createTextNode(values1.get(i)));
    // responseElement.appendChild(e1);
    // Element e2 = document.createElement(elementName2);
    // e2.appendChild(document.createTextNode(values2.get(i)));
    // responseElement.appendChild(e2);
    // }
    // }
    // return responseWrapper;
    // }

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
        // get ee id's from request
        Collection<String> eeIdResult = getArrayValues( requestElement, "ee_ids" );
        Collection<Long> eeIDLong = new HashSet<Long>();
        for ( String id : eeIdResult )
            eeIDLong.add( Long.parseLong( id ) );

        // Need to get and thaw the experiments.
        Collection<ExpressionExperiment> eeObjs = expressionExperimentService.loadMultiple( eeIDLong );
        for ( ExpressionExperiment ee : eeObjs ) {
            ee = expressionExperimentService.thawLite( ee );
        }

        // get gene id's from request
        Collection<String> geneIdResult = getArrayValues( requestElement, "gene_ids" );
        Collection<Long> geneIDLong = new HashSet<Long>();
        for ( String id : geneIdResult )
            geneIDLong.add( Long.parseLong( id ) );
        Collection<Gene> geneResult = geneService.loadMultiple( geneIDLong );

        log.debug( "XML input read: " + eeIdResult.size() + " experiment ids & " + geneIdResult.size() + " gene ids" );

        Collection<DoubleVectorValueObject> vectors = processedExpressionDataVectorService.getProcessedDataArrays(
                eeObjs, geneResult );

        // start building the wrapper
        // xml is built manually here instead of using the buildWrapper method inherited from AbstractGemmaEndpoint
        // log.info( "Building " + EXPERIMENT_LOCAL_NAME + " XML response" );

        String elementName1 = "dedv";
        String elementName2 = "geneIdList";
        String elementName3 = "eeIdList";

        Element responseWrapper = document.createElementNS( NAMESPACE_URI, EXPERIMENT_LOCAL_NAME );
        Element responseElement = document.createElementNS( NAMESPACE_URI, EXPERIMENT_LOCAL_NAME + RESPONSE );
        responseWrapper.appendChild( responseElement );

        if ( vectors == null || vectors.isEmpty() )
            return buildBadResponse( document, "No " + elementName1 + " result" );
        // responseElement.appendChild( document.createTextNode( "No " + elementName1 + " result" ) );

        // -build single-row Collections to use for ExpressionDataMatrixBuilder
        // -need to do this so that we can use the .getPrefferedData()
        // also necessary to do each data vector at a time because we
        // already have a mapping to the genes
        // of the design elements
        for ( DoubleVectorValueObject dedv : vectors ) {

            double[] convertedDEDV = dedv.getData();

            // data vector string for output
            String elementString1 = encode( convertedDEDV );

            Collection<String> geneidCol = gene2ID( dedv.getGenes() ); //

            // gene ids, space delimited for output
            String elementString2 = encode( geneidCol.toArray() );

            String elementString3 = dedv.getExpressionExperiment().getId().toString();

            Element e1 = document.createElement( elementName1 );
            e1.appendChild( document.createTextNode( elementString1 ) );
            responseElement.appendChild( e1 );

            Element e2 = document.createElement( elementName2 );
            e2.appendChild( document.createTextNode( elementString2 ) );
            responseElement.appendChild( e2 );

            Element e3 = document.createElement( elementName3 );
            e3.appendChild( document.createTextNode( elementString3 ) );
            responseElement.appendChild( e3 );

        }

        watch.stop();
        Long time = watch.getTime();
        // log.info( "Finished generating result. Sending response to client." );
        log.debug( "XML response for design element data vector results built in " + time + "ms." );
        // log.info( "Finished generating matrix. Sending response to client." );

        // naming convention for the xml file report
        String filename = "dedv-";
        if ( geneResult.size() > 10 )
            filename = filename.concat( geneResult.size() + "-" + eeIdResult.size() );
        else {
            for ( String id : geneIdResult )
                filename = filename.concat( id + "-" );
            filename = filename.concat( "" + eeIdResult.size() );
        }

        writeReport( responseWrapper, document, filename );
        return responseWrapper;

    }

    /**
     * @param data
     * @return a string delimited representation of the double array passed in.
     */
    private String encode( double[] data ) {

        StringBuffer result = new StringBuffer();

        for ( int i = 0; i < data.length; i++ ) {
            if ( i == 0 )
                result.append( data[i] );
            else
                result.append( DELIMITER + data[i] );
        }

        return result.toString();
    }

    /**
     * helper method to convert Gene to ID's (string format). Order will not be maintained, since the original
     * collection was a HashSet object anyways
     */
    private Collection<String> gene2ID( Collection<Gene> geneCol ) {
        Collection<String> geneIds = new HashSet<String>();
        for ( Gene gene : geneCol ) {
            geneIds.add( gene.getId().toString() );
        }
        return geneIds;
    }

}
