package ubic.gemma.core.datastructure.matrix.io;

import org.springframework.util.Assert;
import ubic.gemma.core.datastructure.matrix.ExpressionDataMatrix;
import ubic.gemma.core.datastructure.matrix.SingleCellExpressionDataMatrix;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

public interface SingleCellExpressionDataMatrixWriter extends ExpressionDataMatrixWriter {

    @Override
    default int write( ExpressionDataMatrix<?> matrix, Writer writer ) throws IOException {
        Assert.isInstanceOf( SingleCellExpressionDataMatrix.class, matrix );
        return write( ( SingleCellExpressionDataMatrix<?> ) matrix, writer );
    }

    int write( SingleCellExpressionDataMatrix<?> matrix, Writer writer ) throws IOException;

    @Override
    default int write( ExpressionDataMatrix<?> matrix, OutputStream stream ) throws IOException {
        Assert.isInstanceOf( SingleCellExpressionDataMatrix.class, matrix );
        return write( ( SingleCellExpressionDataMatrix<?> ) matrix, stream );
    }

    default int write( SingleCellExpressionDataMatrix<?> matrix, OutputStream stream ) throws IOException {
        return write( matrix, new OutputStreamWriter( stream, StandardCharsets.UTF_8 ) );
    }
}
