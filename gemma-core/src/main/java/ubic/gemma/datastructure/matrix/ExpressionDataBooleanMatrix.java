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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;

import ubic.basecode.dataStructure.matrix.ObjectMatrixImpl;
import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import cern.colt.matrix.ObjectMatrix1D;

/**
 * Matrix of booleans mapped from an ExpressionExperiment.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ExpressionDataBooleanMatrix extends BaseExpressionDataMatrix<Boolean> {

    private static final long serialVersionUID = 1L;
    private ObjectMatrixImpl<CompositeSequence, Integer, Boolean> matrix;

    /**
     * @param vectors
     * @param dimensions
     * @param qtypes
     */
    public ExpressionDataBooleanMatrix( Collection<? extends DesignElementDataVector> vectors,
            List<QuantitationType> qtypes ) {
        init();
        Collection<DesignElementDataVector> selectedVectors = selectVectors( vectors, qtypes );
        vectorsToMatrix( selectedVectors );
    }

    public ExpressionDataBooleanMatrix( Collection<? extends DesignElementDataVector> vectors ) {
        init();

        for ( DesignElementDataVector dedv : vectors ) {
            if ( !dedv.getQuantitationType().getRepresentation().equals( PrimitiveType.BOOLEAN ) ) {
                throw new IllegalStateException( "Cannot convert non-boolean quantitation types into boolean matrix" );
            }
        }

        selectVectors( vectors );
        vectorsToMatrix( vectors );
    }

    public int columns() {
        return matrix.columns();
    }

    /**
     * Fill in the data
     * 
     * @param vectors
     * @param maxSize
     * @return
     */
    private ObjectMatrixImpl<CompositeSequence, Integer, Boolean> createMatrix(
            Collection<? extends DesignElementDataVector> vectors, int maxSize ) {
        ObjectMatrixImpl<CompositeSequence, Integer, Boolean> mat = new ObjectMatrixImpl<CompositeSequence, Integer, Boolean>(
                vectors.size(), maxSize );

        // initialize the matrix to false
        for ( int i = 0; i < mat.rows(); i++ ) {
            for ( int j = 0; j < mat.columns(); j++ ) {
                mat.set( i, j, Boolean.FALSE );
            }
        }
        for ( int j = 0; j < mat.columns(); j++ ) {
            mat.addColumnName( j );
        }

        ByteArrayConverter bac = new ByteArrayConverter();
        Map<Integer, CompositeSequence> rowNames = new TreeMap<Integer, CompositeSequence>();

        for ( DesignElementDataVector vector : vectors ) {
            BioAssayDimension dimension = vector.getBioAssayDimension();
            byte[] bytes = vector.getData();

            CompositeSequence designElement = vector.getDesignElement();
            Integer rowIndex = this.rowElementMap.get( designElement );
            assert rowIndex != null;
            rowNames.put( rowIndex, designElement );

            boolean[] vals = getVals( bac, vector, bytes );

            Collection<BioAssay> bioAssays = dimension.getBioAssays();

            if ( bioAssays.size() != vals.length ) {
                throw new IllegalStateException( "Expected " + vals.length + " bioassays at design element "
                        + designElement + ", got " + bioAssays.size() );
            }

            Iterator<BioAssay> it = bioAssays.iterator();
            for ( int j = 0; j < bioAssays.size(); j++ ) {
                BioAssay bioAssay = it.next();
                Integer column = this.columnAssayMap.get( bioAssay );
                assert column != null;
                mat.set( rowIndex, column, vals[j] );
            }

        }

        for ( int i = 0; i < mat.rows(); i++ ) {
            mat.addRowName( rowNames.get( i ) );
        }

        assert mat.getRowNames().size() == mat.rows();

        return mat;
    }

    /**
     * Note that if we have trouble interpreting the data, it gets left as false.
     * 
     * @param bac
     * @param vector
     * @param bytes
     * @return
     */
    private boolean[] getVals( ByteArrayConverter bac, DesignElementDataVector vector, byte[] bytes ) {
        boolean[] vals = null;
        if ( vector.getQuantitationType().getRepresentation().equals( PrimitiveType.BOOLEAN ) ) {
            vals = bac.byteArrayToBooleans( bytes );
        } else if ( vector.getQuantitationType().getRepresentation().equals( PrimitiveType.CHAR ) ) {
            char[] charVals = bac.byteArrayToChars( bytes );
            vals = new boolean[charVals.length];
            int j = 0;
            for ( char c : charVals ) {
                if ( c == 'P' ) {
                    vals[j] = true;
                } else if ( c == 'M' ) {
                    vals[j] = false;
                } else if ( c == 'A' ) {
                    vals[j] = false;
                } else {
                    vals[j] = false;
                }
                j++;
            }
        } else if ( vector.getQuantitationType().getRepresentation().equals( PrimitiveType.STRING ) ) {
            String val = bac.byteArrayToAsciiString( bytes );
            String[] fields = StringUtils.split( val, '\t' );
            vals = new boolean[fields.length];
            int j = 0;
            for ( String c : fields ) {
                if ( c.equals( "P" ) ) {
                    vals[j] = true;
                } else if ( c.equals( "M" ) ) {
                    vals[j] = false;
                } else if ( c.equals( "A" ) ) {
                    vals[j] = false;
                } else {
                    vals[j] = false;
                }
                j++;
            }
        }
        return vals;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.datastructure.matrix.ExpressionDataMatrix#get(ubic.gemma.model.expression.designElement.DesignElement,
     * ubic.gemma.model.expression.bioAssay.BioAssay)
     */
    public Boolean get( CompositeSequence designElement, BioAssay bioAssay ) {
        return this.matrix.get( matrix.getRowIndexByName( designElement ), matrix
                .getColIndexByName( this.columnAssayMap.get( bioAssay ) ) );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#get(java.util.List, java.util.List)
     */
    public Boolean[][] get( List designElements, List bioAssays ) {
        // TODO Implement me
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getColumn(ubic.gemma.model.expression.bioAssay.BioAssay)
     */
    public Boolean[] getColumn( BioAssay bioAssay ) {
        int index = this.columnAssayMap.get( bioAssay );
        return getColumn( index );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getColumn(java.lang.Integer)
     */
    public Boolean[] getColumn( Integer index ) {
        ObjectMatrix1D rawResult = this.matrix.viewColumn( index );
        Boolean[] res = new Boolean[rawResult.size()];
        int i = 0;
        for ( Object o : rawResult.toArray() ) {
            res[i] = ( Boolean ) o;
            i++;
        }
        return res;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getColumns(java.util.List)
     */
    public Boolean[][] getColumns( List bioAssays ) {
        // TODO Implement me
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getMatrix()
     */
    public Boolean[][] getRawMatrix() {
        // TODO Implement me
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getRow(ubic.gemma.model.expression.designElement.DesignElement
     * )
     */
    public Boolean[] getRow( CompositeSequence designElement ) {
        Integer row = this.rowElementMap.get( designElement );
        if ( row == null ) return null;
        Object[] rawRow = matrix.getRow( row );
        Boolean[] result = new Boolean[rawRow.length];
        for ( int i = 0, k = rawRow.length; i < k; i++ ) {
            assert rawRow[i] instanceof Boolean : "Got a " + rawRow[i].getClass().getName();
            result[i] = ( Boolean ) rawRow[i];
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getRows(java.util.List)
     */
    @SuppressWarnings("unchecked")
    public Boolean[][] getRows( List designElements ) {
        if ( designElements == null ) {
            return null;
        }

        Boolean[][] result = new Boolean[designElements.size()][];
        int i = 0;
        for ( CompositeSequence element : ( List<CompositeSequence> ) designElements ) {
            Boolean[] rowResult = getRow( element );
            result[i] = rowResult;
            i++;
        }
        return result;
    }

    public int rows() {
        return matrix.rows();
    }

    @Override
    protected void vectorsToMatrix( Collection<? extends DesignElementDataVector> vectors ) {
        if ( vectors == null || vectors.size() == 0 ) {
            throw new IllegalArgumentException();
        }

        int maxSize = setUpColumnElements();

        this.matrix = createMatrix( vectors, maxSize );

    }

    public void set( int row, int column, Boolean value ) {
        throw new UnsupportedOperationException();
    }

    public Boolean get( int row, int column ) {
        return matrix.get( row, column );
    }

    public Boolean[] getRow( Integer index ) {
        return matrix.getRow( index );
    }

}
