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
package ubic.gemma.datastructure.matrix;

import java.util.Collection;
import java.util.List;

import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.genome.biosequence.BioSequence;

/**
 * Represents a matrix of data from an expression experiment.
 * 
 * @author pavlidis
 * @author keshav
 * @version $Id$
 */
public interface ExpressionDataMatrix<T> {

    /**
     * Return a row that 'came from' the given design element. NOTE that this might be only part of a row if the
     * experiment includes data from multiple array designs!
     * 
     * @param designElement
     * @return
     */
    public T[] getRow( DesignElement designElement );

    /**
     * Access a single row of the matrix, by index.
     * 
     * @param index
     * @return
     */
    public T[] getRow( Integer index );

    /**
     * Access a single column of the matrix.
     * 
     * @param bioAssay
     * @return T[]
     */
    public T[] getColumn( BioAssay bioAssay );

    /**
     * Access a single value of the matrix.
     * 
     * @param designElement
     * @param bioAssay
     * @return T
     */
    public T get( DesignElement designElement, BioAssay bioAssay );

    /**
     * Access a single value of the matrix.
     * 
     * @param designElement
     * @param bioMaterial
     * @return T
     */
    public T get( DesignElement designElement, BioMaterial bioMaterial );

    /**
     * @param row
     * @param column
     * @return
     */
    public T get( int row, int column );

    /**
     * Set a value in the matrix, by index
     * 
     * @param row
     * @param column
     * @param value
     */
    public void set( int row, int column, T value );

    /**
     * Set a value in the matrix
     * 
     * @param bioSequence
     * @param bioMaterial
     * @param value
     */
    public void set( BioSequence bioSequence, BioMaterial bioMaterial, T value );

    /**
     * Access a submatrix
     * 
     * @param designElements
     * @param bioAssays
     * @return T[][]
     */
    public T[][] get( List<DesignElement> designElements, List<BioAssay> bioAssays );

    /**
     * Access a submatrix
     * 
     * @param designElements
     * @return T[][]
     */
    public T[][] getRows( List<DesignElement> designElements );

    /**
     * Access a submatrix
     * 
     * @param bioAssays
     * @return T[][]
     */
    public T[][] getColumns( List<BioAssay> bioAssays );

    /**
     * Access the entire matrix.
     * 
     * @return T[][]
     */
    public T[][] getMatrix();

    /**
     * @return list (in index order) of elements representing the row 'labels'.
     */
    public List<ExpressionDataMatrixRowElement> getRowElements();

    /**
     * @param index
     * @return BioMaterial FIXME technically this can still be a collection. See bug 629
     */
    public BioMaterial getBioMaterialForColumn( int index );

    /**
     * @param index
     * @return
     */
    public BioSequence getBioSequenceForRow( int index );

    /**
     * @param index
     * @return
     */
    public Collection<DesignElement> getDesignElementsForRow( int index );

    /**
     * @param index
     * @return BioAssay
     */
    public Collection<BioAssay> getBioAssaysForColumn( int index );

    /**
     * Total number of columns.
     * 
     * @return int
     */
    public int columns();

    /**
     * Number of columns that use the given design element. Useful if the matrix includes data from more than one array
     * design.
     * 
     * @param el
     * @return
     */
    public int columns( DesignElement el );

    /**
     * @return int
     */
    public int rows();

}
