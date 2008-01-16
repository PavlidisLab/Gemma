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

import org.acegisecurity.acl.basic.BasicAclExtendedDao;
import org.acegisecurity.acl.basic.NamedEntityObjectIdentity;

import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.model.common.protocol.Hardware;
import ubic.gemma.model.common.protocol.HardwareService;
import ubic.gemma.model.common.protocol.Protocol;
import ubic.gemma.model.common.protocol.ProtocolService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.PersisterHelper;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * Tests of ACL management.
 * 
 * @author keshav
 * @version $Id$
 */
public class PersistAclInterceptorTest extends BaseSpringContextTest {
    private BasicAclExtendedDao basicAclExtendedDao;
    private PersisterHelper persisterHelper;
    ArrayDesignService arrayDesignService;
    ExpressionExperimentService expressionExperimentService;
    ProtocolService protocolService;
    HardwareService hardwareService;

    /**
     * Calling the method saveArrayDesign, which should have the PersistAclInterceptor.invoke called on it after the
     * actual method invocation.
     * 
     * @throws Exception
     */
    public void testAddPermissionsInterceptor() throws Exception {
        ArrayDesign ad = this.getTestPersistentArrayDesign( 5, true, false, false ); // need to modify

        if ( basicAclExtendedDao.getAcls( new NamedEntityObjectIdentity( ad ) ) == null ) {
            arrayDesignService.remove( ad );
            fail( "Failed to create ACL for " + ad );
        }

        arrayDesignService.remove( ad );
        // make sure it got deleted.
        if ( basicAclExtendedDao.getAcls( new NamedEntityObjectIdentity( ad ) ) != null ) {
            fail( "Failed to  delete ACL for " + ad );
        }

    }

    /**
     * @throws Exception
     */
    public void testCascadeCreateAndDelete() throws Exception {
        setComplete();
        ExpressionExperiment ee = this.getTestPersistentCompleteExpressionExperiment( false );
        if ( basicAclExtendedDao.getAcls( new NamedEntityObjectIdentity( ee ) ) == null ) {
            fail( "Failed to create ACL for " + ee );
        }

        assert ee.getExperimentalDesign() != null : "No experimentalDesign";
        ExperimentalDesign ed = ee.getExperimentalDesign();

        if ( basicAclExtendedDao.getAcls( new NamedEntityObjectIdentity( ed ) ) == null ) {
            fail( "Failed to cascade create ACL for " + ed );
        }

        expressionExperimentService.delete( ee );

        if ( basicAclExtendedDao.getAcls( new NamedEntityObjectIdentity( ee ) ) != null ) {
            fail( "Failed to  delete ACL for " + ee );
        }

        if ( basicAclExtendedDao.getAcls( new NamedEntityObjectIdentity( ed ) ) != null ) {
            fail( "Failed to cascade delete ACL after deleting entity for " + ed );
        }

    }

    public void testNoCascadeDelete() throws Exception {
        Protocol p = Protocol.Factory.newInstance();
        p.setName( "protocol" );

        Hardware h = Hardware.Factory.newInstance();
        h.setName( "hardware" );

        h = ( Hardware ) persisterHelper.persist( h );
        assert h != null;
        AuditTrail ad = AuditTrail.Factory.newInstance();
        ad = ( AuditTrail ) persisterHelper.persist( ad );
        p.setAuditTrail( ad );

        p.getHardwares().add( h );
        p = protocolService.findOrCreate( p );

        // make sure the ACL for h is there in the first place.
        if ( basicAclExtendedDao.getAcls( new NamedEntityObjectIdentity( h ) ) == null ) {
            fail( "No ACL created or exists for " + h );
        }

        protocolService.remove( p );

        // make sure the ACL for h is still there
        if ( basicAclExtendedDao.getAcls( new NamedEntityObjectIdentity( h ) ) == null ) {
            fail( "Inappropriate cascade delete of ACL for " + h );
        }

        hardwareService.remove( h );

        // now it should be gone.
        if ( basicAclExtendedDao.getAcls( new NamedEntityObjectIdentity( h ) ) != null ) {
            fail( "Failed to  delete ACL for " + h );
        }

    }

    /**
     * @param basicAclExtendedDao The basicAclExtendedDao to set.
     */
    public void setBasicAclExtendedDao( BasicAclExtendedDao basicAclExtendedDao ) {
        this.basicAclExtendedDao = basicAclExtendedDao;
    }

    /**
     * @param persisterHelper The persisterHelper to set.
     */
    @Override
    public void setPersisterHelper( PersisterHelper persisterHelper ) {
        this.persisterHelper = persisterHelper;
    }

    /**
     * @param arrayDesignService The arrayDesignService to set.
     */
    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

    /**
     * @param expressionExperimentService The expressionExperimentService to set.
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    /**
     * @param hardwareService The hardwareService to set.
     */
    public void setHardwareService( HardwareService hardwareService ) {
        this.hardwareService = hardwareService;
    }

    /**
     * @param protocolService The protocolService to set.
     */
    public void setProtocolService( ProtocolService protocolService ) {
        this.protocolService = protocolService;
    }
}
