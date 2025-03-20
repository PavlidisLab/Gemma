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
package ubic.gemma.core.security.audit;

import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.Assertions;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.common.auditAndSecurity.AuditAction;
import ubic.gemma.model.common.auditAndSecurity.Auditable;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.util.Collection;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Test of adding audit events when objects are created, updated or deleted.
 * Note: this test used to use genes, but we removed auditability from genes.
 *
 * @author pavlidis
 */
public class AuditAdviceTest extends BaseSpringContextTest {

    @Autowired
    private AuditEventService auditEventService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private SessionFactory sessionFactory;

    @Test
    public void testAuditCreateAndDeleteExpressionExperiment() {
        ExpressionExperiment ee = this.getTestPersistentCompleteExpressionExperiment( true );

        Collection<Long> trailIds = new HashSet<>();
        Collection<Long> eventIds = new HashSet<>();

        this.checkEEAuditTrails( ee, trailIds, eventIds );

        assertEquals( 1, ee.getAuditTrail().getEvents().size() );

        ee = expressionExperimentService.load( ee.getId() ); // so not thawed, which tests lazy issue in the advice.
        assertNotNull( ee );

        ee.setShortName( this.randomName() );

        expressionExperimentService.update( ee );

        ee = expressionExperimentService.thawLite( ee );

        // make sure we added an update event on the ee
        assertEquals( 2, auditEventService.getEvents( ee ).size() );

        expressionExperimentService.remove( ee );

        this.checkDeletedTrails( trailIds, eventIds );

    }

    @Autowired
    private BioMaterialService bioMaterialService;

    @Test
    public void testCascadingCreateOnUpdate() {
        ExpressionExperiment ee = this.getTestPersistentCompleteExpressionExperiment( false );

        ee = this.expressionExperimentService.load( ee.getId() );
        assertNotNull( ee );
        ee = expressionExperimentService.thawLite( ee );

        // should have create only
        assertEquals( 1, ee.getAuditTrail().getEvents().size() );

        BioMaterial bm = BioMaterial.Factory.newInstance();
        bm.setSourceTaxon( ee.getTaxon() );
        bm = bioMaterialService.create( bm );

        BioAssay ba = BioAssay.Factory.newInstance();
        String name = RandomStringUtils.randomAlphabetic( 20 );
        ba.setName( name );
        ba.setArrayDesignUsed( ee.getBioAssays().iterator().next().getArrayDesignUsed() );
        ba.setSampleUsed( bm );
        ee.getBioAssays().add( ba );

        this.expressionExperimentService.update( ee );
        assertNotNull( ee.getAuditTrail() );

        // should have create and 1 updates
        assertEquals( 2, ee.getAuditTrail().getEvents().size() );

        Session session = sessionFactory.openSession();
        session.update( ee );

        session.close();

        this.expressionExperimentService.update( ee );
        this.expressionExperimentService.update( ee );
        this.expressionExperimentService.update( ee );

        assertEquals( 5, ee.getAuditTrail().getEvents().size() );

    }

    @Test
    public void testCascadingCreateWithAssociatedAuditable() {
        ExpressionExperiment ee = this.getTestPersistentCompleteExpressionExperiment( false );

        ee = this.expressionExperimentService.load( ee.getId() );
        assertNotNull( ee );
        ee = expressionExperimentService.thawLite( ee );

        assertEquals( 16, ee.getBioAssays().size() );

        assertNotNull( ee.getBioAssays().iterator().next().getId() );

        assertEquals( 1, ee.getAuditTrail().getEvents().size() );

    }

    @Test
    public void testSimpleAuditFindOrCreate() {
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setDescription( "From test" );
        ee.setName( RandomStringUtils.randomAlphabetic( 20 ) );
        ee.setTaxon( taxonService.load( 1L ) );
        ee = expressionExperimentService.findOrCreate( ee );

        assertNotNull( ee.getAuditTrail() );
        assertEquals( 1, ee.getAuditTrail().getEvents().size() );
        assertNotNull( ee.getCurationDetails() );
        assertNotNull( ee.getCurationDetails().getId() );
        assertNull( ee.getCurationDetails().getLastUpdated() );
        AuditTrail auditTrail = ee.getAuditTrail();
        assertNotNull( auditTrail.getEvents().stream()
                .filter( ae -> ae.getAction() == AuditAction.CREATE )
                .findFirst()
                .orElse( null ).getId() );
    }

    /*
     * Torture test. Passes fine with a single thread.
     */
    @SuppressWarnings("Duplicates") // Not in this project
    @Test
    @Category(SlowTest.class)
    public void testAuditFindOrCreateConcurrentTorture() {
        int numThreads = 14; // too high and we run out of connections, which is not what we're testing.
        final int numExperimentsPerThread = 5;
        final int numUpdates = 10;
        final Random random = new Random();
        final AtomicInteger c = new AtomicInteger( 0 );
        Collection<Future<?>> futures = new HashSet<>();
        ExecutorService es = Executors.newFixedThreadPool( numThreads );
        for ( int i = 0; i < numThreads; i++ ) {
            for ( int j = 0; j < numExperimentsPerThread; j++ ) {
                log.debug( "Starting experiment " + j );
                futures.add( es.submit( () -> {
                    ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
                    ee.setDescription( "From test" );
                    ee.setShortName( RandomStringUtils.randomAlphabetic( 20 ) );
                    ee.setName( RandomStringUtils.randomAlphabetic( 20 ) );
                    ee.setTaxon( taxonService.load( 1L ) );
                    ee = expressionExperimentService.findOrCreate( ee );

                    assertNotNull( ee.getAuditTrail() );
                    assertEquals( 1, ee.getAuditTrail().getEvents().size() );
                    assertNotNull( ee.getCurationDetails() );
                    assertNotNull( ee.getCurationDetails().getId() );
                    assertNull( ee.getCurationDetails().getLastUpdated() );
                    AuditTrail auditTrail = ee.getAuditTrail();
                    assertNotNull( auditTrail.getEvents().stream()
                            .filter( ae -> ae.getAction() == AuditAction.CREATE )
                            .findFirst()
                            .orElse( null ).getId() );

                    for ( int q = 0; q < numUpdates; q++ ) {
                        expressionExperimentService.update( ee );
                        assertEquals( 2 + q, ee.getAuditTrail().getEvents().size() );
                        AuditTrail auditTrail1 = ee.getAuditTrail();
                        assertEquals( (auditTrail1.getEvents().isEmpty() ? null : auditTrail1.getEvents().get( auditTrail1.getEvents().size() - 1 )).getDate(), ee.getCurationDetails().getLastUpdated() );
                        c.incrementAndGet();
                    }
                } ) );
            }
        }

        long maxWaits = 20_000_000_000L; // 20 s in ns
        long startTime = System.nanoTime();
        for ( Future<?> f : futures ) {
            long elapsed = System.nanoTime() - startTime;
            Assertions.assertThat( f ).succeedsWithin( Math.max( maxWaits - elapsed, 0 ), TimeUnit.NANOSECONDS );
        }

        Assertions.assertThat( c ).hasValue( numThreads * numExperimentsPerThread * numUpdates );
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
        return getJdbcTemplate()
                .queryForObject( "SELECT COUNT(*) FROM AUDIT_TRAIL WHERE ID = ?", Integer.class, atid ) == 0;
    }

    private boolean checkDeletedEvent( Long i ) {
        return getJdbcTemplate()
                .queryForObject( "SELECT COUNT(*) FROM AUDIT_EVENT WHERE ID = ?", Integer.class, i ) == 0;
    }

    private void checkDeletedTrails( Collection<Long> trailIds, Collection<Long> eventIds ) {

        for ( Long id : trailIds ) {
            assertTrue( this.checkDeletedAuditTrail( id ) );
        }

        for ( Long id : eventIds ) {
            assertTrue( this.checkDeletedEvent( id ) );
        }

    }

    private void checkEEAuditTrails( ExpressionExperiment
            ee, Collection<Long> trailIds, Collection<Long> eventIds ) {
        this.checkAuditTrail( ee, trailIds, eventIds );

        Collection<ExperimentalFactor> experimentalFactors = ee.getExperimentalDesign().getExperimentalFactors();
        assertTrue( experimentalFactors.size() > 0 );

    }

}
