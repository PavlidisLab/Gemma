/*
 * The Gemma project
 * 
 * Copyright (c) 2006 Columbia University
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
package ubic.gemma.visualization;

/**
 * A value object to visualize the ExpressionDataMatrix
 * 
 * @author keshav
 * @version $Id$
 */
public class ExpressionDataMatrixVisualization {

    private ExpressionDataMatrix expressionDataMatrix = null;

    private String outfile = null;

    private int imageWidth = 0;

    private int imageHeight = 0;

    /**
     * @return int
     */
    public int getImageHeight() {
        return imageHeight;
    }

    /**
     * @param imageHeight
     */
    public void setImageHeight( int imageHeight ) {
        this.imageHeight = imageHeight;
    }

    /**
     * @return int
     */
    public int getImageWidth() {
        return imageWidth;
    }

    /**
     * @param imageWidth
     */
    public void setImageWidth( int imageWidth ) {
        this.imageWidth = imageWidth;
    }

    /**
     * @return ExpressionDataMatrix
     */
    public ExpressionDataMatrix getExpressionDataMatrix() {
        return expressionDataMatrix;
    }

    /**
     * @param expressionDataMatrix
     */
    public void setExpressionDataMatrix( ExpressionDataMatrix expressionDataMatrix ) {
        this.expressionDataMatrix = expressionDataMatrix;
    }

    /**
     * @return String
     */
    public String getOutfile() {
        return outfile;
    }

    /**
     * @param outfile
     */
    public void setOutfile( String outfile ) {
        this.outfile = outfile;
    }

}
