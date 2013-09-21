/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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
 * @see ubic.gemma.model.common.description.VocabCharacteristic
 */
public class VocabCharacteristicImpl extends ubic.gemma.model.common.description.VocabCharacteristic {
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 8530426256054538222L;

    @Override
    public boolean equals( Object object ) {
        if ( !super.equals( object ) ) return false;
        if ( !( object instanceof VocabCharacteristic ) ) return false;
        VocabCharacteristic that = ( VocabCharacteristic ) object;
        return ObjectUtils.equals( this.getCategoryUri(), that.getCategoryUri() )
                && ObjectUtils.equals( this.getValueUri(), that.getValueUri() );
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder( 17, 3 ).appendSuper( super.hashCode() ).append( this.getCategoryUri() )
                .append( this.getValueUri() ).toHashCode();
    }

    /**
     * @see ubic.gemma.model.common.description.Characteristic#toString()
     */
    @Override
    public String toString() {
        // return toString( 0 );
        return super.toString() + " categoryUri=" + this.getCategoryUri() + " valueUri=" + this.getValueUri();
    }

    protected String toString( int indent ) {
        String ind = StringUtils.repeat( "   ", indent );
        StringBuilder buf = new StringBuilder();
        buf.append( ind + this.getValue() + "\n" );

        ++indent;
        return buf.toString();
    }
}