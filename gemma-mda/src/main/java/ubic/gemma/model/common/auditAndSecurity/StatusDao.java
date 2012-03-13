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
package ubic.gemma.model.common.auditAndSecurity;

import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.persistence.BaseDao;

/**
 * @author paul
 * @version $Id$
 */
public interface StatusDao extends BaseDao<Status> {

    
    /**
     * Update the 'last updated' date and trouble/validated flags
     * 
     * @param a entity to update the status for
     * @param auditEventType used to updated the appropriate status flags, can be null
     */
    void update( Auditable a, AuditEventType auditEventType );

    /**
     * Automatically turns off 'validated' if the value is 'true'.
     * 
     * @param a
     * @param value
     */
    public void setTroubled( Auditable a, boolean value );

    /**
     * @param a
     * @param value
     */
    public void setValidated( Auditable a, boolean value );

    /**
     * Create a persistent default status.
     * 
     * @return
     */
    public Status create();

    /**
     * @param d
     */
    public void initializeStatus( Auditable d );

    /**
     * @param d
     */
    public Status load( Long id );


}
