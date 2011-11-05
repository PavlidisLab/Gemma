/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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

import ubic.gemma.model.expression.designElement.CompositeSequence;

/**
 * Encapsulates information about the row 'label' for a ExpressionDataMatrix. Normal applications do not need to deal
 * with this very much (I hope).
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ExpressionDataMatrixRowElement implements Comparable<ExpressionDataMatrixRowElement> {

    private CompositeSequence designElement;

    private Integer index;

    public ExpressionDataMatrixRowElement( ExpressionDataMatrix matrix, int i ) {
        this.index = i;
        this.designElement = matrix.getDesignElementForRow( i );
    }

    public ExpressionDataMatrixRowElement( ExpressionDataMatrixRowElement toCopy ) {
        this.index = toCopy.getIndex();
        this.designElement = toCopy.getDesignElement();
    }

    @Override
    public boolean equals( Object obj ) {
        if ( !( obj instanceof ExpressionDataMatrixRowElement ) ) return false;
        return this.index.equals( ( ( ExpressionDataMatrixRowElement ) obj ).getIndex() );
    }

    @Override
    public int hashCode() {
        return index.hashCode();
    }

    @Override
    public String toString() {
        return designElement.getName() + " " + designElement.getId();
    }

    public CompositeSequence getDesignElement() {
        return designElement;
    }

    public void setDesignElement( CompositeSequence designElement ) {
        this.designElement = designElement;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex( Integer index ) {
        this.index = index;
    }

    public int compareTo( ExpressionDataMatrixRowElement o ) {
        return o.getDesignElement().getName().compareTo( this.getDesignElement().getName() );
    }

}
