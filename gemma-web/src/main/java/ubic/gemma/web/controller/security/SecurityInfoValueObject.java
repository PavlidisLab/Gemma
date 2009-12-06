/*
 * The Gemma project
 * 
 * Copyright (c) 2009 University of British Columbia
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
package ubic.gemma.web.controller.security;

import java.util.Collection;

import ubic.gemma.model.common.auditAndSecurity.Securable;

/**
 * Carries security information about an entity
 * 
 * @author paul
 * @version $Id$
 */
public class SecurityInfoValueObject {

    String entityClazz;

    Long entityId;

    boolean isPublic;

    Collection<String> groupsThatCanRead;

    Collection<String> groupsThatCanWrite;

    /**
     * @param s to initialize. Security information will not be filled in.
     */
    public SecurityInfoValueObject( Securable s ) {
        this.entityClazz = s.getClass().getName();
        this.entityId = s.getId();
    }

    /**
     * @return the entityClazz
     */
    public String getEntityClazz() {
        return entityClazz;
    }

    /**
     * @param entityClazz the entityClazz to set
     */
    public void setEntityClazz( String entityClazz ) {
        this.entityClazz = entityClazz;
    }

    /**
     * @return the entityId
     */
    public Long getEntityId() {
        return entityId;
    }

    /**
     * @param entityId the entityId to set
     */
    public void setEntityId( Long entityId ) {
        this.entityId = entityId;
    }

    /**
     * @return the isPublic
     */
    public boolean isPublic() {
        return isPublic;
    }

    /**
     * @param isPublic the isPublic to set
     */
    public void setPublic( boolean isPublic ) {
        this.isPublic = isPublic;
    }

    /**
     * @return the groupsThatCanRead
     */
    public Collection<String> getGroupsThatCanRead() {
        return groupsThatCanRead;
    }

    /**
     * @param groupsThatCanRead the groupsThatCanRead to set
     */
    public void setGroupsThatCanRead( Collection<String> groupsThatCanRead ) {
        this.groupsThatCanRead = groupsThatCanRead;
    }

    /**
     * @return the groupsThatCanWrite
     */
    public Collection<String> getGroupsThatCanWrite() {
        return groupsThatCanWrite;
    }

    /**
     * @param groupsThatCanWrite the groupsThatCanWrite to set
     */
    public void setGroupsThatCanWrite( Collection<String> groupsThatCanWrite ) {
        this.groupsThatCanWrite = groupsThatCanWrite;
    }

}
