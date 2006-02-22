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

import edu.columbia.gemma.BaseDAOTestCase;
import edu.columbia.gemma.common.auditAndSecurity.AuditTrail;
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
public class PersistAclInterceptorTest extends BaseDAOTestCase {
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

        ad = ( ArrayDesign ) this.getPersisterHelper().persist( ad );

        if ( basicAclExtendedDao.getAcls( new NamedEntityObjectIdentity( ad ) ) == null ) {
            ads.remove( ad );
            fail( "Failed to create ACL for " + ad );
        }

        ads.remove( ad );
        // make sure it got deleted.
        if ( basicAclExtendedDao.getAcls( new NamedEntityObjectIdentity( ad ) ) != null ) {
            fail( "Failed to  delete ACL for " + ad );
        }

    }

    /**
     * @throws Exception
     */
    public void testCascadeCreateAndDelete() throws Exception {
        ExpressionExperimentService ees = ( ExpressionExperimentService ) ctx.getBean( "expressionExperimentService" );
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setDescription( "From test" );
        ee.setName( "Test experiment" );

        ExperimentalDesign ed = ExperimentalDesign.Factory.newInstance();
        ed.setName( "foo" );
        ee.getExperimentalDesigns().add( ed );
        ee = ( ExpressionExperiment ) this.getPersisterHelper().persist( ee );

        if ( basicAclExtendedDao.getAcls( new NamedEntityObjectIdentity( ee ) ) == null ) {
            fail( "Failed to create ACL for " + ee );
        }

        ed = ee.getExperimentalDesigns().iterator().next();

        if ( basicAclExtendedDao.getAcls( new NamedEntityObjectIdentity( ed ) ) == null ) {
            fail( "Failed to cascade create ACL for " + ed );
        }

        ees.delete( ee );

        if ( basicAclExtendedDao.getAcls( new NamedEntityObjectIdentity( ee ) ) != null ) {
            fail( "Failed to  delete ACL for " + ee );
        }

        if ( basicAclExtendedDao.getAcls( new NamedEntityObjectIdentity( ed ) ) != null ) {
            fail( "Failed to cascade delete ACL after deleting entity for " + ed );
        }

    }

    public void testNoCascadeDelete() throws Exception {
        ProtocolService ps = ( ProtocolService ) ctx.getBean( "protocolService" );
        Protocol p = Protocol.Factory.newInstance();
        p.setName( "protocol" );

        Hardware h = Hardware.Factory.newInstance();
        h.setName( "hardware" );

        HardwareService hs = ( HardwareService ) ctx.getBean( "hardwareService" );
        h = ( Hardware ) this.getPersisterHelper().persist( h );
        assert h != null;
        AuditTrail ad = AuditTrail.Factory.newInstance();
        ad = ( AuditTrail ) this.getPersisterHelper().persist( ad );
        p.setAuditTrail( ad );

        p.getHardwares().add( h );
        p = ps.findOrCreate( p );

        // make sure the ACL for h is there in the first place.
        if ( basicAclExtendedDao.getAcls( new NamedEntityObjectIdentity( h ) ) == null ) {
            fail( "No ACL created or exists for " + h );
        }

        ps.remove( p );

        // make sure the ACL for h is still there
        if ( basicAclExtendedDao.getAcls( new NamedEntityObjectIdentity( h ) ) == null ) {
            fail( "Inappropriate cascade delete of ACL for " + h );
        }

        hs.remove( h );

        // now it should be gone.
        if ( basicAclExtendedDao.getAcls( new NamedEntityObjectIdentity( h ) ) != null ) {
            fail( "Failed to  delete ACL for " + h );
        }

    }
}
