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
package ubic.gemma.security.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.auditAndSecurity.AuditAction;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserService;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.Treatment;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.security.authentication.UserDetailsImpl;
import ubic.gemma.security.authentication.UserManager;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * Test of adding audit events when objects are created, updated or deleted.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class AuditAdviceTest extends BaseSpringContextTest {

    @Autowired
    UserManager userManager;

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private GeneService geneService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserService userService;

    @Test
    public void testAuditCreateAndDeleteExpressionExperiment() throws Exception {
        ExpressionExperiment ee = this.getTestPersistentCompleteExpressionExperiment( true );

        Collection<Long> trailIds = new HashSet<Long>();
        Collection<Long> eventIds = new HashSet<Long>();

        checkEEAuditTrails( ee, trailIds, eventIds );

        assertEquals( 2, ee.getAuditTrail().getEvents().size() ); // 2 because of the way create happens

        ee = expressionExperimentService.load( ee.getId() ); // so not thawed, which tests lazy issue in the advice.

        ee.setShortName( randomName() );

        expressionExperimentService.update( ee );

        // make sure we added an update event on the ee (3 because of the way create happens)
        assertEquals( 3, ee.getAuditTrail().getEvents().size() );

        expressionExperimentService.thawLite( ee );

        // check that we haven't added an update event to the design -- only a create.
        assertEquals( 1, ee.getExperimentalDesign().getAuditTrail().getEvents().size() );

        expressionExperimentService.delete( ee );

        checkDeletedTrails( trailIds, eventIds );

    }

    @Test
    public void testCascadingCreateOnUpdate() throws Exception {
        Gene g = this.getTestPeristentGene();

        g = this.geneService.load( g.getId() );
        g = this.geneService.thaw( g );

        // should have create and 1 because we update to add the gene product.
        assertEquals( 2, g.getAuditTrail().getEvents().size() );

        GeneProduct gp = GeneProduct.Factory.newInstance();
        String name = RandomStringUtils.randomAlphabetic( 20 );
        gp.setName( name );
        g.getProducts().add( gp );
        gp.setGene( g );
        this.geneService.update( g );
        assertNotNull( g.getAuditTrail() );

        // should have create and 2 updates, because we update to add the gene product.
        assertEquals( 3, g.getAuditTrail().getEvents().size() );

        for ( GeneProduct prod : g.getProducts() ) {
            assertNotNull( prod.getAuditTrail() );
            Collection<AuditEvent> events = prod.getAuditTrail().getEvents();
            assertEquals( 1, events.size() );
            for ( AuditEvent e : events ) {
                assertNotNull( e.getId() );
                assertNotNull( e.getAction() );
            }
        }

        this.geneService.update( g );
        this.geneService.update( g );
        this.geneService.update( g );

        assertEquals( 6, g.getAuditTrail().getEvents().size() );

        /*
         * Check we didn't get any extra events added to children.
         */
        for ( GeneProduct prod : g.getProducts() ) {
            assertEquals( 1, prod.getAuditTrail().getEvents().size() );
        }
    }

    /**
     * Test of simple case.
     * 
     * @throws Exception
     */
    @Test
    public void testCascadingCreateWithAssociatedAuditable() throws Exception {
        Gene g = this.getTestPeristentGene();

        g = this.geneService.load( g.getId() );
        g = this.geneService.thaw( g );

        // should have create and 1 because we update to add the gene product.
        assertEquals( 2, g.getAuditTrail().getEvents().size() );

        assertEquals( 1, g.getProducts().size() );

        for ( GeneProduct prod : g.getProducts() ) {
            assertNotNull( prod.getAuditTrail() );

            assertNotNull( prod.getStatus() );
            assertNotNull( prod.getStatus().getId() );

            this.auditTrailService.thaw( prod );
            Collection<AuditEvent> events = prod.getAuditTrail().getEvents();
            assertEquals( 1, events.size() );
            for ( AuditEvent e : events ) {
                assertNotNull( e.getId() );
                assertEquals( AuditAction.CREATE, e.getAction() );
            }
        }

    }

    @Test
    public void testSimpleAuditCreateUpdateUser() throws Exception {
        String USERNAME = RandomStringUtils.randomAlphabetic( RANDOM_STRING_LENGTH );
        try {
            userManager.loadUserByUsername( USERNAME );
        } catch ( UsernameNotFoundException e ) {
            String encodedPassword = passwordEncoder.encodePassword( USERNAME, USERNAME );
            UserDetailsImpl u = new UserDetailsImpl( encodedPassword, USERNAME, true, null, null, null, new Date() );
            userManager.createUser( u );
        }
        User user = userService.findByUserName( USERNAME );

        auditTrailService.thaw( user );

        assertEquals( "Should have a 'create'", 1, user.getAuditTrail().getEvents().size() );

        assertNotNull( user.getAuditTrail() );
        assertNotNull( user.getAuditTrail().getCreationEvent().getId() );

        user.setFax( RandomStringUtils.randomNumeric( 10 ) ); // change something.
        userService.update( user );

        int sizeAfterFirstUpdate = user.getAuditTrail().getEvents().size();

        assertEquals( "Should have a 'create' and an 'update'", 2, user.getAuditTrail().getEvents().size() );

        assertTrue( sizeAfterFirstUpdate > 1 );

        assertEquals( AuditAction.UPDATE, user.getAuditTrail().getLast().getAction() );
        // third time.
        user.setFax( RandomStringUtils.randomNumeric( 10 ) ); // change something.
        userService.update( user );
        // assertEquals( 3, user.getAuditTrail().getEvents().size() );
        assertTrue( sizeAfterFirstUpdate < user.getAuditTrail().getEvents().size() );

    }

    @Test
    public void testSimpleAuditFindOrCreate() throws Exception {
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setDescription( "From test" );
        ee.setName( RandomStringUtils.randomAlphabetic( 20 ) );
        ee = expressionExperimentService.findOrCreate( ee );

        assertNotNull( ee.getAuditTrail() );
        assertEquals( 1, ee.getAuditTrail().getEvents().size() );
        assertNotNull( ee.getStatus() );
        assertNotNull( ee.getStatus().getId() );
        assertNotNull( ee.getStatus().getCreateDate() );
        assertNotNull( ee.getAuditTrail().getCreationEvent().getId() );
    }

    private void checkAuditTrail( Auditable c, Collection<Long> trailIds, Collection<Long> eventIds ) {

        AuditTrail auditTrail = c.getAuditTrail();
        assertNotNull( "No audit trail for " + c, auditTrail );

        trailIds.add( auditTrail.getId() );

        assertTrue( "Trail but no events for " + c, auditTrail.getEvents().size() > 0 );

        for ( AuditEvent ae : auditTrail.getEvents() ) {
            eventIds.add( ae.getId() );
        }

    }

    private boolean checkDeletedAuditTrail( Long atid ) {
        return this.simpleJdbcTemplate.queryForInt( "SELECT COUNT(*) FROM AUDIT_TRAIL WHERE ID = ?", atid ) == 0;
    }

    private boolean checkDeletedEvent( Long i ) {
        return this.simpleJdbcTemplate.queryForInt( "SELECT COUNT(*) FROM AUDIT_EVENT WHERE ID = ?", i ) == 0;
    }

    private void checkDeletedTrails( Collection<Long> trailIds, Collection<Long> eventIds ) {

        for ( Long id : trailIds ) {
            assertTrue( checkDeletedAuditTrail( id ) );
        }

        for ( Long id : eventIds ) {
            assertTrue( checkDeletedEvent( id ) );
        }

    }

    /**
     * @param ee
     * @param trailIds
     * @param eventIds
     */
    private void checkEEAuditTrails( ExpressionExperiment ee, Collection<Long> trailIds, Collection<Long> eventIds ) {
        checkAuditTrail( ee, trailIds, eventIds );

        for ( BioAssay ba : ee.getBioAssays() ) {
            checkAuditTrail( ba, trailIds, eventIds );
            for ( BioMaterial bm : ba.getSamplesUsed() ) {
                checkAuditTrail( bm, trailIds, eventIds );
                for ( Characteristic c : bm.getCharacteristics() ) {
                    checkAuditTrail( c, trailIds, eventIds );
                }

                for ( Treatment t : bm.getTreatments() ) {
                    checkAuditTrail( t, trailIds, eventIds );
                    checkAuditTrail( t.getAction(), trailIds, eventIds );
                    // for ( CompoundMeasurement cm : t.getCompoundMeasurements() ) {
                    // checkAuditTrail( cm.getCompound().getCompoundIndices(), trailIds, eventIds );
                    // }
                }
            }
        }

        Collection<ExperimentalFactor> experimentalFactors = ee.getExperimentalDesign().getExperimentalFactors();
        assertTrue( experimentalFactors.size() > 0 );

        for ( ExperimentalFactor ef : experimentalFactors ) {
            checkAuditTrail( ef, trailIds, eventIds );
            for ( FactorValue fv : ef.getFactorValues() ) {
                for ( Characteristic c : fv.getCharacteristics() ) {
                    checkAuditTrail( c, trailIds, eventIds );
                }
            }
        }

    }

}
