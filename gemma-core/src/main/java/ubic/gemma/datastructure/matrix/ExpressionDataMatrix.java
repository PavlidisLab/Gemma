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
import ubic.gemma.model.expression.designElement.DesignElement;

/**
 * Represents a matrix of data from an expression experiment.
 * 
 * @author pavlidis
 * @version $Id$
 */
public interface ExpressionDataMatrix<T> {

    /**
     * Access a single row of the matrix.
     * 
     * @param designElement
     * @return
     */
    public T[] getRow( DesignElement designElement );

    /**
     * Access a single column of the matrix.
     * 
     * @param bioAssay
     * @return
     */
    public T[] getColumn( BioAssay bioAssay );

    /**
     * Access a single value of the matrix.
     * 
     * @param designElement
     * @param bioAssay
     * @return
     */
    public T get( DesignElement designElement, BioAssay bioAssay );

    /**
     * Access a submatrix
     * 
     * @param designElements
     * @param bioAssays
     * @return
     */
    public T[][] get( List<DesignElement> designElements, List<BioAssay> bioAssays );

    /**
     * Access a submatrix
     * 
     * @param designElements
     * @return
     */
    public T[][] getRows( List<DesignElement> designElements );

    /**
     * Access a submatrix
     * 
     * @param bioAssays
     * @return
     */
    public T[][] getColumns( List<BioAssay> bioAssays );

    /**
     * Access the entire matrix.
     * 
     * @return
     */
    public T[][] getMatrix();

    /**
     * Gets all the design elements in the matrix.
     * 
     * @return Map<DesignElement,Integer>
     */
    public Collection<DesignElement> getRowMap();

    /**
     * Gets all the bioassays in the matrix.
     * 
     * @return Map<BioAssay,Integer>
     */
    public Collection<BioAssay> getColumnMap();

    public int columns();

    public int rows();

}
