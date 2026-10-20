package ubic.gemma.persistence.service.expression.arrayDesign;

import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseDatabaseTest5;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.DatabaseType;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.persistence.util.Filter;

import javax.annotation.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@ContextConfiguration
@TestExecutionListeners(value = WithSecurityContextTestExecutionListener.class,
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class ArrayDesignDaoTest extends BaseDatabaseTest5 {

    @Configuration
    @TestComponent
    static class ArrayDesignDaoTestContextConfiguration extends BaseDatabaseTestContextConfiguration {

        @Bean
        public ArrayDesignDao arrayDesignDao( SessionFactory sessionFactory ) {
            return new ArrayDesignDaoImpl( sessionFactory );
        }
    }

    @Autowired
    private ArrayDesignDao arrayDesignDao;

    @Test
    @Tag("slow")
    public void testThaw() {
        Taxon taxon = Taxon.Factory.newInstance( "test" );
        sessionFactory.getCurrentSession().persist( taxon );
        ArrayDesign ad = new ArrayDesign();
        ad.setPrimaryTaxon( taxon );
        ad = arrayDesignDao.create( ad );
        ExternalDatabase ed = ExternalDatabase.Factory.newInstance( "test", DatabaseType.SEQUENCE );
        sessionFactory.getCurrentSession().persist( ed );

        Set<CompositeSequence> probes = new HashSet<>();
        for ( int i = 0; i < 200; i++ ) {
            CompositeSequence cs = CompositeSequence.Factory.newInstance( "cs" + i, ad );
            BioSequence bs = BioSequence.Factory.newInstance( "s" + i, taxon );
            DatabaseEntry de = DatabaseEntry.Factory.newInstance();
            de.setExternalDatabase( ed );
            bs.setSequenceDatabaseEntry( de );
            cs.setBiologicalCharacteristic( bs );
            probes.add( cs );
        }
        for ( CompositeSequence cs : probes ) {
            sessionFactory.getCurrentSession().persist( cs.getBiologicalCharacteristic().getSequenceDatabaseEntry() );
        }
        for ( CompositeSequence cs : probes ) {
            sessionFactory.getCurrentSession().persist( cs.getBiologicalCharacteristic() );
        }
        ad.setCompositeSequences( probes );
        arrayDesignDao.update( ad );

        arrayDesignDao.thaw( ad );

        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();

        ad = arrayDesignDao.load( ad.getId() );
        assertNotNull( ad );
        assertFalse( Hibernate.isInitialized( ad.getCompositeSequences() ) );
        arrayDesignDao.thaw( ad );
        assertTrue( Hibernate.isInitialized( ad.getCompositeSequences() ) );
        assertTrue( Hibernate.isInitialized( ad.getCompositeSequences().iterator().next().getBiologicalCharacteristic() ) );
        assertTrue( Hibernate.isInitialized( ad.getCompositeSequences().iterator().next().getBiologicalCharacteristic().getSequenceDatabaseEntry() ) );
        assertEquals( 200, ad.getCompositeSequences().size() );

        sessionFactory.getCurrentSession().update( ad );
        sessionFactory.getCurrentSession().flush();
    }

    /**
     * A dataset switched to a generic platform must be able to say what it was submitted on WITHOUT anyone
     * walking its assays — the field lives on BioAssay, and reading it through the sample list costs the whole
     * list for one line of header.
     */
    @Test
    @WithMockUser
    public void testLoadOriginalPlatformValueObjectsForEE() {
        Taxon taxon = Taxon.Factory.newInstance( "opTaxon" );
        sessionFactory.getCurrentSession().persist( taxon );
        ArrayDesign submitted = platform( taxon, "GPL_SUBMITTED" );
        ArrayDesign generic = platform( taxon, "GPL_GENERIC" );

        // two assays, both switched from `submitted` onto `generic`
        ExpressionExperiment ee = experimentWith( taxon, generic, submitted, 2 );

        assertThat( arrayDesignDao.loadOriginalPlatformValueObjectsForEE( ee.getId() ) )
                .singleElement()
                .hasFieldOrPropertyWithValue( "shortName", "GPL_SUBMITTED" );
        // and the used platform is still what the other call answers
        assertThat( arrayDesignDao.loadValueObjectsForEE( ee.getId() ) )
                .singleElement()
                .hasFieldOrPropertyWithValue( "shortName", "GPL_GENERIC" );
    }

    /**
     * 🛑 A no-op switch — the original recorded as the platform already in use — must NOT come back. Otherwise
     * "as originally submitted" renders on every dataset, naming the platform the reader is already looking at.
     */
    @Test
    @WithMockUser
    public void testANoopSwitchIsNotAnOriginalPlatform() {
        Taxon taxon = Taxon.Factory.newInstance( "noopTaxon" );
        sessionFactory.getCurrentSession().persist( taxon );
        ArrayDesign same = platform( taxon, "GPL_SAME" );

        ExpressionExperiment ee = experimentWith( taxon, same, same, 2 );

        assertThat( arrayDesignDao.loadOriginalPlatformValueObjectsForEE( ee.getId() ) ).isEmpty();
    }

    /** A dataset that was never switched has no original at all. */
    @Test
    @WithMockUser
    public void testNeverSwitchedHasNoOriginalPlatform() {
        Taxon taxon = Taxon.Factory.newInstance( "plainTaxon" );
        sessionFactory.getCurrentSession().persist( taxon );
        ArrayDesign used = platform( taxon, "GPL_ONLY" );

        ExpressionExperiment ee = experimentWith( taxon, used, null, 2 );

        assertThat( arrayDesignDao.loadOriginalPlatformValueObjectsForEE( ee.getId() ) ).isEmpty();
    }

    /** More than one submitted platform is legitimate — the assays need not have come from a single one. */
    @Test
    @WithMockUser
    public void testADatasetCanHaveMoreThanOneOriginalPlatform() {
        Taxon taxon = Taxon.Factory.newInstance( "multiTaxon" );
        sessionFactory.getCurrentSession().persist( taxon );
        ArrayDesign generic = platform( taxon, "GPL_GENERIC_M" );
        ArrayDesign firstSubmitted = platform( taxon, "GPL_SUBMITTED_A" );
        ArrayDesign secondSubmitted = platform( taxon, "GPL_SUBMITTED_B" );

        ExpressionExperiment ee = experimentWith( taxon, generic, firstSubmitted, 1 );
        addAssay( ee, taxon, generic, secondSubmitted, "extra" );
        sessionFactory.getCurrentSession().flush();

        assertThat( arrayDesignDao.loadOriginalPlatformValueObjectsForEE( ee.getId() ) )
                .extracting( "shortName" )
                .containsExactlyInAnyOrder( "GPL_SUBMITTED_A", "GPL_SUBMITTED_B" );
    }

    private ArrayDesign platform( Taxon taxon, String shortName ) {
        ArrayDesign ad = new ArrayDesign();
        ad.setPrimaryTaxon( taxon );
        ad.setShortName( shortName );
        ad.setName( shortName );
        ad.setTechnologyType( TechnologyType.ONECOLOR );
        return arrayDesignDao.create( ad );
    }

    private ExpressionExperiment experimentWith( Taxon taxon, ArrayDesign used, @Nullable ArrayDesign original, int assays ) {
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setShortName( "EE_" + used.getShortName() + "_" + original );
        ee.setName( ee.getShortName() );
        sessionFactory.getCurrentSession().persist( ee );
        for ( int i = 0; i < assays; i++ ) {
            addAssay( ee, taxon, used, original, String.valueOf( i ) );
        }
        sessionFactory.getCurrentSession().flush();
        return ee;
    }

    private void addAssay( ExpressionExperiment ee, Taxon taxon, ArrayDesign used, @Nullable ArrayDesign original, String suffix ) {
        BioMaterial bm = BioMaterial.Factory.newInstance( "bm_" + ee.getShortName() + "_" + suffix );
        bm.setSourceTaxon( taxon );
        sessionFactory.getCurrentSession().persist( bm );
        BioAssay ba = BioAssay.Factory.newInstance();
        ba.setName( "ba_" + ee.getShortName() + "_" + suffix );
        ba.setSampleUsed( bm );
        ba.setArrayDesignUsed( used );
        ba.setOriginalPlatform( original );
        sessionFactory.getCurrentSession().persist( ba );
        ee.getBioAssays().add( ba );
    }

    @Test
    public void testGetFilterableProperties() {
        assertThat( arrayDesignDao.getFilterableProperties() )
                .contains( "taxon", "taxon.ncbiId" )
                // recursive properties are limited
                .doesNotContain( "mergedInto.mergedInto.id" );
    }

    @Test
    public void testGetFilter() {
        assertThat( arrayDesignDao.getFilter( "id", Filter.Operator.eq, "1" ) )
                .hasFieldOrPropertyWithValue( "objectAlias", "ad" )
                .hasFieldOrPropertyWithValue( "propertyName", "id" )
                .hasFieldOrPropertyWithValue( "requiredValue", 1L );
    }

    @Test
    public void testGetFilterWhenPropertyDoesNotExist() {
        assertThatThrownBy( () -> arrayDesignDao.getFilter( "foo.bar", Filter.Operator.eq, "joe" ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessageContainingAll( "foo", ArrayDesign.class.getName() );
    }

    @Test
    public void testGetFilterWhenPropertyExceedsMaxDepth() {
        assertThat( arrayDesignDao.getFilterableProperties() )
                .doesNotContain( "primaryTaxon.externalDatabase.databaseSupplier.name" );
        assertThatThrownBy( () -> arrayDesignDao.getFilter( "primaryTaxon.externalDatabase.databaseSupplier.name", Filter.Operator.eq, "joe" ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessageContainingAll( "primaryTaxon.externalDatabase.databaseSupplier.name", ArrayDesign.class.getName() );
    }

    @Test
    public void testGetFilterTechnologyType() {
        assertThat( arrayDesignDao.getFilterablePropertyType( "technologyType" ) )
                .isAssignableFrom( TechnologyType.class );
        assertThat( arrayDesignDao.getFilterablePropertyDescription( "technologyType" ) )
                .isNull();
        assertThat( arrayDesignDao.getFilterablePropertyAllowedValues( "technologyType" ) )
                .contains( TechnologyType.SEQUENCING );
    }

    @Test
    public void testTaxonPropertyResolutionInGetFilter() {
        assertThat( arrayDesignDao.getFilter( "taxon", Filter.Operator.eq, "1" ) )
                .hasFieldOrPropertyWithValue( "objectAlias", "t" )
                .hasFieldOrPropertyWithValue( "propertyName", "id" )
                .hasFieldOrPropertyWithValue( "requiredValue", 1L );
        assertThat( arrayDesignDao.getFilter( "taxon.ncbiId", Filter.Operator.eq, "9606" ) )
                .hasFieldOrPropertyWithValue( "objectAlias", "t" )
                .hasFieldOrPropertyWithValue( "propertyName", "ncbiId" )
                .hasFieldOrPropertyWithValue( "requiredValue", 9606 );
        assertThat( arrayDesignDao.getFilter( "taxon.commonName", Filter.Operator.eq, "human" ) )
                .hasFieldOrPropertyWithValue( "objectAlias", "t" )
                .hasFieldOrPropertyWithValue( "propertyName", "commonName" )
                .hasFieldOrPropertyWithValue( "requiredValue", "human" );
        assertThat( arrayDesignDao.getFilter( "needsAttention", Filter.Operator.eq, "true" ) )
                .hasFieldOrPropertyWithValue( "objectAlias", "s" )
                .hasFieldOrPropertyWithValue( "propertyName", "needsAttention" )
                .hasFieldOrPropertyWithValue( "requiredValue", true );
        assertThat( arrayDesignDao.getFilter( "technologyType", Filter.Operator.eq, "ONECOLOR" ) )
                .hasFieldOrPropertyWithValue( "objectAlias", "ad" )
                .hasFieldOrPropertyWithValue( "propertyName", "technologyType" )
                .hasFieldOrPropertyWithValue( "requiredValue", TechnologyType.ONECOLOR );
    }

    @Test
    @WithMockUser
    public void testCountExpressionExperiments() {
        Taxon taxon = Taxon.Factory.newInstance( "test" );
        sessionFactory.getCurrentSession().persist( taxon );
        ArrayDesign ad = new ArrayDesign();
        ad.setPrimaryTaxon( taxon );
        ad = arrayDesignDao.create( ad );
        assertThat( arrayDesignDao.countExpressionExperiments( ad ) ).isEqualTo( 0 );
    }

    @Test
    public void testGetGenes() {
        Taxon taxon = Taxon.Factory.newInstance( "test" );
        sessionFactory.getCurrentSession().persist( taxon );
        ArrayDesign ad = new ArrayDesign();
        ad.setPrimaryTaxon( taxon );
        ad = arrayDesignDao.create( ad );
        assertThat( arrayDesignDao.getGenes( ad, true ) ).isEmpty();
        assertThat( arrayDesignDao.getGenes( ad, false ) ).isEmpty();
        assertThat( arrayDesignDao.countGenes( ad, true ) ).isZero();
        assertThat( arrayDesignDao.countGenes( ad, false ) ).isZero();
    }

    @Test
    public void testGenesByCompositeSequence() {
        Taxon taxon = Taxon.Factory.newInstance( "test" );
        sessionFactory.getCurrentSession().persist( taxon );
        ArrayDesign ad = new ArrayDesign();
        ad.setPrimaryTaxon( taxon );
        ad = arrayDesignDao.create( ad );
        arrayDesignDao.getGenesByCompositeSequence( ad, true );
        arrayDesignDao.getGenesByCompositeSequence( ad, false );
        arrayDesignDao.getGenesByCompositeSequence( Collections.singleton( ad ), true );
        arrayDesignDao.getGenesByCompositeSequence( Collections.singleton( ad ), false );
        assertEquals( 0, arrayDesignDao.countCompositeSequencesWithGenes( ad, true ) );
        assertEquals( 0, arrayDesignDao.countCompositeSequencesWithGenes( ad, false ) );
        assertEquals( 0, arrayDesignDao.countCompositeSequencesWithGenes( Collections.singleton( ad ), true ) );
        assertEquals( 0, arrayDesignDao.countCompositeSequencesWithGenes( Collections.singleton( ad ), false ) );
    }
}