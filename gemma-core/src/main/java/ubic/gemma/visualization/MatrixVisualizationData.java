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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix2DNamed;
import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
import ubic.basecode.gui.ColorMatrix;
import ubic.basecode.gui.JMatrixDisplay;
import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author keshav
 * @version $Id$
 * @spring.bean id="matrixVisualizationData"
 * @spring.property name="compositeSequenceService" ref="compositeSequenceService"
 */
public class MatrixVisualizationData {
    private Log log = LogFactory.getLog( this.getClass() );

    private ExpressionExperiment expressionExperiment = null;

    private Collection<DesignElement> designElements = null;

    private CompositeSequenceService compositeSequenceService = null;

    private Map dataMap = new HashMap();

    private List rowNames = null;

    private List colNames = null;

    /**
     * 
     *
     */
    public MatrixVisualizationData() {

    }

    /**
     * @param expressionExperiment
     * @param designElements
     */
    public MatrixVisualizationData( ExpressionExperiment expressionExperiment, Collection<DesignElement> designElements ) {

        this.expressionExperiment = expressionExperiment;
        this.designElements = designElements;

        for ( DesignElement designElement : designElements ) {
            // FIXME I have made the association between DesignElement and DesignElementDataVector bi-directional.
            String key = ( ( CompositeSequence ) designElement ).getName();
            DesignElementDataVector vector = ( ( CompositeSequence ) designElement ).getDesignElementDataVector();

            dataMap.put( key, vector );
        }
    }

    /**
     * @param data
     * @param outfile
     */
    public void visualize( double[][] data, String outfile ) {
        assert rowNames != null && colNames != null : "Labels not set";

        DoubleMatrixNamed matrix = new DenseDoubleMatrix2DNamed( data );
        matrix.setRowNames( rowNames );
        matrix.setColumnNames( colNames );
        ColorMatrix colorMatrix = new ColorMatrix( matrix );
        JMatrixDisplay display = new JMatrixDisplay( colorMatrix );
        try {
            display.saveImage( outfile );
            display.setLabelsVisible( true );
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    /**
     * 
     *
     */
    public void printData() {
        assert designElements != null : "Design Elements not initialized";

        for ( DesignElement designElement : designElements ) {
            ByteArrayConverter converter = new ByteArrayConverter();
            DesignElementDataVector vector = ( ( CompositeSequence ) designElement ).getDesignElementDataVector();
            String key = ( ( CompositeSequence ) designElement ).getName();

            byte[] byteData = vector.getData();
            double[] data = converter.byteArrayToDoubles( byteData );

            log.warn( key );
            for ( int j = 0; j < data.length; j++ ) {
                log.warn( "data: " + data[j] );
            }
        }
    }

    /**
     * @return Collection<DesignElement>
     */
    public Collection<DesignElement> getDesignElements() {
        return designElements;
    }

    /**
     * @param designElements
     */
    public void setDesignElements( Collection<DesignElement> designElements ) {
        this.designElements = designElements;
    }

    /**
     * @return ExpressionExperiment
     */
    public ExpressionExperiment getExpressionExperiment() {
        return expressionExperiment;
    }

    /**
     * @param expressionExperiment
     */
    public void setExpressionExperiment( ExpressionExperiment expressionExperiment ) {
        this.expressionExperiment = expressionExperiment;
    }

    /**
     * @return CompositeSequenceService
     */
    public CompositeSequenceService getCompositeSequenceService() {
        return compositeSequenceService;
    }

    /**
     * @param compositeSequenceService
     */
    public void setCompositeSequenceService( CompositeSequenceService compositeSequenceService ) {
        this.compositeSequenceService = compositeSequenceService;
    }

    /**
     * @param rowNames The rowNames to set.
     */
    public void setRowNames( List rowNames ) {
        this.rowNames = rowNames;
    }

    /**
     * @param colNames The colNames to set.
     */
    public void setColNames( List colNames ) {
        this.colNames = colNames;
    }

}
