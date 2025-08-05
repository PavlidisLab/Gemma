package ubic.gemma.core.datastructure.matrix.io;

import ubic.gemma.core.datastructure.matrix.ExpressionDataMatrix;
import ubic.gemma.model.common.quantitationtype.ScaleType;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/**
 * Base interface for writing {@link ExpressionDataMatrix}.
 * @author poirigui
 */
public interface ExpressionDataMatrixWriter<T extends ExpressionDataMatrix<?>> {

    /**
     * Flush every time a complete line is written.
     * <p>
     * This is not very efficient, but it can be used to view the output of a matrix as it is being written.
     */
    void setAutoFlush( boolean autoFlush );

    /**
     * Set the scale type to use when writing the matrix.
     * <p>
     * When null, the original values must be used.
     */
    void setScaleType( @Nullable ScaleType scaleType );

    /**
     * Write the matrix to the given writer.
     * @return the number of vectors written
     * @throws UnsupportedOperationException if the matrix cannot be written to a text output (i.e. if this is a binary
     * format).
     */
    int write( T matrix, Writer writer ) throws IOException, UnsupportedOperationException;

    /**
     * Write the matrix to the given output stream.
     * @return the number of vectors written
     */
    default int write( T matrix, OutputStream stream ) throws IOException {
        return write( matrix, new OutputStreamWriter( stream, StandardCharsets.UTF_8 ) );
    }
}
