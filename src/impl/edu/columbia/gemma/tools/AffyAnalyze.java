/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.tools;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rosuda.JRclient.REXP;
import org.rosuda.JRclient.RSrvException;

import baseCode.dataStructure.matrix.DenseDoubleMatrix2DNamed;
import baseCode.util.RCommand;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesign;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class AffyAnalyze {

    RCommand rc;

    /**
     * Name of the variable where the affybatch data are stored in the R namespace.
     */
    public static final String AFFYBATCH_VARIABLE_NAME = "affybatch";

    private static Log log = LogFactory.getLog( AffyAnalyze.class.getName() );

    public AffyAnalyze() {
        rc = RCommand.newInstance();
        rc.voidEval( "library(affy)" );
    }

    public void finalize() {
        rc.disconnect();
    }

    /**
     * Available normalization methods.
     */
    public enum normalizeMethod {
        QUANTILE, MEDIAN, MEAN
    };

    /**
     * Create a (minimal) AffyBatch object from a matrix. The object is retained in the R namespace.
     * 
     * @param affyBatchMatrix Rows represent probes, columns represent samples. The order of rows must be the same as in
     *        the native CEL file.
     * @param arrayDesign An arraydesign object which will be used to determine the CDF file to use, based on the array
     *        name.
     */
    public void AffyBatch( DenseDoubleMatrix2DNamed celMatrix, ArrayDesign arrayDesign ) {

        if ( celMatrix == null ) throw new IllegalArgumentException( "Null matrix" );

        int rows = celMatrix.rows();
        int cols = celMatrix.columns();

        if ( rows == 0 || cols == 0 ) throw new IllegalArgumentException( "Empty matrix?" );

        double[] unrolledMatrix = unrollMatrix( celMatrix );

        try {
            rc.voidEval( "rows<-" + rows );
            rc.voidEval( "cols<-" + cols );
            rc.assign( "unrolledMatrix", unrolledMatrix );
            rc.assign( "cdfName", arrayDesign.getName() ); // Example "Mouse430_2".
            rc.voidEval( "datamatrix<-matrix(unrolledMatrix, nrow=rows, ncol=cols)" );
            rc.voidEval( "rm(unrolledMatrix)" ); // maybe this saves memory...
            String affyBatchRCmd = AFFYBATCH_VARIABLE_NAME + "<-new(\"AffyBatch\", exprs=datamatrix, cdfName=cdfName )";
            rc.voidEval( affyBatchRCmd );
            rc.voidEval( "rm(datamatrix)" ); // maybe saves memory...

            // String res = rc.eval( "class(" + AFFYBATCH_VARIABLE_NAME + ")" ).asString();
            // assert ( res.equals( "AffyBatch" ) );

        } catch ( RSrvException e ) {
            log.error( e, e );
            String error = rc.getLastError();
            log.error( "Last error from R was " + error );
            throw new RuntimeException( e );
        }
    }

    /**
     * Copy a matrix into an array, so that rows are represented consecutively in the array.
     * 
     * @param celMatrix
     * @return array representation of the matrix.
     */
    private double[] unrollMatrix( DenseDoubleMatrix2DNamed celMatrix ) {
        // unroll the matrix into an array (RServe has no interface for passing a 2-d array). Unfortunately this makes a
        // copy of the data...and R will probably make yet
        // another copy. If there was a way to get the raw element array from the DenseDoubleMatrix2DNamed, that would
        // be better.
        int rows = celMatrix.rows();
        int cols = celMatrix.columns();
        double[] unrolledMatrix = new double[rows * cols];

        int k = 0;
        for ( int i = 0; i < rows; i++ ) {
            for ( int j = 0; j < cols; j++ ) {
                unrolledMatrix[k] = celMatrix.getQuick( i, j );
                k++;
            }
        }
        return unrolledMatrix;
    }

    /**
     * Corresponds to a R method of the same name in the affy package.
     * 
     * @param name
     * @return
     */
    private String cleanCdfName( String name ) {
        if ( name == null || name.length() == 0 )
            throw new IllegalArgumentException( "invalid name (null or zero length" );
        name = bioCName( name );
        name = name.toLowerCase();
        name = name.replaceAll( "_", "" );
        name = name.replaceAll( "-", "" );
        name = name.replaceAll( " ", "" );
        return name;
    }

    /**
     * Special cases for internal bioconductor names, corresponds to the "mapCdfName". We do not replicate the behavior
     * of a bug in some versions of affy which caused the corresponding method in BioConductor to return "cdenv.example"
     * for all of the special cases.
     * 
     * @param cdfName
     * @return
     */
    private String bioCName( String cdfName ) {
        if ( cdfName.equals( "cdfenv.example" ) ) {
            return "cdfenv.example";
        } else if ( cdfName.equals( "3101_a03" ) ) {
            return "hu6800";
        } else if ( cdfName.equals( "EColiGenome" ) ) {
            return "ecoli";
        } else {
            return cdfName;
        }
    }

    /**
     * @param celMatrix
     * @param arrayDesign
     * @return
     */
    @SuppressWarnings("unchecked")
    public DenseDoubleMatrix2DNamed rma( DenseDoubleMatrix2DNamed celMatrix, ArrayDesign arrayDesign ) {
        AffyBatch( celMatrix, arrayDesign );
        rc.voidEval( "v<-rma(" + AFFYBATCH_VARIABLE_NAME + ")" );
        log.info( "Done with RMA" );
        rc.voidEval( "m<-exprs(v)" );
        REXP r = rc.eval( "m" );
        double[][] results = r.asDoubleMatrix();

        // getting the row names.
        List rowNamesREXP = rc.eval( "dimnames(m)[1][[1]]" ).asVector();
        assert ( rowNamesREXP != null );
        List<String> rowNames = new ArrayList<String>();
        for ( Iterator iter = rowNamesREXP.iterator(); iter.hasNext(); ) {
            REXP element = ( REXP ) iter.next();
            String rowName = element.asString();
            rowNames.add( rowName );
        }

        // This doesn't work: we get null from the first line.
        // List colNamesREXP = rc.eval( "dimnames(m)[2][[1]]" ).asVector();
        // assert ( colNamesREXP != null );
        // List<String> colNames = new ArrayList<String>();
        // for ( Iterator iter = colNamesREXP.iterator(); iter.hasNext(); ) {
        // REXP element = ( REXP ) iter.next();
        // String rowName = element.asString();
        // colNames.add( rowName );
        // }

        // clean up.
        rc.voidEval( "rm(v)" );
        rc.voidEval( "rm(m)" );

        DenseDoubleMatrix2DNamed resultObject = new DenseDoubleMatrix2DNamed( results );
        resultObject.setRowNames( rowNames );

        // note that we assume that rma gives us back the columns in the order we provided them. This could be
        // dangerous!
        resultObject.setColumnNames( celMatrix.getColNames() );

        return resultObject;

    }

    /**
     * @param celMatrix
     * @param arrayDesign
     * @return
     */
    public DenseDoubleMatrix2DNamed normalize( DenseDoubleMatrix2DNamed celMatrix, ArrayDesign arrayDesign ) {
        AffyBatch( celMatrix, arrayDesign );

        // FIXME : implement
        return null;
    }

    /**
     * Do something about background.
     * 
     * @param celMatrix
     * @param arrayDesign
     * @return
     */
    public DenseDoubleMatrix2DNamed backgroundTreat( DenseDoubleMatrix2DNamed celMatrix, ArrayDesign arrayDesign ) {
        AffyBatch( celMatrix, arrayDesign );
        // FIXME : implement
        return null;
    }

}
