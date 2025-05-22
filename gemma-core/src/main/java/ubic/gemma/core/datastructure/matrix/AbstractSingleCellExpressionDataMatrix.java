package ubic.gemma.core.datastructure.matrix;

public abstract class AbstractSingleCellExpressionDataMatrix<T> extends AbstractExpressionDataMatrix<T> implements SingleCellExpressionDataMatrix<T> {

    @Override
    protected String getRowLabel( int i ) {
        return getDesignElementForRow( i ).getName();
    }

    @Override
    protected String getColumnLabel( int j ) {
        return getBioAssayForColumn( j ).getName() + ":" + getCellIdForColumn( j );
    }
}
