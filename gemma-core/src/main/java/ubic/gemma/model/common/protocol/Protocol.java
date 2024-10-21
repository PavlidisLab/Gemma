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
package ubic.gemma.model.common.protocol;

import ubic.gemma.model.common.auditAndSecurity.Securable;
import ubic.gemma.model.common.AbstractDescribable;
import ubic.gemma.model.common.description.Characteristic;

import java.io.Serializable;
import java.util.Set;

public class Protocol extends AbstractDescribable implements Securable, Serializable {

    private static final long serialVersionUID = -1902891452989019766L;

    /**
     * Characteristics describing the protocol.
     */
    private Set<Characteristic> characteristics;

    public Set<Characteristic> getCharacteristics() {
        return characteristics;
    }

    public void setCharacteristics( Set<Characteristic> characteristics ) {
        this.characteristics = characteristics;
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object )
            return true;
        if ( !( object instanceof Protocol ) )
            return false;
        Protocol that = ( Protocol ) object;
        if ( getId() != null && that.getId() != null ) {
            return getId().equals( that.getId() );
        } else {
            return false;
        }
    }

    public static final class Factory {

        public static Protocol newInstance() {
            return new Protocol();
        }

    }

}