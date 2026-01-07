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

import org.apache.commons.lang3.ArrayUtils;
import ubic.basecode.dataStructure.matrix.AbstractMatrix;
import ubic.basecode.dataStructure.matrix.ObjectMatrixImpl;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BulkExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.*;

/**
 * Matrix of booleans mapped from an ExpressionExperiment.
 *
 * @author pavlidis
 */
public class ExpressionDataBooleanMatrix extends AbstractMultiAssayExpressionDataMatrix<Boolean> {

    private final ObjectMatrixImpl<CompositeSequence, Integer, Boolean> matrix;

    public ExpressionDataBooleanMatrix( ExpressionExperiment ee, Collection<? extends BulkExpressionDataVector> vectors ) {
        super( ee );
        for ( BulkExpressionDataVector dedv : vectors ) {
            if ( !dedv.getQuantitationType().getRepresentation().equals( PrimitiveType.BOOLEAN ) ) {
                throw new IllegalStateException( "Cannot convert non-boolean quantitation types into boolean matrix" );
            }
        }
        this.matrix = vectorsToMatrix( selectVectors( vectors ) );
    }

    public ExpressionDataBooleanMatrix( ExpressionExperiment ee, Collection<? extends BulkExpressionDataVector> vectors,
            List<QuantitationType> qtypes ) {
        super( ee );
        this.matrix = vectorsToMatrix( selectVectors( vectors, qtypes ) );
    }

    @Override
    public Boolean get( int row, int column ) {
        return matrix.get( row, column );
    }

    @Override
    public Boolean[] getColumn( int column ) {
        Boolean[] res = new Boolean[matrix.rows()];
        for ( int i = 0; i < res.length; i++ ) {
            res[i] = this.matrix.get( i, column );
        }
        return res;
    }

    @Override
    public Boolean[][] getRawMatrix() {
        Boolean[][] dMatrix = new Boolean[matrix.rows()][matrix.columns()];
        for ( int i = 0; i < dMatrix.length; i++ ) {
            for ( int j = 0; j < dMatrix[i].length; j++ ) {
                dMatrix[i][j] = matrix.get( i, j );
            }
        }
        return dMatrix;
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
        Boolean[] row = new Boolean[matrix.columns()];
        for ( int j = 0; j < row.length; j++ ) {
            row[j] = this.matrix.get( index, j );
        }
        return row;
    }

    @Override
    public ExpressionDataBooleanMatrix sliceRows( List<CompositeSequence> designElements ) {
        throw new UnsupportedOperationException( "Slicing rows is not supported for multi-assay boolean matrices." );
    }

    @Override
    public boolean hasMissingValues() {
        for ( int i = 0; i < matrix.rows(); i++ ) {
            for ( int j = 0; j < matrix.columns(); j++ ) {
                if ( matrix.get( i, j ) == null )
                    return true;
            }
        }
        return false;
    }

    @Override
    protected String format( int row, int column ) {
        Boolean val = matrix.get( row, column );
        return val != null ? String.valueOf( val ) : "";
    }

    private ObjectMatrixImpl<CompositeSequence, Integer, Boolean> vectorsToMatrix( Collection<? extends BulkExpressionDataVector> vectors ) {
        if ( vectors.isEmpty() ) {
            throw new IllegalArgumentException();
        }
        return this.createMatrix( vectors );
    }

    /**
     * Fill in the data
     */
    private ObjectMatrixImpl<CompositeSequence, Integer, Boolean> createMatrix(
            Collection<? extends BulkExpressionDataVector> vectors ) {
        ObjectMatrixImpl<CompositeSequence, Integer, Boolean> mat = new ObjectMatrixImpl<>( vectors.size(), columns() );

        // initialize the matrix to false
        for ( int i = 0; i < mat.rows(); i++ ) {
            for ( int j = 0; j < mat.columns(); j++ ) {
                mat.set( i, j, Boolean.FALSE );
            }
        }
        for ( int j = 0; j < mat.columns(); j++ ) {
            mat.addColumnName( j );
        }

        Map<Integer, CompositeSequence> rowNames = new TreeMap<>();

        for ( BulkExpressionDataVector vector : vectors ) {
            BioAssayDimension dimension = vector.getBioAssayDimension();

            CompositeSequence designElement = vector.getDesignElement();

            int rowIndex = getRowIndex( designElement );
            assert rowIndex != -1;

            rowNames.put( rowIndex, designElement );

            boolean[] vals = this.getVals( vector );

            Collection<BioAssay> bioAssays = dimension.getBioAssays();

            if ( bioAssays.size() != vals.length ) {
                throw new IllegalStateException(
                        "Expected " + vals.length + " bioassays at design element " + designElement + ", got "
                                + bioAssays.size() );
            }

            Iterator<BioAssay> it = bioAssays.iterator();
            this.setMatBioAssayValues( mat, rowIndex, ArrayUtils.toObject( vals ), bioAssays, it );
        }

        for ( int i = 0; i < mat.rows(); i++ ) {
            mat.addRowName( rowNames.get( i ) );
        }

        assert mat.getRowNames().size() == mat.rows();

        return mat;
    }

    /**
     * Note that if we have trouble interpreting the data, it gets left as false.
     */
    private boolean[] getVals( DesignElementDataVector vector ) {
        boolean[] vals = null;
        if ( vector.getQuantitationType().getRepresentation().equals( PrimitiveType.BOOLEAN ) ) {
            vals = vector.getDataAsBooleans();
        } else if ( vector.getQuantitationType().getRepresentation().equals( PrimitiveType.CHAR ) ) {
            char[] charVals = vector.getDataAsChars();
            vals = new boolean[charVals.length];
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
        } else if ( vector.getQuantitationType().getRepresentation().equals( PrimitiveType.STRING ) ) {
            String[] fields = vector.getDataAsTabbedStrings();
            vals = new boolean[fields.length];
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
        }
        return vals;
    }

    private <R, C, V> void setMatBioAssayValues( AbstractMatrix<R, C, V> mat, Integer rowIndex, V[] vals,
            Collection<BioAssay> bioAssays, Iterator<BioAssay> it ) {
        for ( int j = 0; j < bioAssays.size(); j++ ) {
            BioAssay bioAssay = it.next();
            int column = getColumnIndex( bioAssay );
            assert column != -1;
            mat.set( rowIndex, column, vals[j] );
        }
    }
}
