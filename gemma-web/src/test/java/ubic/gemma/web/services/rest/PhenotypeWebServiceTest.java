/*
 * The gemma-web project
 * 
 * Copyright (c) 2014 University of British Columbia
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

package ubic.gemma.web.services.rest;

import net.sf.json.JSONArray;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.core.association.phenotype.PhenotypeAssociationManagerService;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.association.phenotype.PhenotypeMappingType;
import ubic.gemma.model.common.description.CitationValueObject;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabaseValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.phenotype.valueObject.*;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.core.testing.BaseSpringWebTest;

import java.io.InputStream;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.junit.Assert.*;

/**
 * Like PhenotypeAssociationTest but exercises web service functionality.
 *
 * @author paul
 */
public class PhenotypeWebServiceTest extends BaseSpringWebTest {
    private static final String TEST_EXTERNAL_DATABASE = "EXTERNAL_DATABASE_TEST_NAME";
    @Autowired
    private OntologyService os;
    @Autowired
    private PhenotypeAssociationManagerService phenotypeAssociationManagerService;

    private final Integer geneNCBI = new Integer( RandomStringUtils.randomNumeric( 6 ) );

    @Autowired
    private PhenotypeWebService pws;

    @Before
    public void setup() throws Exception {

        try (InputStream stream = this.getClass().getResourceAsStream( "/data/loader/ontology/dotest.owl.xml" )) {
            assertNotNull( stream );
            os.getDiseaseOntologyService().loadTermsInNameSpace( stream );

            createGene();
            createExternalDatabase();

            OntologyTerm term = os.getDiseaseOntologyService().getTerm( "http://purl.obolibrary.org/obo/DOID_2531" );
            assertNotNull( term );

            createLiteratureEvidence( this.geneNCBI, "http://purl.obolibrary.org/obo/DOID_2531" );
        }
    }

    @Test
    public void test1() {
        JSONArray response = pws.findPhenotypeGenes( 1L, "http://purl.obolibrary.org/obo/DOID_2531" );
        assertTrue( response.toString().contains( "http://purl.obolibrary.org/obo/DOID_2531" ) );
        assertTrue( response.toString().contains( "RAT1" ) );

        System.err.println( response.toString( 4 ) );
    }

    private void createExternalDatabase() {
        ExternalDatabase externalDatabase = ExternalDatabase.Factory.newInstance();
        externalDatabase.setName( TEST_EXTERNAL_DATABASE );
        externalDatabase.setWebUri( "http://www.test.ca/" );
        externalDatabase = externalDatabaseService.findOrCreate( externalDatabase );
        assertNotNull( externalDatabaseService.find( TEST_EXTERNAL_DATABASE ) );
    }

    private void createGene() {
        Taxon humanTaxon = this.taxonService.findByCommonName( "human" );
        Gene gene = Gene.Factory.newInstance();
        gene.setName( "RAT1" );
        gene.setOfficialName( "RAT1" );
        gene.setOfficialSymbol( "RAT1" );
        gene.setNcbiGeneId( this.geneNCBI );
        // the taxon is already populated in the test database
        gene.setTaxon( humanTaxon );
        gene.getProducts().add( super.getTestPersistentGeneProduct( gene ) );
        gene = ( Gene ) this.persisterHelper.persist( gene );
    }

    private void createLiteratureEvidence( int geneNCBIid, String uri ) {
        LiteratureEvidenceValueObject litEvidence = new LiteratureEvidenceValueObject(-1L);
        litEvidence.setDescription( "Test Description" );
        litEvidence.setEvidenceCode( "TAS" );
        litEvidence.setGeneNCBI( geneNCBIid );
        litEvidence.setClassName( "LiteratureEvidenceValueObject" );
        CitationValueObject citationValueObject = new CitationValueObject();
        citationValueObject.setPubmedAccession( "1" );

        ExternalDatabaseValueObject externalDatabaseValueObject = new ExternalDatabaseValueObject();
        externalDatabaseValueObject.setName( TEST_EXTERNAL_DATABASE );

        EvidenceSourceValueObject evidenceSourceValueObject = new EvidenceSourceValueObject( "url_link",
                externalDatabaseValueObject );

        SortedSet<CharacteristicValueObject> phenotypes = new TreeSet<>();

        CharacteristicValueObject characteristicValueObject = new CharacteristicValueObject( -1L, uri );

        phenotypes.add( characteristicValueObject );

        litEvidence.setPhenotypes( phenotypes );

        SortedSet<PhenotypeAssPubValueObject> phenotypeAssPubVO = new TreeSet<>();

        PhenotypeAssPubValueObject phenotypeAssPubValueObject = new PhenotypeAssPubValueObject();
        phenotypeAssPubValueObject.setType( "Primary" );
        phenotypeAssPubValueObject.setCitationValueObject( citationValueObject );
        phenotypeAssPubVO.add( phenotypeAssPubValueObject );

        litEvidence.setPhenotypeAssPubVO( phenotypeAssPubVO );
        litEvidence.setEvidenceSource( evidenceSourceValueObject );
        // those extra fields tell us where the phenotype came from if different than the one given
        litEvidence.setPhenotypeMapping( PhenotypeMappingType.INFERRED_CURATED.toString() );
        litEvidence.setOriginalPhenotype( "Original Value Test" );
        litEvidence.setEvidenceCode( GOEvidenceCode.EXP.toString() );

        ValidateEvidenceValueObject e = this.phenotypeAssociationManagerService.makeEvidence( litEvidence );
        assertNull( e );
    }

}
