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
package ubic.gemma.core.datastructure.matrix;

import ubic.gemma.model.expression.designElement.CompositeSequence;

/**
 * Encapsulates information about the row 'label' for a ExpressionDataMatrix. Normal applications do not need to deal
 * with this very much (I hope).
 *
 * @author pavlidis
 */
@Deprecated
public class ExpressionDataMatrixRowElement implements Comparable<ExpressionDataMatrixRowElement> {

    private final CompositeSequence designElement;
    private final int index;

    public ExpressionDataMatrixRowElement( ExpressionDataMatrix<?> matrix, int i ) {
        this.designElement = matrix.getDesignElementForRow( i );
        this.index = i;
    }

    public ExpressionDataMatrixRowElement( ExpressionDataMatrixRowElement toCopy ) {
        this.designElement = toCopy.getDesignElement();
        this.index = toCopy.getIndex();
    }

    @Override
    public int compareTo( ExpressionDataMatrixRowElement o ) {
        return o.getDesignElement().getName().compareTo( this.getDesignElement().getName() );
    }

    public CompositeSequence getDesignElement() {
        return designElement;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode( index );
    }

    @Override
    public boolean equals( Object obj ) {
        return obj instanceof ExpressionDataMatrixRowElement
                && this.index == ( ( ( ExpressionDataMatrixRowElement ) obj ).getIndex() );
    }

    @Override
    public String toString() {
        return designElement.getName() + " " + designElement.getId();
    }
}
