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

package ubic.gemma.model.common.auditAndSecurity;

import ubic.gemma.model.common.AbstractDescribable;

import java.io.Serializable;
import java.util.Objects;

/**
 * Representing a person or organization that can be contacted about, or is the source of, data in the system. A contact
 * has role and can be the member of a security group.
 *
 * @author Paul
 */
public class Contact extends AbstractDescribable implements Serializable {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 2410750024818017046L;
    private String email;

    /**
     * No-arg constructor added to satisfy javabean contract
     */
    public Contact() {
    }

    @Override
    public Long getId() {
        return super.getId();
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object )
            return true;
        if ( !( object instanceof Contact ) )
            return false;
        Contact that = ( Contact ) object;
        if ( this.getId() != null && that.getId() != null ) {
            return getId().equals( that.getId() );
        }
        return Objects.equals( getName(), that.getName() )
                && Objects.equals( getEmail(), that.getEmail() );
    }

    @Override
    public String getName() {
        return super.getName();
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail( String email ) {
        this.email = email;
    }

    public static final class Factory {

        public static ubic.gemma.model.common.auditAndSecurity.Contact newInstance() {
            return new ubic.gemma.model.common.auditAndSecurity.Contact();
        }

    }

}