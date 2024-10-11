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
    default void write( ExpressionDataMatrix<?> matrix, Writer writer ) throws IOException {
        Assert.isInstanceOf( BulkExpressionDataMatrix.class, matrix );
        write( ( BulkExpressionDataMatrix<?> ) matrix, writer );
    }

    void write( BulkExpressionDataMatrix<?> matrix, Writer writer ) throws IOException;

    @Override
    default void write( ExpressionDataMatrix<?> matrix, OutputStream stream ) throws IOException {
        Assert.isInstanceOf( BulkExpressionDataMatrix.class, matrix );
        write( ( BulkExpressionDataMatrix<?> ) matrix, stream );
    }

    default void write( BulkExpressionDataMatrix<?> matrix, OutputStream stream ) throws IOException {
        write( matrix, new OutputStreamWriter( stream ) );
    }
}
