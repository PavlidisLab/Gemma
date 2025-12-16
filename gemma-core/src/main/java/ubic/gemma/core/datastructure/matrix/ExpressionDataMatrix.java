/*
 * The Gemma project
 *
 * Copyright (c) 2006 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.core.datastructure.matrix;

import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Represents a matrix of data from an {@link ExpressionExperiment}.
 * <p>
 * The rows of this matrix represent design elements.
 *
 * @param <T> type of scalar held in the matrix
 * @author pavlidis
 * @author keshav
 * @see BulkExpressionDataMatrix
 * @see SingleCellExpressionDataMatrix
 */
public interface ExpressionDataMatrix<T> {

    /**
     * Return the expression experiment this matrix is holding data for, if known.
     */
    @Nullable
    ExpressionExperiment getExpressionExperiment();

    /**
     * Obtain the quantitation type for this matrix.
     */
    QuantitationType getQuantitationType();

    /**
     * Obtain all the design elements in this data matrix.
     */
    List<CompositeSequence> getDesignElements();

    /**
     * Return a design element for a given index.
     *
     * @throws IndexOutOfBoundsException if the supplied index is not within zero and {@link #rows()}
     */
    CompositeSequence getDesignElementForRow( int index );

    /**
     * Obtain the total number of columns.
     */
    int columns();

    /**
     * Access a single column of the matrix.
     *
     * @param column index
     * @return T[]
     * @throws IndexOutOfBoundsException if the supplied index is not within zero and {@link #columns()}
     */
    T[] getColumn( int column );

    /**
     * @return int
     */
    int rows();

    /**
     * Access a single row of the matrix, by index. A complete row is returned.
     *
     * @param index i
     * @return t[]
     * @throws IndexOutOfBoundsException if the supplied index is not within zero and {@link #rows()}
     */
    T[] getRow( int index );

    /**
     * Return a row that 'came from' the given design element.
     *
     * @param designElement de
     * @return the corresponding row or null if the design element is not found in the matrix
     */
    @Nullable
    T[] getRow( CompositeSequence designElement );

    /**
     * @return the index for the given design element, or -1 if not found
     */
    int getRowIndex( CompositeSequence designElement );

    /**
     * Obtain all the rows that correspond to the given design element, or {@code null} if the design element is not
     * found.
     */
    @Nullable
    int[] getRowIndices( CompositeSequence designElement );

    /**
     * Slice the given design elements (rows) from the matrix.
     *
     * @throws IllegalArgumentException if any of the requested design element is not present in the matrix
     */
    ExpressionDataMatrix<T> sliceRows( List<CompositeSequence> designElements );

    /**
     * @return list of elements representing the row 'labels'.
     * @deprecated use {@link #getDesignElements()} instead
     */
    @Deprecated
    List<ExpressionDataMatrixRowElement> getRowElements();

    /**
     * @throws IndexOutOfBoundsException if the supplied index is not within zero and {@link #rows()}
     * @deprecated use {@link #getDesignElementForRow(int)} instead
     */
    @Deprecated
    ExpressionDataMatrixRowElement getRowElement( int row );

    /**
     * Access a single value of the matrix by row and column.
     *
     * @throws IndexOutOfBoundsException if either the row or column is outside the matrix bounds
     */
    T get( int row, int column );
}
