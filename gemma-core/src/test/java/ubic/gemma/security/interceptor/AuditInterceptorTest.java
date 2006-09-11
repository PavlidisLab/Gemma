/*
 * The Gemma project
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
package ubic.gemma.security.interceptor;

import org.apache.commons.lang.RandomStringUtils;

import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.testing.BaseTransactionalSpringContextTest;

/**
 * @author pavlidis
 * @version $Id$
 */
public class AuditInterceptorTest extends BaseTransactionalSpringContextTest {
    ExpressionExperimentService expressionExperimentService;
    private UserService userService;

    public void testSimpleAuditFindOrCreate() throws Exception {
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setDescription( "From test" );
        ee.setName( "Test experiment" );
        ee = expressionExperimentService.findOrCreate( ee );
        assertNotNull( ee.getAuditTrail() );
        assertEquals( 1, ee.getAuditTrail().getEvents().size() );
        flushSession(); // have to do or cascade insert doesn't happen
        assertNotNull( ee.getAuditTrail().getCreationEvent().getId() );
    }

    public void testSimpleAuditCreateUpdateUser() throws Exception {
        User user = User.Factory.newInstance();
        user.setUserName( RandomStringUtils.randomAlphabetic( 20 ) );
        user.setEmail( RandomStringUtils.randomAlphabetic( 20 ) + "@gemma.com" );
        user.setDescription( "From test" );
        user.setName( "Test" );
        user = userService.create( user );

        assertNotNull( user.getAuditTrail() );
        user.setFax( RandomStringUtils.randomNumeric( 10 ) ); // change something.

        userService.update( user );

        flushSession(); // have to do or cascade insert doesn't happen

        assertNotNull( user.getAuditTrail().getCreationEvent().getId() );

        /*
         * FIXME - this does not work in maven builds. It's fine in eclipse.
         */
        // assertEquals( 2, user.getAuditTrail().getEvents().size() );
        // assertEquals( AuditAction.UPDATE, user.getAuditTrail().getLast().getAction() );
        // third time.
        // user.setFax( RandomStringUtils.randomNumeric( 10 ) ); // change something.
        // userService.update( user );
        // flushSession(); // have to do or cascade insert doesn't happen
        // assertEquals( 3, user.getAuditTrail().getEvents().size() );
    }

    /**
     * @param expressionExperimentService The expressionExperimentService to set.
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    public void setUserService( UserService userService ) {
        this.userService = userService;
    }

    // FIXME add tests on collections and of update, create, remove...

}
