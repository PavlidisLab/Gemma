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

import java.util.List;

import baseCode.dataStructure.matrix.DoubleMatrixNamed;
import baseCode.util.RCommand;

/**
 * Object used by the marray bioconductor package. See marrayRaw, marrayInfo, marrayLayout in the package documentations
 * for details on these objects and how they are used by marray.
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class MArrayRaw extends RCommander {

    private String layoutName = null;

    public MArrayRaw() {
        super();
        rc.voidEval( "library(marray)" );
    }

    public MArrayRaw( RCommand rc ) {
        super( rc );
        rc.voidEval( "library(marray)" );
    }

    /**
     * @param red Matrix of red channel intensities
     * @param green Matrix of green channel intensities
     * @param redBg Matrix of red background intensities
     * @param greenBg Matrix of green background intensities
     * @param weights Optional matrix of weights
     * @return The name of the variable in the R context.
     */
    @SuppressWarnings("unchecked")
    public String makeMArrayRaw( DoubleMatrixNamed red, DoubleMatrixNamed green, DoubleMatrixNamed redBg,
            DoubleMatrixNamed greenBg, DoubleMatrixNamed weights ) {
        log.debug( "Making marrayRaw object" );
        String rawObjectName = "marrayraw." + red.hashCode();

        String redMaName = rc.assignMatrix( red );
        String greenMaName = rc.assignMatrix( green );
        String redBgName = rc.assignMatrix( redBg );
        String greenBgName = rc.assignMatrix( greenBg );

        String rowNameVar = makeMArrayInfo( red.getRowNames() );
        String colNameVar = makeMArrayInfo( red.getColNames() );

        String weightsName = null;
        if ( weights != null ) weightsName = rc.assignMatrix( weights );

        String makeRawCmd = rawObjectName + "<-new(\"marrayRaw\", maRf=" + redMaName + ", maGf=" + greenMaName
                + ", maRb=" + redBgName + ", maGb=" + greenBgName + ( weights == null ? "" : ", maW=" + weightsName )
                + ( this.layoutName == null ? "" : ", maLayout=" + this.layoutName ) + ", maGnames=" + rowNameVar
                + ", maTargets=" + colNameVar + ")";

        rc.voidEval( makeRawCmd );

        // sanity check.
        double[] c = rc.eval( "maGb(" + rawObjectName + ")[1,]" ).asDoubleArray();
        if ( c == null || c.length == 0 ) {
            throw new RuntimeException(
                    "marrayRaw value was not propertly set: " + rc.getLastError() == null ? "(no error message)" : rc
                            .getLastError() );
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
        String arrayLayoutName = "layout." + this.hashCode();
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

        String arrayLayoutName = "layout." + this.hashCode();
        String makeLayoutCmd = arrayLayoutName + "<-new(\"marrayLayout\", maNgr=" + 1 + ", maNgc=" + 1 + ", maNsr=" + 1
                + ", maNsc=" + numSpots + ", maNspots=" + numSpots + ", maSub=TRUE)";

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
        String infoName = "info." + labels.hashCode();
        String labelsVarName = rc.assignStringList( labels );
        String makeInfoCmd = infoName + "<-new(\"marrayInfo\", maLabels=" + labelsVarName + ")";
        rc.voidEval( makeInfoCmd );
        return infoName;
    }

}
