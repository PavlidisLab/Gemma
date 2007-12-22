/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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

import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author pavlidis
 * @version $Id$
 */
public class AuditTrailDaoImplTest extends BaseSpringContextTest {

    Auditable auditable;

    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();

        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setName( "testing" );

        ArrayDesignService ads = ( ArrayDesignService ) getBean( "arrayDesignService" );
        ad = ads.create( ad );
        auditable = ad;

    }

    public void testHandleAddEventAuditableAuditEvent() throws Exception {
        AuditTrailDao atd = ( AuditTrailDao ) getBean( "auditTrailDao" );
        AuditEvent auditEvent = AuditEvent.Factory.newInstance();
        auditEvent.setAction( AuditAction.UPDATE );
        auditEvent.setNote( "this is a test" );
        auditEvent = atd.addEvent( auditable, auditEvent );
        assertNotNull( auditEvent.getId() );
        assertTrue( auditable.getAuditTrail().getEvents().size() > 1 );

    }

}
