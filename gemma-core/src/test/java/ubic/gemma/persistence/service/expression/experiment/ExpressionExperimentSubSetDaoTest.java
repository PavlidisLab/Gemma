package ubic.gemma.persistence.service.expression.experiment;

import org.hibernate.SessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.genome.Taxon;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@ContextConfiguration
public class ExpressionExperimentSubSetDaoTest extends BaseDatabaseTest {

    @Configuration
    @TestComponent
    static class ExpressionExperimentSubSetDaoTestContextConfiguration extends BaseDatabaseTestContextConfiguration {

        @Bean
        public ExpressionExperimentSubSetDao expressionExperimentSubSetDao( SessionFactory sessionFactory ) {
            return new ExpressionExperimentSubSetDaoImpl( sessionFactory );
        }
    }

    @Autowired
    private ExpressionExperimentSubSetDao dao;

    private Taxon taxon;
    private ArrayDesign ad;

    @Before
    public void setUp() {
        taxon = new Taxon();
        sessionFactory.getCurrentSession().persist( taxon );
        ad = new ArrayDesign();
        ad.setPrimaryTaxon( taxon );
        sessionFactory.getCurrentSession().persist( ad );
    }

    @Test
    public void testRemove() {
        BioMaterial bm = new BioMaterial();
        bm.setSourceTaxon( taxon );
        sessionFactory.getCurrentSession().persist( bm );
        BioAssay ba = new BioAssay();
        ba.setArrayDesignUsed( ad );
        ba.setSampleUsed( bm );
        bm.getBioAssaysUsedIn().add( ba );
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.getBioAssays().add( ba );
        sessionFactory.getCurrentSession().persist( ee );
        ExpressionExperimentSubSet subset = new ExpressionExperimentSubSet();
        subset.setSourceExperiment( ee );
        subset.getBioAssays().add( ba );
        subset = dao.create( subset );
        dao.remove( subset );
        assertTrue( sessionFactory.getCurrentSession().contains( ba ) );
        assertTrue( sessionFactory.getCurrentSession().contains( bm ) );
    }

    @Test
    public void testRemoveWhenBioAssayIsOwnedByTheSubset() {
        ExpressionExperiment ee = new ExpressionExperiment();
        sessionFactory.getCurrentSession().persist( ee );
        ExpressionExperimentSubSet subset = new ExpressionExperimentSubSet();
        BioMaterial bm = new BioMaterial();
        bm.setSourceTaxon( taxon );
        sessionFactory.getCurrentSession().persist( bm );
        BioAssay ba = new BioAssay();
        ba.setArrayDesignUsed( ad );
        ba.setSampleUsed( bm );
        bm.getBioAssaysUsedIn().add( ba );
        sessionFactory.getCurrentSession().persist( ba );
        subset.setSourceExperiment( ee );
        subset.getBioAssays().add( ba );
        subset = dao.create( subset );
        dao.remove( subset );
        assertFalse( sessionFactory.getCurrentSession().contains( ba ) );
        assertFalse( sessionFactory.getCurrentSession().contains( bm ) );
    }

    @Test
    public void testRemoveWhenBioMaterialIsUsedByAnotherBioAssay() {
        ExpressionExperiment ee = new ExpressionExperiment();
        sessionFactory.getCurrentSession().persist( ee );
        ExpressionExperimentSubSet subset = new ExpressionExperimentSubSet();
        BioMaterial bm = new BioMaterial();
        bm.setSourceTaxon( taxon );
        sessionFactory.getCurrentSession().persist( bm );
        BioAssay ba = new BioAssay();
        ba.setArrayDesignUsed( ad );
        ba.setSampleUsed( bm );
        bm.getBioAssaysUsedIn().add( ba );
        sessionFactory.getCurrentSession().persist( ba );
        BioAssay ba2 = new BioAssay();
        ba2.setArrayDesignUsed( ad );
        ba2.setSampleUsed( bm );
        bm.getBioAssaysUsedIn().add( ba2 );
        sessionFactory.getCurrentSession().persist( ba2 );
        subset.setSourceExperiment( ee );
        subset.getBioAssays().add( ba );
        subset = dao.create( subset );
        dao.remove( subset );
        assertFalse( sessionFactory.getCurrentSession().contains( ba ) );
        assertTrue( sessionFactory.getCurrentSession().contains( ba2 ) );
        assertTrue( sessionFactory.getCurrentSession().contains( bm ) );
    }
}