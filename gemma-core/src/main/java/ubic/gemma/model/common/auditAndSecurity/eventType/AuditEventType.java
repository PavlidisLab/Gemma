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
package ubic.gemma.model.common.auditAndSecurity.eventType;

import ubic.gemma.model.common.auditAndSecurity.curation.Curatable;

import java.io.Serializable;
import java.util.Date;

public abstract class AuditEventType implements Serializable {

    private static final long serialVersionUID = -7397754091918396538L;
    final private Long id = null;

    /**
     * No-arg constructor added to satisfy javabean contract
     *
     * @author Paul
     */
    @SuppressWarnings("WeakerAccess") // required by spring
    public AuditEventType() {
    }

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public Long getId() {
        return this.id;
    }

    /**
     * Sets the lastUpdated property of the curation details of the given curatable object to current Date.
     *
     * @param curatable the object to update the last update property of.
     */
    public void updateLastUpdated( Curatable curatable ) {
        curatable.getCurationDetails().setLastUpdated( new Date() );
    }

    @Override
    public int hashCode() {
        int hashCode = 0;
        //noinspection ConstantConditions // Hibernate populates id through reflection
        hashCode = 29 * ( hashCode + ( id == null ? 0 : id.hashCode() ) );
        return hashCode;
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof AuditEventType ) ) {
            return false;
        }
        final AuditEventType that = ( AuditEventType ) object;
        //noinspection ConstantConditions // Hibernate populates id through reflection
        return this.id != null && that.getId() != null && this.id.equals( that.getId() );
    }

}