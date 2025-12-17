package ubic.gemma.core.datastructure.matrix.io;

import ubic.gemma.core.datastructure.matrix.BulkExpressionDataMatrix;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.expression.bioAssayData.BulkExpressionDataVector;
import ubic.gemma.model.genome.biosequence.BioSequence;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Writer;

/**
 * Writes a {@link BulkExpressionDataMatrix} to a JSON format.
 * @author paul
 */
public class JsonMatrixWriter implements BulkExpressionDataMatrixWriter {

    @Override
    public void setAutoFlush( boolean autoFlush ) {
        throw new UnsupportedOperationException( "The JSON matrix writer does not support auto-flushing." );
    }

    @Override
    public void setScaleType( @Nullable ScaleType scaleType ) {
        throw new UnsupportedOperationException( " The JSON matrix writer does not support setting scale type." );
    }

    @Override
    public int write( BulkExpressionDataMatrix<?> matrix, Class<? extends BulkExpressionDataVector> vectorType, Writer writer ) throws IOException, UnsupportedOperationException {
        int columns = matrix.columns();
        int rows = matrix.rows();

        StringBuilder buf = new StringBuilder();

        buf.append( "{ 'numRows' : " ).append( matrix.rows() ).append( ", 'rows': " );

        buf.append( "[" );

        for ( int j = 0; j < rows; j++ ) {

            if ( j > 0 )
                buf.append( "," );
            buf.append( "{" );
            buf.append( "'id' : \"" ).append( matrix.getDesignElementForRow( j ).getName() ).append( "\"" );
            BioSequence biologicalCharacteristic = matrix.getDesignElementForRow( j ).getBiologicalCharacteristic();
            if ( biologicalCharacteristic != null )
                buf.append( ", 'sequence' : \"" ).append( biologicalCharacteristic.getName() ).append( "\"" );

            buf.append( ", 'data' : [" );
            for ( int i = 0; i < columns; i++ ) {
                Object val = matrix.get( j, i );
                if ( i > 0 )
                    buf.append( "," );
                buf.append( val );
            }

            buf.append( "]}\n" );
        }
        buf.append( "\n]}" );
        writer.write( buf.toString() );
        return rows;
    }
}
