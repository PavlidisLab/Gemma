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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ubic.basecode.dataStructure.matrix.AbstractNamedMatrix;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix2DNamed;
import ubic.gemma.analysis.preprocess.DedvRankService;
import ubic.gemma.analysis.preprocess.DedvRankService.Method;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneService;

/**
 * Given a collection of gene IDs, a collection of experiment IDs, and the method, the service will return 
 * a list of genes mapped to a list of space delimited ranks.  Each rank in the space delimited result is 
 * ordered based on the a list of experiments returned in the field, "ee_ids".  The output can be pictured
 * as a matrix where the rows are the genes and the columns are the experiments.  
 * (Ranks are per-array based.)
 * Method can be one of the following: MIN, MAX, MEAN, MEDIAN, VARIANCE
 * @author gavin
 * @version$Id$
 */
public class DEDVRankEndpoint extends AbstractGemmaEndpoint {

    private static Log log = LogFactory.getLog( DEDVRankEndpoint.class );

    // private DesignElementDataVectorService designElementDataVectorService;
    private GeneService geneService;
    private ExpressionExperimentService expressionExperimentService;
    private DedvRankService dedvRankService;

    /**
     * The local name of the expected request/response.
     */
    private static final String LOCAL_NAME = "dEDVRank";

    /**
     * Sets the "business service" to delegate to.
     */
    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

    // public void setDesignElementDataVectorService( DesignElementDataVectorService designElementDataVectorService ) {
    // this.designElementDataVectorService = designElementDataVectorService;
    // }

    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    public void setDedvRankService( DedvRankService dedvRankService ) {
        this.dedvRankService = dedvRankService;
    }

    /**
     * Reads the given <code>requestElement</code>, and sends a the response back.
     * 
     * @param requestElement the contents of the SOAP message as DOM elements
     * @param document a DOM document to be used for constructing <code>Node</code>s
     * @return the response element
     */
    @SuppressWarnings("unchecked")
    protected Element invokeInternal( Element requestElement, Document document ) throws Exception {
        StopWatch watch = new StopWatch();
        watch.start();

        setLocalName( LOCAL_NAME );
        // get ee id's from request
        Collection<String> eeIdInput = getArrayValues( requestElement, "ee_ids" );
        Collection<Long> eeIDLong = new HashSet<Long>();
        for ( String id : eeIdInput )
            eeIDLong.add( Long.parseLong( id ) );

        // Need to get and thaw the experiments.
        Collection<ExpressionExperiment> eeInput = expressionExperimentService.loadMultiple( eeIDLong );
        // for ( ExpressionExperiment ee : eeInput ) {
        // expressionExperimentService.thawLite( ee );
        // }
        if ( eeInput.isEmpty() || eeInput == null )
            return buildBadResponse( document, "Expression experiment(s) cannot be found or incorrect input" );

        // get gene id's from request
        Collection<String> geneIdInput = getArrayValues( requestElement, "gene_ids" );
        Collection<Long> geneIDLong = new HashSet<Long>();
        for ( String id : geneIdInput )
            geneIDLong.add( Long.parseLong( id ) );
        Collection<Gene> geneInput = geneService.loadMultiple( geneIDLong );
        if ( geneInput.isEmpty() || geneInput == null )
            return buildBadResponse( document, "Gene(s) cannot be found or incorrect input" );

        // get method - MAX, MIN, MEAN, MEDIAN, VARIANCE
        Collection<String> methodIn = getNodeValues( requestElement, "method" );
        // expect one value only
        String methodString = "";
        for ( String type : methodIn )
            methodString = type;
        Method method = getMethod( methodString );
        if ( method == null ) return buildBadResponse( document, "Incorrect method input" );

        log.info( "XML input read: " + eeInput.size() + " experiment ids & " + geneInput.size() + " gene ids"
                + " and method: " + methodString );

        // main call to DedvRankService to obtain rank results
        DenseDoubleMatrix2DNamed rankMatrix = ( DenseDoubleMatrix2DNamed ) dedvRankService.getRankMatrix( geneInput,
                eeInput, method );

        // start building the wrapper
        // xml is built manually here instead of using the buildWrapper method inherited from AbstractGemmaEndpoint
        Element responseWrapper = document.createElementNS( NAMESPACE_URI, LOCAL_NAME );
        Element responseElement = document.createElementNS( NAMESPACE_URI, LOCAL_NAME + RESPONSE );
        responseWrapper.appendChild( responseElement );

        if ( rankMatrix == null ) return buildBadResponse( document, "No ranking result" );

        // -build single-row Collections to use for ExpressionDataMatrixBuilder
        // -need to do this so that we can use the .getPrefferedData()
        // also necessary to do each data vector at a time because we
        // already have a mapping to the genes
        // of the design elements
        Collection<Gene> rowNames = rankMatrix.getRowNames();
        Collection<ExpressionExperiment> colNames = rankMatrix.getColNames();
        // boolean eeTrack = false;
        for ( Gene geneRow : rowNames ) {
            Element e1 = document.createElement( "gene_ids" );
            e1.appendChild( document.createTextNode( geneRow.getId().toString() ) );
            responseElement.appendChild( e1 );           
            double[] rowData = rankMatrix.getRowByName( geneRow );
            Element e2 = document.createElement( "ranks" );
            e2.appendChild( document.createTextNode( encode( rowData ) ) );
            responseElement.appendChild( e2 );
        }
        for ( ExpressionExperiment ee : colNames ) {
            Element e3 = document.createElement( "ee_ids" );
            e3.appendChild( document.createTextNode( ee.getId().toString() ) );
            responseElement.appendChild( e3 );
        }

        // for ( ExpressionExperiment ee : colNames ){
        // Element e2 = document.createElement( "ee_"+ee.getId().toString() );
        // e2.appendChild( document.createTextNode( Double.toString( rowData[eeIndex] )) );
        // responseElement.appendChild( e2 );
        // eeIndex++;
        //                
        // if (eeTrack==false){
        // //keep track of the ee id's that were used; print the ee ids in a separate node
        // Element e3 = document.createElement( "ee_ids" );
        // e3.appendChild( document.createTextNode( ee.getId().toString() ));
        // responseElement.appendChild( e3 );
        //                    
        // }
        // }
        // eeTrack=true;
        // }

        watch.stop();
        Long time = watch.getTime();

        log.info( "XML response for dedv rank results built in " + time + "ms." );

        return responseWrapper;

    }

    /**
     * Return the corresponding DedvRankService constant for MAX, MIN, MEAN, MEDIAN, VARIANCE
     * 
     * @param methodString
     * @return
     */
    private Method getMethod( String methodString ) {
        if ( methodString.equalsIgnoreCase( "median" ) )
            return Method.MEDIAN;
        else if ( methodString.equalsIgnoreCase( "max" ) )
            return Method.MAX;
        else if ( methodString.equalsIgnoreCase( "mean" ) )
            return Method.MEAN;
        else if ( methodString.equalsIgnoreCase( "min" ) )
            return Method.MIN;
        else if ( methodString.equalsIgnoreCase( "variance" ) )
            return Method.VARIANCE;
        else
            return null;
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

}
