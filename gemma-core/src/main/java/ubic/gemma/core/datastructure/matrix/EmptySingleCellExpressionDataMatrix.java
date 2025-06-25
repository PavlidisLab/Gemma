package ubic.gemma.core.datastructure.matrix;

import ubic.gemma.model.util.SparseRangeArrayList;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.Collections;
import java.util.List;

public class EmptySingleCellExpressionDataMatrix implements SingleCellExpressionDataMatrix<Object> {

    private static final Object[] EMPTY_COLUMN = new Object[0];

    private final ExpressionExperiment expressionExperiment;
    private final SingleCellDimension dimension;
    private final QuantitationType quantitationType;
    private final List<BioAssay> bioAssays;

    public EmptySingleCellExpressionDataMatrix( ExpressionExperiment expressionExperiment, SingleCellDimension dimension, QuantitationType quantitationType ) {
        this.expressionExperiment = expressionExperiment;
        this.dimension = dimension;
        this.quantitationType = quantitationType;
        this.bioAssays = new SparseRangeArrayList<>( dimension.getBioAssays(), dimension.getBioAssaysOffset(), dimension.getNumberOfCells() );
    }

    @Override
    public ExpressionExperiment getExpressionExperiment() {
        return expressionExperiment;
    }

    @Override
    public QuantitationType getQuantitationType() {
        return quantitationType;
    }

    @Override
    public List<CompositeSequence> getDesignElements() {
        return Collections.emptyList();
    }

    @Override
    public CompositeSequence getDesignElementForRow( int index ) {
        throw new IndexOutOfBoundsException();
    }

    @Override
    public int columns() {
        return dimension.getNumberOfCells();
    }

    @Override
    public SingleCellDimension getSingleCellDimension() {
        return dimension;
    }

    @Override
    public Object[] getColumn( int column ) {
        if (column >= 0 && column < columns()) {
            return EMPTY_COLUMN;
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    @Override
    public int rows() {
        return 0;
    }

    @Override
    public Object[] getRow( int index ) {
        throw new IndexOutOfBoundsException();
    }

    @Override
    public Object[] getRow( CompositeSequence designElement ) {
        return null;
    }

    @Override
    public int getRowIndex( CompositeSequence designElement ) {
        return -1;
    }

    @Override
    public int[] getRowIndices( CompositeSequence designElement ) {
        return null;
    }

    @Override
    public List<ExpressionDataMatrixRowElement> getRowElements() {
        return Collections.emptyList();
    }

    @Override
    public ExpressionDataMatrixRowElement getRowElement( int row ) {
        throw new IndexOutOfBoundsException();
    }

    @Override
    public Object get( int row, int column ) {
        throw new IndexOutOfBoundsException();
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
        return dimension.getCellIds();
    }

    @Override
    public String getCellIdForColumn( int j ) {
        return dimension.getCellIds().get( j );
    }
}
