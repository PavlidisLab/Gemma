/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
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
package ubic.gemma.persistence.persister;

import org.junit.Test;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.persistence.service.expression.experiment.EeWriteService;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

/**
 * Pins the behaviourally load-bearing QuantitationType dedup semantic that was
 * lifted out of {@code CommonPersister.persistQuantitationType} into a per-EE
 * local map in {@link EeWriteServiceImpl}.
 * <p>
 * Why this test exists: {@code AbstractPersister.persist} runs under
 * {@link org.hibernate.FlushMode#MANUAL} so a DAO {@code find} cannot see an
 * in-flight {@code create}. Without the local map, N data vectors sharing the
 * same {@code (name, description)} QT would each trigger a fresh DAO create —
 * N duplicate rows. The dedup key matches
 * {@code BusinessKey.matches(QT,QT)} semantics:
 * {@code hashCode(name) + hashCode(description)}.
 *
 * @author Phase 3 (persister QT cache lift)
 */
public class EeWriteServiceImplQtDedupTest extends BaseSpringContextTest {

    @Autowired
    private EeWriteService eeWriteService;

    private EeWriteServiceImpl unwrap() {
        Object target = AopProxyUtils.getSingletonTarget( eeWriteService );
        return ( EeWriteServiceImpl ) ( target != null ? target : eeWriteService );
    }

    /**
     * Three equal-by-(name, description) QT instances hitting the helper with
     * one shared local map must all resolve to the SAME persistent QT entity,
     * and only one QUANTITATION_TYPE row must be inserted.
     */
    @Test
    @Transactional
    public void testQuantitationTypeDedupWithinSingleEePersist() {
        int before = countRowsInTable( "QUANTITATION_TYPE" );

        String name = "qt_dedup_" + randomName();
        String description = "shared QT for dedup test";

        QuantitationType a = newQt( name, description );
        QuantitationType b = newQt( name, description );
        QuantitationType c = newQt( name, description );

        Map<Integer, QuantitationType> qtCache = new HashMap<>();
        EeWriteServiceImpl impl = unwrap();
        QuantitationType pa = impl.findOrCreateQuantitationType( a, qtCache );
        QuantitationType pb = impl.findOrCreateQuantitationType( b, qtCache );
        QuantitationType pc = impl.findOrCreateQuantitationType( c, qtCache );

        assertNotNull( pa.getId() );
        // All three must resolve to the same persistent QT; the cache stops the
        // second + third calls from creating duplicates while the first create
        // is still in the manual-flush window.
        assertSame( pa, pb );
        assertSame( pa, pc );
        assertEquals( pa.getId(), pb.getId() );
        assertEquals( pa.getId(), pc.getId() );

        // Exactly one row was inserted into QUANTITATION_TYPE — not three.
        int after = countRowsInTable( "QUANTITATION_TYPE" );
        assertEquals( "QT dedup failed: expected 1 new QT row, got " + ( after - before ),
                1, after - before );
    }

    /**
     * Distinct-by-(name, description) QT instances hitting the helper with one
     * shared map must each produce a separate persistent row — the dedup is
     * keyed on (name, description), not on object identity.
     */
    @Test
    @Transactional
    public void testQuantitationTypesWithDifferentDescriptionsAreNotDeduped() {
        int before = countRowsInTable( "QUANTITATION_TYPE" );

        String name = "qt_distinct_" + randomName();

        QuantitationType a = newQt( name, "first variant" );
        QuantitationType b = newQt( name, "second variant" );

        Map<Integer, QuantitationType> qtCache = new HashMap<>();
        EeWriteServiceImpl impl = unwrap();
        QuantitationType pa = impl.findOrCreateQuantitationType( a, qtCache );
        QuantitationType pb = impl.findOrCreateQuantitationType( b, qtCache );

        assertNotNull( pa.getId() );
        assertNotNull( pb.getId() );
        // Distinct (name, description) → distinct rows.
        assertEquals( "QT dedup keyed wrong: distinct descriptions collapsed to one row",
                2, countRowsInTable( "QUANTITATION_TYPE" ) - before );
        // And the entities returned should not be the same instance.
        if ( pa.getId().equals( pb.getId() ) ) {
            throw new AssertionError( "Different (name, description) QTs collapsed to same row" );
        }
    }

    private QuantitationType newQt( String name, String description ) {
        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setName( name );
        qt.setDescription( description );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.AMOUNT );
        qt.setScale( ScaleType.LINEAR );
        qt.setIsBackground( false );
        qt.setIsBackgroundSubtracted( false );
        qt.setIsNormalized( false );
        qt.setIsRatio( false );
        qt.setIsMaskedPreferred( false );
        qt.setIsPreferred( true );
        return qt;
    }
}
