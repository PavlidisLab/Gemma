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

import java.util.Collection;

import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.genome.biosequence.BioSequence;

/**
 * Encapsulates information about the row 'label' for a ExpressionDataMatrix. Normal applications do not need to deal
 * with this very much (I hope).
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ExpressionDataMatrixRowElement {

    private BioSequence bioSequence;

    private Collection<DesignElement> designElements;

    private Integer index;

    @SuppressWarnings("unchecked")
    public ExpressionDataMatrixRowElement( ExpressionDataMatrix matrix, int i ) {
        this.index = i;
        this.designElements = matrix.getDesignElementsForRow( i );
        this.bioSequence = matrix.getBioSequenceForRow( i );
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
        if ( designElements.size() == 1 ) {
            return designElements.iterator().next().getName();
        } else {
            String buf = bioSequence.getName();
            buf = buf + ": ";
            for ( DesignElement de : designElements ) {
                buf = buf + de.getName() + ",";
            }
            return buf.replaceFirst( ",$", "" );
        }
    }

    public BioSequence getBioSequence() {
        return bioSequence;
    }

    public void setBioSequence( BioSequence bioSequence ) {
        this.bioSequence = bioSequence;
    }

    public Collection<DesignElement> getDesignElements() {
        return designElements;
    }

    public void setDesignElements( Collection<DesignElement> designElements ) {
        this.designElements = designElements;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex( Integer index ) {
        this.index = index;
    }

}
