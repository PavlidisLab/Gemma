package ubic.gemma.persistence.service.expression.arrayDesign;

import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.persistence.util.TestComponent;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.*;

@ContextConfiguration
public class ArrayDesignDaoTest extends BaseDatabaseTest {

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
    @Category(SlowTest.class)
    public void testThaw() {
        Taxon taxon = Taxon.Factory.newInstance( "test" );
        sessionFactory.getCurrentSession().persist( taxon );
        ArrayDesign ad = new ArrayDesign();
        ad.setPrimaryTaxon( taxon );
        ad = arrayDesignDao.create( ad );

        Set<CompositeSequence> probes = new HashSet<>();
        for ( int i = 0; i < 20000; i++ ) {
            CompositeSequence cs = CompositeSequence.Factory.newInstance( "cs" + i, ad );
            BioSequence bs = BioSequence.Factory.newInstance( "s" + i, taxon );
            sessionFactory.getCurrentSession().persist( bs );
            cs.setBiologicalCharacteristic( bs );
            probes.add( cs );
        }
        ad.setCompositeSequences( probes );
        arrayDesignDao.update( ad );

        ArrayDesign thawedAd = arrayDesignDao.thaw( ad );
        assertSame( ad, thawedAd );

        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();

        ad = arrayDesignDao.load( ad.getId() );
        assertNotNull( ad );
        assertFalse( Hibernate.isInitialized( ad.getCompositeSequences() ) );
        ad = arrayDesignDao.thaw( ad );
        assertNotNull( ad );
        assertTrue( Hibernate.isInitialized( ad.getCompositeSequences() ) );
        assertTrue( Hibernate.isInitialized( ad.getCompositeSequences().iterator().next().getBiologicalCharacteristic() ) );
        assertEquals( 20000, ad.getCompositeSequences().size() );

        sessionFactory.getCurrentSession().update( ad );
        sessionFactory.getCurrentSession().flush();
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
                .hasMessageContainingAll( "foo.bar", ArrayDesign.class.getName() );
    }

    @Test
    public void testGetFilterWhenPropertyExceedsMaxDepth() {
        // those properties are unlisted, but are still accessible
        assertThat( arrayDesignDao.getFilterableProperties() )
                .doesNotContain( "primaryTaxon.externalDatabase.databaseSupplier.name" );
        assertThat( arrayDesignDao.getFilter( "primaryTaxon.externalDatabase.databaseSupplier.name", Filter.Operator.eq, "joe" ) )
                .isNotNull();
    }

    @Test
    public void testGetFilterTechnologyType() {
        arrayDesignDao.getFilterablePropertyType( "technologyType" )
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
}