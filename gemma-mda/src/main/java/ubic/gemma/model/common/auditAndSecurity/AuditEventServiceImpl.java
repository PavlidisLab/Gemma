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
/**
 * This is only generated once! It will never be overwritten.
 * You can (and have to!) safely modify it by hand.
 */
package ubic.gemma.model.common.auditAndSecurity;

/**
 * @see ubic.gemma.model.common.auditAndSecurity.AuditEventService
 * @author pavlidis
 * @version $Id$
 */
public class AuditEventServiceImpl extends ubic.gemma.model.common.auditAndSecurity.AuditEventServiceBase {

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditEventService#getUpdatedSinceDate(java.util.Date)
     */
    @Override
    protected java.util.Collection handleGetUpdatedSinceDate( java.util.Date date ) throws java.lang.Exception {
        return this.getAuditEventDao().getUpdatedSinceDate( date );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditEventService#getNewSinceDate(java.util.Date)
     */
    @Override
    protected java.util.Collection handleGetNewSinceDate( java.util.Date date ) throws java.lang.Exception {
        return this.getAuditEventDao().getNewSinceDate( date );
    }

    @Override
    protected void handleThaw( AuditEvent auditEvent ) throws Exception {
        this.getAuditEventDao().thaw( auditEvent );
    }

}