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
package ubic.gemma.persistence.service.common.description;

import org.apache.commons.lang3.RandomStringUtils;
import org.hibernate.Hibernate;
import org.hibernate.QueryException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.Statement;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.FactorValueService;
import ubic.gemma.persistence.service.maintenance.TableMaintenanceUtil;

import java.util.*;

import static org.junit.Assert.*;

/**
 * @author luke
 */
public class CharacteristicServiceTest extends BaseSpringContextTest {

    @Autowired
    private CharacteristicService characteristicService;

    @Autowired
    private ExpressionExperimentService eeService;

    @Autowired
    private BioMaterialService bmService;

    @Autowired
    private FactorValueService fvService;

    private ExpressionExperiment ee;
    private Characteristic eeChar1;
    private Characteristic eeChar2;

    @Before
    public void setUp() throws Exception {
        ee = this.getTestPersistentBasicExpressionExperiment();
        ee.setCharacteristics( this.getTestPersistentCharacteristics( 2 ) );
        Characteristic[] eeChars = ee.getCharacteristics().toArray( new Characteristic[0] );
        eeChar1 = eeChars[0];
        eeChar2 = eeChars[1];
        eeService.update( ee );

        BioAssay ba = ee.getBioAssays().toArray( new BioAssay[0] )[0];
        BioMaterial bm = ba.getSampleUsed();
        bm.setCharacteristics( this.getTestPersistentCharacteristics( 1 ) );
        bmService.update( bm );

        for ( ExperimentalFactor ef : testHelper.getExperimentalFactors( ee.getExperimentalDesign() ) ) {
            eeService.addFactor( ee, ef );
        }

        ExperimentalFactor ef = ee.getExperimentalDesign().getExperimentalFactors().iterator().next();

        for ( FactorValue f : testHelper.getFactorValues( ef ) ) {
            eeService.addFactorValue( ee, f );
        }

        FactorValue fv = ef.getFactorValues().iterator().next();
        fv.setCharacteristics( this.getTestPersistentStatements( 1 ) );
        fvService.update( fv );

        tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries( null, false );
    }

    @After
    public void tearDown() {
        if ( ee != null ) {
            runAsAdmin();
            eeService.remove( ee );
        }
    }

    @Test
    public final void testGetParents() {
        Map<Characteristic, Identifiable> charToParent;
        charToParent = characteristicService.getParents( Collections.singletonList( eeChar1 ), null, false, true );
        assertEquals( ee, charToParent.get( eeChar1 ) );
        assertNull( charToParent.get( eeChar2 ) );
    }

    @Test
    public void testBrowse() {
        characteristicService.browse( 10, 10, "category", true );
    }

    @Test(expected = QueryException.class)
    public void testBrowseWithInvalidField() {
        characteristicService.browse( 10, 10, "foo", true );
    }

    @Autowired
    private TableMaintenanceUtil tableMaintenanceUtil;

    @Test
    public void testFindExperimentsByUris() {
        Map<Class<? extends Identifiable>, Map<String, Set<ExpressionExperiment>>> result = characteristicService.findExperimentsByUris( Collections.singletonList( eeChar1.getValueUri() ), true, true, true, null, 10, true, true );
        assertEquals( 1, result.keySet().size() );
        assertTrue( result.containsKey( ExpressionExperiment.class ) );
        ExpressionExperiment ee = result.get( ExpressionExperiment.class ).get( eeChar1.getValueUri() ).iterator().next();
        assertTrue( Hibernate.isInitialized( ee ) );
    }

    @Test
    public void testFindExperimentsByUrisAsProxies() {
        Map<Class<? extends Identifiable>, Map<String, Set<ExpressionExperiment>>> result = characteristicService.findExperimentsByUris( Collections.singletonList( eeChar1.getValueUri() ), true, true, true, null, 10, false, true );
        assertEquals( 1, result.size() );
        Collection<ExpressionExperiment> ees = result.get( ExpressionExperiment.class ).get( eeChar1.getValueUri() );
        ExpressionExperiment ee = result.get( ExpressionExperiment.class ).get( eeChar1.getValueUri() ).iterator().next();
        assertFalse( Hibernate.isInitialized( ee ) );
    }

    @Test
    public void testFindExperimentsByUrisAsAnonymousUser() {
        runAsAnonymous();
        Map<Class<? extends Identifiable>, Map<String, Set<ExpressionExperiment>>> result = characteristicService.findExperimentsByUris( Collections.singletonList( eeChar1.getValueUri() ), true, true, true, null, 10, true, true );
        assertTrue( result.isEmpty() );
    }

    @Test
    public void testFindExperimentsByUrisAsUser() {
        runAsUser( "bob" );
        Map<Class<? extends Identifiable>, Map<String, Set<ExpressionExperiment>>> result = characteristicService.findExperimentsByUris( Collections.singletonList( eeChar1.getValueUri() ), true, true, true, null, 10, true, true );
        assertTrue( result.isEmpty() );
    }

    private Set<Characteristic> getTestPersistentCharacteristics( int n ) {
        Set<Characteristic> chars = new HashSet<>();
        for ( int i = 0; i < n; ++i ) {
            Characteristic c = Characteristic.Factory.newInstance();
            c.setCategory( "test" );
            c.setValue( RandomStringUtils.insecure().nextNumeric( 10 ) );
            c.setValueUri( "http://www.ebi.ac.uk/efo/EFO_" + RandomStringUtils.insecure().nextAlphabetic( 7 ) );
            characteristicService.create( c );
            chars.add( c );
        }
        return chars;
    }

    private Set<Statement> getTestPersistentStatements( int n ) {
        Set<Statement> chars = new HashSet<>();
        for ( int i = 0; i < n; ++i ) {
            Statement c = Statement.Factory.newInstance();
            c.setCategory( "test" );
            c.setValue( RandomStringUtils.insecure().nextNumeric( 10 ) );
            c.setValueUri( "http://www.ebi.ac.uk/efo/EFO_" + RandomStringUtils.insecure().nextAlphabetic( 7 ) );
            characteristicService.create( c );
            chars.add( c );
        }
        return chars;
    }

    // @SuppressWarnings("unchecked")
    // public void testGetByTaxon() throws Exception {
    // TaxonService taxonService = ( TaxonService ) this.getBean( "taxonService" );
    // ExpressionExperimentService eeService = ( ExpressionExperimentService ) this
    // .getBean( "expressionExperimentService" );
    //
    // Taxon taxon = taxonService.findByCommonName( "mouse" );
    // Collection<ExpressionExperiment> list = characteristicService.findByTaxon( taxon );
    // assertNotNull( list );
    // Taxon checkTaxon = eeService.getTaxon( list.iterator().next().getId() );
    // assertEquals( taxon, checkTaxon );
    //
    // }

    // Creating test data for this was difficult. Needed to use a current data base for this test to work.
    // public void testFindByGene() throws Exception {
    // GeneService geneS = (GeneService) this.getBean( "geneService" );
    // Collection<Gene> genes = geneS.findByOfficialSymbol( "grin1" );
    // ExpressionExperimentService eeService = ( ExpressionExperimentService ) this
    // .getBean( "expressionExperimentService" );
    // Collection<Long> results = eeService.findByGene( genes.iterator().next());
    // log.info( results );
    // assertEquals(89, results.size() );
    //
    // }

    // This test uses the DB
    // public void testFindByBibliographicReference(){
    // ExpressionExperimentService eeService = ( ExpressionExperimentService ) this
    // .getBean( "expressionExperimentService" );
    // BibliographicReferenceService bibRefService = ( BibliographicReferenceService ) this
    // .getBean( "bibliographicReferenceService" );
    //
    // BibliographicReference bibRef = bibRefService.load( Long.valueOf( 111 ));
    //
    // Collection<ExpressionExperiment> foundEEs = eeService.findByBibliographicReference( bibRef );
    // assertEquals(1,foundEEs.size());
    // assertEquals(Long.valueOf(8), (Long) foundEEs.iterator().next().getId());
    //
    // }

}
