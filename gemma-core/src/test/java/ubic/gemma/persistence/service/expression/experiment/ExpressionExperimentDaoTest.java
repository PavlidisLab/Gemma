package ubic.gemma.persistence.service.expression.experiment;

import gemma.gsec.acl.domain.AclObjectIdentity;
import gemma.gsec.acl.domain.AclService;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.util.*;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.*;

@ContextConfiguration
@TestExecutionListeners(WithSecurityContextTestExecutionListener.class)
public class ExpressionExperimentDaoTest extends BaseDatabaseTest {

    @Configuration
    @TestComponent
    static class ExpressionExperimentDaoTestContextConfiguration extends BaseDatabaseTestContextConfiguration {

        @Bean
        public ExpressionExperimentDao expressionExperimentDao( SessionFactory sessionFactory ) {
            return new ExpressionExperimentDaoImpl( sessionFactory );
        }
    }

    @Autowired
    private ExpressionExperimentDao expressionExperimentDao;

    @Autowired
    private AclService aclService;

    @Test
    public void testGetFilterableProperties() {
        Assertions.assertThat( expressionExperimentDao.getFilterableProperties() )
                .contains( "experimentalDesign.experimentalFactors.factorValues.characteristics.valueUri" )
                // those are hidden for now (see https://github.com/PavlidisLab/Gemma/pull/789)
                .noneMatch( s -> s.startsWith( "experimentalDesign.experimentalFactors.factorValues.characteristics.predicate" ) )
                .noneMatch( s -> s.startsWith( "experimentalDesign.experimentalFactors.factorValues.characteristics.object." ) )
                .noneMatch( s -> s.startsWith( "experimentalDesign.experimentalFactors.factorValues.characteristics.secondPredicate" ) )
                .noneMatch( s -> s.startsWith( "experimentalDesign.experimentalFactors.factorValues.characteristics.secondObject." ) );
    }

    @Test
    public void testThawTransientEntity() {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setExperimentalDesign( new ExperimentalDesign() );
        BioAssay ba = new BioAssay();
        ba.setSampleUsed( new BioMaterial() );
        ba.setArrayDesignUsed( new ArrayDesign() );
        ee.setBioAssays( Collections.singleton( ba ) );
        ee.setRawExpressionDataVectors( Collections.singleton( new RawExpressionDataVector() ) );
        ee.setProcessedExpressionDataVectors( Collections.singleton( new ProcessedExpressionDataVector() ) );
        expressionExperimentDao.thaw( ee );
        expressionExperimentDao.thawBioAssays( ee );
        expressionExperimentDao.thawWithoutVectors( ee );
        expressionExperimentDao.thawForFrontEnd( ee );
    }

    @Test
    public void testThaw() {
        ExpressionExperiment ee = createExpressionExperiment();
        ee = reload( ee );
        assertFalse( Hibernate.isInitialized( ee.getExperimentalDesign() ) );
        expressionExperimentDao.thaw( ee );
        assertTrue( Hibernate.isInitialized( ee.getExperimentalDesign() ) );
    }

    @Test
    public void testThawBioAssays() {
        ExpressionExperiment ee = createExpressionExperiment();
        ee = reload( ee );
        assertFalse( Hibernate.isInitialized( ee.getExperimentalDesign() ) );
        expressionExperimentDao.thawBioAssays( ee );
        assertTrue( Hibernate.isInitialized( ee.getExperimentalDesign() ) );
        assertTrue( Hibernate.isInitialized( ee.getBioAssays() ) );
    }

    @Test
    public void testThawForFrontEnd() {
        ExpressionExperiment ee = createExpressionExperiment();
        ee = reload( ee );
        assertFalse( Hibernate.isInitialized( ee.getExperimentalDesign() ) );
        expressionExperimentDao.thawForFrontEnd( ee );
        assertTrue( Hibernate.isInitialized( ee.getExperimentalDesign() ) );
    }

    @Test
    public void testThawWithoutVectors() {
        ExpressionExperiment ee = createExpressionExperiment();
        ee = reload( ee );
        assertFalse( Hibernate.isInitialized( ee.getExperimentalDesign() ) );
        expressionExperimentDao.thawWithoutVectors( ee );
        assertTrue( Hibernate.isInitialized( ee.getExperimentalDesign() ) );
    }

    @Test
    public void testLoadReference() {
        ExpressionExperiment ee = createExpressionExperiment();
        assertSame( ee, expressionExperimentDao.loadReference( ee.getId() ) );
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().evict( ee );
        ExpressionExperiment proxy = expressionExperimentDao.loadReference( ee.getId() );
        assertFalse( Hibernate.isInitialized( proxy ) );
        assertEquals( ee.getId(), proxy.getId() );
        assertFalse( Hibernate.isInitialized( proxy ) );
    }

    @Test
    public void testLoadMultipleReferences() {
        ExpressionExperiment ee1 = createExpressionExperiment();
        ExpressionExperiment ee2 = createExpressionExperiment();
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().evict( ee1 );
        sessionFactory.getCurrentSession().evict( ee2 );
        Collection<ExpressionExperiment> ees = expressionExperimentDao.loadReference( Arrays.asList( ee1.getId(), ee2.getId() ) );
        for ( ExpressionExperiment ee : ees ) {
            assertFalse( Hibernate.isInitialized( ee ) );
        }
    }

    @Test
    @WithMockUser
    public void testGetTechnologyTypeUsageFrequency() {
        expressionExperimentDao.getTechnologyTypeUsageFrequency();
        expressionExperimentDao.getTechnologyTypeUsageFrequency( Collections.singleton( 1L ) );
    }

    @Test
    @WithMockUser
    public void testGetArrayDesignUsageFrequency() {
        expressionExperimentDao.getArrayDesignsUsageFrequency( -1 );
        expressionExperimentDao.getArrayDesignsUsageFrequency( Collections.singleton( 1L ), -1 );
    }

    @Test
    @WithMockUser
    public void testGetOriginalPlatformUsageFrequency() {
        expressionExperimentDao.getOriginalPlatformsUsageFrequency( -1 );
        expressionExperimentDao.getOriginalPlatformsUsageFrequency( Collections.singleton( 1L ), -1 );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testGetCategoriesWithUsageFrequency() {
        Characteristic c = createCharacteristic( "foo", "foo", "bar", "bar" );
        Assertions.assertThat( expressionExperimentDao.getCategoriesUsageFrequency( null, null, null, null ) )
                .containsEntry( c, 1L );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testGetAnnotationUsageFrequency() {
        Characteristic c = createCharacteristic( "foo", "foo", "bar", "bar" );
        Assertions.assertThat( expressionExperimentDao.getAnnotationsUsageFrequency( null, null, 10, 1, null, null, null, null ) )
                .containsEntry( c, 1L );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testGetAnnotationUsageFrequencyRetainMentionedTerm() {
        Characteristic c = createCharacteristic( "foo", "foo", "bar", "http://bar" );
        Characteristic c1 = createCharacteristic( "foo", "foo", "bar", "bar" );
        Assertions.assertThat( expressionExperimentDao.getAnnotationsUsageFrequency( null, null, 10, 2, null, null, null, Collections.singleton( "http://bar" ) ) )
                .containsEntry( c, 1L ) // bypasses the minimum frequency requirement
                .doesNotContainKey( c1 );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testGetAnnotationUsageFrequencyExcludingFreeTextTerms() {
        Characteristic c = createCharacteristic( "foo", "foo", "bar", "bar" );
        Characteristic c1 = createCharacteristic( "foo", "foo", "bar", null );
        Map<Characteristic, Long> cs = expressionExperimentDao.getAnnotationsUsageFrequency( null, null, 10, 1, null, null, Collections.singleton( null ), null );
        Assertions.assertThat( cs )
                .containsEntry( c, 1L )
                .doesNotContainKey( c1 );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testGetAnnotationUsageFrequencyExcludingFreeTextCategories() {
        Characteristic c = createCharacteristic( "foo", "foo", "bar", "http://bar" );
        Characteristic c1 = createCharacteristic( "foo", null, "bar", null );
        Characteristic c2 = createCharacteristic( null, null, "bar", null );
        Assertions.assertThat( expressionExperimentDao.getAnnotationsUsageFrequency( null, null, 10, 1, null, Collections.singleton( null ), null, null ) )
                .containsEntry( c, 1L )
                .doesNotContainKey( c1 )
                .containsEntry( c2, 1L ); // uncategorized is not a free-text category
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testGetAnnotationUsageFrequencyExcludingUncategorizedTerms() {
        Characteristic c = createCharacteristic( "foo", "foo", "bar", "http://bar" );
        Characteristic c1 = createCharacteristic( "bar", null, "bar", "http://bar" );
        Characteristic c2 = createCharacteristic( null, null, "bar", "http://bar" );
        Assertions.assertThat( expressionExperimentDao.getAnnotationsUsageFrequency( null, null, 10, 1, null, Collections.singleton( ExpressionExperimentDao.UNCATEGORIZED ), null, null ) )
                .containsEntry( c, 1L )
                .containsEntry( c1, 1L ) // free-text category is not uncategorized
                .doesNotContainKey( c2 );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testGetAnnotationUsageFrequencyWithUncategorizedCategory() {
        Characteristic c = createCharacteristic( null, null, "bar", "bar" );
        Characteristic c1 = createCharacteristic( "foo", "foo", "bar", null );
        Characteristic c2 = createCharacteristic( "foo", null, "bar", null );
        Assertions.assertThat( expressionExperimentDao.getAnnotationsUsageFrequency( null, null, 10, 1, ExpressionExperimentDao.UNCATEGORIZED, null, null, null ) )
                .containsEntry( c, 1L )
                .doesNotContainKey( c1 )
                .doesNotContainKey( c2 );
    }

    private Characteristic createCharacteristic( @Nullable String category, @Nullable String categoryUri, String value, @Nullable String valueUri ) {
        ExpressionExperiment ee = new ExpressionExperiment();
        sessionFactory.getCurrentSession().persist( ee );
        aclService.createAcl( new AclObjectIdentity( ExpressionExperiment.class, ee.getId() ) );
        Characteristic c = new Characteristic();
        c.setCategory( category );
        c.setCategoryUri( categoryUri );
        c.setValue( value );
        c.setValueUri( valueUri );
        sessionFactory.getCurrentSession().persist( c );
        sessionFactory.getCurrentSession()
                .createSQLQuery( "insert into EXPRESSION_EXPERIMENT2CHARACTERISTIC (ID, CATEGORY, CATEGORY_URI, `VALUE`, VALUE_URI, EXPRESSION_EXPERIMENT_FK, LEVEL) values (:id, :category, :categoryUri, :value, :valueUri, :eeId, :level)" )
                .setParameter( "id", c.getId() )
                .setParameter( "category", c.getCategory() )
                .setParameter( "categoryUri", c.getCategoryUri() )
                .setParameter( "value", c.getValue() )
                .setParameter( "valueUri", c.getValueUri() )
                .setParameter( "eeId", ee.getId() )
                .setParameter( "level", ExpressionExperiment.class )
                .executeUpdate();
        return c;
    }

    @Test
    @WithMockUser("bob")
    public void testGetPerTaxonCount() {
        ExpressionExperiment ee1 = createExpressionExperiment();
        ExpressionExperiment ee2 = createExpressionExperiment();

        Taxon taxon = new Taxon();
        sessionFactory.getCurrentSession().persist( taxon );
        ee1.setTaxon( taxon );

        Taxon taxon2 = new Taxon();
        sessionFactory.getCurrentSession().persist( taxon2 );
        ee2.setTaxon( taxon2 );

        Map<Taxon, Long> counts = expressionExperimentDao.getPerTaxonCount();
        assertTrue( counts.isEmpty() );

        // FIXME
        // allow bob to see the dataset
        // MutableAcl acl = aclService.createAcl( new AclObjectIdentity( ee1 ) );
        // Sid bobSid = new AclPrincipalSid( "bob" );
        // assertEquals( bobSid, acl.getOwner() );
        // aclService.updateAcl( acl );

        // counts = expressionExperimentDao.getPerTaxonCount();
        // assertTrue( counts.containsKey( taxon ) );
        // assertEquals( 1L, counts.get( taxon ).longValue() );
    }

    @Test
    @WithMockUser
    public void testFilterAndCountByArrayDesign() {
        Filter f = expressionExperimentDao.getFilter( "bioAssays.arrayDesignUsed.id", Long.class, Filter.Operator.eq, 1L );
        assertEquals( "ee", f.getObjectAlias() );
        assertEquals( "id", f.getPropertyName() );
        assertTrue( f.getRequiredValue() instanceof Subquery );
        expressionExperimentDao.load( Filters.by( f ), null );
        expressionExperimentDao.count( Filters.by( f ) );
    }

    @Test
    public void testSubquery() {
        Filter f = expressionExperimentDao.getFilter( "allCharacteristics.valueUri", Filter.Operator.in, Collections.singleton( "http://example.com" ) );
        Assertions.assertThat( f.getOperator() )
                .isEqualTo( Filter.Operator.inSubquery );
        Assertions.assertThat( f.getRequiredValue() )
                .isNotNull()
                .asInstanceOf( InstanceOfAssertFactories.type( Subquery.class ) )
                .satisfies( s -> {
                    Assertions.assertThat( s )
                            .hasFieldOrPropertyWithValue( "entityName", "ubic.gemma.model.expression.experiment.ExpressionExperiment" )
                            .hasFieldOrPropertyWithValue( "propertyName", "id" );
                    Assertions.assertThat( s.getAliases() )
                            .contains( new Subquery.Alias( null, "allCharacteristics", "ac" ) );
                    Assertions.assertThat( s.getFilter() )
                            .hasFieldOrPropertyWithValue( "objectAlias", "ac" )
                            .hasFieldOrPropertyWithValue( "propertyName", "valueUri" )
                            .hasFieldOrPropertyWithValue( "operator", Filter.Operator.in )
                            .hasFieldOrPropertyWithValue( "requiredValue", Collections.singletonList( "http://example.com" ) );
                } );
        assertEquals( " and (ee.id in (select e.id from ubic.gemma.model.expression.experiment.ExpressionExperiment e join e.allCharacteristics ac where ac.valueUri in (:ac_valueUri1)))",
                FilterQueryUtils.formRestrictionClause( Filters.by( f ) ) );
    }

    @Test
    public void testSubqueryWithMultipleJointures() {
        Filter f = expressionExperimentDao.getFilter( "experimentalDesign.experimentalFactors.factorValues.characteristics.valueUri", Filter.Operator.in, Collections.singleton( "http://example.com" ) );
        Assertions.assertThat( f.getOperator() )
                .isEqualTo( Filter.Operator.inSubquery );
        Assertions.assertThat( f.getRequiredValue() )
                .isNotNull()
                .asInstanceOf( InstanceOfAssertFactories.type( Subquery.class ) )
                .satisfies( s -> {
                    Assertions.assertThat( s )
                            .hasFieldOrPropertyWithValue( "entityName", "ubic.gemma.model.expression.experiment.ExpressionExperiment" )
                            .hasFieldOrPropertyWithValue( "propertyName", "id" );
                    Assertions.assertThat( s.getAliases() )
                            .containsExactly( new Subquery.Alias( null, "experimentalDesign", "alias1" ),
                                    new Subquery.Alias( "alias1", "experimentalFactors", "alias2" ),
                                    new Subquery.Alias( "alias2", "factorValues", "alias3" ),
                                    new Subquery.Alias( "alias3", "characteristics", "fvc" ) );
                    Assertions.assertThat( s.getFilter() )
                            .hasFieldOrPropertyWithValue( "objectAlias", "fvc" )
                            .hasFieldOrPropertyWithValue( "propertyName", "valueUri" )
                            .hasFieldOrPropertyWithValue( "operator", Filter.Operator.in )
                            .hasFieldOrPropertyWithValue( "requiredValue", Collections.singletonList( "http://example.com" ) );
                } );
        // assertEquals( "and (ee.id in (select e.id from ubic.gemma.model.expression.experiment.ExpressionExperiment e join e.allCharacteristics ac where ac.valueUri in (:ac_valueUri1)))",
        //         FilterQueryUtils.formRestrictionClause( Filters.by( f ) ) );
    }

    @Test
    public void testRemoveExperimentWithSharedBioMaterial() {
        Taxon taxon = new Taxon();
        sessionFactory.getCurrentSession().persist( taxon );
        ArrayDesign arrayDesign = new ArrayDesign();
        arrayDesign.setPrimaryTaxon( taxon );
        sessionFactory.getCurrentSession().persist( arrayDesign );
        BioMaterial bm = new BioMaterial();
        bm.setSourceTaxon( taxon );
        sessionFactory.getCurrentSession().persist( bm );
        BioAssay ba1 = new BioAssay();
        ba1.setArrayDesignUsed( arrayDesign );
        ba1.setSampleUsed( bm );
        bm.getBioAssaysUsedIn().add( ba1 );
        BioAssay ba2 = new BioAssay();
        ba2.setArrayDesignUsed( arrayDesign );
        ba2.setSampleUsed( bm );
        ExpressionExperiment ee1 = new ExpressionExperiment();
        ee1.getBioAssays().add( ba1 );
        ExpressionExperiment ee2 = new ExpressionExperiment();
        ee2.getBioAssays().add( ba2 );
        sessionFactory.getCurrentSession().persist( ee1 );
        sessionFactory.getCurrentSession().persist( ee2 );
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();
        ee1 = expressionExperimentDao.load( ee1.getId() );
        assertNotNull( ee1 );
        expressionExperimentDao.remove( ee1 );
        // verify that the sample still exists and is still attached to ee2
        bm = ( BioMaterial ) sessionFactory.getCurrentSession().get( BioMaterial.class, bm.getId() );
        assertNotNull( bm );
        assertFalse( bm.getBioAssaysUsedIn().contains( ba1 ) );
        assertTrue( bm.getBioAssaysUsedIn().contains( ba2 ) );
    }

    private ExpressionExperiment reload( ExpressionExperiment e ) {
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().evict( e );
        return expressionExperimentDao.load( e.getId() );
    }

    private ExpressionExperiment createExpressionExperiment() {
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setExperimentalDesign( new ExperimentalDesign() );
        return expressionExperimentDao.create( ee );
    }
}