/*
 * The Gemma project.
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
package ubic.gemma.model.common.description;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * @see ubic.gemma.model.common.description.Characteristic
 * @author pavlidis
 * @version $Id$
 */
public class CharacteristicImpl extends Characteristic {
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 163962374233046021L;

    @Override
    public boolean equals( Object object ) {
        if ( object == null ) return false;
        if ( this == object ) return true;
        if ( !( object instanceof Characteristic ) ) return false;
        Characteristic that = ( Characteristic ) object;
        if ( this.getId() != null && that.getId() != null ) return this.getId().equals( that.getId() );

        /*
         * at this point, we know we have two Characteristics, at least one of which is transient, so we have to look at
         * the fields; we can't just compare the hashcodes because they also look at the id, so comparing one transient
         * and one persistent would always fail...
         */
        return ObjectUtils.equals( this.getCategory(), that.getCategory() )
                && ObjectUtils.equals( this.getValue(), that.getValue() );
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder( 17, 1 ).append( this.getId() ).append( this.getCategory() )
                .append( this.getValue() ).toHashCode();
    }

    @Override
    public String toString() {
        if (StringUtils.isBlank( this.getCategory())) {
            return "[No category] Value = " + this.getValue();
        }
        return "Category = " + this.getCategory() + " Value = " + this.getValue();
    }

}