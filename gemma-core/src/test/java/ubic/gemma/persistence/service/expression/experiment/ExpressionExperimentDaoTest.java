package ubic.gemma.persistence.service.expression.experiment;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.common.auditAndSecurity.CurationDetailsDao;
import ubic.gemma.persistence.util.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

@ContextConfiguration
public class ExpressionExperimentDaoTest extends BaseDatabaseTest {

    @Configuration
    @TestComponent
    static class ExpressionExperimentDaoTestContextConfiguration extends BaseDatabaseTestContextConfiguration {

        @Bean
        public ExpressionExperimentDao expressionExperimentDao( SessionFactory sessionFactory ) {
            return new ExpressionExperimentDaoImpl( sessionFactory );
        }

        @Bean
        public CurationDetailsDao curationDetailsDao() {
            return mock( CurationDetailsDao.class );
        }
    }

    @Autowired
    private ExpressionExperimentDao expressionExperimentDao;

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
        ee.setBioAssays( Collections.singleton( new BioAssay() ) );
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