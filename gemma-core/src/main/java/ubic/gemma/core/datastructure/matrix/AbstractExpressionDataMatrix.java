package ubic.gemma.core.datastructure.matrix;

import java.text.NumberFormat;

public abstract class AbstractExpressionDataMatrix<T> implements ExpressionDataMatrix<T> {

    private static final int MAX_ROWS_TO_STRING = 200;
    private static final int MAX_COLS_TO_STRING = 20;

    private static final NumberFormat nf = NumberFormat.getInstance();

    static {
        nf.setMaximumFractionDigits( 4 );
    }

    @Override
    public String toString() {
        int columns = this.columns();
        int rows = this.rows();

        StringBuilder buf = new StringBuilder();
        buf.append( rows ).append( " x " ).append( columns )
                .append( " matrix of " ).append( formatRepresentation() ).append( " values" );
        if ( rows > MAX_ROWS_TO_STRING ) {
            buf.append( ", showing up to " ).append( MAX_ROWS_TO_STRING ).append( " rows" );
        }
        if ( columns > MAX_COLS_TO_STRING ) {
            if ( rows > MAX_ROWS_TO_STRING ) {
                buf.append( " and " );
            } else {
                buf.append( ", showing up to " );
            }
            buf.append( MAX_COLS_TO_STRING ).append( " columns" );

        }
        buf.append( "\n" );

        for ( int i = 0; i < columns; i++ ) {
            buf.append( "\t" ).append( getColumnLabel( i ) );
            if ( i == MAX_COLS_TO_STRING ) {
                buf.append( "\tStopping after " + MAX_COLS_TO_STRING + " columns..." );
                break;
            }
        }
        buf.append( "\n" );

        for ( int j = 0; j < rows; j++ ) {
            buf.append( getRowLabel( j ) );
            for ( int i = 0; i < columns; i++ ) {
                buf.append( "\t" ).append( format( j, i ) );
                if ( i == MAX_COLS_TO_STRING ) {
                    buf.append( "\t..." );
                    break;
                }
            }
            buf.append( "\n" );
            if ( j == MAX_ROWS_TO_STRING ) {
                buf.append( "\nStopping after " + MAX_ROWS_TO_STRING + " rows..." );
                break;
            }
        }

        return buf.toString();
    }

    /**
     * Produce a string representation of the type of values held in the matrix.
     */
    protected String formatRepresentation() {
        return getQuantitationType().getRepresentation().name().toLowerCase();
    }

    /**
     * Obtain a label suitable for describing a row of the matrix.
     */
    protected abstract String getRowLabel( int i );

    /**
     * Obtain a label suitable for describing a column of the matrix.
     */
    protected abstract String getColumnLabel( int j );

    /**
     * Format a matrix entry at a particular row/column.
     */
    protected abstract String format( int i, int j );

    protected String format( double val ) {
        if ( Double.isNaN( val ) ) {
            return String.valueOf( val );
        } else {
            synchronized ( nf ) {
                return nf.format( val );
            }
        }
    }

    protected String format( int val ) {
        return String.valueOf( val );
    }
}
