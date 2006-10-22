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
package ubic.gemma.datastructure.matrix;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import ubic.basecode.dataStructure.matrix.DoubleMatrix2DNamedFactory;
import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * TODO - DOCUMENT ME
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ExpressionDataDoubleMatrix implements ExpressionDataMatrix {

    private DoubleMatrixNamed matrix;

    public ExpressionDataDoubleMatrix( ExpressionExperiment expressionExperiment, QuantitationType quantitationType ) {
    }

    public ExpressionDataDoubleMatrix( ExpressionExperiment expressionExperiment,
            Collection<DesignElement> designElements, QuantitationType quantitationType ) {

    }

    public ExpressionDataDoubleMatrix( Collection<DesignElementDataVector> dataVectors ) {

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#get(ubic.gemma.model.expression.designElement.DesignElement,
     *      ubic.gemma.model.expression.bioAssay.BioAssay)
     */
    public Double get( DesignElement designElement, BioAssay bioAssay ) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#get(java.util.List, java.util.List)
     */
    public Double[][] get( List designElements, List bioAssays ) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getColumn(ubic.gemma.model.expression.bioAssay.BioAssay)
     */
    public Double[] getColumn( BioAssay bioAssay ) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getColumns(java.util.List)
     */
    public Double[][] getColumns( List bioAssays ) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getMatrix()
     */
    public Double[][] getMatrix() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getRow(ubic.gemma.model.expression.designElement.DesignElement)
     */
    public Double[] getRow( DesignElement designElement ) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getRows(java.util.List)
     */
    public Double[][] getRows( List designElements ) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Convert {@link DesignElementDataVector}s into a {@link DoubleMatrixNamed}.
     * 
     * @param vectors
     * @return
     */
    private DoubleMatrixNamed vectorsToDoubleMatrix( Collection<DesignElementDataVector> vectors ) {
        if ( vectors == null || vectors.size() == 0 ) {
            return null;
        }

        ByteArrayConverter bac = new ByteArrayConverter();

        List<BioAssay> bioAssays = ( List<BioAssay> ) vectors.iterator().next().getBioAssayDimension().getBioAssays();

        assert bioAssays.size() > 0 : "Empty BioAssayDimension for the vectors";

        DoubleMatrixNamed matrix = DoubleMatrix2DNamedFactory.fastrow( vectors.size(), bioAssays.size() );

        // Use BioMaterial names to represent the column in the matrix (as it can span multiple BioAssays)
        for ( BioAssay assay : bioAssays ) {
            StringBuilder buf = new StringBuilder();
            List<BioMaterial> bms = new ArrayList<BioMaterial>( assay.getSamplesUsed() );
            // Collections.sort( bms ); // FIXME this should use a sort.
            for ( BioMaterial bm : bms ) {
                buf.append( bm.getName() );
            }
            matrix.addColumnName( buf.toString() );
        }

        int rowNum = 0;
        for ( DesignElementDataVector vector : vectors ) {
            String name = vector.getDesignElement().getName();
            matrix.addRowName( name );
            byte[] bytes = vector.getData();
            double[] vals = bac.byteArrayToDoubles( bytes );
            assert vals.length == bioAssays.size() : "Number of values in vector (" + vals.length
                    + ") don't match number of Bioassays (" + bioAssays.size() + ")";
            for ( int i = 0; i < vals.length; i++ ) {
                matrix.setQuick( rowNum, i, vals[i] );
            }
            rowNum++;
        }
        return matrix;
    }

}
