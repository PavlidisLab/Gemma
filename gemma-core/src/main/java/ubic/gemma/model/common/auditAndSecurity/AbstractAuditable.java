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

/**
 * An entity which can have an audit trail attached to it.
 *
 * @author Paul
 */
public abstract class AbstractAuditable extends AbstractDescribable implements Auditable {

    private AuditTrail auditTrail = new AuditTrail();

    @Override
    public AuditTrail getAuditTrail() {
        return this.auditTrail;
    }

    @Override
    public void setAuditTrail( AuditTrail auditTrail ) {
        this.auditTrail = auditTrail;
    }
}