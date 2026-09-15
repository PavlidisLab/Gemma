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
 */
package ubic.gemma.persistence.service.expression.experiment;

import org.hibernate.NonUniqueObjectException;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseDatabaseTest5;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignDao;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignDaoImpl;
import ubic.gemma.persistence.service.expression.bioAssayData.BioAssayDimensionService;

import java.util.ArrayList;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

/**
 * Fast H2-backed regression guard for the QT entity-identity collision that
 * {@code DataUpdater.addCountData} re-run path tripped (commit {@code ef838e08fc}).
 * <p>
 * <h2>Bug shape</h2>
 * Before the fix, {@code ExpressionExperimentDaoImpl.removeQts} called
 * {@code session.delete(qt)} directly on a {@link QuantitationType} reference
 * handed in by the caller. When that reference was detached but a managed
 * copy with the same id had already entered the {@code PersistenceContext}
 * (e.g. via {@code ee.quantitationTypes} initialisation inside
 * {@code ensureEeInSession} or the {@code ee.getQuantitationTypes().contains(qt)}
 * assertion at line 4671 of {@code ExpressionExperimentDaoImpl}),
 * {@code StatefulPersistenceContext.checkUniqueness} threw
 * {@link NonUniqueObjectException} ("QuantitationType#170"). The fix
 * re-resolves the QT through {@code session.get(QuantitationType.class, id)}
 * before deletion so the managed instance is what gets deleted.
 * <p>
 * <h2>Coverage scope</h2>
 * The original {@code DataUpdaterTest.testLoadRNASeqData} that surfaced the
 * bug is {@code @Tag("slow")} and not in the default {@code mvn verify}
 * surefire path. The full {@code DataUpdater.addCountData} orchestration
 * (GEO load, BLAT, NCBI, preprocessor, sample-correlation, PCA, etc.) is
 * impractical to wire on top of {@code BaseDatabaseTest5}; this test pivots
 * to a tighter surface that still pins the actual fix: the
 * {@code ExpressionExperimentDataVectorServiceImpl.replaceAllRawDataVectors}
 * → {@code removeRawDataVectors} → {@code removeQts} chain, driven with a
 * detached caller-side QT just as {@code DataUpdaterImpl.replaceData} does
 * in the addCountData re-run path. Without the fix, this test reproduces
 * the {@code NonUniqueObjectException}; with the fix, the replace succeeds
 * and the old QT/vectors are gone.
 * <p>
 * Bug #2 in commit {@code ef838e08fc} (the {@code addCountData} re-thaw
 * after {@code replaceData} that surfaced the duplicate-name guard) is an
 * orchestration-level concern in {@code DataUpdaterImpl} and remains
 * covered by the slow {@code DataUpdaterTest.testLoadRNASeqData} second
 * invocation. The DAO-level fix in {@code removeQts} is the one this test
 * pins; the two fixes ship together but are independent (bug #2 cannot
 * happen below the addCountData orchestration).
 */
@ContextConfiguration
public class ReplaceAllRawDataVectorsQtCollisionTest extends BaseDatabaseTest5 {

    @Configuration
    @TestComponent
    static class ReplaceAllRawDataVectorsQtCollisionTestContextConfiguration extends BaseDatabaseTestContextConfiguration {

        @Bean
        public ExpressionExperimentDao expressionExperimentDao( SessionFactory sessionFactory ) {
            return new ExpressionExperimentDaoImpl( sessionFactory );
        }

        @Bean
        public ArrayDesignDao arrayDesignDao( SessionFactory sessionFactory ) {
            return new ArrayDesignDaoImpl( sessionFactory );
        }

        @Bean
        public SingleCellDimensionExperimentDao singleCellDimensionExperimentDao( SessionFactory sessionFactory ) {
            return new SingleCellDimensionExperimentDaoImpl( sessionFactory );
        }

        /**
         * BAD is pre-persisted in the test (id != null), so {@code createDimensionIfNecessary}
         * short-circuits without invoking the service. Mock is safe.
         */
        @Bean
        public BioAssayDimensionService bioAssayDimensionService() {
            return mock();
        }

        /**
         * QTs are persisted directly via the session in the test setup, so the raw replace
         * path never reaches {@code createQuantitationTypeIfNecessary}. Mock is safe.
         */
        @Bean
        public QuantitationTypeService quantitationTypeService() {
            return mock();
        }

        @Bean
        public ExpressionExperimentDataVectorService expressionExperimentDataVectorService(
                ExpressionExperimentDao expressionExperimentDao,
                BioAssayDimensionService bioAssayDimensionService,
                QuantitationTypeService quantitationTypeService ) {
            return new ExpressionExperimentDataVectorServiceImpl( expressionExperimentDao,
                    bioAssayDimensionService, quantitationTypeService );
        }
    }

    @Autowired
    private ExpressionExperimentDataVectorService dataVectorService;

    @Autowired
    private ExpressionExperimentDao expressionExperimentDao;

    /**
     * Reproduce the {@link NonUniqueObjectException} bug from commit {@code ef838e08fc}.
     * <p>
     * Recipe (matches the addCountData re-run path that surfaced the bug):
     * <ol>
     *   <li>Persist EE + platform + BAD + QT_old + raw vectors via the DAO.</li>
     *   <li>Flush + evict the EE so the caller-side reference is detached, exactly as
     *       {@code DataUpdaterImpl.addCountData} holds across the {@code replaceData}
     *       transaction boundary.</li>
     *   <li>Build new vectors carrying a fresh QT_new (also persistent).</li>
     *   <li>Call {@code dataVectorService.replaceAllRawDataVectors(ee_detached, newVectors)}.
     *       Internally this calls {@code addRawDataVectors(QT_new)} (which initialises
     *       {@code ee.quantitationTypes}, bringing the managed QT_old into the session)
     *       then the stray-removal loop calls {@code removeRawDataVectors(ee, QT_old_detached)}
     *       — the DAO-side {@code ensureEeInSession} + {@code removeQts} are where the
     *       collision used to fire.</li>
     * </ol>
     * Assertions:
     * <ul>
     *   <li>No {@link NonUniqueObjectException} (the regression guard for the fix).</li>
     *   <li>QT_old is no longer attached to the EE; QT_new is attached.</li>
     *   <li>Raw vector count matches the new vector count.</li>
     * </ul>
     */
    @Test
    public void replaceAllRawDataVectorsWithDetachedExistingQtDoesNotCollide() {
        // --- setup: persist EE + platform + old QT + old raw vectors via the DAO ---
        ExpressionExperiment ee = new ExpressionExperiment();
        QuantitationType qtOld = newQt( "Counts-old", true );
        sessionFactory.getCurrentSession().persist( qtOld );
        ee.getQuantitationTypes().add( qtOld );

        ArrayDesign platform = createPlatform();
        BioAssayDimension bad = new BioAssayDimension();
        sessionFactory.getCurrentSession().persist( bad );
        for ( CompositeSequence cs : platform.getCompositeSequences() ) {
            RawExpressionDataVector v = new RawExpressionDataVector();
            v.setBioAssayDimension( bad );
            v.setDesignElement( cs );
            v.setExpressionExperiment( ee );
            v.setQuantitationType( qtOld );
            v.setData( new byte[0] );
            ee.getRawExpressionDataVectors().add( v );
        }
        ee = expressionExperimentDao.create( ee );
        Long eeId = ee.getId();
        Long badId = bad.getId();
        Long platformId = platform.getId();

        // --- detach pass 1: clear the session, reload the EE, force-initialise the raw
        // vectors so the QT_old reference is captured on the still-managed graph. ---
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();
        ExpressionExperiment eeReloaded = expressionExperimentDao.load( eeId );
        assertThat( eeReloaded ).isNotNull();
        // Force-init the raw vector graph (vectors + their QT + their BAD + their CS)
        // AND the quantitationTypes collection — this mirrors experimentService.thaw(ee)
        // in DataUpdaterImpl.addCountData (line 235 reads ee.getQuantitationTypes()),
        // so the detached EE the caller hands to replaceAllRawDataVectors has both
        // collections initialised.
        eeReloaded.getQuantitationTypes().size();
        eeReloaded.getBioAssays().size(); // checkVectors → ee.getBioAssays().containsAll(...)
        eeReloaded.getRawExpressionDataVectors().size();
        for ( RawExpressionDataVector v : eeReloaded.getRawExpressionDataVectors() ) {
            v.getQuantitationType().getName();
            v.getBioAssayDimension().getId();
            v.getDesignElement().getName();
        }

        // --- detach pass 2: clear again so the entire eeReloaded graph (including
        // QT_old, BAD, vectors, CompositeSequences) is now DETACHED — exactly the
        // shape DataUpdaterImpl.addCountData hands to replaceAllRawDataVectors after
        // the cross-transaction thaw. ---
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();

        // --- now build the new vectors. The BAD must be re-loaded as a managed instance
        // (createDimensionIfNecessary short-circuits on id != null but Hibernate still
        // needs to resolve any proxy state when the vectors are persisted), as must the
        // CompositeSequences (FK lookups on insert). ---
        BioAssayDimension badManaged = sessionFactory.getCurrentSession().get( BioAssayDimension.class, badId );
        ArrayDesign platformManaged = sessionFactory.getCurrentSession().get( ArrayDesign.class, platformId );
        QuantitationType qtNew = newQt( "Counts-new", true );
        sessionFactory.getCurrentSession().persist( qtNew );

        // The caller-side EE: detached, carrying detached QT_old in both its
        // quantitationTypes collection AND its raw vectors. We pass THIS ee into the
        // service, mirroring DataUpdaterImpl.replaceData's call shape. The vectors we
        // pass in, however, carry managed BAD + CS so insert side doesn't blow up on
        // unrelated proxies.
        Collection<RawExpressionDataVector> newVectors = new ArrayList<>();
        for ( CompositeSequence cs : platformManaged.getCompositeSequences() ) {
            RawExpressionDataVector nv = new RawExpressionDataVector();
            nv.setBioAssayDimension( badManaged );
            nv.setDesignElement( cs );
            nv.setExpressionExperiment( eeReloaded );
            nv.setQuantitationType( qtNew );
            nv.setData( new byte[0] );
            newVectors.add( nv );
        }

        // --- act + regression guard: must not throw NonUniqueObjectException ---
        final ExpressionExperiment eeArg = eeReloaded;
        assertThatCode( () -> dataVectorService.replaceAllRawDataVectors( eeArg, newVectors ) )
                .doesNotThrowAnyException();

        // --- assertions: old QT/vectors gone, new QT/vectors present ---
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();
        ExpressionExperiment eeAfter = expressionExperimentDao.load( eeId );
        assertThat( eeAfter ).isNotNull();
        assertThat( eeAfter.getQuantitationTypes() )
                .extracting( QuantitationType::getName )
                .containsExactly( "Counts-new" );
        assertThat( eeAfter.getRawExpressionDataVectors() )
                .hasSize( newVectors.size() )
                .allSatisfy( v -> assertThat( v.getQuantitationType().getName() ).isEqualTo( "Counts-new" ) );
    }

    private QuantitationType newQt( String name, boolean preferred ) {
        QuantitationType qt = new QuantitationType();
        qt.setName( name );
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.COUNT );
        qt.setScale( ScaleType.COUNT );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        qt.setIsPreferred( preferred );
        return qt;
    }

    private ArrayDesign createPlatform() {
        Taxon taxon = new Taxon();
        sessionFactory.getCurrentSession().persist( taxon );
        ArrayDesign platform = new ArrayDesign();
        platform.setPrimaryTaxon( taxon );
        for ( int i = 0; i < 10; i++ ) {
            CompositeSequence cs = new CompositeSequence();
            cs.setName( "cs" + i );
            cs.setArrayDesign( platform );
            platform.getCompositeSequences().add( cs );
        }
        sessionFactory.getCurrentSession().persist( platform );
        return platform;
    }
}
