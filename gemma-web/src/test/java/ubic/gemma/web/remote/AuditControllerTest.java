/*
 * The Gemma project
 * 
 * Copyright (c) 2012 University of British Columbia
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

package ubic.gemma.web.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.CommentedEventImpl;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.testing.BaseSpringWebTest;

/**
 * @author Paul
 * @version $Id$
 */
public class AuditControllerTest extends BaseSpringWebTest {

    @Autowired
    private AuditController auditController;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Test
    public void testAddUpdateEvent() {
        ExpressionExperiment e = this.getTestPersistentExpressionExperiment();

        EntityDelegator ed = new EntityDelegator( e );

        assertEquals( "ubic.gemma.model.expression.experiment.ExpressionExperimentImpl", ed.getClassDelegatingFor() );

        auditController.addAuditEvent( ed, "CommentedEvent", "foo", "bar" );

        e = expressionExperimentService.load( e.getId() );
        assertNotNull( e );
        e = expressionExperimentService.thawLite( e );
        assertNotNull( e );
        AuditTrail auditTrail = e.getAuditTrail();
        assertNotNull( auditTrail );
        AuditEvent lastEvent = auditTrail.getLast();
        assertNotNull( lastEvent );
        AuditEventType eventType = lastEvent.getEventType();
        assertNotNull( eventType );
        assertEquals( CommentedEventImpl.class, eventType.getClass() );

    }
}
