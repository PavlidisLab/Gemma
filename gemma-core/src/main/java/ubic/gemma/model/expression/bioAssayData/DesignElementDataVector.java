/*
 * The Gemma project.
 *
 * Copyright (c) 2006-2012 University of British Columbia
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
package ubic.gemma.model.expression.bioAssayData;

import lombok.Getter;
import lombok.Setter;
import ubic.gemma.model.expression.designElement.CompositeSequence;

import java.util.Objects;

/**
 * Data vector associated to a {@link CompositeSequence}.
 */
@Getter
@Setter
public abstract class DesignElementDataVector extends DataVector {

    private static final long serialVersionUID = -4185333066166517308L;

    private CompositeSequence designElement;

    @Override
    public int hashCode() {
        return Objects.hash( super.hashCode(), designElement );
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object )
            return true;
        if ( !( object instanceof DesignElementDataVector ) )
            return false;
        DesignElementDataVector other = ( DesignElementDataVector ) object;
        if ( getId() != null && other.getId() != null ) {
            return Objects.equals( getId(), other.getId() );
        }
        return Objects.equals( designElement, ( ( DesignElementDataVector ) object ).designElement )
                && super.equals( object );
    }
}