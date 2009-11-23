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
package ubic.gemma.model.common.auditAndSecurity;

import ubic.gemma.persistence.BaseDao;

/**
 * @see ubic.gemma.model.common.auditAndSecurity.AuditTrail
 */
public interface AuditTrailDao extends BaseDao<AuditTrail> {
    /**
     * This constant is used as a transformation flag; entities can be converted automatically into value objects or
     * other types, different methods in a class implementing this interface support this feature: look for an
     * <code>int</code> parameter called <code>transform</code>.
     * <p/>
     * This specific flag denotes no transformation will occur.
     */
    public final static int TRANSFORM_NONE = 0;

    /**
     * <p>
     * Add the given event to the audit trail of the given Auditable entity
     * </p>
     */
    public ubic.gemma.model.common.auditAndSecurity.AuditEvent addEvent( ubic.gemma.model.common.Auditable auditable,
            ubic.gemma.model.common.auditAndSecurity.AuditEvent auditEvent );

    /**
     * 
     */
    public void thaw( ubic.gemma.model.common.Auditable auditable );

    /**
     * <p>
     * thaws the given audit trail
     * </p>
     */
    public void thaw( ubic.gemma.model.common.auditAndSecurity.AuditTrail auditTrail );

}
