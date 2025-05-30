package ubic.gemma.core.datastructure;

import org.springframework.util.Assert;

import javax.annotation.Nullable;
import java.util.Arrays;

/**
 * A quasi-universal matrix mask.
 */
public class MatrixMask {
    /**
     * Mask individual elements of the given matrix.
     */
    public static MatrixMask maskElements( int rows, int cols, boolean[][] mask ) {
        return new MatrixMask( rows, cols, null, null, mask, null, false );
    }

    /**
     * Mask individual elements of the given matrix at the specified coordinates.
     * <p>
     * This uses a sparse representation internally, which is more efficient for large matrices with few masked
     * elements.
     */
    public static MatrixMask maskElements( int rows, int cols, int[] i, int[] j ) {
        return new MatrixMask( rows, cols, null, null, null, makeSparseMask( i, j, rows, cols ), false );
    }

    private static int[] makeSparseMask( int[] i, int[] j, int nrows, int ncols ) {
        int[] sm = new int[i.length];
        for ( int k = 0; k < i.length; k++ ) {
            if ( i[k] < 0 || i[k] >= nrows ) {
                throw new IndexOutOfBoundsException( "Row index out of bounds: " + j[k] );
            }
            if ( j[k] < 0 || j[k] >= ncols ) {
                throw new IndexOutOfBoundsException( "Column index out of bounds: " + j[k] );
            }
            sm[k] = i[k] * ncols + j[k];
        }
        Arrays.sort( sm );
        return sm;
    }

    /**
     * Mask whole rows of the given matrix.
     */
    public static MatrixMask maskRows( int rows, int cols, boolean[] rowMask ) {
        return new MatrixMask( rows, cols, rowMask, null, null, null, false );
    }

    /**
     * Mask whole columns of the given matrix.
     */
    public static MatrixMask maskColumns( int rows, int cols, boolean[] columnMask ) {
        return new MatrixMask( rows, cols, null, columnMask, null, null, false );
    }

    private final int rows, cols;

    /**
     * Mask whole rows.
     */
    @Nullable
    private final boolean[] rowMask;
    /**
     * Mask whole columns.
     */
    @Nullable
    private final boolean[] columnMask;
    /**
     * Mask individual elements.
     */
    @Nullable
    private final boolean[][] mask;
    /**
     * Mask individual indices.
     */
    @Nullable
    private final int[] sparseMask;
    /**
     * Indicate if the mask is inverted.
     */
    private final boolean inverted;

    private MatrixMask( int rows, int cols, @Nullable boolean[] rowMask, @Nullable boolean[] columnMask, @Nullable boolean[][] mask, @Nullable int[] sparseMask, boolean inverted ) {
        Assert.isTrue( rowMask == null || rows == rowMask.length, "Row mask must have the same number of rows as the matrix." );
        Assert.isTrue( columnMask == null || cols == columnMask.length, "Column mask must have the same number of columns as the matrix." );
        Assert.isTrue( mask == null || rows == mask.length, "Mask must have the same number of rows as the matrix." );
        Assert.isTrue( mask == null || mask.length == 0 || cols == mask[1].length, "Mask must have the same number of columns as the matrix." );
        this.rows = rows;
        this.cols = cols;
        this.rowMask = rowMask;
        this.columnMask = columnMask;
        this.mask = mask;
        this.sparseMask = sparseMask;
        this.inverted = inverted;
    }

    /**
     * Indicate if this mask has any column-wise masking.
     */
    public boolean hasColumnMask() {
        return columnMask != null;
    }

    /**
     * Indicate if this mask has any row-wise masking.
     */
    public boolean hasRowMask() {
        return rowMask != null;
    }

    /**
     * Indicate if this mask has any element-wise masking.
     */
    public boolean hasElementMask() {
        return mask != null || sparseMask != null;
    }

    public boolean isRowMasked( int row ) {
        return rowMask != null && rowMask[row] ^ inverted;
    }

    public boolean isColumnMasked( int column ) {
        return columnMask != null && columnMask[column] ^ inverted;
    }

    public boolean isMasked( int row, int column ) {
        if ( isRowMasked( row ) ) {
            return true;
        }
        if ( isColumnMasked( column ) ) {
            return true;
        }
        if ( mask != null && mask[row][column] ^ inverted ) {
            return true;
        }
        if ( sparseMask != null && ( Arrays.binarySearch( sparseMask, row * cols + column ) >= 0 ) ^ inverted ) {
            return true;
        }
        return false;
    }

    /**
     * Invert the mask of this matrix, allowing access to the previously masked elements.
     */
    public MatrixMask inverted() {
        return new MatrixMask( rows, cols, rowMask, columnMask, mask, sparseMask, !inverted );
    }
}
