package ubic.gemma.core.datastructure.matrix;

import no.uib.cipr.matrix.sparse.CompRowMatrix;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.util.Assert;
import ubic.gemma.core.datastructure.sparse.SparseRangeArrayList;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;
import java.util.*;

public class SingleCellExpressionDataIntMatrix extends AbstractSingleCellExpressionDataMatrix<Integer> implements ExpressionDataPrimitiveIntMatrix {

    private static final Comparator<CompositeSequence> designElementComparator = Comparator.comparing( CompositeSequence::getName )
            .thenComparing( CompositeSequence::getId, Comparator.nullsLast( Comparator.naturalOrder() ) );

    private final ExpressionExperiment expressionExperiment;
    private final QuantitationType quantitationType;
    private final SingleCellDimension singleCellDimension;
    private final CompRowMatrix matrix;
    private final List<CompositeSequence> designElements;
    private final List<BioAssay> bioAssays;

    /**
     * Row elements, only computed on-demand.
     */
    @Nullable
    private List<ExpressionDataMatrixRowElement> rowElements;

    public SingleCellExpressionDataIntMatrix( Collection<SingleCellExpressionDataVector> vectors ) {
        Assert.isTrue( !vectors.isEmpty(), "At least one vector must be supplied. Use EmptyExpressionDataMatrix for empty data matrices instead." );
        Assert.isTrue( vectors.stream().map( SingleCellExpressionDataVector::getQuantitationType ).distinct().count() == 1,
                "All vectors must have the same quantitation type." );
        Assert.isTrue( vectors.stream().map( SingleCellExpressionDataVector::getSingleCellDimension ).distinct().count() == 1,
                "All vectors must have the same single-cell dimension." );
        SingleCellExpressionDataVector vector = vectors.iterator().next();
        Assert.isTrue( vector.getQuantitationType().getRepresentation().equals( PrimitiveType.INT ),
                "Vectors must use the " + PrimitiveType.INT + " representation." );
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
        matrix = new CompRowMatrix( rows, singleCellDimension.getNumberOfCellIds(), nz );
        designElements = new ArrayList<>( sortedVectors.size() );
        i = 0;
        for ( SingleCellExpressionDataVector v : sortedVectors ) {
            designElements.add( v.getDesignElement() );
            int[] row = v.getDataAsInts();
            int[] indices = v.getDataIndices();
            for ( int j = 0; j < row.length; j++ ) {
                matrix.set( i, indices[j], row[j] );
            }
            i++;
        }
        bioAssays = new SparseRangeArrayList<>( singleCellDimension.getBioAssays(), singleCellDimension.getBioAssaysOffset(), singleCellDimension.getNumberOfCellIds() );
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
    public Integer get( int row, int column ) {
        return getAsInt( row, column );
    }

    @Override
    public int getAsInt( int row, int column ) {
        return ( int ) Math.rint( matrix.get( row, column ) );
    }

    @Override
    public Integer[] getColumn( int column ) {
        return ArrayUtils.toObject( getColumnAsInts( column ) );
    }

    @Override
    public int[] getColumnAsInts( int column ) {
        int[] vec = new int[rows()];
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
    public Integer[] getRow( CompositeSequence designElement ) {
        return ArrayUtils.toObject( getRowAsInts( designElement ) );
    }

    @Override
    public int[] getRowAsInts( CompositeSequence designElement ) {
        int ix = getRowIndex( designElement );
        if ( ix == -1 ) {
            return null;
        }
        return getRowAsInts( ix );
    }

    @Override
    public Integer[] getRow( int index ) {
        return ArrayUtils.toObject( getRowAsInts( index ) );
    }

    @Override
    public int[] getRowAsInts( int index ) {
        int[] vec = new int[matrix.numColumns()];
        Arrays.fill( vec, 0 );
        int[] rowptr = matrix.getRowPointers();
        int[] colind = matrix.getColumnIndices();
        double[] data = matrix.getData();
        for ( int i = rowptr[index]; i < rowptr[index + 1]; i++ ) {
            vec[colind[i]] = ( int ) Math.rint( data[i] );
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
    public ExpressionDataMatrix<Integer> sliceRows( List<CompositeSequence> designElements ) {
        throw new UnsupportedOperationException( "Slicing single-cell integer matrices is not supported." );
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
        } else {
            return new ExpressionDataMatrixRowElement( this, row );
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
        return format( getAsInt( i, j ) );
    }
}
