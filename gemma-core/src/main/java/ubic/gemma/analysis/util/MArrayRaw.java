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

import java.io.IOException;
import java.util.List;

import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
import ubic.basecode.util.RServeClient;

/**
 * Object used by the marray bioconductor package. See marrayRaw, marrayInfo, marrayLayout in the package documentations
 * for details on these objects and how they are used by marray.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class MArrayRaw extends RCommander {

    private String layoutName = null;

    public MArrayRaw() throws IOException {
        super();
        rc.voidEval( "library(marray)" );
    }

    /**
     * Create marrayRaw object when background and weights are not used or already taken account of.
     * 
     * @param red Matrix of red channel intensities
     * @param green Matrix of green channel intensities
     * @return
     */
    public String makeMArrayRaw( DoubleMatrixNamed<String, String> red, DoubleMatrixNamed<String, String> green ) {
        return this.makeMArrayRaw( red, green, null, null, null );
    }

    /**
     * @param red Matrix of red channel intensities
     * @param green Matrix of green channel intensities
     * @param redBg Matrix of red background intensities, can be null
     * @param greenBg Matrix of green background intensities, can be null
     * @param weights Optional matrix of weights
     * @return The name of the variable in the R context.
     */
    @SuppressWarnings("unchecked")
    public String makeMArrayRaw( DoubleMatrixNamed<String, String> red, DoubleMatrixNamed<String, String> green,
            DoubleMatrixNamed<String, String> redBg, DoubleMatrixNamed<String, String> greenBg,
            DoubleMatrixNamed<String, String> weights ) {

        if ( red == null || green == null ) throw new IllegalArgumentException( "Signal matrices must not be null" );

        log.debug( "Making marrayRaw object" );
        String rawObjectName = "marrayraw." + RServeClient.variableIdentityNumber( red );

        String redMaName = rc.assignMatrix( red );
        String greenMaName = rc.assignMatrix( green );

        // if the redBg or greenBg are null
        String redBgName = null;
        if ( redBg != null ) redBgName = rc.assignMatrix( redBg );

        String greenBgName = null;
        if ( greenBg != null ) greenBgName = rc.assignMatrix( greenBg );

        String rowNameVar = makeMArrayInfo( red.getRowNames() );
        String colNameVar = makeMArrayInfo( red.getColNames() );

        /* Check everything is okay */
        List<String> sanityRed = rc.stringListEval( rowNameVar + "@maLabels" );
        assert sanityRed != null && sanityRed.size() == red.getRowNames().size();
        List<String> sanityGreen = rc.stringListEval( colNameVar + "@maLabels" );
        assert sanityGreen != null && sanityGreen.size() == red.getColNames().size();

        String weightsName = null;
        if ( weights != null ) weightsName = rc.assignMatrix( weights );

        String makeRawCmd = rawObjectName + "<-new(\"marrayRaw\", maRf=" + redMaName + ", maGf=" + greenMaName
                + ( redBg == null ? "" : ", maRb=" + redBgName ) + ( greenBg == null ? "" : ", maGb=" + greenBgName )
                + ( weights == null ? "" : ", maW=" + weightsName )
                + ( this.layoutName == null ? "" : ", maLayout=" + this.layoutName ) + ", maGnames=" + rowNameVar
                + ", maTargets=" + colNameVar + ")";

        rc.voidEval( makeRawCmd );

        // sanity check.
        double[] c = rc.doubleArrayEval( "maGf(" + rawObjectName + ")[1,]" );
        if ( c == null || c.length == 0 ) {
            throw new RuntimeException( "marrayRaw value was not propertly set while running command: ' " + makeRawCmd
                    + "' - Error message was: "
                    + ( rc.getLastError() == null ? "(no error message)" : rc.getLastError() ) );
        }

        return rawObjectName;
    }

    /**
     * The marrayLayout object is needed for some types of normalization, such as print-tip.
     * 
     * @param gridRows The number of major grid rows, or rows of "blocks" of spots.
     * @param gridColumns The number of major grid columns, or columns of "blocks" of spots.
     * @param rowsPerGrid
     * @param colsPerGrid
     * @return The name of the variable in the R context.
     */
    public String makeMArrayLayout( int gridRows, int gridColumns, int rowsPerGrid, int colsPerGrid ) {
        log.debug( "Making layout" );
        int numSpots = gridRows * gridColumns * rowsPerGrid * colsPerGrid;
        String arrayLayoutName = "layout." + RServeClient.variableIdentityNumber( this );
        String makeLayoutCmd = arrayLayoutName + "<-new(\"marrayLayout\", maNgr=" + gridRows + ", maNgc=" + gridColumns
                + ", maNsr=" + rowsPerGrid + ", maNsc=" + colsPerGrid + ", maNspots=" + numSpots + ", maSub=TRUE)";

        rc.voidEval( makeLayoutCmd );
        this.layoutName = arrayLayoutName;
        return arrayLayoutName;
    }

    /**
     * The marrayLayout object is needed for some types of normalization, such as print-tip. This method creates an
     * absolutely minimal layout when there is really no layout information.
     * 
     * @param numSpots
     * @return The name of the variable in the R context.
     */
    public String makeMArrayLayout( int numSpots ) {
        log.debug( "Making layout" );

        String arrayLayoutName = "layout." + RServeClient.variableIdentityNumber( this );
        String makeLayoutCmd = arrayLayoutName + "<-new(\"marrayLayout\", maNgr=1 , maNgc=1, maNsr=1, maNsc="
                + numSpots + ", maNspots=" + numSpots + ", maSub=TRUE)";

        rc.voidEval( makeLayoutCmd );
        this.layoutName = arrayLayoutName;
        return arrayLayoutName;
    }

    /**
     * Create a stripped-down marrayInfo object, where only the "labels" slot is filled in.
     * 
     * @param labels
     * @return The name of the variable in the R context.
     */
    public String makeMArrayInfo( List<String> labels ) {
        log.debug( "Making info" );
        String infoName = "info." + RServeClient.variableIdentityNumber( labels );
        String labelsVarName = rc.assignStringList( labels );
        String makeInfoCmd = infoName + "<-new(\"marrayInfo\", maLabels=" + labelsVarName + ")";
        rc.voidEval( makeInfoCmd );
        return infoName;
    }

}
