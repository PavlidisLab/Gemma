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
package edu.columbia.gemma.security.interceptor;

import org.acegisecurity.acl.basic.BasicAclExtendedDao;
import org.acegisecurity.acl.basic.NamedEntityObjectIdentity;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;

import edu.columbia.gemma.BaseServiceTestCase;
import edu.columbia.gemma.common.protocol.Hardware;
import edu.columbia.gemma.common.protocol.HardwareService;
import edu.columbia.gemma.common.protocol.Protocol;
import edu.columbia.gemma.common.protocol.ProtocolService;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesign;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesignService;
import edu.columbia.gemma.expression.experiment.ExperimentalDesign;
import edu.columbia.gemma.expression.experiment.ExpressionExperiment;
import edu.columbia.gemma.expression.experiment.ExpressionExperimentService;

/**
 * Tests of ACL management.
 * 
 * @author keshav
 * @version $Id$
 */
public class PersistAclInterceptorTest extends BaseServiceTestCase {
    private BasicAclExtendedDao basicAclExtendedDao;

    protected void setUp() throws Exception {
        super.setUp();
        basicAclExtendedDao = ( BasicAclExtendedDao ) ctx.getBean( "basicAclExtendedDao" );

    } /*
         * @see TestCase#tearDown()
         */

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Calling the method saveArrayDesign, which should have the PersistAclInterceptor.invoke called on it after the
     * actual method invocation.
     * <p>
     * 
     * @throws Exception
     */
    public void testAddPermissionsInterceptor() throws Exception {
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setName( "fooblyDoobly" );
        ArrayDesignService ads = ( ArrayDesignService ) ctx.getBean( "arrayDesignService" );
        ad = ads.findOrCreate( ad );

        try {
            basicAclExtendedDao.create( AddOrRemoveFromACLInterceptor.getAclEntry( ad ) );
            fail( "Whoops, ACL entry doesn't exist for " + ad );
        } catch ( DataIntegrityViolationException e ) {
            // ok
        }
        ads.remove( ad );
        // make sure it got deleted.
        try {
            basicAclExtendedDao.delete( new NamedEntityObjectIdentity( ad ) );
            fail( "Failed to delete ACL after deleting entity for " + ad );
        } catch ( DataAccessException e ) {
            // ok
        }
    }

    /**
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public void testCascadeCreateAndDelete() throws Exception {
        ExpressionExperimentService ees = ( ExpressionExperimentService ) ctx.getBean( "expressionExperimentService" );
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setDescription( "From test" );
        ee.setName( "Test experiment" );

        ExperimentalDesign ed = ExperimentalDesign.Factory.newInstance();
        ed.setName( "foo" );
        ee.getExperimentalDesigns().add( ed );
        ee = ees.findOrCreate( ee );

        try {
            basicAclExtendedDao.create( AddOrRemoveFromACLInterceptor.getAclEntry( ee ) );
            fail( "Whoops, ACL entry doesn't exist for " + ee );
        } catch ( DataIntegrityViolationException e ) {
            // ok
        }

        ed = ee.getExperimentalDesigns().iterator().next();
        try {
            basicAclExtendedDao.create( AddOrRemoveFromACLInterceptor.getAclEntry( ed ) );
            fail( "Failed to create ACL entry on create of entity " + ed );
        } catch ( DataIntegrityViolationException e ) {
            // ok
        }

        ees.remove( ee );

        try {
            basicAclExtendedDao.delete( new NamedEntityObjectIdentity( ee ) );
            fail( "Failed to delete ACL for " + ee );
        } catch ( DataAccessException e ) {
            // ok
        }

        // now after delete, the acl for ed should also be gone:
        try {
            basicAclExtendedDao.delete( new NamedEntityObjectIdentity( ed ) );
            fail( "Failed to cascade delete ACL after deleting entity for " + ee );
        } catch ( DataAccessException e ) {
            // ok
        }

    }

    public void testNoCascadeDelete() throws Exception {
        ProtocolService ps = ( ProtocolService ) ctx.getBean( "protocolService" );
        Protocol p = Protocol.Factory.newInstance();
        p.setName( "protocol" );

        Hardware h = Hardware.Factory.newInstance();
        h.setName( "hardware" );

        HardwareService hs = ( HardwareService ) ctx.getBean( "hardwareService" );
        h = hs.findOrCreate( h );

        p.getHardwares().add( h );
        ps.findOrCreate( p );
        ps.remove( p );

        // make sure the ACL for h is still there
        try {
            basicAclExtendedDao.create( AddOrRemoveFromACLInterceptor.getAclEntry( h ) );
            fail( "Whoops, ACL entry doesn't exist for " + h + ", indicates inappropriate cascade of ACL!" );
        } catch ( DataIntegrityViolationException e ) {
            // ok
        }

        hs.remove( h );
    }
}
