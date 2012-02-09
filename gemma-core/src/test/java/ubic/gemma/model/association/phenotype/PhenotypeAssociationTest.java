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

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.association.phenotype.PhenotypeAssociationManagerService;
import ubic.gemma.genome.gene.service.GeneService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.EvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.ExperimentalEvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.GeneEvidenceValueObject;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author nicolas
 * @version $Id$
 */
public class PhenotypeAssociationTest extends BaseSpringContextTest {

    @Autowired
    private GeneService geneService;

    @Autowired
    private PhenotypeAssociationManagerService phenoAssoService;

    private String geneNCBI = RandomStringUtils.randomNumeric( 6 );
    private EvidenceValueObject evidence = null;
    private String phenotypeValue = RandomStringUtils.randomAlphabetic( 6 );
    private Gene gene = null;
    private GeneEvidenceValueObject geneValue = null;
    private String primaryPubmed = "17699851";

    // @Before
    public void setup() {

        // Evidence
        CharacteristicValueObject phenotype = new CharacteristicValueObject( this.phenotypeValue, "phenotypeCategory",
                "phenotypeValueUri", "phenotypeCategoryUri" );
        CharacteristicValueObject caracteristic = new CharacteristicValueObject( "chaValue", "chaCategory",
                "chaValueUri", "chaCategoryUri" );

        Set<String> relevantPublication = new HashSet<String>();
        relevantPublication.add( "17699851" );

        Set<CharacteristicValueObject> phenotypes = new HashSet<CharacteristicValueObject>();
        phenotypes.add( phenotype );

        Set<CharacteristicValueObject> characteristics = new HashSet<CharacteristicValueObject>();
        characteristics.add( caracteristic );

        this.evidence = new ExperimentalEvidenceValueObject( "test Description", null, new Boolean( true ), "IC",
                phenotypes, this.primaryPubmed, relevantPublication, characteristics, null );

        // Make sure a Gene exist in the database with the NCBI id
        this.gene = makeGene( this.geneNCBI );
    }

    // @After
    public void tearDown() {
        if ( this.gene != null ) {
            this.gene.getPhenotypeAssociations().clear();
            Gene myGene = this.geneService.load( this.geneValue.getId() );
            this.geneService.remove( myGene );
        }
    }

    @Test
    public void testPhenotypeAssoService() {
        // TODO the create method is now using the ontology to find phenotype, since the ontologgy is not loaded in
        // test, wont work

        /*
         * this.geneValue = this.phenoAssoService.create( this.geneNCBI, this.evidence );
         * 
         * assertNotNull( this.geneValue ); assertNotNull( this.geneValue.getEvidence() ); assertTrue(
         * !this.geneValue.getEvidence().isEmpty() );
         * 
         * assertTrue( this.phenoAssoService.findGenesWithEvidence( this.geneValue.getName(), null ).size() > 0 );
         * 
         * for ( EvidenceValueObject evidenceValueObject : this.geneValue.getEvidence() ) {
         * 
         * EvidenceValueObject evidenceVO = this.phenoAssoService.load( evidenceValueObject.getDatabaseId() );
         * assertNotNull( evidenceVO );
         * 
         * this.phenoAssoService.remove( evidenceValueObject.getDatabaseId() ); evidenceVO = this.phenoAssoService.load(
         * evidenceValueObject.getDatabaseId() ); assertNull( evidenceVO ); }
         */
    }

    private Gene makeGene( String ncbiId ) {

        Taxon rat = Taxon.Factory.newInstance();
        rat.setIsGenesUsable( new Boolean( true ) );
        rat.setNcbiId( new Integer( 10116 ) );
        rat.setScientificName( "Rattus norvegicus" );
        rat.setIsSpecies( new Boolean( true ) );
        this.persisterHelper.persist( rat );

        Gene g = Gene.Factory.newInstance();
        g.setName( "RAT1" );
        g.setOfficialName( "RAT1" );
        g.setOfficialSymbol( "RAT1" );
        g.setNcbiGeneId( new Integer( ncbiId ) );
        g.setTaxon( rat );
        g.getProducts().add( super.getTestPersistentGeneProduct( g ) );
        g = ( Gene ) this.persisterHelper.persist( g );
        return g;
    }

}
