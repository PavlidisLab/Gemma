/*
 * The Gemma project
 *
 * Copyright (c) 2006 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.persistence.service.expression.experiment;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.common.auditAndSecurity.AuditAction;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.blacklist.BlacklistedEntityService;
import ubic.gemma.persistence.service.common.description.CharacteristicService;
import ubic.gemma.persistence.service.expression.bioAssayData.RawExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialService;
import ubic.gemma.persistence.service.maintenance.TableMaintenanceUtil;
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Subquery;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.*;

/**
 * @author kkeshav
 * @author pavlidis
 */
public class ExpressionExperimentServiceIntegrationTest extends BaseSpringContextTest {

    private static final String EE_NAME = RandomStringUtils.randomAlphanumeric( 20 );

    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private RawExpressionDataVectorService rawExpressionDataVectorService;
    @Autowired
    private BlacklistedEntityService blacklistedEntityService;
    @Autowired
    private TableMaintenanceUtil tableMaintenanceUtil;
    @Autowired
    private CharacteristicService characteristicService;
    @Autowired
    private ExpressionExperimentSetService expressionExperimentSetService;
    @Autowired
    private BioMaterialService bioMaterialService;

    /**
     * A collection of {@link ExpressionExperiment} that will be removed at the end of the test.
     */
    private List<ExpressionExperiment> ees;

    @Before
    public void setUp() {
        ees = new ArrayList<>();
    }

    @After
    public void tearDown() {
        expressionExperimentService.remove( ees );
        ees.clear();
    }

    @Test
    public final void testFindByAccession() {
        ExpressionExperiment ee = createExpressionExperiment();
        DatabaseEntry de = createDatabaseEntry();

        ee.setAccession( de );
        expressionExperimentService.update( ee );

        DatabaseEntry accessionEntry = DatabaseEntry.Factory.newInstance( de.getExternalDatabase() );
        accessionEntry.setAccession( de.getAccession() );

        Collection<ExpressionExperiment> expressionExperiment = expressionExperimentService
                .findByAccession( accessionEntry );
        assertFalse( expressionExperiment.isEmpty() );
    }

    @Test
    public void testFindByFactor() {
        ExpressionExperiment ee = createExpressionExperiment();
        ExperimentalDesign design = ee.getExperimentalDesign();
        assertNotNull( design.getExperimentalFactors() );
        ExperimentalFactor ef = design.getExperimentalFactors().iterator().next();
        assertNotNull( ef );
        ExpressionExperiment eeFound = expressionExperimentService.findByFactor( ef );
        assertNotNull( eeFound );
        assertEquals( eeFound.getId(), ee.getId() );
    }

    @Test
    public void testFindByFactorValue() {
        ExpressionExperiment ee = createExpressionExperiment();
        ExperimentalDesign design = ee.getExperimentalDesign();
        assertNotNull( design.getExperimentalFactors() );
        ExperimentalFactor ef = design.getExperimentalFactors().iterator().next();
        FactorValue fv = ef.getFactorValues().iterator().next();
        ExpressionExperiment eeFound = expressionExperimentService.findByFactorValue( fv );
        assertNotNull( eeFound );
        assertEquals( eeFound.getId(), ee.getId() );

    }

    @Test
    public void testFindByFactorValueId() {
        ExpressionExperiment ee = createExpressionExperiment();
        ExperimentalDesign design = ee.getExperimentalDesign();
        assertNotNull( design.getExperimentalFactors() );
        ExperimentalFactor ef = design.getExperimentalFactors().iterator().next();
        FactorValue fv = ef.getFactorValues().iterator().next();
        assertNotNull( fv.getId() );
        ExpressionExperiment eeFound = expressionExperimentService.findByFactorValue( fv.getId() );
        assertNotNull( eeFound );
        assertEquals( eeFound.getId(), ee.getId() );

    }

    @Test
    public void testLoadAllValueObjects() {
        ExpressionExperiment ee = createExpressionExperiment();
        Collection<ExpressionExperimentValueObject> vos = expressionExperimentService.loadAllValueObjects();
        assertThat( vos )
                .extracting( ExpressionExperimentValueObject::getId )
                .contains( ee.getId() ); // FIXME: use containsExactly, but there are unexpected fixtures from other tests
    }

    @Test
    public void testGetByTaxon() {
        ExpressionExperiment ee = createExpressionExperiment();
        Taxon taxon = taxonService.findByCommonName( "mouse" );
        Collection<ExpressionExperiment> list = expressionExperimentService.findByTaxon( taxon );
        assertNotNull( list );
        assertTrue( list.contains( ee ) );
        Taxon checkTaxon = expressionExperimentService.getTaxon( list.iterator().next() );
        assertEquals( taxon, checkTaxon );
    }

    @Test
    public final void testGetDesignElementDataVectorsByQt() {
        ExpressionExperiment ee = createExpressionExperiment();
        QuantitationType quantitationType = ee.getRawExpressionDataVectors().iterator().next().getQuantitationType();
        Collection<QuantitationType> quantitationTypes = new HashSet<>();
        quantitationTypes.add( quantitationType );
        Collection<RawExpressionDataVector> vectors = rawExpressionDataVectorService.find( quantitationTypes );
        assertEquals( 12, vectors.size() );
    }

    @Test
    public final void testGetPerTaxonCount() {
        ExpressionExperiment ee = createExpressionExperiment();
        Map<Taxon, Long> counts = expressionExperimentService.getPerTaxonCount();
        Long oldCount = counts.get( taxonService.findByCommonName( "mouse" ) );
        assertNotNull( counts );
        expressionExperimentService.remove( ee );
        ees.remove( ee );
        counts = expressionExperimentService.getPerTaxonCount();
        assertEquals( oldCount - 1, counts.getOrDefault( taxonService.findByCommonName( "mouse" ), 0L ).longValue() );
    }

    @Test
    public final void testGetQuantitationTypes() {
        ExpressionExperiment ee = createExpressionExperiment();
        Collection<QuantitationType> types = expressionExperimentService.getQuantitationTypes( ee );
        assertEquals( 2, types.size() );
    }

    @Test
    public void testGetPreferredQuantitationType() {
        ExpressionExperiment ee = createExpressionExperiment();
        QuantitationType qt = expressionExperimentService.getPreferredQuantitationType( ee ).orElse( null );
        assertNotNull( qt );
        assertTrue( qt.getIsPreferred() );
    }

    @Test
    public void testGetBioMaterialCount() {
        ExpressionExperiment ee = createExpressionExperiment();
        assertEquals( 8, expressionExperimentService.getBioMaterialCount( ee ) );
    }

    @Test
    public void testGetQuantitationTypeCount() {
        ExpressionExperiment ee = createExpressionExperiment();
        Map<QuantitationType, Long> qts = expressionExperimentService.getQuantitationTypeCount( ee );
        assertEquals( 2, qts.size() );
    }

    @Test
    public void testGetRawDataVectorCount() {
        ExpressionExperiment ee = createExpressionExperiment();
        assertEquals( 24, expressionExperimentService.getRawDataVectorCount( ee ) );
    }

    @Test
    public final void testGetRawExpressionDataVectors() {
        ExpressionExperiment eel = this.getTestPersistentCompleteExpressionExperiment( false );
        ees.add( eel );
        Collection<CompositeSequence> designElements = new HashSet<>();
        QuantitationType quantitationType = eel.getRawExpressionDataVectors().iterator().next().getQuantitationType();
        Collection<RawExpressionDataVector> allv = eel.getRawExpressionDataVectors();

        assertNotNull( quantitationType );

        assertTrue( allv.size() > 1 );

        for ( RawExpressionDataVector anAllv : allv ) {
            CompositeSequence designElement = anAllv.getDesignElement();
            assertNotNull( designElement );

            designElements.add( designElement );
            if ( designElements.size() == 2 )
                break;
        }

        assertEquals( 2, designElements.size() );

        Collection<? extends DesignElementDataVector> vectors = rawExpressionDataVectorService
                .find( designElements, quantitationType );

        assertEquals( 2, vectors.size() );
    }

    @Test
    public void testGetFilterableProperties() {
        assertThat( expressionExperimentService.getFilterableProperties() )
                .contains( "id", "characteristics.valueUri", "taxon", "taxon.id", "bioAssayCount" );
    }

    @Test
    public void testGetFilterablePropertyType() {
        assertThat( expressionExperimentService.getFilterablePropertyType( "id" ) ).isEqualTo( Long.class );
    }

    /**
     * EE service has a few extensions for supporting various filtering strategies in the frontend, so we need to test
     * them here.
     */
    @Test
    public final void testGetFilter() {
        assertThat( expressionExperimentService.getFilter( "taxon", Filter.Operator.eq, "9606" ) )
                .hasFieldOrPropertyWithValue( "objectAlias", "taxon" )
                .hasFieldOrPropertyWithValue( "propertyName", "id" )
                .hasFieldOrPropertyWithValue( "requiredValue", 9606L );

        assertThat( expressionExperimentService.getFilter( "bioAssayCount", Filter.Operator.greaterOrEq, "4" ) )
                .hasFieldOrPropertyWithValue( "objectAlias", "ee" )
                .hasFieldOrPropertyWithValue( "propertyName", "bioAssays.size" )
                .hasFieldOrPropertyWithValue( "operator", Filter.Operator.greaterOrEq )
                .hasFieldOrPropertyWithValue( "requiredValue", 4 );

        assertThat( expressionExperimentService.getFilter( "bioAssays.size", Filter.Operator.greaterOrEq, "4" ) )
                .hasFieldOrPropertyWithValue( "objectAlias", "ee" )
                .hasFieldOrPropertyWithValue( "propertyName", "bioAssays.size" )
                .hasFieldOrPropertyWithValue( "operator", Filter.Operator.greaterOrEq )
                .hasFieldOrPropertyWithValue( "requiredValue", 4 );

        Calendar calendar = new GregorianCalendar( 2020, Calendar.JANUARY, 10 );
        calendar.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
        assertThat( expressionExperimentService.getFilter( "lastUpdated", Filter.Operator.greaterOrEq, "2020-01-10" ) )
                .hasFieldOrPropertyWithValue( "objectAlias", "s" )
                .hasFieldOrPropertyWithValue( "propertyName", "lastUpdated" )
                .hasFieldOrPropertyWithValue( "operator", Filter.Operator.greaterOrEq )
                .hasFieldOrPropertyWithValue( "requiredValue", calendar.getTime() );

        assertThat( expressionExperimentService.getFilter( "troubled", Filter.Operator.eq, "true" ) )
                .hasFieldOrPropertyWithValue( "objectAlias", "s" )
                .hasFieldOrPropertyWithValue( "propertyName", "troubled" )
                .hasFieldOrPropertyWithValue( "operator", Filter.Operator.eq )
                .hasFieldOrPropertyWithValue( "requiredValue", true );

        assertThat( expressionExperimentService.getFilter( "needsAttention", Filter.Operator.eq, "false" ) )
                .hasFieldOrPropertyWithValue( "objectAlias", "s" )
                .hasFieldOrPropertyWithValue( "propertyName", "needsAttention" )
                .hasFieldOrPropertyWithValue( "operator", Filter.Operator.eq )
                .hasFieldOrPropertyWithValue( "requiredValue", false );

        assertThat( expressionExperimentService.getFilter( "bioAssays.arrayDesignUsed.technologyType", Filter.Operator.eq, "SEQUENCING" ) )
                .hasFieldOrPropertyWithValue( "objectAlias", "ee" )
                .hasFieldOrPropertyWithValue( "propertyName", "id" )
                .hasFieldOrPropertyWithValue( "operator", Filter.Operator.inSubquery );
    }

    @Test
    public final void testLoadValueObjectsByIds() {
        ExpressionExperiment ee = createExpressionExperiment();
        Collection<Long> ids = new HashSet<>();
        Long id = ee.getId();
        ids.add( id );
        Collection<ExpressionExperimentValueObject> list = expressionExperimentService.loadValueObjectsByIds( ids );
        assertNotNull( list );
        assertThat( list ).hasSize( 1 ).extracting( "id" ).contains( id );
    }

    @Test
    public void testLoadValueObjectsByCharacteristic() {
        ExpressionExperiment ee = createExpressionExperiment();
        Characteristic c = ee.getCharacteristics().stream().findFirst().orElse( null );
        assertThat( c ).isNotNull();
        Filter of = expressionExperimentService.getFilter( "characteristics.id", Filter.Operator.eq, c.getId().toString() );
        assertEquals( ExpressionExperimentDao.OBJECT_ALIAS, of.getObjectAlias() );
        assertEquals( "id", of.getPropertyName() );
        assertEquals( Long.class, of.getPropertyType() );
        assertTrue( of.getRequiredValue() instanceof Subquery );
        Long id = ee.getId();
        Collection<ExpressionExperimentValueObject> list = expressionExperimentService.loadValueObjects( Filters.by( of ), null, 0, 0 );
        assertThat( list ).extracting( "id" ).contains( id );
    }

    @Test
    public void testLoadValueObjectsByFactorValueCharacteristic() {
        ExpressionExperiment ee = createExpressionExperiment();
        FactorValue fv = ee.getExperimentalDesign().getExperimentalFactors().iterator().next()
                .getFactorValues().iterator().next();
        Statement s = new Statement();
        fv.getCharacteristics().add( s );
        expressionExperimentService.update( ee );
        Filter of = expressionExperimentService.getFilter( "experimentalDesign.experimentalFactors.factorValues.characteristics.id", Filter.Operator.eq, String.valueOf( s.getId() ) );
        assertEquals( "id", of.getPropertyName() );
        assertEquals( Long.class, of.getPropertyType() );
        Collection<ExpressionExperimentValueObject> list = expressionExperimentService.loadValueObjects( Filters.by( of ), null, 0, 0 );
        assertThat( list ).extracting( ExpressionExperimentValueObject::getId )
                .containsOnly( ee.getId() );
    }

    @Test
    public void testLoadValueObjectsBySampleUsedCharacteristic() {
        ExpressionExperiment ee = createExpressionExperiment();
        BioMaterial bm = ee.getBioAssays().iterator().next()
                .getSampleUsed();
        Characteristic c = Characteristic.Factory.newInstance();
        bm.getCharacteristics().add( c );
        bioMaterialService.update( bm );
        Filter of = expressionExperimentService.getFilter( "bioAssays.sampleUsed.characteristics.id", Filter.Operator.eq, String.valueOf( c.getId() ) );
        assertEquals( "id", of.getPropertyName() );
        assertEquals( Long.class, of.getPropertyType() );
        Collection<ExpressionExperimentValueObject> list = expressionExperimentService.loadValueObjects( Filters.by( of ), null, 0, 0 );
        assertThat( list ).extracting( ExpressionExperimentValueObject::getId )
                .containsOnly( ee.getId() );
    }

    @Test
    public void testLoadValueObjectsByBioAssay() {
        ExpressionExperiment ee = createExpressionExperiment();
        BioAssay ba = ee.getBioAssays().stream().findFirst().orElse( null );
        assertThat( ba ).isNotNull();
        Filter of = expressionExperimentService.getFilter( "bioAssays.id", Filter.Operator.eq, ba.getId().toString() );
        assertEquals( ExpressionExperimentDao.OBJECT_ALIAS, of.getObjectAlias() );
        assertEquals( "id", of.getPropertyName() );
        assertTrue( of.getRequiredValue() instanceof Subquery );
        Long id = ee.getId();
        Collection<ExpressionExperimentValueObject> list = expressionExperimentService.loadValueObjects( Filters.by( of ), null, 0, 0 );
        assertThat( list )
                .hasSize( 1 )
                .first()
                .extracting( "id" )
                .isEqualTo( id );
    }

    @Test
    public void testLoadBlacklistedValueObjects() {
        ExpressionExperiment ee = createExpressionExperiment();
        blacklistedEntityService.blacklistExpressionExperiment( ee, "Don't feel bad!" );
        assertThat( blacklistedEntityService.isBlacklisted( ee ) ).isTrue();
        Slice<ExpressionExperimentValueObject> result = expressionExperimentService.loadBlacklistedValueObjects( null, null, 0, 10 );
        assertThat( result )
                .extracting( "shortName" )
                .contains( ee.getShortName() );
    }

    @Test
    public void testFilterByAllCharacteristics() {
        Filter f = expressionExperimentService.getFilter( "allCharacteristics.valueUri", Filter.Operator.eq, "http://www.ebi.ac.uk/efo/EFO_000516" );
        assertThat( f )
                .hasFieldOrPropertyWithValue( "objectAlias", "ee" )
                .hasFieldOrPropertyWithValue( "propertyName", "id" )
                .hasFieldOrPropertyWithValue( "operator", Filter.Operator.inSubquery );
        expressionExperimentService.load( Filters.by( f ), null );
    }

    @Test
    public void testFilterByGeeqScore() {
        Filter f = expressionExperimentService.getFilter( "geeq.publicQualityScore", Filter.Operator.greaterOrEq, "0.9" );
        assertThat( f )
                .hasFieldOrPropertyWithValue( "objectAlias", null )
                .hasFieldOrPropertyWithValue( "propertyName", "(case when geeq.manualQualityOverride = true then geeq.manualQualityScore else geeq.detectedQualityScore end)" )
                .hasFieldOrPropertyWithValue( "requiredValue", 0.9 )
                .hasFieldOrPropertyWithValue( "originalProperty", "geeq.publicQualityScore" );
        expressionExperimentService.load( Filters.by( f ), null );
    }

    @Test
    public void testFilterBySuitabilityScoreAsAdmin() {
        expressionExperimentService.getFilter( "geeq.publicSuitabilityScore", Filter.Operator.greaterOrEq, "0.9" );
    }

    @Test(expected = AccessDeniedException.class)
    public void testFilterBySuitabilityScoreAsNonAdmin() {
        try {
            runAsAnonymous();
            expressionExperimentService.getFilter( "geeq.publicSuitabilityScore", Filter.Operator.greaterOrEq, "0.9" );
        } finally {
            runAsAdmin(); // for cleanups
        }
    }

    @Test
    public void testCacheInvalidationWhenACharacteristicIsDeleted() throws TimeoutException {
        ExpressionExperiment ee = createExpressionExperiment();
        Characteristic c = new Characteristic();
        c.setCategory( "bar" );
        c.setValue( "foo" );
        Consumer<? super ExpressionExperimentService.CharacteristicWithUsageStatisticsAndOntologyTerm> consumer = c2 -> {
            assertThat( c2.getCharacteristic() ).isEqualTo( c );
            assertThat( c2.getNumberOfExpressionExperiments() ).isEqualTo( 1L );
        };

        tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries( null, false );
        assertThat( expressionExperimentService.getAnnotationsUsageFrequency( null, null, null, null, null, 0, null, 0, 5000, TimeUnit.MILLISECONDS ) )
                .noneSatisfy( consumer );

        // add the term to the dataset and update the pivot table
        ee.getCharacteristics().add( c );
        expressionExperimentService.update( ee );
        assertThat( c.getId() ).isNotNull();

        // the table is out-of-date
        assertThat( expressionExperimentService.getAnnotationsUsageFrequency( null, null, null, null, null, 0, null, 0, 5000, TimeUnit.MILLISECONDS ) )
                .noneSatisfy( consumer );

        // update the pivot table
        tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries( null, false );
        assertThat( expressionExperimentService.getAnnotationsUsageFrequency( null, null, null, null, null, 0, null, 0, 5000, TimeUnit.MILLISECONDS ) )
                .satisfiesOnlyOnce( consumer );

        // remove the term, which must evict the query cache
        characteristicService.remove( c );
        assertThat( characteristicService.load( c.getId() ) ).isNull();
        assertThat( expressionExperimentService.loadWithCharacteristics( ee.getId() ) )
                .isNotNull()
                .satisfies( e -> {
                    assertThat( e.getCharacteristics() ).doesNotContain( c );
                } );

        // since deletions are cascaded, the change will be reflected immediatly
        assertThat( expressionExperimentService.getAnnotationsUsageFrequency( null, null, null, null, null, 0, null, 0, 5000, TimeUnit.MILLISECONDS ) )
                .noneSatisfy( consumer );
    }

    @Test
    public void testSaveWithTransientEntity() {
        ExpressionExperiment ee = new ExpressionExperiment();
        ExpressionExperiment createdEE = expressionExperimentService.save( ee );
        assertNotNull( createdEE.getId() );
        ees.add( createdEE );
        assertThat( createdEE.getAuditTrail().getEvents() )
                .extracting( AuditEvent::getAction )
                .containsExactly( AuditAction.CREATE );
        ExpressionExperiment updatedEE = expressionExperimentService.save( ee );
        assertThat( updatedEE.getId() ).isEqualTo( createdEE.getId() );
        assertThat( createdEE.getAuditTrail().getEvents() )
                .extracting( AuditEvent::getAction )
                .containsExactly( AuditAction.CREATE, AuditAction.UPDATE );
    }

    @Test
    public void testUpdateWithTransientEntity() {
        ExpressionExperiment ee = new ExpressionExperiment();
        assertThatThrownBy( () -> expressionExperimentService.update( ee ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "ID is required to be non-null" );
    }

    @Test
    public void testRemoveExperimentInSet() {
        ExpressionExperiment ee1 = createExpressionExperiment();
        ExpressionExperiment ee2 = createExpressionExperiment();
        ExpressionExperimentSet eeSet = new ExpressionExperimentSet();
        eeSet.setName( "test" );
        eeSet.setTaxon( ee1.getTaxon() );
        eeSet.getExperiments().add( ee1 );
        eeSet.getExperiments().add( ee2 );
        eeSet = expressionExperimentSetService.create( eeSet );
        expressionExperimentService.remove( ee1 );
        ees.remove( ee1 ); // prevent removal in teardown
        eeSet = expressionExperimentSetService.load( eeSet.getId() );
        assertNotNull( eeSet );
        eeSet = expressionExperimentSetService.thaw( eeSet );
        assertNotNull( eeSet );
        assertThat( eeSet.getExperiments() )
                .containsExactly( ee2 );
    }

    @Test
    public void testStreamExperiments() {
        runAsUser( "bob" );
        ExpressionExperiment bobExperiment = getTestPersistentBasicExpressionExperiment();
        runAsUser( "joe" );
        ExpressionExperiment joeExperiment = getTestPersistentBasicExpressionExperiment();

        runAsAdmin();
        assertThat( expressionExperimentService.streamAll( true ) )
                .contains( bobExperiment, joeExperiment );

        runAsUser( "bob" );
        assertThat( expressionExperimentService.streamAll( true ) )
                .contains( bobExperiment )
                .doesNotContain( joeExperiment );

        runAsUser( "joe" );
        assertThat( expressionExperimentService.streamAll( true ) )
                .contains( joeExperiment )
                .doesNotContain( bobExperiment );

        runAsAnonymous();
        assertThat( expressionExperimentService.streamAll( true ) )
                .doesNotContain( bobExperiment, joeExperiment );

        runAsAdmin();
    }

    @Test
    public void testRemoveExperimentInSingletonSet() {
        ExpressionExperiment ee1 = createExpressionExperiment();
        ExpressionExperimentSet eeSet = new ExpressionExperimentSet();
        eeSet.setName( "test" );
        eeSet.setTaxon( ee1.getTaxon() );
        eeSet.getExperiments().add( ee1 );
        eeSet = expressionExperimentSetService.create( eeSet );
        expressionExperimentService.remove( ee1 );
        ees.remove( ee1 ); // prevent removal in teardown
        eeSet = expressionExperimentSetService.load( eeSet.getId() );
        assertNull( eeSet );
    }

    private ExpressionExperiment createExpressionExperiment() {
        ExpressionExperiment ee = this.getTestPersistentCompleteExpressionExperiment( false );
        ees.add( ee );
        ee.setName( ExpressionExperimentServiceIntegrationTest.EE_NAME );

        Contact c = this.getTestPersistentContact();
        ee.setOwner( c );

        ee.getCharacteristics().add( Characteristic.Factory.newInstance() );

        expressionExperimentService.update( ee );
        return expressionExperimentService.thaw( ee );
    }

    private DatabaseEntry createDatabaseEntry() {
        return this.getTestPersistentDatabaseEntry();
    }
}
