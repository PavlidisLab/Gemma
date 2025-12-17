/*
 * The Gemma project
 *
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.core.datastructure.matrix;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ubic.basecode.dataStructure.matrix.StringMatrix;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BulkExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * @author pavlidis
 */
public class ExpressionDataStringMatrix extends AbstractMultiAssayExpressionDataMatrix<String> {

    private static final Log log = LogFactory.getLog( ExpressionDataStringMatrix.class.getName() );
    private final StringMatrix<Integer, Integer> matrix;

    public ExpressionDataStringMatrix( ExpressionExperiment ee, Collection<? extends BulkExpressionDataVector> vectors ) {
        super( ee );
        this.matrix = vectorsToMatrix( selectVectors( vectors ) );
    }

    @Override
    public String get( int row, int column ) {
        return matrix.get( row, column );
    }

    @Override
    public String[] getColumn( int index ) {
        return this.matrix.getColumn( index );
    }

    @Override
    public String[][] getRawMatrix() {
        String[][] res = new String[this.rows()][];
        for ( int i = 0; i < this.rows(); i++ ) {
            res[i] = this.matrix.getRow( i );
        }
        return res;
    }

    @Override
    public BulkExpressionDataMatrix<String> sliceColumns( List<BioMaterial> bioMaterials ) {
        throw new UnsupportedOperationException( "Slicing columns from a multi-assay string matrix is not supported." );
    }

    @Override
    public ExpressionDataStringMatrix sliceColumns( List<BioMaterial> bioMaterials, BioAssayDimension dimension ) {
        throw new UnsupportedOperationException( "Slicing columns from a multi-assay string matrix is not supported." );
    }

    @Override
    public String[] getRow( int index ) {
        return matrix.getRow( index );
    }

    @Override
    public ExpressionDataMatrix<String> sliceRows( List<CompositeSequence> designElements ) {
        throw new UnsupportedOperationException( "Slicing rows from a multi-assay string matrix is not supported." );
    }

    @Override
    public boolean hasMissingValues() {
        for ( int i = 0; i < matrix.rows(); i++ ) {
            for ( int j = 0; j < matrix.columns(); j++ ) {
                // Note that blank strings don't count as missing.
                if ( matrix.get( i, j ) == null )
                    return true;
            }
        }
        return false;
    }

    @Override
    protected String format( int row, int column ) {
        return matrix.get( row, column );
    }

    private StringMatrix<Integer, Integer> vectorsToMatrix( Collection<? extends BulkExpressionDataVector> vectors ) {
        if ( vectors.isEmpty() ) {
            throw new IllegalArgumentException( "No vectors!" );
        }

        return this.createMatrix( vectors );
    }

    private StringMatrix<Integer, Integer> createMatrix( Collection<? extends BulkExpressionDataVector> vectors ) {

        int numRows = rows();
        int numCols = columns();

        StringMatrix<Integer, Integer> mat = new StringMatrix<>( numRows, numCols );

        for ( int j = 0; j < mat.columns(); j++ ) {
            mat.addColumnName( j );
        }

        // initialize the matrix to "";
        for ( int i = 0; i < mat.rows(); i++ ) {
            for ( int j = 0; j < mat.columns(); j++ ) {
                mat.set( i, j, "" );
            }
        }

        for ( BulkExpressionDataVector vector : vectors ) {

            CompositeSequence designElement = vector.getDesignElement();
            assert designElement != null : "No designelement for " + vector;

            int rowIndex = getRowIndex( designElement );
            assert rowIndex != -1;

            mat.addRowName( rowIndex );

            String[] vals = vector.getDataAsStrings();

            BioAssayDimension dimension = vector.getBioAssayDimension();
            Collection<BioAssay> bioAssays = dimension.getBioAssays();
            assert bioAssays.size() == vals.length : "Expected " + vals.length + " got " + bioAssays.size();

            Iterator<BioAssay> it = bioAssays.iterator();

            for ( int j = 0; j < bioAssays.size(); j++ ) {

                BioAssay bioAssay = it.next();
                int column = getColumnIndex( bioAssay );

                assert column != -1;

                mat.setByKeys( rowIndex, column, vals[j] );
            }

        }

        ExpressionDataStringMatrix.log.debug( "Created a " + mat.rows() + " x " + mat.columns() + " matrix" );
        return mat;
    }

}
