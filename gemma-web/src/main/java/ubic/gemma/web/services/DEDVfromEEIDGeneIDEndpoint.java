package ubic.gemma.web.services;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.analysis.preprocess.ExpressionDataMatrixBuilder;
import ubic.gemma.analysis.service.AnalysisHelperService;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneService;

/**
 * list Experiment IDs, list gene IDs -> output data vectors satisfying both conditions (i.e., string array) 
 * 
 * Potential problem may be the thawing of design element data vector objects.  
 * For the mini-db, it appears to work.
 * 
 * @author gavin, klc
 */

public class DEDVfromEEIDGeneIDEndpoint extends AbstractGemmaEndpoint {

    private static Log log = LogFactory.getLog( ExperimentDEDVEndpoint.class );

    // private ExpressionExperimentService expressionExperimentService;
    // private AnalysisHelperService analysisHelperService;
    private DesignElementDataVectorService designElementDataVectorService;
    private GeneService geneService;
    private ExpressionExperimentService expressionExperimentService;

    /**
     * The local name of the expected request/response.
     */
    private static final String EXPERIMENT_LOCAL_NAME = "dEDVfromEEIDGeneID";
    private static final String DELIMITER = " ";

    /**
     * Sets the "business service" to delegate to.
     */
    // public void setExpressionExperimentService(ExpressionExperimentService
    // ees) {
    // this.expressionExperimentService = ees;
    // }
    //		
    // public void setAnalysisHelperService(AnalysisHelperService
    // analysisHelperService){
    // this.analysisHelperService = analysisHelperService;
    // }
    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

    public void setDesignElementDataVectorService( DesignElementDataVectorService designElementDataVectorService ) {
        this.designElementDataVectorService = designElementDataVectorService;
    }

    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    /**
     * Reads the given <code>requestElement</code>, and sends a the response back.
     * 
     * @param requestElement the contents of the SOAP message as DOM elements
     * @param document a DOM document to be used for constructing <code>Node</code>s
     * @return the response element
     */
    protected Element invokeInternal( Element requestElement, Document document ) throws Exception {
        

        setLocalName( EXPERIMENT_LOCAL_NAME );
        // get ee id's from request
        Collection<String> eeIdResult = getArrayValues( requestElement, "ee_ids" );
        Collection<Long> eeIDLong = new HashSet<Long>();
        for ( String id : eeIdResult )
            eeIDLong.add( Long.parseLong( id ) );

        Collection<ExpressionExperiment> eeObjs = expressionExperimentService.loadMultiple( eeIDLong );
        // get gene id's from request
        Collection<String> geneIdResult = getArrayValues( requestElement, "gene_ids" );
        Collection<Long> geneIDLong = new HashSet<Long>();
        for ( String id : geneIdResult )
            geneIDLong.add( Long.parseLong( id ) );
        Collection<Gene> geneResult = geneService.loadMultiple( geneIDLong );

        Map<DesignElementDataVector, Collection<Gene>> dedvMap = designElementDataVectorService.getVectors( eeObjs, geneResult );

        // start building the wrapper
        //xml is built manually here instead of using the buildWrapper method inherited from AbstractGemmaEndpoint
        String elementName1 = "dedv";
        String elementName2 = "geneIdList";

        Element responseWrapper = document.createElementNS( NAMESPACE_URI, EXPERIMENT_LOCAL_NAME );
        Element responseElement = document.createElementNS( NAMESPACE_URI, EXPERIMENT_LOCAL_NAME + RESPONSE );
        responseWrapper.appendChild( responseElement );

        if ( dedvMap == null || dedvMap.isEmpty() )
            responseElement.appendChild( document.createTextNode( "No " + elementName1 + " result" ) );
        else {

              
            Set<DesignElementDataVector> keys = dedvMap.keySet();
            designElementDataVectorService.thaw( keys );
            ByteArrayConverter converter = new ByteArrayConverter();
            // -build single-row Collections to use for ExpressionDataMatrixBuilder
            // -need to do this so that we can use the .getPrefferedData()
            // also necessary to do each data vector at a time because we
            // already have a mapping to the genes
            // of the design elements
            for ( DesignElementDataVector dedv : keys ) {
                
                byte[] dedvData = dedv.getData();
                double[] convertedDEDV = converter.byteArrayToDoubles( dedvData );

              
                //data vector string for output
                String elementString1 = encode( convertedDEDV );

                Collection<String> geneidCol = gene2ID( dedvMap.get( dedv ) ); //
                
                // gene ids, space delimited for output
                String elementString2 = encode( geneidCol.toArray() ); 

                Element e1 = document.createElement( elementName1 );
                e1.appendChild( document.createTextNode( elementString1 ) );
                responseElement.appendChild( e1 );

                Element e2 = document.createElement( elementName2 );
                e2.appendChild( document.createTextNode( elementString2 ) );
                responseElement.appendChild( e2 );
             
            }
        }

        log.info( "Finished generating matrix. Sending response to client." );
        return responseWrapper;

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
     * @param data
     * @return a string delimited representation of the double array passed in.
     */
    private String encode( Object[] data ) {

        StringBuffer result = new StringBuffer();

        for ( int i = 0; i < data.length; i++ ) {
            if ( i == 0 )
                result.append( data[i] );
            else
                result.append( DELIMITER + data[i] );
        }

        return result.toString();
    }

}
