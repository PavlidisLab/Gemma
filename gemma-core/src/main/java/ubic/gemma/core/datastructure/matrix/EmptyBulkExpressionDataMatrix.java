package ubic.gemma.core.datastructure.matrix;

import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.Collections;
import java.util.List;

/**
 * An empty bulk expression data matrix.
 *
 * @author poirigui
 */
public class EmptyBulkExpressionDataMatrix extends AbstractBulkExpressionDataMatrix<Object> {

    private static final Object[][] EMPTY_MATRIX = new Object[0][0];
    private static final Object[] EMPTY_COLUMN = new Object[0];

    public EmptyBulkExpressionDataMatrix( ExpressionExperiment expressionExperiment, BioAssayDimension dimension, QuantitationType quantitationType ) {
        super( expressionExperiment, dimension, quantitationType, Collections.emptyList() );
    }

    @Override
    public boolean hasMissingValues() {
        return false;
    }

    @Override
    public Object[][] getMatrix() {
        return EMPTY_MATRIX;
    }

    @Override
    public BulkExpressionDataMatrix<Object> sliceColumns( List<BioMaterial> bioMaterials ) {
        if ( bioMaterials.isEmpty() ) {
            return this;
        }
        throw new IllegalArgumentException( "None of the requested samples are present in the matrix." );
    }

    @Override
    public EmptyBulkExpressionDataMatrix sliceColumns( List<BioMaterial> bioMaterials, BioAssayDimension dimension ) {
        if ( bioMaterials.isEmpty() ) {
            return this;
        }
        throw new IllegalArgumentException( "None of the requested samples are present in the matrix." );
    }

    @Override
    public Object[] getColumn( int column ) {
        if ( column >= 0 && column < columns() ) {
            return EMPTY_COLUMN;
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    @Override
    public Object[] getRow( int index ) {
        throw new IndexOutOfBoundsException();
    }

    @Override
    public ExpressionDataMatrix<Object> sliceRows( List<CompositeSequence> designElements ) {
        if ( designElements.isEmpty() ) {
            return this;
        }
        throw new IllegalArgumentException( "None of the requested design elements are present in the matrix." );
    }

    @Override
    public Object get( int row, int column ) {
        throw new IndexOutOfBoundsException();
    }

    @Override
    protected String format( int i, int j ) {
        throw new IndexOutOfBoundsException();
    }
}
