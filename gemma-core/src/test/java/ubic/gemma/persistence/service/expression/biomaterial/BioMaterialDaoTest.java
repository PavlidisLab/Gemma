package ubic.gemma.persistence.service.expression.biomaterial;

import org.hibernate.Hibernate;
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
import ubic.gemma.model.expression.biomaterial.Treatment;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.util.Thaws;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ContextConfiguration
public class BioMaterialDaoTest extends BaseDatabaseTest5 {

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
    public void findAllSubBioMaterials() {
        Taxon taxon = new Taxon();
        sessionFactory.getCurrentSession().persist( taxon );
        BioMaterial bm1 = new BioMaterial();
        bm1.setSourceTaxon( taxon );
        bm1 = bioMaterialDao.create( bm1 );
        BioMaterial bm2 = new BioMaterial();
        bm2.setSourceTaxon( taxon );
        bm2.setSourceBioMaterial( bm1 );
        bm2 = bioMaterialDao.create( bm2 );
        BioMaterial bm3 = new BioMaterial();
        bm3.setSourceTaxon( taxon );
        bm3.setSourceBioMaterial( bm2 );
        bm3 = bioMaterialDao.create( bm3 );
        BioMaterial bm4 = new BioMaterial();
        bm4.setSourceTaxon( taxon );
        bm4.setSourceBioMaterial( bm2 );
        bm4 = bioMaterialDao.create( bm4 );
        assertThat( bioMaterialDao.findSubBioMaterials( bm1, false ) )
                .containsExactlyInAnyOrder( bm2, bm3, bm4 );
    }

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
        Thaws.thawBioMaterial( bm );
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
    public void testThawBioMaterialsForBioAssaysWarmsSourceChain() {
        // Build a three-level source chain (bm3 -> bm2 -> bm1) plus per-level treatments
        // and a factorValue with experimentalFactor; attach the leaf to a BioAssay.
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

        BioMaterial bm1 = new BioMaterial();
        bm1.setSourceTaxon( taxon );
        bm1.getTreatments().add( new Treatment() );
        bm1 = bioMaterialDao.create( bm1 );

        BioMaterial bm2 = new BioMaterial();
        bm2.setSourceTaxon( taxon );
        bm2.setSourceBioMaterial( bm1 );
        bm2.getTreatments().add( new Treatment() );
        bm2 = bioMaterialDao.create( bm2 );

        BioMaterial bm3 = new BioMaterial();
        bm3.setSourceTaxon( taxon );
        bm3.setSourceBioMaterial( bm2 );
        bm3.getFactorValues().add( fv );
        bm3.getTreatments().add( new Treatment() );
        bm3 = bioMaterialDao.create( bm3 );

        ArrayDesign ad = new ArrayDesign();
        ad.setName( "test-ad" );
        ad.setShortName( "test-ad" );
        ad.setPrimaryTaxon( taxon );
        sessionFactory.getCurrentSession().persist( ad );

        BioAssay ba = new BioAssay();
        ba.setName( "test-ba" );
        ba.setSampleUsed( bm3 );
        ba.setArrayDesignUsed( ad );
        sessionFactory.getCurrentSession().persist( ba );

        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();

        // Reload BA fresh -- session is clear, sampleUsed is eager but its source chain is
        // lazy proxies.
        BioAssay loaded = sessionFactory.getCurrentSession().get( BioAssay.class, ba.getId() );
        assertThat( loaded ).isNotNull();
        BioMaterial leaf = loaded.getSampleUsed();
        assertThat( leaf ).isNotNull();
        // sourceBioMaterial is a lazy proxy at this point -- ID is readable but the
        // entity isn't initialized.
        assertThat( Hibernate.isInitialized( leaf.getSourceBioMaterial() ) ).isFalse();
        assertThat( Hibernate.isInitialized( leaf.getTreatments() ) ).isFalse();

        bioMaterialDao.thawBioMaterialsForBioAssays( Arrays.asList( loaded ) );

        // Walk the chain: every level must now have its lazy associations initialized.
        BioMaterial cursor = leaf;
        int depth = 0;
        while ( cursor != null ) {
            assertThat( Hibernate.isInitialized( cursor ) ).as( "bm at depth %d is initialized", depth ).isTrue();
            assertThat( Hibernate.isInitialized( cursor.getTreatments() ) )
                    .as( "treatments at depth %d is initialized", depth ).isTrue();
            for ( FactorValue fvc : cursor.getFactorValues() ) {
                assertThat( Hibernate.isInitialized( fvc.getExperimentalFactor() ) )
                        .as( "factorValue.experimentalFactor at depth %d initialized", depth )
                        .isTrue();
            }
            cursor = cursor.getSourceBioMaterial();
            depth++;
        }
        assertThat( depth ).isEqualTo( 3 );
    }

    @Test
    public void testThawBioMaterialsForBioAssaysEmpty() {
        bioMaterialDao.thawBioMaterialsForBioAssays( java.util.Collections.emptyList() );
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

    @Test
    public void testCreateCircularBioMaterial() {
        Taxon taxon = new Taxon();
        sessionFactory.getCurrentSession().persist( taxon );
        BioMaterial bm1 = new BioMaterial();
        bm1.setSourceTaxon( taxon );
        BioMaterial bm2 = new BioMaterial();
        bm2.setSourceTaxon( taxon );

        bm1.setSourceBioMaterial( bm2 );
        bm2.setSourceBioMaterial( bm1 );

        assertThatThrownBy( () -> bioMaterialDao.create( bm1 ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessage( "A sub-biomaterial cannot be its own source." );
    }

    @Test
    public void testRemoveWithSubBioMaterials() {
        Taxon taxon = new Taxon();
        sessionFactory.getCurrentSession().persist( taxon );
        BioMaterial bm1 = new BioMaterial();
        bm1.setSourceTaxon( taxon );
        bm1 = bioMaterialDao.create( bm1 );
        BioMaterial bm2 = new BioMaterial();
        bm2.setSourceTaxon( taxon );
        bm2.setSourceBioMaterial( bm1 );
        bm2 = bioMaterialDao.create( bm2 );
        bioMaterialDao.remove( bm1 );
        assertThat( bm2.getSourceBioMaterial() ).isNull();
    }
}