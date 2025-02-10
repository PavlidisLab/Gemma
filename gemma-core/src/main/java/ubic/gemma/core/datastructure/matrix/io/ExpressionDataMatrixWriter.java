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
public interface ExpressionDataMatrixWriter {

    /**
     * Set the scale type to use when writing the matrix.
     * <p>
     * When null, the original values must be used.
     */
    void setScaleType( @Nullable ScaleType scaleType );

    int write( ExpressionDataMatrix<?> matrix, Writer writer ) throws IOException, UnsupportedOperationException;

    default int write( ExpressionDataMatrix<?> matrix, OutputStream stream ) throws IOException {
        return write( matrix, new OutputStreamWriter( stream, StandardCharsets.UTF_8 ) );
    }
}
