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

import java.util.Collection;

/**
 * 
 */
public abstract class ProtocolApplication implements java.io.Serializable {

    /**
     * Constructs new instances of {@link ubic.gemma.model.common.protocol.ProtocolApplication}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link ubic.gemma.model.common.protocol.ProtocolApplication}.
         */
        public static ubic.gemma.model.common.protocol.ProtocolApplication newInstance() {
            return new ubic.gemma.model.common.protocol.ProtocolApplicationImpl();
        }

    }

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -6769604761541398325L;
    private java.util.Date activityDate;

    private Long id;

    private Collection<ubic.gemma.model.common.auditAndSecurity.Person> performers = new java.util.HashSet<ubic.gemma.model.common.auditAndSecurity.Person>();

    private ubic.gemma.model.common.protocol.Protocol protocol;

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public ProtocolApplication() {
    }

    /**
     * Returns <code>true</code> if the argument is an ProtocolApplication instance and all identifiers for this entity
     * equal the identifiers of the argument entity. Returns <code>false</code> otherwise.
     */
    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof ProtocolApplication ) ) {
            return false;
        }
        final ProtocolApplication that = ( ProtocolApplication ) object;
        if ( this.id == null || that.getId() == null || !this.id.equals( that.getId() ) ) {
            return false;
        }
        return true;
    }

    /**
     * 
     */
    public java.util.Date getActivityDate() {
        return this.activityDate;
    }

    /**
     * 
     */
    public Long getId() {
        return this.id;
    }

    /**
     * 
     */
    public Collection<ubic.gemma.model.common.auditAndSecurity.Person> getPerformers() {
        return this.performers;
    }

    /**
     * 
     */
    public ubic.gemma.model.common.protocol.Protocol getProtocol() {
        return this.protocol;
    }

    /**
     * Returns a hash code based on this entity's identifiers.
     */
    @Override
    public int hashCode() {
        int hashCode = 0;
        hashCode = 29 * hashCode + ( id == null ? 0 : id.hashCode() );

        return hashCode;
    }

    public void setActivityDate( java.util.Date activityDate ) {
        this.activityDate = activityDate;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public void setPerformers( Collection<ubic.gemma.model.common.auditAndSecurity.Person> performers ) {
        this.performers = performers;
    }

    public void setProtocol( ubic.gemma.model.common.protocol.Protocol protocol ) {
        this.protocol = protocol;
    }

}