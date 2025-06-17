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

package ubic.gemma.web.controller.common.auditAndSecurity;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.model.common.auditAndSecurity.eventType.CommentedEvent;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.web.util.BaseSpringWebTest;
import ubic.gemma.web.util.EntityDelegator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Paul
 */
public class AuditControllerTest extends BaseSpringWebTest {

    @Autowired
    private AuditController auditController;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Test
    public void testAddUpdateEvent() {
        ExpressionExperiment e = this.getTestPersistentExpressionExperiment();

        EntityDelegator<ExpressionExperiment> ed = new EntityDelegator<>( e );

        assertEquals( "ubic.gemma.model.expression.experiment.ExpressionExperiment", ed.getClassDelegatingFor() );

        auditController.addAuditEvent( ed, "CommentedEvent", "foo", "bar" );

        e = expressionExperimentService.load( e.getId() );
        assertNotNull( e );
        e = expressionExperimentService.thawLite( e );
        assertNotNull( e );

        assertThat( e.getAuditTrail().getEvents() )
                .extracting( "eventType" )
                .containsExactly( null, new CommentedEvent() );

    }
}
