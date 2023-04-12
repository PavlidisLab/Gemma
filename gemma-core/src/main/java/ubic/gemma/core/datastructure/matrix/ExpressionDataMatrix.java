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
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.Collection;
import java.util.List;

/**
 * Represents a matrix of data from an expression experiment.
 * Expression data is rather complex, so we have to handle some messy cases.
 * The key problem is how to unambiguously identify rows and columns in the matrix. This is greatly complicated by the
 * fact that experiments can combine data from multiple array designs in various ways.
 * Put it together, and the result is that there can be more than one BioAssay per column; the same BioMaterial can be
 * used in multiple columns (supported implictly). There can also be more than on BioMaterial in one column (we don't
 * support this yet either). The same BioSequence can be found in multiple rows. A row can contain data from more than
 * one DesignElement.
 * There are a few constraints: a particular DesignElement can only be used once, in a single row. At the moment we do
 * not directly support technical replicates, though this should be possible. A BioAssay can only appear in a single
 * column.
 * For some operations a ExpressionDataMatrixRowElement object is offered, which encapsulates a combination of
 * DesignElements, a BioSequence, and an index. The list of these can be useful for iterating over the rows of the
 * matrix.
 *
 * @author pavlidis
 * @author keshav
 */
@SuppressWarnings("unused") // Possible external use
public interface ExpressionDataMatrix<T> {

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
     * Access a single value of the matrix. Note that because there can be multiple bioassays per column and multiple
     * designelements per row, it is possible for this method to retrieve a data that does not come from the bioassay
     * and/or designelement arguments.
     *
     * @param designElement de
     * @param bioAssay      ba
     * @return T t
     */
    T get( CompositeSequence designElement, BioAssay bioAssay );

    /**
     * Access a single value of the matrix. This is generally the easiest way to do it.
     *
     * @param row    row
     * @param column col
     * @return t
     */
    T get( int row, int column );

    /**
     * Access a submatrix
     *
     * @param designElements de
     * @param bioAssays      bas
     * @return T[][]
     */
    T[][] get( List<CompositeSequence> designElements, List<BioAssay> bioAssays );

    /**
     * @return The bioassaydimension that covers all the biomaterials in this matrix.
     * @throws IllegalStateException if there isn't a single bioassaydimension that encapsulates all the biomaterials
     *                               used in the experiment.
     */
    BioAssayDimension getBestBioAssayDimension();

    /**
     * Produce a BioAssayDimension representing the matrix columns for a specific row. The designelement argument is
     * needed because a matrix can combine data from multiple array designs, each of which will generate its own
     * bioassaydimension. Note that if this represents a subsetted data set, the return value may be a lightweight
     * 'fake'.
     *
     * @param designElement de
     * @return bad
     */
    BioAssayDimension getBioAssayDimension( CompositeSequence designElement );

    /**
     * @param index i
     * @return bioassays that contribute data to the column. There can be multiple bioassays if more than one array was
     * used in the study.
     */
    Collection<BioAssay> getBioAssaysForColumn( int index );

    /**
     * @param index i
     * @return BioMaterial. Note that if this represents a subsetted data set, the BioMaterial may be a lightweight
     * 'fake'.
     */
    BioMaterial getBioMaterialForColumn( int index );

    /**
     * Access a single column of the matrix.
     *
     * @param bioAssay i
     * @return T[]
     */
    T[] getColumn( BioAssay bioAssay );

    /**
     * Access a single column of the matrix.
     *
     * @param column index
     * @return T[]
     */
    T[] getColumn( Integer column );

    /**
     * @param bioMaterial bm
     * @return the index of the column for the data for the bioMaterial.
     */
    int getColumnIndex( BioMaterial bioMaterial );

    /**
     * Access a submatrix slice by columns
     *
     * @param bioAssays ba
     * @return t[][]
     */
    T[][] getColumns( List<BioAssay> bioAssays );

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
     * Return the expression experiment this matrix is holding data for.
     *
     * @return ee
     */
    ExpressionExperiment getExpressionExperiment();

    /**
     * Return the quantitation types for this matrix. Often (usually) there will be just one.
     *
     * @return qts
     */
    Collection<QuantitationType> getQuantitationTypes();

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
     * Access a submatrix
     * @param rowIndices of integers to select
     * @return T[][] or null if rowIndices is null or empty.
     */
    T[][] getRows( Collection<Integer> rowIndices);


    /**
     * Convenience function to locate the indices with an (exact match) CompositeSequence.name
     *
     * @param name The CompositeSequence name to look for
     * @return array of row indices matching (usually will just be one value)
     */
    Collection<Integer> findRowsByName(String name);


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
