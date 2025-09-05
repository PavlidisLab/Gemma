package ubic.gemma.core.loader.expression.singleCell;

import no.uib.cipr.matrix.sparse.CompRowMatrix;

import java.util.Arrays;

/**
 * Utilities for {@link CompRowMatrix}.
 * @author poirigui
 */
public class CompRowMatrixUtils {

    /**
     * Select the specified rows from a compressed row matrix.
     */
    public static CompRowMatrix selectRows( CompRowMatrix matrix, int[] rows ) {
        int numRows = rows.length;
        int numColumns = matrix.numColumns();

        for ( int row : rows ) {
            if ( row < 0 || row >= matrix.numRows() ) {
                throw new IndexOutOfBoundsException( "Row index " + row + " is out of bounds for matrix with " + matrix.numRows() + " rows." );
            }
        }

        int[] rowPointers = matrix.getRowPointers();
        int[] columnIndices = matrix.getColumnIndices();
        double[] data = matrix.getData();

        int[][] nzColumns = new int[numRows][];
        double[][] nzData = new double[numRows][];
        for ( int i = 0; i < numRows; i++ ) {
            int row = rows[i];
            nzColumns[i] = Arrays.copyOfRange( columnIndices, rowPointers[row], rowPointers[row + 1] );
            nzData[i] = Arrays.copyOfRange( data, rowPointers[row], rowPointers[row + 1] );
        }
        return createMatrix( numRows, numColumns, nzColumns, nzData );
    }

    /**
     * Select the specified columns from a compressed row matrix.
     * <p>
     * If the requested columns are sorted, the method will perform a more efficient binary search by narrowing down the
     * range for locating columns. If in addition, the requested columns are unique, this method will be able to finish
     * early when it reaches the end of the search range.
     */
    public static CompRowMatrix selectColumns( CompRowMatrix matrix, int[] columns ) {
        int numRows = matrix.numRows();
        int numColumns = columns.length;

        boolean isSorted = true;
        boolean isUnique = true;
        int lastCol = -1;
        for ( int column : columns ) {
            if ( column < 0 || column >= matrix.numColumns() ) {
                throw new IndexOutOfBoundsException( "Column index " + column + " is out of bounds for matrix with " + matrix.numColumns() + " columns." );
            }
            if ( column < lastCol ) {
                isSorted = false;
            }
            if ( column == lastCol ) {
                isUnique = false;
            }
            lastCol = column;
        }

        int[] rowPointers = matrix.getRowPointers();
        int[] columnIndices = matrix.getColumnIndices();
        double[] data = matrix.getData();

        // rewrite the column indices to account for discarded empty cells
        int[][] nzColumns = new int[numRows][];
        double[][] nzData = new double[numRows][];

        // shared cache for column positions
        int[] columnPositions = new int[numColumns];
        int[] columnNewPositions = new int[numColumns];

        for ( int i = 0; i < numRows; i++ ) {
            // check which requested columns are present in this row
            int nnzForRow = 0;
            int start = rowPointers[i];
            int end = rowPointers[i + 1];
            for ( int j = 0; j < numColumns; j++ ) {
                int column = columns[j];
                int pos = Arrays.binarySearch( columnIndices, start, end, column );
                if ( pos >= 0 ) {
                    columnPositions[nnzForRow] = pos;
                    columnNewPositions[nnzForRow] = j;
                    nnzForRow++;
                    // when requested columns are sorted, we can narrow down the binary search range
                    if ( isSorted ) {
                        if ( isUnique ) {
                            // if the requested columns are unique as well, we can skip to the next column and even
                            // finish early
                            if ( pos < end - 1 ) {
                                start = pos + 1;
                            } else {
                                break; // we reached the end of the range, we can skip the remaining columns
                            }
                        } else {
                            start = pos;
                        }
                    }
                }
            }
            nzColumns[i] = new int[nnzForRow];
            nzData[i] = new double[nnzForRow];
            for ( int k = 0; k < nnzForRow; k++ ) {
                nzColumns[i][k] = columnNewPositions[k];
                nzData[i][k] = data[columnPositions[k]];
            }
        }
        return createMatrix( numRows, numColumns, nzColumns, nzData );
    }

    private static CompRowMatrix createMatrix( int numRows, int numColumns, int[][] nzColumns, double[][] nzData ) {
        CompRowMatrix matrix = new CompRowMatrix( numRows, numColumns, nzColumns );
        for ( int i = 0; i < nzColumns.length; i++ ) {
            for ( int j = 0; j < nzColumns[i].length; j++ ) {
                matrix.set( i, nzColumns[i][j], nzData[i][j] );
            }
        }
        return matrix;
    }
}
