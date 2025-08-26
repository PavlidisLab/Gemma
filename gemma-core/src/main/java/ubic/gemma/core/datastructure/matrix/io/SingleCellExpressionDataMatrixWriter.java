package ubic.gemma.core.datastructure.matrix.io;

import ubic.gemma.core.datastructure.matrix.SingleCellExpressionDataMatrix;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

public interface SingleCellExpressionDataMatrixWriter extends ExpressionDataMatrixWriter<SingleCellExpressionDataMatrix<?>, SingleCellExpressionDataVector> {

    default int write( SingleCellExpressionDataMatrix<?> matrix, OutputStream stream ) throws IOException {
        return write( matrix, SingleCellExpressionDataVector.class, stream );
    }

    default int write( SingleCellExpressionDataMatrix<?> matrix, Writer writer ) throws IOException, UnsupportedOperationException {
        return write( matrix, SingleCellExpressionDataVector.class, writer );
    }
}
