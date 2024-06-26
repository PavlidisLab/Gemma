package ubic.gemma.persistence.service.expression.experiment;

import org.hibernate.SessionFactory;
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
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.model.genome.Taxon;

import static org.junit.Assert.*;

@ContextConfiguration
public class ExperimentalFactorDaoTest extends BaseDatabaseTest {

    @Configuration
    @TestComponent
    static class ExperimentalFactorServiceTestContextConfiguration extends BaseDatabaseTestContextConfiguration {

        @Bean
        public ExperimentalFactorDao experimentalFactorDao( SessionFactory sessionFactory ) {
            return new ExperimentalFactorDaoImpl( sessionFactory );
        }
    }

    @Autowired
    private ExperimentalFactorDao experimentalFactorDao;

    @Autowired
    private SessionFactory sessionFactory;

    @Test
    public void testDeleteExperimentalFactor() {
        ExperimentalFactor ef = experimentalFactorDao.create( createExperimentalFactor() );
        experimentalFactorDao.remove( ef );
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
        experimentalFactorDao.remove( ef );

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
}