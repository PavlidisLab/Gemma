package ubic.gemma.core.datastructure.matrix;

import no.uib.cipr.matrix.sparse.CompRowMatrix;
import org.springframework.util.Assert;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.util.ByteArrayUtils;

import java.util.*;

/**
 * @author poirigui
 */
public class DoubleSingleCellExpressionDataMatrix implements SingleCellExpressionDataMatrix<Double> {

    private static final Comparator<CompositeSequence> designElementComparator = Comparator.comparing( CompositeSequence::getName )
            .thenComparing( CompositeSequence::getId );

    private final ExpressionExperiment expressionExperiment;
    private final QuantitationType quantitationType;
    private final SingleCellDimension singleCellDimension;
    private final CompRowMatrix matrix;
    private final List<CompositeSequence> designElements;

    public DoubleSingleCellExpressionDataMatrix( Collection<SingleCellExpressionDataVector> vectors ) {
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
            double[] row = ByteArrayUtils.byteArrayToDoubles( v.getData() );
            int[] indices = v.getDataIndices();
            for ( int j = 0; j < row.length; j++ ) {
                matrix.set( i, indices[j], row[j] );
            }
            i++;
        }
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
        return matrix.get( row, column );
    }

    @Override
    public Double[] getColumn( int column ) {
        Double[] vec = new Double[matrix.numRows()];
        for ( int j = 0; j < matrix.numRows(); j++ ) {
            vec[j] = matrix.get( j, column );
        }
        return vec;
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
        int ix = getRowIndex( designElement );
        if ( ix == -1 ) {
            return null;
        }
        return getRow( ix );
    }

    @Override
    public Double[] getRow( int index ) {
        Double[] vec = new Double[matrix.numColumns()];
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

}
