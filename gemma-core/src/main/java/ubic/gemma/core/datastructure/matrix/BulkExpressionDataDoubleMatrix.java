package ubic.gemma.core.datastructure.matrix;

import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.util.Assert;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BulkExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

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

    private BulkExpressionDataDoubleMatrix( @Nullable ExpressionExperiment expressionExperiment, BioAssayDimension bioAssayDimension, QuantitationType quantitationType, List<CompositeSequence> designElements, DoubleMatrix2D matrix ) {
        super( expressionExperiment, bioAssayDimension, quantitationType, designElements );
        Assert.isTrue( bioAssayDimension.getBioAssays().size() == matrix.columns(),
                "Number of bioassays must match the number of columns in the matrix." );
        Assert.isTrue( designElements.size() == matrix.rows(),
                "Number of design elements must match the number of rows in the matrix." );
        this.matrix = matrix;
        boolean hmv = false;
        for ( int i = 0; i < matrix.rows(); i++ ) {
            for ( int j = 0; j < matrix.columns(); j++ ) {
                if ( Double.isNaN( matrix.getQuick( i, j ) ) ) {
                    hmv = true;
                    break;
                }
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
    public double getAsDouble( CompositeSequence designElement, BioMaterial bioMaterial ) {
        int row = getRowIndex( designElement );
        int column = getColumnIndex( bioMaterial );
        if ( row == -1 || column == -1 ) {
            return Double.NaN;
        }
        return matrix.get( row, column );
    }

    @Override
    public double getAsDouble( CompositeSequence designElement, BioAssay bioAssay ) {
        int row = getRowIndex( designElement );
        int column = getColumnIndex( bioAssay );
        if ( row == -1 || column == -1 ) {
            return Double.NaN;
        }
        return matrix.get( row, column );
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
    public ExpressionDataMatrix<Double> sliceRows( List<CompositeSequence> designElements ) {
        int[] rows = new int[designElements.size()];
        for ( int i = 0; i < designElements.size(); i++ ) {
            CompositeSequence de = designElements.get( i );
            int rowIndex = getRowIndex( de );
            if ( rowIndex == -1 ) {
                throw new IllegalArgumentException( de + " is not found in the matrix." );
            }
            rows[i] = rowIndex;
        }
        return new BulkExpressionDataDoubleMatrix( getExpressionExperiment(), getBioAssayDimension(),
                getQuantitationType(), designElements, matrix.viewSelection( rows, null ) );
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
    public Double[][] getMatrix() {
        Double[][] result = new Double[rows()][];
        for ( int i = 0; i < rows(); i++ ) {
            result[i] = getRow( i );
        }
        return result;
    }

    @Override
    public BulkExpressionDataMatrix<Double> sliceColumns( List<BioMaterial> bioMaterials ) {
        throw new UnsupportedOperationException( "Slicing columns from a bulk double matrix is not supported." );
    }

    @Override
    public BulkExpressionDataDoubleMatrix sliceColumns( List<BioMaterial> bioMaterials, BioAssayDimension dimension ) {
        throw new UnsupportedOperationException( "Slicing columns from a bulk double matrix is not supported." );
    }

    @Override
    public double[][] getMatrixAsDoubles() {
        return matrix.toArray();
    }

    @Override
    protected String format( int i, int j ) {
        return format( matrix.get( i, j ) );
    }
}
