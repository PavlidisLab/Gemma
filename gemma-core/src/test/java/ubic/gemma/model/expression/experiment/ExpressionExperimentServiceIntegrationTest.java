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
package ubic.gemma.model.expression.experiment;

import org.apache.commons.lang3.RandomStringUtils;
import org.hibernate.SessionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.TableMaintenanceUtil;
import ubic.gemma.persistence.service.common.description.CharacteristicDao;
import ubic.gemma.persistence.service.common.description.CharacteristicService;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayDao;
import ubic.gemma.persistence.service.expression.bioAssayData.RawExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.BlacklistedEntityService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;

import java.util.*;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
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

    private static ExpressionExperiment ee = null;
    private ExternalDatabase ed;
    private String accession;

    @Before
    public void setUp() throws Exception {
        ee = this.getTestPersistentCompleteExpressionExperiment( false );
        ee.setName( ExpressionExperimentServiceIntegrationTest.EE_NAME );

        DatabaseEntry accessionEntry = this.getTestPersistentDatabaseEntry();
        accession = accessionEntry.getAccession();
        ed = accessionEntry.getExternalDatabase();
        ee.setAccession( accessionEntry );

        Contact c = this.getTestPersistentContact();
        ee.setOwner( c );

        ee.getCharacteristics().add( Characteristic.Factory.newInstance() );

        expressionExperimentService.update( ee );
        ee = expressionExperimentService.thaw( ee );
    }

    @After
    public void tearDown() {
        if ( ee != null ) {
            expressionExperimentService.remove( ee );
        }
    }

    @Test
    public final void testFindByAccession() {
        DatabaseEntry accessionEntry = DatabaseEntry.Factory.newInstance( ed );
        accessionEntry.setAccession( accession );

        Collection<ExpressionExperiment> expressionExperiment = expressionExperimentService
                .findByAccession( accessionEntry );
        assertTrue( expressionExperiment.size() > 0 );
    }

    @Test
    public void testFindByFactor() {
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
        Collection<ExpressionExperimentValueObject> vos = expressionExperimentService.loadAllValueObjects();
        assertNotNull( vos );
        assertTrue( vos.size() > 0 );
    }

    @Test
    public void testGetByTaxon() {
        Taxon taxon = taxonService.findByCommonName( "mouse" );
        Collection<ExpressionExperiment> list = expressionExperimentService.findByTaxon( taxon );
        assertNotNull( list );
        Taxon checkTaxon = expressionExperimentService.getTaxon( list.iterator().next() );
        assertEquals( taxon, checkTaxon );

    }

    @Test
    public final void testGetDesignElementDataVectorsByQt() {
        QuantitationType quantitationType = ee.getRawExpressionDataVectors().iterator().next().getQuantitationType();
        Collection<QuantitationType> quantitationTypes = new HashSet<>();
        quantitationTypes.add( quantitationType );
        Collection<RawExpressionDataVector> vectors = rawExpressionDataVectorService.find( quantitationTypes );
        assertEquals( 12, vectors.size() );
    }

    @Test
    public final void testGetPerTaxonCount() {
        Map<Taxon, Long> counts = expressionExperimentService.getPerTaxonCount();
        long oldCount = counts.get( taxonService.findByCommonName( "mouse" ) );
        assertNotNull( counts );
        expressionExperimentService.remove( ee );
        ee = null;
        counts = expressionExperimentService.getPerTaxonCount();
        assertEquals( oldCount - 1, counts.getOrDefault( taxonService.findByCommonName( "mouse" ), 0L ).longValue() );
    }

    @Test
    public final void testGetQuantitationTypes() {
        Collection<QuantitationType> types = expressionExperimentService.getQuantitationTypes( ee );
        assertEquals( 2, types.size() );
    }

    @Test
    public final void testGetQuantitationTypesForArrayDesign() {
        ArrayDesign ad = ee.getRawExpressionDataVectors().iterator().next().getDesignElement().getArrayDesign();
        Collection<QuantitationType> types = expressionExperimentService.getQuantitationTypes( ee, ad );
        assertEquals( 2, types.size() );
    }

    @Test
    public final void testGetRawExpressionDataVectors() {
        ExpressionExperiment eel = this.getTestPersistentCompleteExpressionExperiment( false );
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
                .hasFieldOrPropertyWithValue( "objectAlias", "ba" )
                .hasFieldOrPropertyWithValue( "propertyName", "arrayDesignUsed.technologyType" )
                .hasFieldOrPropertyWithValue( "operator", Filter.Operator.eq )
                .hasFieldOrPropertyWithValue( "requiredValue", TechnologyType.SEQUENCING );
    }

    @Test
    public final void testLoadValueObjects() {
        Collection<Long> ids = new HashSet<>();
        Long id = ee.getId();
        ids.add( id );
        Collection<ExpressionExperimentValueObject> list = expressionExperimentService.loadValueObjectsByIds( ids );
        assertNotNull( list );
        assertEquals( 1, list.size() );
    }

    @Test
    public void testLoadValueObjectsByCharacteristic() {
        Collection<Long> ids = new HashSet<>();
        Characteristic c = ee.getCharacteristics().stream().findFirst().orElse( null );
        assertThat( c ).isNotNull();
        Filter of = expressionExperimentService.getFilter( "characteristics.id", Filter.Operator.eq, c.getId().toString() );
        assertEquals( CharacteristicDao.OBJECT_ALIAS, of.getObjectAlias() );
        assertEquals( "id", of.getPropertyName() );
        assertEquals( Long.class, of.getPropertyType() );
        Long id = ee.getId();
        ids.add( id );
        Collection<ExpressionExperimentValueObject> list = expressionExperimentService.loadValueObjects( Filters.by( of ), null, 0, 0 );
        assertEquals( 1, list.size() );
    }

    @Test
    public void testLoadValueObjectsByFactorValueCharacteristic() {
        Filter of = expressionExperimentService.getFilter( "experimentalDesign.experimentalFactors.factorValues.characteristics.id", Filter.Operator.eq, "1" );
        assertEquals( "id", of.getPropertyName() );
        assertEquals( Long.class, of.getPropertyType() );
        Collection<ExpressionExperimentValueObject> list = expressionExperimentService.loadValueObjects( Filters.by( of ), null, 0, 0 );
        assertTrue( list.isEmpty() );
    }

    @Test
    public void testLoadValueObjectsBySampleUsedCharacteristic() {
        Filter of = expressionExperimentService.getFilter( "bioAssays.sampleUsed.characteristics.id", Filter.Operator.eq, "1" );
        assertEquals( "id", of.getPropertyName() );
        assertEquals( Long.class, of.getPropertyType() );
        Collection<ExpressionExperimentValueObject> list = expressionExperimentService.loadValueObjects( Filters.by( of ), null, 0, 0 );
        assertTrue( list.isEmpty() );
    }

    @Test
    public void testLoadValueObjectsByBioAssay() {
        Collection<Long> ids = new HashSet<>();
        BioAssay ba = ee.getBioAssays().stream().findFirst().orElse( null );
        assertThat( ba ).isNotNull();
        Filter of = expressionExperimentService.getFilter( "bioAssays.id", Filter.Operator.eq, ba.getId().toString() );
        assertEquals( BioAssayDao.OBJECT_ALIAS, of.getObjectAlias() );
        assertEquals( "id", of.getPropertyName() );
        Long id = ee.getId();
        ids.add( id );
        Collection<ExpressionExperimentValueObject> list = expressionExperimentService.loadValueObjects( Filters.by( of ), null, 0, 0 );
        assertEquals( 1, list.size() );
    }

    @Test
    public void testLoadBlacklistedValueObjects() {
        blacklistedEntityService.blacklistExpressionExperiment( ee, "Don't feel bad!" );
        assertThat( blacklistedEntityService.isBlacklisted( ee ) ).isTrue();
        Slice<ExpressionExperimentValueObject> result = expressionExperimentService.loadBlacklistedValueObjects( null, null, 0, 10 );
        assertThat( result )
                .hasSize( 1 )
                .first()
                .hasFieldOrPropertyWithValue( "shortName", ee.getShortName() );
    }

    @Test
    public void testFilterByAllCharacteristics() {
        Filter f = expressionExperimentService.getFilter( "allCharacteristics.valueUri", Filter.Operator.eq, "http://www.ebi.ac.uk/efo/EFO_000516" );
        assertThat( f )
                .hasFieldOrPropertyWithValue( "objectAlias", "ac" )
                .hasFieldOrPropertyWithValue( "propertyName", "valueUri" );
        assertThat( expressionExperimentService.load( Filters.by( f ), null ) )
                .isEmpty();
    }

    @Test
    public void testCacheInvalidationWhenACharacteristicIsDeleted() {
        Characteristic c = new Characteristic();
        c.setCategory( "bar" );
        c.setValue( "foo" );
        Consumer<? super ExpressionExperimentService.CharacteristicWithUsageStatisticsAndOntologyTerm> consumer = c2 -> {
            assertThat( c2.getCharacteristic() ).isEqualTo( c );
            assertThat( c2.getNumberOfExpressionExperiments() ).isEqualTo( 1L );
            assertThat( c2.getTerm() ).isNull();
        };

        tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries();
        assertThat( expressionExperimentService.getAnnotationsUsageFrequency( null, 0, 0 ) )
                .noneSatisfy( consumer );

        // add the term to the dataset and update the pivot table
        ee.getCharacteristics().add( c );
        expressionExperimentService.update( ee );
        assertThat( c.getId() ).isNotNull();

        // the table is out-of-date
        assertThat( expressionExperimentService.getAnnotationsUsageFrequency( null, 0, 0 ) )
                .noneSatisfy( consumer );

        // update the pivot table
        tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries();
        assertThat( expressionExperimentService.getAnnotationsUsageFrequency( null, 0, 0 ) )
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
        assertThat( expressionExperimentService.getAnnotationsUsageFrequency( null, 0, 0 ) )
                .noneSatisfy( consumer );
    }
}
