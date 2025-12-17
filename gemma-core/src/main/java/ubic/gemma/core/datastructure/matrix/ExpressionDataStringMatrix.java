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
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BulkExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

/**
 * @author pavlidis
 */
public class ExpressionDataStringMatrix extends AbstractMultiAssayExpressionDataMatrix<String> {

    private static final Log log = LogFactory.getLog( ExpressionDataStringMatrix.class.getName() );

    private final String[][] matrix;
    private final boolean hasMissingValues;

    public ExpressionDataStringMatrix( ExpressionExperiment ee, Collection<? extends BulkExpressionDataVector> vectors ) {
        super( ee );
        List<? extends BulkExpressionDataVector> vectors1 = selectVectors( vectors );
        if ( vectors1.isEmpty() ) {
            throw new IllegalArgumentException( "No vectors!" );
        }
        this.matrix = this.createMatrix( vectors1 );
        this.hasMissingValues = checkMissingValues( this.matrix );
    }

    @Nullable
    @Override
    public String get( int row, int column ) {
        return matrix[row][column];
    }

    @Override
    public String[] getColumn( int index ) {
        if ( index < 0 || index >= columns() ) {
            throw new IndexOutOfBoundsException( "Column index " + index + " is out of bounds." );
        }
        String[] col = new String[rows()];
        for ( int i = 0; i < col.length; i++ ) {
            col[i] = matrix[i][index];
        }
        return col;
    }

    @Override
    public String[][] getMatrix() {
        return matrix;
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
        return matrix[index];
    }

    @Override
    public ExpressionDataMatrix<String> sliceRows( List<CompositeSequence> designElements ) {
        throw new UnsupportedOperationException( "Slicing rows from a multi-assay string matrix is not supported." );
    }

    @Override
    public boolean hasMissingValues() {
        return hasMissingValues;
    }

    @Override
    protected String format( int row, int column ) {
        return matrix[row][column];
    }

    private String[][] createMatrix( List<? extends BulkExpressionDataVector> vectors ) {
        int numRows = rows();
        int numCols = columns();

        // missing values will be filled with nulls
        String[][] mat = new String[numRows][numCols];

        for ( int i = 0; i < vectors.size(); i++ ) {
            BulkExpressionDataVector vector = vectors.get( i );
            CompositeSequence designElement = vector.getDesignElement();
            int rowIndex = getRowIndex( designElement );
            assert rowIndex != -1;
            String[] vals = vector.getDataAsStrings();
            for ( int j = 0; j < vector.getBioAssayDimension().getBioAssays().size(); j++ ) {
                BioAssay bioAssay = vector.getBioAssayDimension().getBioAssays().get( j );
                int column = getColumnIndex( bioAssay );
                mat[i][column] = vals[j];
            }
        }

        ExpressionDataStringMatrix.log.debug( "Created a " + rows() + " x " + columns() + " string matrix." );
        return mat;
    }


    private static boolean checkMissingValues( String[][] matrix ) {
        for ( String[] strings : matrix ) {
            for ( String string : strings ) {
                if ( string == null || string.isEmpty() ) {
                    return true;
                }
            }
        }
        return false;
    }
}
