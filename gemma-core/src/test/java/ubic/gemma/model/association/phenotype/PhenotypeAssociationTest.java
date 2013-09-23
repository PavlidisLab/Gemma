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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.association.phenotype.PhenotypeAssociationManagerService;
import ubic.gemma.genome.gene.service.GeneService;
import ubic.gemma.model.association.phenotype.service.PhenotypeAssociationService;
import ubic.gemma.model.common.description.CitationValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.EvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.LiteratureEvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.ValidateEvidenceValueObject;
import ubic.gemma.ontology.OntologyService;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * This test will likely fail if the full disease ontology is configured to load; instead we want to load a small 'fake'
 * one.
 * 
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
    private PhenotypeAssociationService phenotypeAssociationService;

    @Autowired
    private OntologyService os;

    @Autowired
    private GeneService geneService;

    private static boolean dosLoaded = false;

    @Before
    public void setup() throws Exception {

        if ( !dosLoaded ) {
            os.getDiseaseOntologyService().loadTermsInNameSpace(
                    this.getClass().getResourceAsStream( "/data/loader/ontology/dotest.owl.xml" ) );
            int c = 0;
            while ( !os.getDiseaseOntologyService().isOntologyLoaded() ) {
                Thread.sleep( 1000 );
                log.info( "Waiting for DiseaseOntology to load" );
                if ( ++c > 20 ) {
                    fail( "Ontology load timeout" );
                }
            }
            dosLoaded = true;
        }

        makeGene( this.geneNCBI );

        createLiteratureEvidence();
    }

    @After
    public void tearDown() throws Exception {

        this.gene = this.geneService.load( this.gene.getId() );
        this.gene = geneService.thaw( gene );

        for ( PhenotypeAssociation phenotypeAssociation : this.gene.getPhenotypeAssociations() ) {
            this.phenotypeAssociationService.remove( phenotypeAssociation );
        }

        this.geneService.update( this.gene );
        this.geneService.remove( this.gene );

    }

    @Test
    public void testFindBibliographicReference() {
        assertNotNull( this.phenotypeAssociationManagerService.findBibliographicReference( "1", null ) );
    }

    @Test
    public void testFindCandidateGenes() {

        Set<String> phenotypesValuesUri = new HashSet<String>();
        phenotypesValuesUri.add( "http://purl.obolibrary.org/obo/DOID_162" );

        Collection<GeneValueObject> geneValueObjects = this.phenotypeAssociationManagerService.findCandidateGenes(
                phenotypesValuesUri, null );
        assertNotNull( geneValueObjects );

        // FIXME
        // assertEquals( 1, geneValueObjects.size() );
    }

    @Test
    public void testFindEvidenceByGeneId() {
        Collection<EvidenceValueObject> evidences = this.phenotypeAssociationManagerService
                .findEvidenceByGeneId( this.gene.getId() );
        assertNotNull( evidences );
        assertEquals( 1, evidences.size() );
    }

    @Test
    public void testLoadUpdateDeleteEvidence() {
        // 1- findEvidenceByGeneNCBI
        Collection<EvidenceValueObject> evidences = this.phenotypeAssociationManagerService
                .findEvidenceByGeneNCBI( this.geneNCBI );
        assertNotNull( evidences );
        assertEquals( 1, evidences.size() );

        EvidenceValueObject evidence = evidences.iterator().next();
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

    private void createLiteratureEvidence() {
        this.litEvidence = new LiteratureEvidenceValueObject();
        this.litEvidence.setDescription( "Test Description" );
        this.litEvidence.setEvidenceCode( "TAS" );
        this.litEvidence.setGeneNCBI( this.geneNCBI );
        this.litEvidence.setClassName( "LiteratureEvidenceValueObject" );
        CitationValueObject citationValueObject = new CitationValueObject();
        citationValueObject.setPubmedAccession( "1" );

        SortedSet<CharacteristicValueObject> phenotypes = new TreeSet<CharacteristicValueObject>();

        CharacteristicValueObject characteristicValueObject = new CharacteristicValueObject(
                "http://purl.obolibrary.org/obo/DOID_162" );

        phenotypes.add( characteristicValueObject );

        this.litEvidence.setPhenotypes( phenotypes );
        this.litEvidence.setCitationValueObject( citationValueObject );

        ValidateEvidenceValueObject e = this.phenotypeAssociationManagerService.makeEvidence( this.litEvidence );
        assertNull( e );
    }

    /**
     * @param ncbiId
     */
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

}
