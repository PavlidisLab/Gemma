/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.model.expression.designElement;

import ubic.gemma.model.common.Describable;

/**
 * @see ubic.gemma.model.expression.designElement.CompositeSequence
 * @author pavlidis
 * @version $Id$
 */
public class CompositeSequenceImpl extends ubic.gemma.model.expression.designElement.CompositeSequence {

    /**
     * 
     */
    private static final long serialVersionUID = 2144914030275919838L;

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        Describable other = ( Describable ) obj;
        if ( getId() == null ) {
            if ( other.getId() != null ) return false;
        } else if ( !getId().equals( other.getId() ) ) return false;
        if ( getName() == null ) {
            if ( other.getName() != null ) return false;
        } else if ( !getName().equals( other.getName() ) ) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( getId() == null ) ? 0 : getId().hashCode() );
        result = prime * result + ( ( getName() == null ) ? 0 : getName().hashCode() );
        return result;
    }

}