/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.analysis.util;

import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
import ubic.basecode.util.RServeClient;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;

/**
 * Class to create AffyBatch objects for use in BioConductor analyses.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class AffyBatch extends RCommander {

    public AffyBatch() {
        super();
        rc.voidEval( "library(affy)" );
    }

    public AffyBatch( RServeClient rc ) {
        super( rc );
        rc.voidEval( "library(affy)" );
    }

    /**
     * Available normalization methods.
     */
    public enum normalizeMethod {
        CONSTANT, CONTRASTS, INVARIANTSET, LOESS, QSPLINE, QUANTILES, QUANTILES_ROBUST
    };

    public enum backgroundMethod {
        MAS, RMA, NONE
    };

    public enum expressSummaryStatMethod {
        AVEDIFF, LIWONG, MAS, MEDIANPOLISH, PLAYEROUT
    }

    /**
     * Create a (minimal) AffyBatch object from a matrix. The object is retained in the R namespace.
     * 
     * @param affyBatchMatrix Rows represent probes, columns represent samples. The order of rows must be the same as in
     *        the native CEL file.
     * @param arrayDesign An arraydesign object which will be used to determine the CDF file to use, based on the array
     *        name.
     */
    public String makeAffyBatch( DoubleMatrixNamed celMatrix, ArrayDesign arrayDesign ) {

        if ( celMatrix == null ) throw new IllegalArgumentException( "Null matrix" );

        String matrixName = rc.assignMatrix( celMatrix );
        String abName = "AffyBatch." + matrixName;

        rc.assign( "cdfName", arrayDesign.getName() ); // Example "Mouse430_2".

        String affyBatchRCmd = abName + "<-new(\"AffyBatch\", exprs=" + matrixName + ", cdfName=cdfName )";

        rc.voidEval( affyBatchRCmd );
        rc.voidEval( "rm(" + matrixName + ")" ); // maybe saves memory...

        return abName;
    }

    // /**
    // * Corresponds to a R method of the same name in the affy package.
    // *
    // * @param name
    // * @return
    // */
    // private String cleanCdfName( String name ) {
    // if ( name == null || name.length() == 0 )
    // throw new IllegalArgumentException( "invalid name (null or zero length" );
    // name = bioCName( name );
    // name = name.toLowerCase();
    // name = name.replaceAll( "_", "" );
    // name = name.replaceAll( "-", "" );
    // name = name.replaceAll( " ", "" );
    // return name;
    // }
    //
    // /**
    // * Special cases for internal bioconductor names, corresponds to the "mapCdfName". We do not replicate the
    // behavior
    // * of a bug in some versions of affy which caused the corresponding method in BioConductor to return
    // "cdenv.example"
    // * for all of the special cases.
    // *
    // * @param cdfName
    // * @return
    // */
    // private String bioCName( String cdfName ) {
    // if ( cdfName.equals( "cdfenv.example" ) ) {
    // return "cdfenv.example";
    // } else if ( cdfName.equals( "3101_a03" ) ) {
    // return "hu6800";
    // } else if ( cdfName.equals( "EColiGenome" ) ) {
    // return "ecoli";
    // } else {
    // return cdfName;
    // }
    // }

}
