package ubic.gemma.persistence.service.expression.experiment;

import gemma.gsec.acl.domain.AclObjectIdentity;
import gemma.gsec.acl.domain.AclService;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.model.common.description.Categories;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicUtils;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.*;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.util.*;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

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

    private ExpressionExperiment ee;

    @After
    public void removeFixtures() {
        if ( ee != null ) {
            expressionExperimentDao.remove( ee );
        }
    }

    @Test
    public void loadTroubledIds() {
        Assertions.assertThat( expressionExperimentDao.loadTroubledIds() ).isEmpty();
    }

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
        ee.setNumberOfSamples( 1 );
        ee.setRawExpressionDataVectors( Collections.singleton( new RawExpressionDataVector() ) );
        ee.setProcessedExpressionDataVectors( Collections.singleton( new ProcessedExpressionDataVector() ) );
        ee.setNumberOfDataVectors( 1 );
        expressionExperimentDao.thaw( ee );
        expressionExperimentDao.thawBioAssays( ee );
        expressionExperimentDao.thawLite( ee );
        expressionExperimentDao.thawForFrontEnd( ee );
    }

    @Test
    public void testThaw() {
        ee = createExpressionExperiment();
        ee = reload( ee );
        assertFalse( Hibernate.isInitialized( ee.getExperimentalDesign() ) );
        expressionExperimentDao.thaw( ee );
        assertTrue( Hibernate.isInitialized( ee.getExperimentalDesign() ) );
    }

    @Test
    public void testThawBioAssays() {
        ee = createExpressionExperiment();
        ee = reload( ee );
        assertFalse( Hibernate.isInitialized( ee.getExperimentalDesign() ) );
        expressionExperimentDao.thawBioAssays( ee );
        assertTrue( Hibernate.isInitialized( ee.getExperimentalDesign() ) );
        assertTrue( Hibernate.isInitialized( ee.getBioAssays() ) );
    }

    @Test
    public void testThawForFrontEnd() {
        ee = createExpressionExperiment();
        ee = reload( ee );
        assertFalse( Hibernate.isInitialized( ee.getExperimentalDesign() ) );
        expressionExperimentDao.thawForFrontEnd( ee );
        assertTrue( Hibernate.isInitialized( ee.getExperimentalDesign() ) );
    }

    @Test
    public void testThawLite() {
        ee = createExpressionExperiment();
        ee = reload( ee );
        assertFalse( Hibernate.isInitialized( ee.getExperimentalDesign() ) );
        expressionExperimentDao.thawLite( ee );
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
        Assertions.assertThat( expressionExperimentDao.getCategoriesUsageFrequency( null, null, null, null, -1 ) )
                .containsEntry( CharacteristicUtils.getCategory( c ), 1L );
    }

    @Test
    @WithMockUser
    public void testGetCategoriesUsageFrequencyAsAnonymous() {
        expressionExperimentDao.getCategoriesUsageFrequency( null, null, null, null, -1 );
    }

    /**
     * No ACL filtering is done when explicit IDs are provided, so this should work without {@link WithMockUser}.
     */
    @Test
    public void testGetCategoriesUsageFrequencyWithIds() {
        expressionExperimentDao.getCategoriesUsageFrequency( Collections.singleton( 1L ), null, null, null, -1 );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testGetAnnotationUsageFrequency() {
        Characteristic c = createCharacteristic( "foo", "foo", "bar", "bar" );
        Assertions.assertThat( expressionExperimentDao.getAnnotationsUsageFrequency( null, null, 10, 1, null, null, null, null ) )
                .containsEntry( c, 1L );
    }

    @Test
    @WithMockUser
    public void testGetAnnotationUsageFrequencyAsAnonymous() {
        expressionExperimentDao.getAnnotationsUsageFrequency( null, null, 10, 1, null, null, null, null );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testGetAnnotationUsageFrequencyWithLargeBatch() {
        Characteristic c = createCharacteristic( "foo", "foo", "bar", "bar" );
        List<Long> ees = LongStream.range( 0, 10000 ).boxed().collect( Collectors.toList() );
        Assertions.assertThat( expressionExperimentDao.getAnnotationsUsageFrequency( ees, null, 10, 1, null, null, null, null ) )
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

    /**
     * No ACL filtering is done when explicit IDs are provided, so this should work without {@link WithMockUser}.
     */
    @Test
    public void testGetAnnotationUsageFrequencyWithIds() {
        expressionExperimentDao.getAnnotationsUsageFrequency( Collections.singleton( 1L ), null, 10, 1, null, null, null, null );
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

    @Test
    public void removeWithBioAssayDimension() {
        Taxon taxon = new Taxon();
        sessionFactory.getCurrentSession().persist( taxon );
        ArrayDesign arrayDesign = new ArrayDesign();
        arrayDesign.setPrimaryTaxon( taxon );
        CompositeSequence cs1 = CompositeSequence.Factory.newInstance( "test", arrayDesign );
        arrayDesign.getCompositeSequences().add( cs1 );
        sessionFactory.getCurrentSession().persist( arrayDesign );
        BioMaterial bm = new BioMaterial();
        bm.setSourceTaxon( taxon );
        sessionFactory.getCurrentSession().persist( bm );
        BioAssay ba1 = new BioAssay();
        ba1.setArrayDesignUsed( arrayDesign );
        ba1.setSampleUsed( bm );
        bm.getBioAssaysUsedIn().add( ba1 );
        ee = new ExpressionExperiment();
        ee.getBioAssays().add( ba1 );
        // create quantitations
        QuantitationType qt = new QuantitationType();
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.AMOUNT );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        qt.setScale( ScaleType.COUNT );
        ee.getQuantitationTypes().add( qt );
        BioAssayDimension bad = new BioAssayDimension();
        bad.getBioAssays().add( ba1 );
        sessionFactory.getCurrentSession().persist( bad );
        RawExpressionDataVector vector = new RawExpressionDataVector();
        vector.setExpressionExperiment( ee );
        vector.setDesignElement( cs1 );
        vector.setQuantitationType( qt );
        vector.setBioAssayDimension( bad );
        vector.setData( new byte[0] );
        ee.getRawExpressionDataVectors().add( vector );
        sessionFactory.getCurrentSession().persist( ee );
        ee = reload( ee );
        expressionExperimentDao.remove( ee );
        ee = null; // to prevent a double-remove in the @After method
        sessionFactory.getCurrentSession().flush();
        assertNull( sessionFactory.getCurrentSession().get( BioAssayDimension.class, bad.getId() ) );
    }

    @Test
    @WithMockUser
    public void testLoadValueObjectWithSingleCellData() {
        Taxon taxon = new Taxon();
        sessionFactory.getCurrentSession().persist( taxon );
        ArrayDesign ad = new ArrayDesign();
        ad.setPrimaryTaxon( taxon );
        sessionFactory.getCurrentSession().persist( ad );
        CompositeSequence cs = new CompositeSequence();
        cs.setArrayDesign( ad );
        sessionFactory.getCurrentSession().persist( cs );
        BioMaterial bm = new BioMaterial();
        bm.setSourceTaxon( taxon );
        sessionFactory.getCurrentSession().persist( bm );
        ExpressionExperiment ee = new ExpressionExperiment();
        BioAssay ba = new BioAssay();
        ba.setArrayDesignUsed( ad );
        ba.setSampleUsed( bm );
        ee.getBioAssays().add( ba );
        SingleCellDimension scd = new SingleCellDimension();
        scd.setCellIds( Arrays.asList( "A", "B", "C" ) );
        scd.getBioAssays().add( ba );
        scd.setBioAssaysOffset( new int[] { 0 } );
        CellTypeAssignment cta = new CellTypeAssignment();
        cta.setCellTypeIndices( new int[] { 0, 1, 1, 0 } );
        cta.setCellTypes( Arrays.asList( Characteristic.Factory.newInstance( Categories.CELL_TYPE, "X", null ),
                Characteristic.Factory.newInstance( Categories.CELL_TYPE, "Y", null ) ) );
        cta.setPreferred( true );
        cta.setNumberOfCellTypes( 0 );
        scd.getCellTypeAssignments().add( cta );
        sessionFactory.getCurrentSession().persist( scd );
        QuantitationType qt = new QuantitationType();
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.COUNT );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        qt.setScale( ScaleType.COUNT );
        qt.setIsSingleCellPreferred( true );
        ee.getQuantitationTypes().add( qt );
        SingleCellExpressionDataVector vector = new SingleCellExpressionDataVector();
        vector.setData( ByteArrayUtils.doubleArrayToBytes( new double[] { 1.0, 2.0, 1.0, 2.0 } ) );
        vector.setDataIndices( new int[] { 0, 1, 2, 4 } );
        vector.setExpressionExperiment( ee );
        vector.setDesignElement( cs );
        vector.setQuantitationType( qt );
        vector.setSingleCellDimension( scd );
        ee.getSingleCellExpressionDataVectors().add( vector );
        sessionFactory.getCurrentSession().persist( ee );
        sessionFactory.getCurrentSession().flush();
        ExpressionExperimentValueObject eevo = expressionExperimentDao.loadValueObject( ee );
        assertNotNull( eevo );
        assertNotNull( eevo.getSingleCellDimension() );
        assertEquals( Arrays.asList( "A", "B", "C" ), eevo.getSingleCellDimension().getCellIds() );
        assertNotNull( eevo.getSingleCellDimension().getCellTypeAssignment() );
        Long typeAId = cta.getCellTypes().get( 0 ).getId();
        Long typeBId = cta.getCellTypes().get( 1 ).getId();
        assertEquals( Arrays.asList( typeAId, typeBId, typeBId, typeAId ), eevo.getSingleCellDimension().getCellTypeAssignment().getCellTypeIds() );
    }

    @Test
    public void testGetAllAnnotations() {
        ee = createExpressionExperiment();
        expressionExperimentDao.getAllAnnotations( ee );
    }

    @Test
    public void testGetAnnotationsByLevel() {
        ee = createExpressionExperiment();
        expressionExperimentDao.getExperimentAnnotations( ee );
        expressionExperimentDao.getBioMaterialAnnotations( ee );
        expressionExperimentDao.getExperimentalDesignAnnotations( ee );
    }

    @Test
    public void testAddRawDataVectors() {
        ee = createExpressionExperiment();
        ArrayDesign platform = createPlatform();
        assertNotNull( platform.getId() );
        BioAssayDimension bad = new BioAssayDimension();
        sessionFactory.getCurrentSession().persist( bad );
        for ( int i = 0; i < 3; i++ ) {
            QuantitationType qt = new QuantitationType();
            qt.setName( "qt" + i );
            qt.setGeneralType( GeneralType.QUANTITATIVE );
            qt.setType( StandardQuantitationType.AMOUNT );
            qt.setScale( ScaleType.LOG2 );
            qt.setRepresentation( PrimitiveType.DOUBLE );
            Collection<RawExpressionDataVector> vectors = new ArrayList<>();
            for ( CompositeSequence cs : platform.getCompositeSequences() ) {
                RawExpressionDataVector v = new RawExpressionDataVector();
                v.setBioAssayDimension( bad );
                v.setDesignElement( cs );
                v.setExpressionExperiment( ee );
                v.setQuantitationType( qt );
                v.setData( new byte[0] );
                vectors.add( v );
            }
            assertEquals( 10, expressionExperimentDao.addRawDataVectors( ee, qt, vectors ) );
            Assertions.assertThat( ee.getQuantitationTypes() )
                    .hasSize( i + 1 )
                    .contains( qt );
            Assertions.assertThat( ee.getRawExpressionDataVectors() )
                    .hasSize( 10 * ( i + 1 ) );
        }
    }

    @Test
    public void testRemoveAllRawDataVectors() {
        ee = createExpressionExperimentWithRawVectors();
        expressionExperimentDao.removeAllRawDataVectors( ee );
    }

    @Test
    public void testRemoveRawDataVectors() {
        ExpressionExperiment ee = createExpressionExperimentWithRawVectors();
        QuantitationType qt = ee.getQuantitationTypes().iterator().next();
        assertEquals( 10, expressionExperimentDao.removeRawDataVectors( ee, qt ) );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveRawDataVectorsWhenQtIsUnknown() {
        ee = createExpressionExperimentWithRawVectors();
        QuantitationType qt = new QuantitationType();
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.AMOUNT );
        qt.setScale( ScaleType.LOG2 );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        sessionFactory.getCurrentSession().persist( qt );
        assertEquals( 10, expressionExperimentDao.removeRawDataVectors( ee, qt ) );
    }

    @Test
    public void testReplaceRawDataVectors() {
        ee = createExpressionExperimentWithRawVectors();
        Collection<RawExpressionDataVector> newVectors = new ArrayList<>();
        for ( RawExpressionDataVector v : ee.getRawExpressionDataVectors() ) {
            RawExpressionDataVector newVector = new RawExpressionDataVector();
            newVector.setDesignElement( v.getDesignElement() );
            newVector.setExpressionExperiment( ee );
            newVector.setBioAssayDimension( v.getBioAssayDimension() );
            newVector.setQuantitationType( v.getQuantitationType() );
            newVector.setData( new byte[0] );
            newVectors.add( newVector );
        }
        expressionExperimentDao.replaceRawDataVectors( ee, newVectors.iterator().next().getQuantitationType(), newVectors );
        Assertions.assertThat( ee.getQuantitationTypes() )
                .hasSize( 1 );
        Assertions.assertThat( ee.getRawExpressionDataVectors() )
                .hasSize( 10 );
    }

    @Test
    public void testCreateProcessedDataVectors() {
        ee = createExpressionExperimentWithRawVectors();
        Collection<ProcessedExpressionDataVector> newVectors = new ArrayList<>();
        QuantitationType newQt = new QuantitationType();
        newQt.setGeneralType( GeneralType.QUANTITATIVE );
        newQt.setType( StandardQuantitationType.AMOUNT );
        newQt.setScale( ScaleType.LOG2 );
        newQt.setRepresentation( PrimitiveType.DOUBLE );
        newQt.setIsMaskedPreferred( true );
        for ( RawExpressionDataVector rawVector : ee.getRawExpressionDataVectors() ) {
            ProcessedExpressionDataVector newVector = new ProcessedExpressionDataVector();
            newVector.setExpressionExperiment( ee );
            newVector.setDesignElement( rawVector.getDesignElement() );
            newVector.setBioAssayDimension( rawVector.getBioAssayDimension() );
            newVector.setQuantitationType( newQt );
            newVector.setData( new byte[0] );
            newVectors.add( newVector );
        }
        expressionExperimentDao.createProcessedDataVectors( ee, newVectors );
        assertEquals( 10, ee.getNumberOfDataVectors().intValue() );
        assertNotNull( newQt.getId() );
        Assertions.assertThat( ee.getQuantitationTypes() )
                .hasSize( 2 )
                .contains( newQt );
    }

    @Test
    public void testCreateProcessedDataVectorsWithPersistentQt() {
        ee = createExpressionExperimentWithRawVectors();
        Collection<ProcessedExpressionDataVector> newVectors = new ArrayList<>();
        QuantitationType newQt = new QuantitationType();
        newQt.setGeneralType( GeneralType.QUANTITATIVE );
        newQt.setType( StandardQuantitationType.AMOUNT );
        newQt.setScale( ScaleType.LOG2 );
        newQt.setRepresentation( PrimitiveType.DOUBLE );
        newQt.setIsMaskedPreferred( true );
        for ( RawExpressionDataVector rawVector : ee.getRawExpressionDataVectors() ) {
            ProcessedExpressionDataVector newVector = new ProcessedExpressionDataVector();
            newVector.setExpressionExperiment( ee );
            newVector.setDesignElement( rawVector.getDesignElement() );
            newVector.setBioAssayDimension( rawVector.getBioAssayDimension() );
            newVector.setQuantitationType( newQt );
            newVector.setData( new byte[0] );
            newVectors.add( newVector );
        }
        expressionExperimentDao.createProcessedDataVectors( ee, newVectors );
        assertEquals( 10, ee.getNumberOfDataVectors().intValue() );
        assertNotNull( newQt.getId() );
        Assertions.assertThat( ee.getQuantitationTypes() )
                .hasSize( 2 )
                .contains( newQt );
    }

    @Test
    public void testRemoveProcessedDataVectors() {
        ee = createExpressionExperimentWithProcessedVectors();
        assertEquals( 1, ee.getQuantitationTypes().size() );
        assertEquals( 10, ee.getNumberOfDataVectors().intValue() );
        assertEquals( 10, expressionExperimentDao.removeProcessedDataVectors( ee ) );
        assertEquals( 0, ee.getNumberOfDataVectors().intValue() );
        assertEquals( 0, ee.getQuantitationTypes().size() );
    }

    @Test
    public void testReplaceProcessedDataVectors() {
        ee = createExpressionExperimentWithProcessedVectors();
        Collection<ProcessedExpressionDataVector> newVectors = new ArrayList<>();
        QuantitationType newQt = new QuantitationType();
        newQt.setGeneralType( GeneralType.QUANTITATIVE );
        newQt.setType( StandardQuantitationType.AMOUNT );
        newQt.setScale( ScaleType.LOG2 );
        newQt.setRepresentation( PrimitiveType.DOUBLE );
        newQt.setIsMaskedPreferred( true );
        for ( ProcessedExpressionDataVector v : ee.getProcessedExpressionDataVectors() ) {
            ProcessedExpressionDataVector newVector = new ProcessedExpressionDataVector();
            newVector.setExpressionExperiment( ee );
            newVector.setDesignElement( v.getDesignElement() );
            newVector.setBioAssayDimension( v.getBioAssayDimension() );
            newVector.setQuantitationType( newQt );
            newVector.setData( new byte[0] );
            newVectors.add( newVector );
        }
        assertEquals( 10, ee.getNumberOfDataVectors().intValue() );
        Assertions.assertThat( ee.getQuantitationTypes() )
                .hasSize( 1 );
        assertEquals( 10, expressionExperimentDao.replaceProcessedDataVectors( ee, newVectors ) );
        assertNotNull( newQt.getId() );
        Assertions.assertThat( ee.getQuantitationTypes() )
                .hasSize( 1 )
                .contains( newQt );
        assertEquals( 10, ee.getNumberOfDataVectors().intValue() );
    }

    @Test
    public void testReplaceProcessedDataVectorsReusingTheSameQT() {
        ee = createExpressionExperimentWithProcessedVectors();
        Collection<ProcessedExpressionDataVector> newVectors = new ArrayList<>();
        for ( ProcessedExpressionDataVector v : ee.getProcessedExpressionDataVectors() ) {
            ProcessedExpressionDataVector newVector = new ProcessedExpressionDataVector();
            newVector.setExpressionExperiment( ee );
            newVector.setDesignElement( v.getDesignElement() );
            newVector.setBioAssayDimension( v.getBioAssayDimension() );
            newVector.setQuantitationType( v.getQuantitationType() );
            newVector.setData( new byte[0] );
            newVectors.add( newVector );
        }
        assertEquals( 10, ee.getNumberOfDataVectors().intValue() );
        assertEquals( 10, expressionExperimentDao.replaceProcessedDataVectors( ee, newVectors ) );
        assertEquals( 10, ee.getNumberOfDataVectors().intValue() );
    }

    @Test
    public void testReplaceProcessedDataVectorsWithDetachedExperiment() {
        ee = createExpressionExperimentWithProcessedVectors();
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().evict( ee );
        Collection<ProcessedExpressionDataVector> newVectors = new ArrayList<>();
        QuantitationType newQt = new QuantitationType();
        newQt.setGeneralType( GeneralType.QUANTITATIVE );
        newQt.setType( StandardQuantitationType.AMOUNT );
        newQt.setScale( ScaleType.LOG2 );
        newQt.setRepresentation( PrimitiveType.DOUBLE );
        newQt.setIsMaskedPreferred( true );
        sessionFactory.getCurrentSession().persist( newQt );
        ee.getQuantitationTypes().add( newQt );
        for ( ProcessedExpressionDataVector v : ee.getProcessedExpressionDataVectors() ) {
            ProcessedExpressionDataVector newVector = new ProcessedExpressionDataVector();
            newVector.setExpressionExperiment( ee );
            newVector.setDesignElement( v.getDesignElement() );
            newVector.setBioAssayDimension( v.getBioAssayDimension() );
            newVector.setQuantitationType( newQt );
            newVector.setData( new byte[0] );
            newVectors.add( newVector );
        }
        assertEquals( 10, ee.getNumberOfDataVectors().intValue() );
        assertEquals( 10, expressionExperimentDao.replaceProcessedDataVectors( ee, newVectors ) );
        assertEquals( 10, ee.getNumberOfDataVectors().intValue() );
    }

    @Test
    public void testGetGenesUsedByProcessedVectors() {
        ExpressionExperiment ee = createExpressionExperimentWithProcessedVectors();
        Assertions.assertThat( expressionExperimentDao.getGenesUsedByPreferredVectors( ee ) )
                .isEmpty();
    }

    @Test
    public void testGetArrayDesignUsed() {
        ExpressionExperiment ee = createExpressionExperimentWithProcessedVectors();
        QuantitationType qt = ee.getQuantitationTypes().iterator().next();
        // EE does not have any sample
        Assertions.assertThat( expressionExperimentDao.getArrayDesignsUsed( ee ) )
                .isEmpty();
        // but the vectors can be used to determine the platform
        Assertions.assertThat( expressionExperimentDao.getArrayDesignsUsed( ee, qt, ProcessedExpressionDataVector.class ) )
                .hasSize( 1 );
    }

    private ExpressionExperiment reload( ExpressionExperiment e ) {
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().evict( e );
        return expressionExperimentDao.load( e.getId() );
    }

    private ExpressionExperiment createExpressionExperiment() {
        ee = ExpressionExperiment.Factory.newInstance();
        ee.setExperimentalDesign( new ExperimentalDesign() );
        return expressionExperimentDao.create( ee );
    }

    private ExpressionExperiment createExpressionExperimentWithRawVectors() {
        ee = new ExpressionExperiment();
        QuantitationType qt = new QuantitationType();
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.AMOUNT );
        qt.setScale( ScaleType.LOG2 );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        ee.getQuantitationTypes().add( qt );
        Taxon taxon = new Taxon();
        sessionFactory.getCurrentSession().persist( taxon );
        ArrayDesign platform = createPlatform();
        BioAssayDimension bad = new BioAssayDimension();
        sessionFactory.getCurrentSession().persist( bad );
        for ( CompositeSequence cs : platform.getCompositeSequences() ) {
            RawExpressionDataVector v = new RawExpressionDataVector();
            v.setBioAssayDimension( bad );
            v.setDesignElement( cs );
            v.setExpressionExperiment( ee );
            v.setQuantitationType( qt );
            v.setData( new byte[0] );
            ee.getRawExpressionDataVectors().add( v );
        }
        return expressionExperimentDao.create( ee );
    }

    private ExpressionExperiment createExpressionExperimentWithProcessedVectors() {
        ee = new ExpressionExperiment();
        QuantitationType qt = new QuantitationType();
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.AMOUNT );
        qt.setScale( ScaleType.LOG2 );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        qt.setIsMaskedPreferred( true );
        ee.getQuantitationTypes().add( qt );
        ArrayDesign platform = createPlatform();
        BioAssayDimension bad = new BioAssayDimension();
        sessionFactory.getCurrentSession().persist( bad );
        for ( CompositeSequence cs : platform.getCompositeSequences() ) {
            ProcessedExpressionDataVector v = new ProcessedExpressionDataVector();
            v.setBioAssayDimension( bad );
            v.setDesignElement( cs );
            v.setExpressionExperiment( ee );
            v.setQuantitationType( qt );
            v.setData( new byte[0] );
            ee.getProcessedExpressionDataVectors().add( v );
        }
        ee.setNumberOfDataVectors( 10 );
        return expressionExperimentDao.create( ee );
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
        assertNotNull( platform.getId() );
        return platform;
    }
}
