/*
 * The Gemma project
 *
 * Copyright (c) 2011 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.persistence.service.association.phenotype;

import gemma.gsec.authentication.UserDetailsImpl;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.annotation.DirtiesContext;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.providers.HumanPhenotypeOntologyService;
import ubic.basecode.ontology.providers.MammalianPhenotypeOntologyService;
import ubic.gemma.core.association.phenotype.PhenotypeAssociationManagerService;
import ubic.gemma.core.ontology.providers.MondoOntologyService;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.association.phenotype.PhenotypeMappingType;
import ubic.gemma.model.common.description.CharacteristicValueObject;
import ubic.gemma.model.common.description.CitationValueObject;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabaseValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.model.genome.gene.phenotype.EvidenceFilter;
import ubic.gemma.model.genome.gene.phenotype.valueObject.*;
import ubic.gemma.persistence.service.association.phenotype.service.PhenotypeAssociationService;

import java.io.InputStream;
import java.util.*;

import static org.junit.Assert.*;

/**
 * This test will likely fail if the full disease ontology is configured to load; instead we want to load a small 'fake'
 * one.
 *
 * @author nicolas
 */
@Category(SlowTest.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class PhenotypeAssociationTest extends BaseSpringContextTest {

    private static final String TEST_PHENOTYPE_URI = "http://purl.obolibrary.org/obo/DOID_162";
    private static final String TEST_EXTERNAL_DATABASE = "EXTERNAL_DATABASE_TEST_NAME";
    private final Integer geneNCBI = new Integer( RandomStringUtils.randomNumeric( 6 ) );

    @Autowired
    private PhenotypeAssociationManagerService phenotypeAssociationManagerService;
    @Autowired
    private PhenotypeAssociationService phenotypeAssociationService;
    @Autowired
    private UserManager userManager;
    @Autowired
    private MondoOntologyService diseaseOntologyService;
    @Autowired
    private HumanPhenotypeOntologyService humanPhenotypeOntologyService;
    @Autowired
    private MammalianPhenotypeOntologyService mammalianPhenotypeOntologyService;

    private Gene gene = null;
    private LiteratureEvidenceValueObject litEvidence = null;
    private Taxon humanTaxon = null;

    @Before
    public void setUp() throws Exception {
        // this part dirties the context
        if ( !diseaseOntologyService.isOntologyLoaded() ) {
            try ( InputStream stream = new ClassPathResource( "/data/loader/ontology/dotest.owl.xml" ).getInputStream() ) {
                diseaseOntologyService.initialize( stream, false );
            }
            humanPhenotypeOntologyService.initialize( true, false );
            mammalianPhenotypeOntologyService.initialize( true, false );
        }
        // create what will be needed for tests
        this.createGene();
        this.createExternalDatabase();
        this.createLiteratureEvidence( this.geneNCBI, PhenotypeAssociationTest.TEST_PHENOTYPE_URI );
    }

    @After
    public void tearDown() {
        this.runAsAdmin();
        phenotypeAssociationService.removeAll();
    }

    @Test
    public void testFindBibliographicReference() {
        assertNotNull( this.phenotypeAssociationManagerService.findBibliographicReference( "1" ) );
    }

    @Test
    public void testFindCandidateGenes() {

        Set<String> phenotypesValuesUri = new HashSet<>();
        phenotypesValuesUri.add( PhenotypeAssociationTest.TEST_PHENOTYPE_URI );

        Collection<GeneEvidenceValueObject> geneValueObjects = this.phenotypeAssociationManagerService
                .findCandidateGenes( phenotypesValuesUri, null );
        assertNotNull( geneValueObjects );

        assertEquals( 1, geneValueObjects.size() );

        assertTrue( geneValueObjects.iterator().next().getTaxonId() != null );

        // test other scenarios.
        super.runAsAnonymous();
        geneValueObjects = this.phenotypeAssociationManagerService.findCandidateGenes( phenotypesValuesUri, null );
        assertEquals( 1, geneValueObjects.size() );

        // user creates evidence
        String userName = RandomStringUtils.randomAlphabetic( 10 );
        this.makeUser( userName );
        this.runAsUser( userName );
        String testuri = "http://purl.obolibrary.org/obo/DOID_14566";
        this.createLiteratureEvidence( this.geneNCBI, testuri );

        phenotypesValuesUri.add( "http://purl.obolibrary.org/obo/DOID_14566" );

        // user can see their own data and public data
        geneValueObjects = this.phenotypeAssociationManagerService.findCandidateGenes( phenotypesValuesUri, null );
        assertEquals( 1, geneValueObjects.size() );
        assertEquals( 2, geneValueObjects.iterator().next().getPhenotypesValueUri().size() );

        // anonymous can't see the user's data
        super.runAsAnonymous();
        geneValueObjects = this.phenotypeAssociationManagerService.findCandidateGenes( phenotypesValuesUri, null );
        assertEquals( 1, geneValueObjects.size() );
        assertEquals( 1, geneValueObjects.iterator().next().getPhenotypesValueUri().size() );

        // admin can see everything
        this.runAsAdmin();
        geneValueObjects = this.phenotypeAssociationManagerService.findCandidateGenes( phenotypesValuesUri, null );
        assertEquals( 1, geneValueObjects.size() );
        assertEquals( 2, geneValueObjects.iterator().next().getPhenotypesValueUri().size() );

    }

    @Test
    public void testFindEvidenceByGeneId() {
        Collection<EvidenceValueObject<? extends PhenotypeAssociation>> evidences = this.phenotypeAssociationManagerService
                .findEvidenceByGeneId( this.gene.getId() );
        assertNotNull( evidences );
        assertEquals( 1, evidences.size() );
    }

    @Test
    public void testFindPhenotypesForBibliographicReference() {
        Collection<PhenotypeAssociation> phenotypes = this.phenotypeAssociationService
                .findPhenotypesForBibliographicReference( "1" );
        assertEquals( 1, phenotypes.size() );
    }

    @Test
    public void testloadAllNeurocartaPhenotypes() {
        Collection<PhenotypeValueObject> np = this.phenotypeAssociationManagerService.loadAllNeurocartaPhenotypes();
        assertTrue( np.size() > 0 );
    }

    @Test
    public void testLoadAllPhenotypeUris() {
        Set<String> uris = this.phenotypeAssociationService.loadAllUsedPhenotypeUris();
        assertTrue( !uris.isEmpty() );
    }

    @Test
    public void testLoadStatistics() {
        ExternalDatabaseStatisticsValueObject a = this.phenotypeAssociationService.loadStatisticsOnAllEvidence( "" );
        assertNotNull( a );
        Collection<ExternalDatabaseStatisticsValueObject> b = this.phenotypeAssociationService
                .loadStatisticsOnExternalDatabases( "" );
        assertNotNull( b );
    }

    @Test
    public void testLoadUpdateDeleteEvidence() {
        // 1- findEvidenceByGeneNCBI
        Collection<EvidenceValueObject<? extends PhenotypeAssociation>> evidences = this.phenotypeAssociationManagerService
                .findEvidenceByGeneNCBI( this.geneNCBI );
        assertNotNull( evidences );
        assertEquals( 1, evidences.size() );

        EvidenceValueObject<? extends PhenotypeAssociation> evidence = evidences.iterator().next();
        assertEquals( evidence.getGeneNCBI(), litEvidence.getGeneNCBI() );

        // 2- load
        evidence = this.phenotypeAssociationManagerService.load( evidence.getId() );

        assertNotNull( evidence );
        assertNotNull( evidence.getPhenotypes() );
        assertTrue( !evidence.getPhenotypes().isEmpty() );

        String description = "new Description for evidence";
        evidence.setDescription( description );

        // 3- update
        this.phenotypeAssociationManagerService.update( evidence );
        evidence = this.phenotypeAssociationManagerService.load( evidence.getId() );
        assertEquals( description, evidence.getDescription() );

        // 4- remove
        this.phenotypeAssociationManagerService.remove( evidence.getId() );

        assertNull( this.phenotypeAssociationManagerService.load( evidence.getId() ) );
    }

    @Test
    public void testSearchOntologyForPhenotypes() throws SearchException {

        // simulate someone looking for cancer, it should be found in the ontology file
        assertTrue( !this.phenotypeAssociationManagerService.searchOntologyForPhenotypes( "can", null ).isEmpty() );

        // this should not return anything
        assertTrue(
                this.phenotypeAssociationManagerService.searchOntologyForPhenotypes( "canloixys", null ).isEmpty() );
    }

    @Test
    public void testFindGenesWithEvidence() throws SearchException {
        assertTrue( !this.phenotypeAssociationManagerService.findGenesWithEvidence( "RA", this.humanTaxon.getId() )
                .isEmpty() );

        assertTrue( this.phenotypeAssociationManagerService.findGenesWithEvidence( "XXXX", this.humanTaxon.getId() )
                .isEmpty() );
    }

    @Test
    public void testFindGenesWithPhenotype() {
        OntologyTerm term = diseaseOntologyService.getTerm( "http://purl.obolibrary.org/obo/DOID_2531" );
        assertNotNull( term );

        this.createLiteratureEvidence( this.geneNCBI, "http://purl.obolibrary.org/obo/DOID_2531" );

        Map<GeneValueObject, OntologyTerm> r = this.phenotypeAssociationManagerService
                .findGenesForPhenotype( term.getUri(), this.humanTaxon.getId(), true );

        assertTrue( r.size() > 0 );

    }

    @Test
    public void testLoadTree() {
        Collection<SimpleTreeValueObject> tree = this.phenotypeAssociationManagerService
                .loadAllPhenotypesByTree( new EvidenceFilter() );
        Assertions.assertThat( tree )
                .hasSize( 3 )
                .extracting( "valueUri" )
                .containsExactlyInAnyOrder(
                        "http://purl.obolibrary.org/obo/DOID_162",
                        "http://purl.obolibrary.org/obo/DOID_14566",
                        "http://purl.obolibrary.org/obo/DOID_4" );
    }

    @Test
    public void testFindEvidenceByFilters() {
        Collection<EvidenceValueObject<? extends PhenotypeAssociation>> evidenceVO = this.phenotypeAssociationManagerService
                .findEvidenceByFilters( this.humanTaxon.getId(), 10, null );
        assertTrue( evidenceVO != null && evidenceVO.size() != 0 );

        super.runAsAnonymous();
        evidenceVO = this.phenotypeAssociationManagerService.findEvidenceByFilters( this.humanTaxon.getId(), 10, null );
        assertTrue( evidenceVO.isEmpty() );
    }

    @Test
    public void testLoadEvidenceWithExternalDatabaseName() {
        assertTrue( !this.phenotypeAssociationManagerService
                .loadEvidenceWithExternalDatabaseName( PhenotypeAssociationTest.TEST_EXTERNAL_DATABASE, PhenotypeAssociationDaoImpl.DEFAULT_PA_LIMIT, 0 )
                .isEmpty() );

        assertTrue( this.phenotypeAssociationManagerService.loadEvidenceWithoutExternalDatabaseName().isEmpty() );
    }

    // copied from AclAdviceTest
    private void makeUser( String username ) {
        try {
            this.userManager.loadUserByUsername( username );
        } catch ( UsernameNotFoundException e ) {
            this.userManager.createUser( new UserDetailsImpl( "foo", username, true, null,
                    RandomStringUtils.randomAlphabetic( 10 ) + "@gmail.com", "key", new Date() ) );
        }
    }

    private void createExternalDatabase() {
        ExternalDatabase externalDatabase = ExternalDatabase.Factory.newInstance();
        externalDatabase.setName( PhenotypeAssociationTest.TEST_EXTERNAL_DATABASE );
        externalDatabase.setWebUri( "http://www.test.ca/" );
        externalDatabaseService.findOrCreate( externalDatabase );
        assertNotNull( externalDatabaseService.findByName( PhenotypeAssociationTest.TEST_EXTERNAL_DATABASE ) );
    }

    private void createLiteratureEvidence( int geneNCBIid, String uri ) {
        this.litEvidence = new LiteratureEvidenceValueObject( -1L );
        this.litEvidence.setDescription( "Test Description" );
        this.litEvidence.setEvidenceCode( "TAS" );
        this.litEvidence.setGeneNCBI( geneNCBIid );
        this.litEvidence.setClassName( "LiteratureEvidenceValueObject" );
        CitationValueObject citationValueObject = new CitationValueObject();
        citationValueObject.setPubmedAccession( "1" );

        ExternalDatabaseValueObject externalDatabaseValueObject = new ExternalDatabaseValueObject();
        externalDatabaseValueObject.setName( PhenotypeAssociationTest.TEST_EXTERNAL_DATABASE );

        EvidenceSourceValueObject evidenceSourceValueObject = new EvidenceSourceValueObject( "url_link",
                externalDatabaseValueObject );

        SortedSet<CharacteristicValueObject> phenotypes = new TreeSet<>();

        CharacteristicValueObject characteristicValueObject = new CharacteristicValueObject( "", uri );

        phenotypes.add( characteristicValueObject );

        this.litEvidence.setPhenotypes( phenotypes );

        SortedSet<PhenotypeAssPubValueObject> phenotypeAssPubVO = new TreeSet<>();

        PhenotypeAssPubValueObject phenotypeAssPubValueObject = new PhenotypeAssPubValueObject();
        phenotypeAssPubValueObject.setType( "Primary" );
        phenotypeAssPubValueObject.setCitationValueObject( citationValueObject );
        phenotypeAssPubVO.add( phenotypeAssPubValueObject );

        this.litEvidence.setPhenotypeAssPubVO( phenotypeAssPubVO );
        this.litEvidence.setEvidenceSource( evidenceSourceValueObject );
        // those extra fields tell us where the phenotype came from if different than the one given
        this.litEvidence.setPhenotypeMapping( PhenotypeMappingType.INFERRED_CURATED.toString() );
        this.litEvidence.setOriginalPhenotype( "Original Value Test" );

        ValidateEvidenceValueObject e = this.phenotypeAssociationManagerService.makeEvidence( this.litEvidence );
        assertNull( e );
    }

    private void createGene() {
        this.humanTaxon = this.taxonService.findByCommonName( "human" );
        this.gene = Gene.Factory.newInstance();
        this.gene.setName( "RAT1" );
        this.gene.setOfficialName( "RAT1" );
        this.gene.setOfficialSymbol( "RAT1" );
        this.gene.setNcbiGeneId( this.geneNCBI );
        // the taxon is already populated in the test database
        this.gene.setTaxon( humanTaxon );
        this.gene.getProducts().add( super.getTestPersistentGeneProduct( this.gene ) );
        this.gene = ( Gene ) this.persisterHelper.persist( this.gene );
    }

}
