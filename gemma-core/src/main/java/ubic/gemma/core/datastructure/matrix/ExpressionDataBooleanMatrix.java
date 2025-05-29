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

import cern.colt.matrix.ObjectMatrix1D;
import org.apache.commons.lang3.ArrayUtils;
import ubic.basecode.dataStructure.matrix.ObjectMatrixImpl;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BulkExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;

import java.util.*;

/**
 * Matrix of booleans mapped from an ExpressionExperiment.
 *
 * @author pavlidis
 */
public class ExpressionDataBooleanMatrix extends AbstractMultiAssayExpressionDataMatrix<Boolean> {

    private ObjectMatrixImpl<CompositeSequence, Integer, Boolean> matrix;

    public ExpressionDataBooleanMatrix( Collection<? extends BulkExpressionDataVector> vectors ) {
        this.init();

        for ( BulkExpressionDataVector dedv : vectors ) {
            if ( !dedv.getQuantitationType().getRepresentation().equals( PrimitiveType.BOOLEAN ) ) {
                throw new IllegalStateException( "Cannot convert non-boolean quantitation types into boolean matrix" );
            }
        }

        this.selectVectors( vectors );
        this.vectorsToMatrix( vectors );
    }

    public ExpressionDataBooleanMatrix( Collection<? extends BulkExpressionDataVector> vectors,
            List<QuantitationType> qtypes ) {
        this.init();
        Collection<BulkExpressionDataVector> selectedVectors = this.selectVectors( vectors, qtypes );
        this.vectorsToMatrix( selectedVectors );
    }

    @Override
    public int columns() {
        return matrix.columns();
    }

    @Override
    public Boolean get( int row, int column ) {
        return matrix.get( row, column );
    }

    @Override
    public Boolean[] getColumn( int index ) {
        ObjectMatrix1D rawResult = this.matrix.viewColumn( index );
        Boolean[] res = new Boolean[rawResult.size()];
        int i = 0;
        for ( Object o : rawResult.toArray() ) {
            res[i] = ( Boolean ) o;
            i++;
        }
        return res;
    }

    @Override
    public Boolean[][] getRawMatrix() {
        Boolean[][] dMatrix = new Boolean[matrix.rows()][matrix.columns()];
        for ( int i = 0; i < matrix.rows(); i++ ) {
            Object[] rawRow = matrix.getRow( i );
            for ( int j = 0; j < rawRow.length; j++ ) {
                dMatrix[i][j] = ( Boolean ) rawRow[i];
            }
        }

        return dMatrix;
    }

    @Override
    public Boolean[] getRow( int index ) {
        Object[] rawRow = matrix.getRow( index );
        Boolean[] row = new Boolean[matrix.rows()];
        for ( int i = 0; i < matrix.rows(); i++ ) {
            row[i] = ( Boolean ) rawRow[i];
        }
        return row;
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
    public int rows() {
        return matrix.rows();
    }

    @Override
    protected String format( int row, int column ) {
        Boolean val = matrix.get( row, column );
        return val != null ? String.valueOf( val ) : "";
    }

    @Override
    protected void vectorsToMatrix( Collection<? extends BulkExpressionDataVector> vectors ) {
        if ( vectors.isEmpty() ) {
            throw new IllegalArgumentException();
        }

        int maxSize = this.setUpColumnElements();

        this.matrix = this.createMatrix( vectors, maxSize );

    }

    /**
     * Fill in the data
     */
    private ObjectMatrixImpl<CompositeSequence, Integer, Boolean> createMatrix(
            Collection<? extends BulkExpressionDataVector> vectors, int maxSize ) {
        ObjectMatrixImpl<CompositeSequence, Integer, Boolean> mat = new ObjectMatrixImpl<>( vectors.size(), maxSize );

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

            Integer rowIndex = this.rowElementMap.get( designElement );
            assert rowIndex != null;

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

}
