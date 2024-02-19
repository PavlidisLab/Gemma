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

import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.List;

/**
 * Represents a matrix of data from an {@link ExpressionExperiment}.
 *
 * @author pavlidis
 * @author keshav
 */
public interface ExpressionDataMatrix<T> {

    /**
     * Return the expression experiment this matrix is holding data for.
     */
    ExpressionExperiment getExpressionExperiment();

    /**
     * Total number of columns.
     *
     * @return int
     */
    int columns();

    /**
     * Number of columns that use the given design element. Useful if the matrix includes data from more than one array
     * design.
     *
     * @param el el
     * @return int
     */
    int columns( CompositeSequence el );

    /**
     * Access a single value of the matrix. This is generally the easiest way to do it.
     *
     * @param row    row
     * @param column col
     * @return t
     */
    T get( int row, int column );

    /**
     * Access a single column of the matrix.
     *
     * @param column index
     * @return T[]
     */
    T[] getColumn( Integer column );

    /**
     * Obtain all the design elements in this data matrix.
     */
    List<CompositeSequence> getDesignElements();

    /**
     * @param index i
     * @return cs
     */
    CompositeSequence getDesignElementForRow( int index );

    /**
     * Access the entire matrix.
     *
     * @return T[][]
     */
    T[][] getRawMatrix();

    /**
     * Return a row that 'came from' the given design element.
     *
     * @param designElement de
     * @return t
     */
    T[] getRow( CompositeSequence designElement );

    /**
     * Access a single row of the matrix, by index. A complete row is returned.
     *
     * @param index i
     * @return t[]
     */
    T[] getRow( Integer index );

    /**
     * @return list of elements representing the row 'labels'.
     */
    List<ExpressionDataMatrixRowElement> getRowElements();

    int getRowIndex( CompositeSequence designElement );

    /**
     * Access a submatrix
     *
     * @param designElements de
     * @return T[][]
     */
    T[][] getRows( List<CompositeSequence> designElements );

    /**
     * @return true if any values are null or NaN (for Doubles); all other values are considered non-missing.
     */
    boolean hasMissingValues();

    /**
     * @return int
     */
    int rows();

    /**
     * Set a value in the matrix, by index
     *
     * @param row    row
     * @param column col
     * @param value  val
     */
    void set( int row, int column, T value );
}
