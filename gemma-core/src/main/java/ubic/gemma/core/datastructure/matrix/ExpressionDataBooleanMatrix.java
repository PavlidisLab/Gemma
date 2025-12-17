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
package ubic.gemma.core.datastructure.matrix;

import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BulkExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.Collection;
import java.util.List;

/**
 * Matrix of booleans mapped from an ExpressionExperiment.
 *
 * @author pavlidis
 */
public class ExpressionDataBooleanMatrix extends AbstractMultiAssayExpressionDataMatrix<Boolean> {

    private final Boolean[][] matrix;

    private final boolean hasMissingValues;

    public ExpressionDataBooleanMatrix( ExpressionExperiment ee, Collection<? extends BulkExpressionDataVector> vectors ) {
        super( ee );
        this.matrix = createMatrix( selectVectors( vectors ) );
        this.hasMissingValues = checkMissingValues( this.matrix );
    }

    public ExpressionDataBooleanMatrix( ExpressionExperiment ee, Collection<? extends BulkExpressionDataVector> vectors, List<QuantitationType> qtypes ) {
        super( ee );
        this.matrix = createMatrix( selectVectors( vectors, qtypes ) );
        this.hasMissingValues = checkMissingValues( this.matrix );
    }

    @Override
    public Boolean get( int row, int column ) {
        return matrix[row][column];
    }

    @Override
    public Boolean[] getColumn( int column ) {
        if ( column < 0 || column >= columns() ) {
            throw new IndexOutOfBoundsException( "Column index " + column + " is out of bounds." );
        }
        Boolean[] res = new Boolean[rows()];
        for ( int i = 0; i < res.length; i++ ) {
            res[i] = this.matrix[i][column];
        }
        return res;
    }

    @Override
    public Boolean[][] getMatrix() {
        return matrix;
    }

    @Override
    public BulkExpressionDataMatrix<Boolean> sliceColumns( List<BioMaterial> bioMaterials ) {
        throw new UnsupportedOperationException( "Slicing rows is not supported for multi-assay boolean matrices." );
    }

    @Override
    public ExpressionDataBooleanMatrix sliceColumns( List<BioMaterial> bioMaterials, BioAssayDimension dimension ) {
        throw new UnsupportedOperationException( "Slicing rows is not supported for multi-assay boolean matrices." );
    }

    @Override
    public Boolean[] getRow( int index ) {
        return matrix[index];
    }

    @Override
    public ExpressionDataBooleanMatrix sliceRows( List<CompositeSequence> designElements ) {
        throw new UnsupportedOperationException( "Slicing rows is not supported for multi-assay boolean matrices." );
    }

    @Override
    public boolean hasMissingValues() {
        return hasMissingValues;
    }

    @Override
    protected String format( int row, int column ) {
        Boolean val = matrix[row][column];
        return val != null ? String.valueOf( val ) : "";
    }

    /**
     * Fill in the data
     */
    private Boolean[][] createMatrix( List<? extends BulkExpressionDataVector> vectors ) {
        // gaps in the matrix will be null
        Boolean[][] mat = new Boolean[rows()][columns()];
        for ( int i = 0; i < vectors.size(); i++ ) {
            BulkExpressionDataVector vector = vectors.get( i );
            boolean[] vec = getVals( vector );
            List<BioAssay> bioAssays = vector.getBioAssayDimension().getBioAssays();
            for ( int j = 0; j < bioAssays.size(); j++ ) {
                BioAssay ba = bioAssays.get( j );
                int column = getColumnIndex( ba );
                mat[i][column] = vec[j];
            }
        }
        return mat;
    }

    /**
     * Note that if we have trouble interpreting the data, it gets left as false.
     */
    private boolean[] getVals( DesignElementDataVector vector ) {
        if ( vector.getQuantitationType().getRepresentation().equals( PrimitiveType.BOOLEAN ) ) {
            return vector.getDataAsBooleans();
        } else if ( vector.getQuantitationType().getRepresentation().equals( PrimitiveType.CHAR ) ) {
            char[] charVals = vector.getDataAsChars();
            boolean[] vals = new boolean[charVals.length];
            int j = 0;
            for ( char c : charVals ) {
                switch ( c ) {
                    case 'P':
                        vals[j] = true;
                        break;
                    case 'M':
                        vals[j] = false;
                        break;
                    case 'A':
                        vals[j] = false;
                        break;
                    default:
                        vals[j] = false;
                        break;
                }
                j++;
            }
            return vals;
        } else if ( vector.getQuantitationType().getRepresentation().equals( PrimitiveType.STRING ) ) {
            String[] fields = vector.getDataAsTabbedStrings();
            boolean[] vals = new boolean[fields.length];
            int j = 0;
            for ( String c : fields ) {
                switch ( c ) {
                    case "P":
                        vals[j] = true;
                        break;
                    case "M":
                        vals[j] = false;
                        break;
                    case "A":
                        vals[j] = false;
                        break;
                    default:
                        vals[j] = false;
                        break;
                }
                j++;
            }
            return vals;
        } else {
            throw new IllegalArgumentException( "Unsupported representation: " + vector.getQuantitationType().getRepresentation() );
        }
    }

    private static boolean checkMissingValues( Boolean[][] matrix ) {
        for ( Boolean[] vector : matrix ) {
            for ( Boolean b : vector ) {
                if ( b == null ) {
                    return true;
                }
            }
        }
        return false;
    }
}
