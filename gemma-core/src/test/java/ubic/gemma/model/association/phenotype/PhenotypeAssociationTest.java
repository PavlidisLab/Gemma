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
package ubic.gemma.model.association.phenotype;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.RandomStringUtils;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.association.phenotype.PhenotypeAssoOntologyHelper;
import ubic.gemma.association.phenotype.PhenotypeAssoOntologyHelperImpl;
import ubic.gemma.association.phenotype.PhenotypeAssociationManagerService;
import ubic.gemma.genome.gene.service.GeneService;
import ubic.gemma.model.common.description.CitationValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.EvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.LiteratureEvidenceValueObject;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author nicolas
 * @version $Id$
 */
public class PhenotypeAssociationTest extends BaseSpringContextTest {

    @Autowired
    private PhenotypeAssociationManagerService phenotypeAssociationManagerService;

    private int geneNCBI = new Integer( RandomStringUtils.randomNumeric( 6 ) );

    private Gene gene = null;

    private LiteratureEvidenceValueObject litEvidence = null;

    @Autowired
    private GeneService geneService;

    @Before
    public void setup() {
        // make a test gene
        // makeGene( this.geneNCBI );
        // mock the ontology
        // mockOntology();
        // create an literature Evidence
        // createLiteratureEvidence();
    }

    @After
    public void tearDown() {

        // this.gene = this.geneService.load( this.gene.getId() );

        // for ( PhenotypeAssociation phenotypeAssociation : this.gene.getPhenotypeAssociations() ) {
        // this.phenotypeAssociationService.remove( phenotypeAssociation );
        // }

        // this.geneService.update( this.gene );
        // this.geneService.remove( this.gene );

        Collection<Gene> genes = geneService.loadAll();
        for ( Gene g : genes ) {
            try {
                geneService.remove( g );
            } catch ( Exception e ) {

            }
        }

    }

    // @Test
    public void testFindBibliographicReference() {

        assertNotNull( this.phenotypeAssociationManagerService.findBibliographicReference( "1", null ) );
    }

    // @Test
    public void testFindCandidateGenes() {

        Set<String> phenotypesValuesUri = new HashSet<String>();
        phenotypesValuesUri.add( "testUri" );

        Collection<GeneValueObject> geneValueObjects = this.phenotypeAssociationManagerService.findCandidateGenes(
                phenotypesValuesUri, null );

        assertTrue( geneValueObjects != null && geneValueObjects.size() == 1 );
    }

    @Test
    public void testFindEvidenceByGeneId() {
        // Collection<EvidenceValueObject> evidences = this.phenotypeAssociationManagerService
        // .findEvidenceByGeneId( this.gene.getId() );
        // assertTrue( evidences != null && evidences.size() == 1 );
    }

    // @Test
    public void testLoadUpdateDeleteEvidence() {
        // 1- findEvidenceByGeneNCBI
        Collection<EvidenceValueObject> evidences = this.phenotypeAssociationManagerService
                .findEvidenceByGeneNCBI( this.geneNCBI );
        assertTrue( evidences != null && evidences.size() == 1 );

        @SuppressWarnings("null")
        EvidenceValueObject evidence = evidences.iterator().next();
        assertTrue( evidence.equals( this.litEvidence ) );

        // 2- load
        evidence = this.phenotypeAssociationManagerService.load( evidence.getId() );

        assertNotNull( evidence );

        evidence.setDescription( "new Description" );
        // 3- update
        this.phenotypeAssociationManagerService.update( evidence );
        evidence = this.phenotypeAssociationManagerService.load( evidence.getId() );
        assertTrue( evidence.getDescription().equals( "new Description" ) );

        // 4- remove
        this.phenotypeAssociationManagerService.remove( evidence.getId() );

        assertNull( this.phenotypeAssociationManagerService.load( evidence.getId() ) );
    }

    @SuppressWarnings("unused")
    private void createLiteratureEvidence() {
        this.litEvidence = new LiteratureEvidenceValueObject();
        this.litEvidence.setDescription( "Test Description" );
        this.litEvidence.setEvidenceCode( "TAS" );
        this.litEvidence.setGeneNCBI( this.geneNCBI );

        CitationValueObject citationValueObject = new CitationValueObject();
        citationValueObject.setPubmedAccession( "1" );

        SortedSet<CharacteristicValueObject> phenotypes = new TreeSet<CharacteristicValueObject>();

        CharacteristicValueObject characteristicValueObject = new CharacteristicValueObject( "testUri" );

        phenotypes.add( characteristicValueObject );

        this.litEvidence.setPhenotypes( phenotypes );
        this.litEvidence.setCitationValueObject( citationValueObject );

        this.phenotypeAssociationManagerService.makeEvidence( this.litEvidence );
    }

    /**
     * @param ncbiId
     */
    @SuppressWarnings("unused")
    private void makeGene( int ncbiId ) {
        this.gene = Gene.Factory.newInstance();
        this.gene.setName( "RAT1" );
        this.gene.setOfficialName( "RAT1" );
        this.gene.setOfficialSymbol( "RAT1" );
        this.gene.setNcbiGeneId( new Integer( ncbiId ) );
        // the taxon is already populated in the test database
        this.gene.setTaxon( this.taxonService.findByCommonName( "human" ) );
        this.gene.getProducts().add( super.getTestPersistentGeneProduct( this.gene ) );
        this.gene = ( Gene ) this.persisterHelper.persist( this.gene );
    }

    /**
     * @throws SecurityException
     * @throws NoSuchMethodException
     */
    @SuppressWarnings("unused")
    private void mockOntology() throws SecurityException, NoSuchMethodException {

        OntologyTerm mockedOntoloyTerm = EasyMock.createMock( OntologyTerm.class );

        org.easymock.EasyMock.expect( mockedOntoloyTerm.getUri() ).andReturn( "testUri" ).anyTimes();
        org.easymock.EasyMock.expect( mockedOntoloyTerm.getLabel() ).andReturn( "testLabel" ).anyTimes();

        Collection<OntologyTerm> emptyCollection = new HashSet<OntologyTerm>();

        org.easymock.EasyMock.expect( mockedOntoloyTerm.getChildren( true ) ).andReturn( emptyCollection ).anyTimes();
        org.easymock.EasyMock.expect( mockedOntoloyTerm.getChildren( false ) ).andReturn( emptyCollection ).anyTimes();

        EasyMock.replay( mockedOntoloyTerm );

        // we only mock 1 method of the class in this case, the other methods of the object behave normally
        PhenotypeAssoOntologyHelper phenotypeAssoOntologyHelperMocked = EasyMock
                .createMockBuilder( PhenotypeAssoOntologyHelperImpl.class )
                .addMockedMethods(
                        new Method[] { PhenotypeAssoOntologyHelperImpl.class.getMethod( "findOntologyTermByUri",
                                String.class ) } ).createMock();

        org.easymock.EasyMock.expect( phenotypeAssoOntologyHelperMocked.findOntologyTermByUri( "testUri" ) )
                .andReturn( mockedOntoloyTerm ).anyTimes();

        EasyMock.replay( phenotypeAssoOntologyHelperMocked );

        // We cannot do this == this is not a mock object being modified!

        // this.phenotypeAssociationManagerService.setOntologyHelper( phenotypeAssoOntologyHelperMocked );
    }

}
