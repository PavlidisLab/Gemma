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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.association.phenotype.PhenotypeAssociationManagerService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.EvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.GeneEvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.UrlEvidenceValueObject;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author nicolas
 * @version $Id$
 */
public class PhenotypeAssociationTest extends BaseSpringContextTest {

    @Autowired
    private UrlEvidenceDao urlDao;

    @Autowired
    private GeneService geneService;

    @Autowired
    private PhenotypeAssociationManagerService phenoAssoService;

    private String geneNCBI = "";
    private UrlEvidenceValueObject evidence = null;
    private String phenotypeValue = "";
    private String phenotypeCategory = "";
    private Set<CharacteristicValueObject> phenotypes = null;

    private Gene gene;

    @Before
    public void setup() {

        // Gene NCBI used
        geneNCBI = RandomStringUtils.randomNumeric( 6 );
        // Phenotype
        phenotypeValue = RandomStringUtils.randomAlphabetic( 6 );
        phenotypeCategory = RandomStringUtils.randomAlphabetic( 6 );

        // Evidence
        CharacteristicValueObject phenotype = new CharacteristicValueObject( phenotypeValue, phenotypeCategory );
        phenotypes = new HashSet<CharacteristicValueObject>();
        phenotypes.add( phenotype );

        evidence = new UrlEvidenceValueObject( "test_description", null, false, "IC", phenotypes, "www.test.com" );

        // Make sure a Gene exist in the database with the NCBI id
        this.gene = makeGene( geneNCBI );
    }

    @After
    public void tearDown() {
        if ( gene != null ) {
            gene.getPhenotypeAssociations().clear();
            geneService.remove( gene );
        }
    }

    @Test
    public void testPhenotypeAssociation() {

        // ********************************************************************************************
        // 1 - call the service to add the phenotype association and save the results to the database
        // ********************************************************************************************
        GeneEvidenceValueObject geneValue = phenoAssoService.create( geneNCBI, evidence );

        assertNotNull( geneValue );
        assertNotNull( geneValue.getEvidence() );
        assertTrue( !geneValue.getEvidence().isEmpty() );

        // ********************************************************************************************
        // 2 - call the service to find all gene for a given phenotype
        // ********************************************************************************************
        Collection<GeneEvidenceValueObject> geneInfoValueObjects = phenoAssoService
                .findCandidateGenes( phenotypeValue );

        assertNotNull( geneInfoValueObjects );
        assertTrue( !geneInfoValueObjects.isEmpty() );

        // ********************************************************************************************
        // 3 - call the service to find all evidence and phenotypes for a gene
        // ********************************************************************************************
        Collection<EvidenceValueObject> evidence = phenoAssoService.findEvidenceByGeneNCBI( geneNCBI );
        assertNotNull( evidence );

        // ********************************************************************************************
        // 4 - Delete the Association
        // ********************************************************************************************
        for ( EvidenceValueObject evidenceValueObject : evidence ) {
            phenoAssoService.remove( evidenceValueObject.getDatabaseId() );
        }
        assertEquals( 0, phenoAssoService.findCandidateGenes( phenotypeValue ).size() );

    }

    // @Test
    public void testDaoUrlEvidence() {

        // create
        UrlEvidence urlEvidence = new UrlEvidenceImpl();
        urlEvidence.setDescription( "testDescription" );
        urlEvidence.setName( "testname" );
        urlEvidence.setUrl( "www.test.com" );
        UrlEvidence entityReturn = urlDao.create( urlEvidence );
        assertNotNull( entityReturn.getId() );

        // update
        urlEvidence.setUrl( "www.testupdate.com" );
        urlDao.update( urlEvidence );

        // load
        UrlEvidence urlEvidenceLoad = urlDao.load( entityReturn.getId() );
        assertNotNull( urlEvidenceLoad );
        assertTrue( urlEvidenceLoad.getUrl().equals( "www.testupdate.com" ) );

        // remove
        urlDao.remove( entityReturn.getId() );
        assertNull( urlDao.load( entityReturn.getId() ) );
    }

    // not a junit test, used to check values // not used
    public void testFindPhenotypeAssociations() {

        // call to the service
        Collection<EvidenceValueObject> evidence = phenoAssoService.findEvidenceByGeneNCBI( "2" );

        for ( EvidenceValueObject evidenceVO : evidence ) {

            System.out.println( "Found evidence: " + evidenceVO.getDatabaseId() + "   " + evidenceVO.getDescription() );
            System.out.println( "With phenotypes: " );

            for ( CharacteristicValueObject phenotype : evidenceVO.getPhenotypes() ) {
                System.out.println( "Value :" + phenotype.getValue() );

            }
            System.out.println();
            System.out.println();
        }
    }

    // not a junit test, used to check values
    public void testFindCandidateGenes() {

        Collection<GeneEvidenceValueObject> geneInfoValueObjects = phenoAssoService.findCandidateGenes( "CANCER" );

        for ( GeneEvidenceValueObject geneInfoValueObject : geneInfoValueObjects ) {

            System.out.println( "" );
            System.out.println( "Gene name: " + geneInfoValueObject.getName() );
            System.out.println( "" );
            System.out.println();
            System.out.println();

            for ( EvidenceValueObject evidence : geneInfoValueObject.getEvidence() ) {

                System.out.println( "Found evidence: " + evidence.getDatabaseId() + "   " + evidence.getDescription() );
                System.out.println( "With phenotypes: " );

                for ( CharacteristicValueObject phenotype : evidence.getPhenotypes() ) {
                    System.out.println( "Value :" + phenotype.getValue() );

                }
                System.out.println();
                System.out.println();
            }
        }
    }

    private Gene makeGene( String ncbiId ) {

        Taxon rat = Taxon.Factory.newInstance();
        rat.setIsGenesUsable( true );
        rat.setNcbiId( 10116 );
        rat.setScientificName( "Rattus norvegicus" );
        rat.setIsSpecies( true );
        persisterHelper.persist( rat );

        Gene g = Gene.Factory.newInstance();
        g.setName( "RAT1" );
        g.setOfficialName( "RAT1" );
        g.setOfficialSymbol( "RAT1" );
        g.setNcbiGeneId( Integer.parseInt( ncbiId ) );
        g.setTaxon( rat );
        g.getProducts().add( super.getTestPersistentGeneProduct( g ) );
        g = ( Gene ) persisterHelper.persist( g );
        return g;
    }

}
