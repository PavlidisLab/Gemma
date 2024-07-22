package ubic.gemma.persistence.service.expression.biomaterial;

import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.Treatment;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Taxon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ContextConfiguration
public class BioMaterialDaoTest extends BaseDatabaseTest {

    @Configuration
    @TestComponent
    static class BioMaterialDaoTestContextConfiguration extends BaseDatabaseTestContextConfiguration {

        @Bean
        public BioMaterialDao bioMaterialDao( SessionFactory sessionFactory ) {
            return new BioMaterialDaoImpl( sessionFactory );
        }
    }

    @Autowired
    private BioMaterialDao bioMaterialDao;

    @Test
    public void testThaw() {
        Taxon taxon = new Taxon();
        sessionFactory.getCurrentSession().persist( taxon );
        ExperimentalDesign ed = new ExperimentalDesign();
        sessionFactory.getCurrentSession().persist( ed );
        ExperimentalFactor ef = new ExperimentalFactor();
        ef.setExperimentalDesign( ed );
        ef.setType( FactorType.CATEGORICAL );
        sessionFactory.getCurrentSession().persist( ef );
        FactorValue fv = new FactorValue();
        fv.setExperimentalFactor( ef );
        sessionFactory.getCurrentSession().persist( fv );
        BioMaterial bm = new BioMaterial();
        bm.setSourceTaxon( taxon );
        bm.getFactorValues().add( fv );
        bm.getTreatments().add( new Treatment() );
        sessionFactory.getCurrentSession().persist( bm );
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();
        bm = bioMaterialDao.load( bm.getId() );
        assertThat( bm ).isNotNull();
        assertThat( bm.getTreatments() )
                .matches( ts -> !Hibernate.isInitialized( ts ) );
        assertThat( bm.getFactorValues() )
                .extracting( FactorValue::getExperimentalFactor )
                .noneMatch( Hibernate::isInitialized );
        bioMaterialDao.thaw( bm );
        assertThat( bm.getTreatments() )
                .matches( Hibernate::isInitialized )
                .hasSize( 1 );
        assertThat( bm.getFactorValues() )
                .satisfies( Hibernate::isInitialized )
                .hasSize( 1 )
                .allSatisfy( fv2 -> {
                    assertThat( fv2.getExperimentalFactor() )
                            .matches( Hibernate::isInitialized );
                } );
    }

    @Test
    public void testCreateWithMultipleFactorValueForSameExperimentalFactor() {
        ExperimentalDesign ed = new ExperimentalDesign();
        sessionFactory.getCurrentSession().persist( ed );
        ExperimentalFactor ef = new ExperimentalFactor();
        ef.setExperimentalDesign( ed );
        ef.setType( FactorType.CATEGORICAL );
        FactorValue fv1 = FactorValue.Factory.newInstance( ef );
        fv1.setValue( "foo" );
        ef.getFactorValues().add( fv1 );
        FactorValue fv2 = FactorValue.Factory.newInstance( ef );
        fv2.setValue( "bar" );
        ef.getFactorValues().add( fv2 );
        sessionFactory.getCurrentSession().persist( ef );
        BioMaterial bm = new BioMaterial();
        bm.getFactorValues().add( fv1 );
        bm.getFactorValues().add( fv2 );
        assertThatThrownBy( () -> bioMaterialDao.create( bm ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessageStartingWith( "BioMaterial has more than one factor values for ExperimentalFactor Id=" + ef.getId() + " Type=CATEGORICAL:" );
    }
}