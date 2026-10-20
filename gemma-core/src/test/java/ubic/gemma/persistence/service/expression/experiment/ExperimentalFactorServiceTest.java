package ubic.gemma.persistence.service.expression.experiment;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseDatabaseTest5;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialDaoImpl;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialReadService;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialService;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialServiceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ContextConfiguration
public class ExperimentalFactorServiceTest extends BaseDatabaseTest5 {

    @Configuration
    @TestComponent
    static class ExperimentalFactorServiceTestContextConfiguration extends BaseDatabaseTestContextConfiguration {

        @Bean
        public ExperimentalFactorService experimentalFactorService( ExperimentalFactorDao experimentalFactorDao, BioMaterialService bioMaterialService ) {
            return new ExperimentalFactorServiceImpl( experimentalFactorDao, mock(), bioMaterialService );
        }

        @Bean
        public BioMaterialService bioMaterialService( SessionFactory sessionFactory ) {
            // A spy so a test can observe WHEN findByFactor is called relative to the detach in
            // ExperimentalFactorServiceImpl.remove. It delegates to the real implementation, so every other test
            // in this class is unaffected.
            return spy( new BioMaterialServiceImpl( new BioMaterialDaoImpl( sessionFactory ), mock(), mock(), mock(), mock(), mock( BioMaterialReadService.class ) ) );
        }

        @Bean
        public ExperimentalFactorDao experimentalFactorDao( SessionFactory sessionFactory ) {
            return new ExperimentalFactorDaoImpl( sessionFactory );
        }

        @Bean
        public ExperimentalFactorReadService experimentalFactorReadService() {
            return mock( ExperimentalFactorReadService.class );
        }
    }

    @Autowired
    private ExperimentalFactorService experimentalFactorService;

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private BioMaterialService bioMaterialService;

    @Test
    public void testDeleteExperimentalFactor() {
        ExperimentalFactor ef = experimentalFactorService.create( createExperimentalFactor() );
        experimentalFactorService.remove( ef );
    }

    @Test
    public void testDeleteExperimentalFactorUsedByASample() {
        ExpressionExperiment ee = new ExpressionExperiment();
        ExperimentalDesign ed = new ExperimentalDesign();
        ExperimentalFactor ef = new ExperimentalFactor();
        ef.setType( FactorType.CATEGORICAL );
        ef.setExperimentalDesign( ed );
        FactorValue fv = new FactorValue();
        fv.setExperimentalFactor( ef );
        ef.getFactorValues().add( fv );
        ed.getExperimentalFactors().add( ef );
        ee.setExperimentalDesign( ed );
        sessionFactory.getCurrentSession().persist( ee );

        // create a sample using the factor
        ArrayDesign ad = createArrayDesign();
        BioAssay ba = new BioAssay();
        ba.setArrayDesignUsed( ad );
        BioMaterial bm = new BioMaterial();
        bm.setSourceTaxon( ad.getPrimaryTaxon() );
        bm.getFactorValues().add( fv );
        bm.getBioAssaysUsedIn().add( ba );
        ba.setSampleUsed( bm );
        ee.getBioAssays().add( ba );
        sessionFactory.getCurrentSession().persist( bm );

        // reload and remove the factor
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();
        ee = ( ExpressionExperiment ) sessionFactory.getCurrentSession().get( ExpressionExperiment.class, ee.getId() );
        assertNotNull( ee );
        ef = ( ExperimentalFactor ) sessionFactory.getCurrentSession().get( ExperimentalFactor.class, ef.getId() );
        assertNotNull( ef );
        experimentalFactorService.remove( ef );

        ed = ( ExperimentalDesign ) sessionFactory.getCurrentSession().get( ExperimentalDesign.class, ed.getId() );
        assertNotNull( ed );
        assertFalse( ed.getExperimentalFactors().contains( ef ) );

        // reload and verify cascading behaviour
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().evict( ee );
        ee = ( ExpressionExperiment ) sessionFactory.getCurrentSession().get( ExpressionExperiment.class, ee.getId() );
        assertNotNull( ee );
        assertFalse( ee.getExperimentalDesign().getExperimentalFactors().contains( ef ) );
        assertFalse( ee.getBioAssays().iterator().next().getSampleUsed().getFactorValues().contains( fv ) );
        assertNull( sessionFactory.getCurrentSession().get( ExperimentalFactor.class, ef.getId() ) );
        assertNull( sessionFactory.getCurrentSession().get( FactorValue.class, fv.getId() ) );
    }

    private ExperimentalFactor createExperimentalFactor() {
        ExperimentalDesign ed = new ExperimentalDesign();
        sessionFactory.getCurrentSession().persist( ed );
        ExperimentalFactor ef = new ExperimentalFactor();
        ef.setExperimentalDesign( ed );
        ef.setType( FactorType.CATEGORICAL );
        return ef;
    }

    private ArrayDesign createArrayDesign() {
        Taxon taxon = new Taxon();
        ArrayDesign ad = new ArrayDesign();
        ad.setPrimaryTaxon( taxon );
        sessionFactory.getCurrentSession().persist( taxon );
        sessionFactory.getCurrentSession().persist( ad );
        return ad;
    }

    /**
     * 🛑 The factor is STILL IN its design's factor collection when {@code bioMaterialService.findByFactor} is
     * called. Detaching first denies the call.
     * <p>
     * {@code BioMaterialService#findByFactor} is {@code @Secured({"IS_AUTHENTICATED_ANONYMOUSLY","ACL_SECURABLE_READ"})}.
     * An ExperimentalFactor is a SecuredChild, and {@code ParentIdentityRetrievalStrategyImpl} resolves its ACL
     * parent via {@code ExpressionExperimentDao.findIdByFactor}, whose HQL joins {@code ed.experimentalFactors}.
     * Removing the factor from that collection first makes the query auto-flush the pending removal and match no
     * row: null parent identity, no inherited ACL, and the vote denies — for an administrator, because the lookup
     * found nothing rather than a permission being refused.
     * <p>
     * 🛑 This context has no security interceptor, so there is no 403 to assert here; what it pins is the ordering
     * the interceptor depends on. That is also why the first attempt at this bug missed: removing the caller's
     * duplicate detach in {@code applyDesignChange} left this one, one level down, one line before the call that
     * actually trips.
     * <p>
     * cab measured it on GSE19804 twice — 2026-09-09 and again on 2026-09-10 against the first fix, with a
     * byte-identical stack naming {@code ExperimentalFactorServiceImpl.remove} line 70.
     */
    @Test
    public void testFactorIsStillAttachedToItsDesignWhenFindByFactorIsCalled() {
        ExpressionExperiment ee = new ExpressionExperiment();
        ExperimentalDesign ed = new ExperimentalDesign();
        ExperimentalFactor ef = new ExperimentalFactor();
        ef.setType( FactorType.CATEGORICAL );
        ef.setExperimentalDesign( ed );
        ed.getExperimentalFactors().add( ef );
        ee.setExperimentalDesign( ed );
        sessionFactory.getCurrentSession().persist( ee );
        sessionFactory.getCurrentSession().flush();

        java.util.List<Boolean> attachedAtCallTime = new java.util.ArrayList<>();
        doAnswer( inv -> {
            attachedAtCallTime.add( ed.getExperimentalFactors()
                    .contains( ( ExperimentalFactor ) inv.getArgument( 0 ) ) );
            return inv.callRealMethod();
        } ).when( bioMaterialService ).findByFactor( any( ExperimentalFactor.class ) );

        experimentalFactorService.remove( ef );

        assertEquals( 1, attachedAtCallTime.size(), "findByFactor was not called at all" );
        assertTrue( attachedAtCallTime.get( 0 ),
                "the factor was detached from its design before the secured findByFactor call, so the ACL parent "
                        + "lookup that authorizes it would find no row" );
        // the detach still happens, just later -- the cascade concern it exists for is unchanged
        assertFalse( ed.getExperimentalFactors().contains( ef ) );
    }
}
