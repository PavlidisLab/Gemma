package ubic.gemma.core.datastructure.matrix;

import no.uib.cipr.matrix.sparse.CompRowMatrix;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.util.Assert;
import ubic.gemma.core.datastructure.SparseRangeArrayList;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;
import java.util.*;

import static ubic.gemma.core.analysis.stats.DataVectorDescriptive.getMissingCountValue;

/**
 * @author poirigui
 */
public class SingleCellExpressionDataDoubleMatrix extends AbstractSingleCellExpressionDataMatrix<Double> implements ExpressionDataPrimitiveDoubleMatrix {

    private static final Comparator<CompositeSequence> designElementComparator = Comparator.comparing( CompositeSequence::getName )
            .thenComparing( CompositeSequence::getId, Comparator.nullsLast( Comparator.naturalOrder() ) );

    private final ExpressionExperiment expressionExperiment;
    private final QuantitationType quantitationType;
    private final SingleCellDimension singleCellDimension;
    private final CompRowMatrix matrix;
    private final List<CompositeSequence> designElements;
    private final List<BioAssay> bioAssays;

    /**
     * Indicate if this matrix is a count matrix.
     */
    private final boolean isCount;
    /**
     * Default value for a missing (or zero) count. This is only used if the matrix is a count matrix as indicated by
     * {@link #isCount}.
     */
    private final double defaultCountValue;

    /**
     * Row elements, only computed on-demand.
     */
    @Nullable
    private List<ExpressionDataMatrixRowElement> rowElements;

    public SingleCellExpressionDataDoubleMatrix( Collection<SingleCellExpressionDataVector> vectors ) {
        Assert.isTrue( !vectors.isEmpty(), "At least one vector must be supplied. Use EmptyExpressionDataMatrix for empty data matrices instead." );
        Assert.isTrue( vectors.stream().map( SingleCellExpressionDataVector::getQuantitationType ).distinct().count() == 1,
                "All vectors must have the same quantitation type." );
        Assert.isTrue( vectors.stream().map( SingleCellExpressionDataVector::getSingleCellDimension ).distinct().count() == 1,
                "All vectors must have the same single-cell dimension." );
        SingleCellExpressionDataVector vector = vectors.iterator().next();
        Assert.isTrue( vector.getQuantitationType().getRepresentation().equals( PrimitiveType.DOUBLE ), "Vectors must use the " + PrimitiveType.DOUBLE + " representation." );
        expressionExperiment = vector.getExpressionExperiment();
        quantitationType = vector.getQuantitationType();
        singleCellDimension = vector.getSingleCellDimension();
        // sort vectors by CS
        List<SingleCellExpressionDataVector> sortedVectors = new ArrayList<>( vectors );
        sortedVectors.sort( Comparator.comparing( SingleCellExpressionDataVector::getDesignElement, designElementComparator ) );
        int rows = sortedVectors.size();
        int i = 0;
        int[][] nz = new int[rows][];
        for ( SingleCellExpressionDataVector v : sortedVectors ) {
            nz[i++] = v.getDataIndices();
        }
        matrix = new CompRowMatrix( rows, singleCellDimension.getNumberOfCells(), nz );
        designElements = new ArrayList<>( sortedVectors.size() );
        i = 0;
        for ( SingleCellExpressionDataVector v : sortedVectors ) {
            designElements.add( v.getDesignElement() );
            double[] row = v.getDataAsDoubles();
            int[] indices = v.getDataIndices();
            for ( int j = 0; j < row.length; j++ ) {
                matrix.set( i, indices[j], row[j] );
            }
            i++;
        }
        if ( quantitationType.getType() == StandardQuantitationType.COUNT ) {
            isCount = true;
            defaultCountValue = getMissingCountValue( quantitationType );
        } else {
            isCount = false;
            defaultCountValue = 0;
        }
        bioAssays = new SparseRangeArrayList<>( singleCellDimension.getBioAssays(), singleCellDimension.getBioAssaysOffset(), singleCellDimension.getNumberOfCells() );
    }

    /**
     * Obtain the sparse matrix underlying this.
     */
    public CompRowMatrix getMatrix() {
        return matrix;
    }

    @Override
    public ExpressionExperiment getExpressionExperiment() {
        return expressionExperiment;
    }

    @Override
    public int columns() {
        return matrix.numColumns();
    }

    @Override
    public Double get( int row, int column ) {
        return getAsDouble( row, column );
    }

    @Override
    public double getAsDouble( int row, int column ) {
        double result = matrix.get( row, column );
        if ( result == 0 && isCount && defaultCountValue != 0 ) {
            int j = Arrays.binarySearch( matrix.getColumnIndices(), matrix.getRowPointers()[row], matrix.getRowPointers()[row + 1], column );
            if ( j < 0 ) {
                result = defaultCountValue;
            }
        }
        return result;
    }

    @Override
    public Double[] getColumn( int column ) {
        return ArrayUtils.toObject( getColumnAsDoubles( column ) );
    }

    @Override
    public double[] getColumnAsDoubles( int column ) {
        double[] vec = new double[rows()];
        for ( int i = 0; i < vec.length; i++ ) {
            vec[i] = get( i, column );
        }
        return vec;
    }

    @Override
    public List<BioAssay> getBioAssays() {
        return bioAssays;
    }

    @Override
    public BioAssay getBioAssayForColumn( int j ) {
        return bioAssays.get( j );
    }

    @Override
    public BioMaterial getBioMaterialForColumn( int j ) {
        return bioAssays.get( j ).getSampleUsed();
    }

    @Override
    public List<String> getCellIds() {
        return singleCellDimension.getCellIds();
    }

    @Override
    public String getCellIdForColumn( int j ) {
        return singleCellDimension.getCellIds().get( j );
    }

    @Override
    public List<CompositeSequence> getDesignElements() {
        return designElements;
    }

    @Override
    public CompositeSequence getDesignElementForRow( int index ) {
        return designElements.get( index );
    }

    @Override
    public Double[] getRow( CompositeSequence designElement ) {
        return ArrayUtils.toObject( getRowAsDoubles( designElement ) );
    }

    @Override
    public double[] getRowAsDoubles( CompositeSequence designElement ) {
        int ix = getRowIndex( designElement );
        if ( ix == -1 ) {
            return null;
        }
        return getRowAsDoubles( ix );
    }

    @Override
    public Double[] getRow( int index ) {
        return ArrayUtils.toObject( getRowAsDoubles( index ) );
    }

    @Override
    public double[] getRowAsDoubles( int index ) {
        double[] vec = new double[matrix.numColumns()];
        Arrays.fill( vec, defaultCountValue );
        int[] rowptr = matrix.getRowPointers();
        int[] colind = matrix.getColumnIndices();
        double[] data = matrix.getData();
        for ( int i = rowptr[index]; i < rowptr[index + 1]; i++ ) {
            vec[colind[i]] = data[i];
        }
        return vec;
    }

    @Override
    public int getRowIndex( CompositeSequence designElement ) {
        return Math.max( Collections.binarySearch( designElements, designElement, designElementComparator ), -1 );
    }

    @Override
    public int[] getRowIndices( CompositeSequence designElement ) {
        int i = getRowIndex( designElement );
        if ( i == -1 ) {
            return null;
        }
        int j = i;
        while ( designElements.get( j ).equals( designElement ) ) {
            j++;
        }
        int[] indices = new int[j - i];
        for ( int k = i; k < j; k++ ) {
            indices[k - i] = k;
        }
        return indices;
    }

    @Override
    public List<ExpressionDataMatrixRowElement> getRowElements() {
        if ( rowElements == null ) {
            List<ExpressionDataMatrixRowElement> re = new ArrayList<>( designElements.size() );
            for ( int i = 0; i < designElements.size(); i++ ) {
                re.add( new ExpressionDataMatrixRowElement( this, i ) );
            }
            rowElements = Collections.unmodifiableList( re );
        }
        return rowElements;
    }

    @Override
    public ExpressionDataMatrixRowElement getRowElement( int row ) {
        if ( rowElements != null ) {
            return rowElements.get( row );
        } else if ( row >= 0 && row < rows() ) {
            return new ExpressionDataMatrixRowElement( this, row );
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    @Override
    public int rows() {
        return matrix.numRows();
    }

    @Override
    public QuantitationType getQuantitationType() {
        return quantitationType;
    }

    @Override
    public SingleCellDimension getSingleCellDimension() {
        return singleCellDimension;
    }

    @Override
    protected String format( int i, int j ) {
        return format( getAsDouble( i, j ) );
    }
}
