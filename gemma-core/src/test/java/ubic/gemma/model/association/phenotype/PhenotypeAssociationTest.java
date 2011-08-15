package ubic.gemma.model.association.phenotype;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.association.phenotype.PhenotypeAssociationManagerService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.EvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.UrlEvidenceValueObject;
import ubic.gemma.testing.BaseSpringContextTest;

public class PhenotypeAssociationTest extends BaseSpringContextTest {

    @Autowired
    private UrlEvidenceDao urlDao;

    @Autowired
    private PhenotypeAssociationManagerService phenoAssoService;

    private String geneNCBI = "";
    private UrlEvidenceValueObject evidence = null;
    private String phenotypeValue = "";
    private String phenotypeCategory = "";
    private Collection<CharacteristicValueObject> phenotypes = null;

    @Before
    public void setup() {

        // Gene NCBI used
        geneNCBI = "44444444";
        // Phenotype
        phenotypeValue = "testValue";
        phenotypeCategory = "testCategory";

        // Evidence
        CharacteristicValueObject phenotype = new CharacteristicValueObject( phenotypeValue, phenotypeCategory );
        phenotypes = new HashSet<CharacteristicValueObject>();
        phenotypes.add( phenotype );

        evidence = new UrlEvidenceValueObject( "test_name", "test_description", null, false, "IC", phenotypes,
                "www.test.com" );

        // Make sure a Gene exist in the database with the NCBI id
        makeGene( geneNCBI );
    }

    @Test
    public void testPhenotypeAssociation() {

        // ********************************************************************************************
        // 1 - call the service to add the phenotype association and save the results to the database
        // ********************************************************************************************
        GeneValueObject geneValue = phenoAssoService.linkGeneToPhenotype( geneNCBI, evidence );
        assertTrue( geneValue.getEvidences() != null && geneValue.getEvidences().size() >= 1 );
        // ********************************************************************************************
        // 2 - call the service to find all gene for a given phenotype
        // ********************************************************************************************
        Collection<GeneValueObject> geneInfoValueObjects = phenoAssoService.findCandidateGenes( phenotypeValue );
        assertTrue( geneInfoValueObjects != null && geneInfoValueObjects.size() >= 1 );
        // ********************************************************************************************
        // 3 - call the service to find all evidences and phenotypes for a gene
        // ********************************************************************************************
        GeneValueObject geneInfoValueObject = phenoAssoService.findPhenotypeAssociations( geneNCBI );
        assertNotNull( geneInfoValueObject.getEvidences() );
        // ********************************************************************************************
        // 4 - Delete the Association
        // ********************************************************************************************
        for ( EvidenceValueObject evidenceValueObject : geneInfoValueObject.getEvidences() ) {
            phenoAssoService.removePhenotypeAssociation( evidenceValueObject.getDatabaseId() );
        }
        assertTrue( phenoAssoService.findCandidateGenes( phenotypeValue ).size() == 0 );
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

    // not a junit test, used to check values
    public void testFindPhenotypeAssociations() {

        // call to the service
        GeneValueObject geneInfoValueObject = phenoAssoService.findPhenotypeAssociations( "2" );

        System.out.println( "" );
        System.out.println( "Gene name: " + geneInfoValueObject.getName() );
        System.out.println( "" );
        System.out.println();
        System.out.println();

        for ( EvidenceValueObject evidence : geneInfoValueObject.getEvidences() ) {

            System.out.println( "Found evidence: " + evidence.getDatabaseId() + "   " + evidence.getName() + "   "
                    + evidence.getDescription() );
            System.out.println( "With phenotypes: " );

            for ( CharacteristicValueObject phenotype : evidence.getPhenotypes() ) {
                System.out.println( "Value :" + phenotype.getValue() );

            }
            System.out.println();
            System.out.println();
        }
    }

    // not a junit test, used to check values
    public void testFindCandidateGenes() {

        Collection<GeneValueObject> geneInfoValueObjects = phenoAssoService.findCandidateGenes( "CANCER" );

        for ( GeneValueObject geneInfoValueObject : geneInfoValueObjects ) {

            System.out.println( "" );
            System.out.println( "Gene name: " + geneInfoValueObject.getName() );
            System.out.println( "" );
            System.out.println();
            System.out.println();

            for ( EvidenceValueObject evidence : geneInfoValueObject.getEvidences() ) {

                System.out.println( "Found evidence: " + evidence.getDatabaseId() + "   " + evidence.getName() + "   "
                        + evidence.getDescription() );
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
        g.setNcbiId( ncbiId );
        g.setTaxon( rat );
        List<GeneProduct> ggg = new ArrayList<GeneProduct>();
        ggg.add( super.getTestPersistentGeneProduct( g ) );
        g.setProducts( ggg );
        g = ( Gene ) persisterHelper.persist( g );
        return g;
    }

}
