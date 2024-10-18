package ubic.gemma.core.datastructure.matrix.io;

import org.springframework.util.Assert;
import ubic.gemma.core.datastructure.matrix.BulkExpressionDataMatrix;
import ubic.gemma.core.datastructure.matrix.ExpressionDataMatrix;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

public interface BulkExpressionDataMatrixWriter extends ExpressionDataMatrixWriter {

    @Override
    default int write( ExpressionDataMatrix<?> matrix, Writer writer ) throws IOException {
        Assert.isInstanceOf( BulkExpressionDataMatrix.class, matrix );
        return write( ( BulkExpressionDataMatrix<?> ) matrix, writer );
    }

    int write( BulkExpressionDataMatrix<?> matrix, Writer writer ) throws IOException;

    @Override
    default int write( ExpressionDataMatrix<?> matrix, OutputStream stream ) throws IOException {
        Assert.isInstanceOf( BulkExpressionDataMatrix.class, matrix );
        return write( ( BulkExpressionDataMatrix<?> ) matrix, stream );
    }

    default int write( BulkExpressionDataMatrix<?> matrix, OutputStream stream ) throws IOException {
        return write( matrix, new OutputStreamWriter( stream ) );
    }
}
