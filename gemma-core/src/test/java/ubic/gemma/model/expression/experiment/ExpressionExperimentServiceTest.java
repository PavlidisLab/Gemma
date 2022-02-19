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
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.common.description.CharacteristicDao;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayDao;
import ubic.gemma.persistence.service.expression.bioAssayData.RawExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.genome.taxon.TaxonDao;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.ObjectFilter;

import java.util.*;

import static org.junit.Assert.*;
import static org.assertj.core.api.Assertions.*;

/**
 * @author kkeshav
 * @author pavlidis
 */
public class ExpressionExperimentServiceTest extends BaseSpringContextTest {

    private static final String EE_NAME = RandomStringUtils.randomAlphanumeric( 20 );
    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private RawExpressionDataVectorService rawExpressionDataVectorService;
    private ExpressionExperiment ee = null;
    private ExternalDatabase ed;
    private String accession;
    private boolean persisted = false;

    @Before
    public void setup() {

        if ( !persisted ) {
            ee = this.getTestPersistentCompleteExpressionExperiment( false );
            ee.setName( ExpressionExperimentServiceTest.EE_NAME );

            DatabaseEntry accessionEntry = this.getTestPersistentDatabaseEntry();
            accession = accessionEntry.getAccession();
            ed = accessionEntry.getExternalDatabase();
            ee.setAccession( accessionEntry );

            Contact c = this.getTestPersistentContact();
            ee.setOwner( c );

            ee.getCharacteristics().add( Characteristic.Factory.newInstance() );

            expressionExperimentService.update( ee );
            ee = expressionExperimentService.thaw( ee );

            persisted = true;
        } else {
            log.debug( "Skipping making new ee for test" );
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
        ExpressionExperimentService eeService = this.getBean( ExpressionExperimentService.class );

        Taxon taxon = taxonService.findByCommonName( "mouse" );
        Collection<ExpressionExperiment> list = expressionExperimentService.findByTaxon( taxon );
        assertNotNull( list );
        Taxon checkTaxon = eeService.getTaxon( list.iterator().next() );
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
        counts = expressionExperimentService.getPerTaxonCount();
        assertEquals( oldCount - 1, counts.get( taxonService.findByCommonName( "mouse" ) ).longValue() );
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

    /**
     * EE service has a few extensions for supporting various filtering strategies in the frontend, so we need to test
     * them here.
     */
    @Test
    public final void testGetObjectFilter() {
        assertThat( expressionExperimentService.getObjectFilter( "taxon", ObjectFilter.Operator.eq, "9606" ) )
                .hasFieldOrPropertyWithValue( "objectAlias", "taxon" )
                .hasFieldOrPropertyWithValue( "propertyName", "id" )
                .hasFieldOrPropertyWithValue( "requiredValue", 9606L );

        assertThat( expressionExperimentService.getObjectFilter( "bioAssayCount", ObjectFilter.Operator.greaterOrEq, "4" ) )
                .hasFieldOrPropertyWithValue( "objectAlias", "ee" )
                .hasFieldOrPropertyWithValue( "propertyName", "bioAssays.size" )
                .hasFieldOrPropertyWithValue( "operator", ObjectFilter.Operator.greaterOrEq )
                .hasFieldOrPropertyWithValue( "requiredValue", 4 );

        Calendar calendar = new GregorianCalendar( 2020, Calendar.JANUARY, 10 );
        calendar.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
        assertThat( expressionExperimentService.getObjectFilter( "lastUpdated", ObjectFilter.Operator.greaterOrEq, "2020-01-10" ) )
                .hasFieldOrPropertyWithValue( "objectAlias", "s" )
                .hasFieldOrPropertyWithValue( "propertyName", "lastUpdated" )
                .hasFieldOrPropertyWithValue( "operator", ObjectFilter.Operator.greaterOrEq )
                .hasFieldOrPropertyWithValue( "requiredValue", calendar.getTime() );

        assertThat( expressionExperimentService.getObjectFilter( "troubled", ObjectFilter.Operator.eq, "true" ) )
                .hasFieldOrPropertyWithValue( "objectAlias", "s" )
                .hasFieldOrPropertyWithValue( "propertyName", "troubled" )
                .hasFieldOrPropertyWithValue( "operator", ObjectFilter.Operator.eq )
                .hasFieldOrPropertyWithValue( "requiredValue", true );

        assertThat( expressionExperimentService.getObjectFilter( "needsAttention", ObjectFilter.Operator.eq, "false" ) )
                .hasFieldOrPropertyWithValue( "objectAlias", "s" )
                .hasFieldOrPropertyWithValue( "propertyName", "needsAttention" )
                .hasFieldOrPropertyWithValue( "operator", ObjectFilter.Operator.eq )
                .hasFieldOrPropertyWithValue( "requiredValue", false );
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
    public void testLoadValueObjectsPreFilterByCharacteristic() {
        Collection<Long> ids = new HashSet<>();
        Filters filters = Filters.singleFilter( expressionExperimentService.getObjectFilter( "characteristics.id", ObjectFilter.Operator.eq, ee.getCharacteristics().stream().findFirst().orElse( null ).getId().toString() ) );
        ObjectFilter of = filters.iterator().next()[0];
        assertEquals( CharacteristicDao.OBJECT_ALIAS, of.getObjectAlias() );
        assertEquals( "id", of.getPropertyName() );
        Long id = ee.getId();
        ids.add( id );
        Collection<ExpressionExperimentValueObject> list = expressionExperimentService.loadValueObjectsPreFilter( filters, null, 0, 0 );
        assertEquals( 1, list.size() );
    }

    @Test
    public void testLoadValueObjectsPreFilterByBioAssay() {
        Collection<Long> ids = new HashSet<>();
        Filters filters = Filters.singleFilter( expressionExperimentService.getObjectFilter( "bioAssays.id", ObjectFilter.Operator.eq, ee.getBioAssays().stream().findFirst().orElse( null ).getId().toString() ) );
        ObjectFilter of = filters.iterator().next()[0];
        assertEquals( BioAssayDao.OBJECT_ALIAS, of.getObjectAlias() );
        assertEquals( "id", of.getPropertyName() );
        Long id = ee.getId();
        ids.add( id );
        Collection<ExpressionExperimentValueObject> list = expressionExperimentService.loadValueObjectsPreFilter( filters, null, 0, 0 );
        assertEquals( 1, list.size() );
    }

}
