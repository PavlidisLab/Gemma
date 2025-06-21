package ubic.gemma.core.datastructure.matrix;

import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import org.apache.commons.lang3.ArrayUtils;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BulkExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;

import javax.annotation.Nullable;
import java.util.List;

public class BulkExpressionDataDoubleMatrix extends AbstractBulkExpressionDataMatrix<Double> implements BulkExpressionDataPrimitiveDoubleMatrix {

    private final DoubleMatrix2D matrix;
    private final boolean hasMissingValues;

    public BulkExpressionDataDoubleMatrix( List<? extends BulkExpressionDataVector> vectors ) {
        super( vectors );
        this.matrix = new DenseDoubleMatrix2D( vectors.size(), getBioAssayDimension().getBioAssays().size() );
        boolean hmv = false;
        for ( int i = 0; i < vectors.size(); i++ ) {
            double[] row = vectors.get( i ).getDataAsDoubles();
            for ( int j = 0; j < row.length; j++ ) {
                matrix.setQuick( i, j, row[j] );
                hmv |= Double.isNaN( row[j] );
            }
        }
        this.hasMissingValues = hmv;
    }

    @Override
    public boolean hasMissingValues() {
        return hasMissingValues;
    }

    @Override
    public Double[] getColumn( int column ) {
        return ArrayUtils.toObject( getColumnAsDoubles( column ) );
    }

    @Override
    public double[] getColumnAsDoubles( int column ) {
        return matrix.viewColumn( column ).toArray();
    }

    @Override
    public double[] getColumnAsDoubles( BioAssay bioAssay ) {
        int column = getColumnIndex( bioAssay );
        if ( column == -1 ) {
            return null;
        }
        return getColumnAsDoubles( column );
    }

    @Override
    public Double[] getRow( int index ) {
        return ArrayUtils.toObject( getRowAsDoubles( index ) );
    }

    @Override
    public double[] getRowAsDoubles( int index ) {
        return matrix.viewRow( index ).toArray();
    }

    @Nullable
    @Override
    public double[] getRowAsDoubles( CompositeSequence designElement ) {
        int row = getRowIndex( designElement );
        if ( row == -1 ) {
            return null;
        }
        return getRowAsDoubles( row );
    }

    @Override
    public Double get( int row, int column ) {
        return getAsDouble( row, column );
    }

    @Override
    public double getAsDouble( int row, int column ) {
        return matrix.get( row, column );
    }

    @Override
    public Double[][] getRawMatrix() {
        Double[][] result = new Double[rows()][];
        for ( int i = 0; i < rows(); i++ ) {
            result[i] = getRow( i );
        }
        return result;
    }

    @Override
    public double[][] getRawMatrixAsDoubles() {
        return matrix.toArray();
    }

    @Override
    protected String format( int i, int j ) {
        return format( matrix.get( i, j ) );
    }
}
