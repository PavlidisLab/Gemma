/*
 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
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

package ubic.gemma.loader.protein;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.association.Gene2GeneProteinAssociation;
import ubic.gemma.model.association.Gene2GeneProteinAssociationService;
import ubic.gemma.model.common.description.DatabaseEntryService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * Tests the loader for string protein interactions: Test 3 out of the 4 scenarios for loading protein protein
 * interactions namely:
 * <ul>
 * <li>Local biomart file, local string file, one taxon
 * <li>Remote biomart file, local string file one taxon
 * <li>Remote bimart file, local string file multi taxon
 * </ul>
 * The only scenario not tested is downloading from string website simply too long recommended usage is to use local
 * file. As is downloads a file from biomart any changes in biomart interface will be picked up. Should add some more
 * error scenarios.
 * 
 * @author ldonnison
 * @version $Id$
 */
public class StringBiomartGene2GeneProteinLoaderTest extends BaseSpringContextTest {

    @Autowired
    private DatabaseEntryService databaseService = null;

    @Autowired
    private Gene2GeneProteinAssociationService gene2GeneProteinAssociationService;

    private Collection<Gene2GeneProteinAssociation> geneAssociationRat = null;

    private Collection<Gene2GeneProteinAssociation> geneAssociationZebra = null;

    @Autowired
    private GeneService geneService = null;
    private Collection<Gene> genesRat = null;
    private Collection<Gene> genesZebra = null;

    private Taxon rat = null;
    private StringBiomartGene2GeneProteinAssociationLoader stringBiomartGene2GeneProteinAssociationLoader = null;

    private Collection<Taxon> taxa = null;
    private Taxon zebraFish = null;

    /**
     * Set up and save some taxa.
     * 
     * @return taxa
     */
    public Collection<Taxon> getTaxonToProcess() {
        taxa = new ArrayList<Taxon>();

        zebraFish = Taxon.Factory.newInstance();
        zebraFish.setIsGenesUsable( true );
        zebraFish.setNcbiId( 7955 );
        zebraFish.setScientificName( "Danio rerio" );
        zebraFish.setIsSpecies( true );

        persisterHelper.persist( zebraFish );
        taxa.add( ( Taxon ) persisterHelper.persist( zebraFish ) );

        rat = Taxon.Factory.newInstance();
        rat.setIsGenesUsable( true );
        rat.setNcbiId( 10116 );
        rat.setScientificName( "Rattus norvegicus" );
        rat.setIsSpecies( true );
        persisterHelper.persist( rat );
        taxa.add( ( Taxon ) persisterHelper.persist( rat ) );

        return taxa;
    }

    /**
     * Test data note ENSRNOP00000036045 mapped to two ncib genes
     */
    public void getTestPeristentGenesRat() {
        genesRat = new ArrayList<Gene>();
        // ENSRNOG00000023387 ENSRNOT00000028988 679739 ENSRNOP00000036045
        Gene geneRatOne = Gene.Factory.newInstance();
        geneRatOne.setName( "RAT1" );
        geneRatOne.setOfficialName( "RAT1" );
        geneRatOne.setOfficialSymbol( "RAT1" );
        geneRatOne.setNcbiId( "679739" );
        geneRatOne.setTaxon( rat );
        List<GeneProduct> geneProduct = new ArrayList<GeneProduct>();
        geneProduct.add( super.getTestPersistentGeneProduct( geneRatOne ) );
        geneRatOne.setProducts( geneProduct );
        geneRatOne = ( Gene ) persisterHelper.persist( geneRatOne );
        genesRat.add( geneRatOne );

        // ENSRNOG00000023387 ENSRNOT00000028988 692052 ENSRNOP00000036045
        Gene geneRatTwo = Gene.Factory.newInstance();
        geneRatTwo.setName( "RAT2" );
        geneRatTwo.setOfficialName( "RAT2" );
        geneRatTwo.setOfficialSymbol( "RAT2" );
        geneRatTwo.setNcbiId( "692052" );
        geneRatTwo.setTaxon( rat );
        List<GeneProduct> geneProductTwo = new ArrayList<GeneProduct>();
        geneProductTwo.add( super.getTestPersistentGeneProduct( geneRatTwo ) );
        geneRatTwo.setProducts( geneProductTwo );
        geneRatTwo = ( Gene ) persisterHelper.persist( geneRatTwo );
        genesZebra.add( geneRatTwo );

        // ENSRNOG00000017115 ENSRNOT00000023122 297436 ENSRNOP00000023122
        Gene geneRatThree = Gene.Factory.newInstance();
        geneRatThree.setName( "RAT3" );
        geneRatThree.setOfficialName( "RAT3" );
        geneRatThree.setOfficialSymbol( "RAT4" );
        geneRatThree.setNcbiId( "297436" );
        geneRatThree.setTaxon( rat );
        List<GeneProduct> geneProductThree = new ArrayList<GeneProduct>();
        geneProductThree.add( super.getTestPersistentGeneProduct( geneRatThree ) );
        geneRatThree.setProducts( geneProductThree );
        geneRatThree = ( Gene ) persisterHelper.persist( geneRatThree );
        genesRat.add( geneRatThree );

        Gene2GeneProteinAssociation existingGene2GeneProteinAssociationOne = Gene2GeneProteinAssociation.Factory
                .newInstance();
        existingGene2GeneProteinAssociationOne.setFirstGene( geneRatOne );
        existingGene2GeneProteinAssociationOne.setSecondGene( geneRatThree );

        Gene2GeneProteinAssociation existingGene2GeneProteinAssociationTwo = Gene2GeneProteinAssociation.Factory
                .newInstance();
        existingGene2GeneProteinAssociationTwo.setFirstGene( geneRatOne );
        existingGene2GeneProteinAssociationTwo.setSecondGene( geneRatTwo );

        geneAssociationRat = new ArrayList<Gene2GeneProteinAssociation>();
        geneAssociationRat.add( existingGene2GeneProteinAssociationTwo );
        geneAssociationRat.add( existingGene2GeneProteinAssociationOne );

    }

    public void getTestPeristentGenesZebra() {
        genesZebra = new ArrayList<Gene>();
        // ENSDARG00000074913 ENSDART00000109786 751652 ENSDARP00000098813
        Gene geneZebraOne = Gene.Factory.newInstance();
        geneZebraOne.setName( "zgc:153184" );
        geneZebraOne.setOfficialName( "zgc:153184" );
        geneZebraOne.setOfficialSymbol( "zgc:153184" );
        geneZebraOne.setNcbiId( "751652" );
        geneZebraOne.setTaxon( zebraFish );
        List<GeneProduct> geneProduct = new ArrayList<GeneProduct>();
        geneProduct.add( super.getTestPersistentGeneProduct( geneZebraOne ) );
        geneZebraOne.setProducts( geneProduct );
        geneZebraOne = ( Gene ) persisterHelper.persist( geneZebraOne );
        genesZebra.add( geneZebraOne );

        // ENSDARG00000060734 ENSDART00000085868 571540 ENSDARP00000080303
        Gene geneZebraTwo = Gene.Factory.newInstance();
        geneZebraTwo.setName( "appl1" );
        geneZebraTwo.setOfficialName( "appl1" );
        geneZebraTwo.setOfficialSymbol( "appl1" );
        geneZebraTwo.setNcbiId( "571540" );
        geneZebraTwo.setTaxon( zebraFish );
        List<GeneProduct> geneProductTwo = new ArrayList<GeneProduct>();
        geneProductTwo.add( super.getTestPersistentGeneProduct( geneZebraTwo ) );
        geneZebraTwo.setProducts( geneProductTwo );
        geneZebraTwo = ( Gene ) persisterHelper.persist( geneZebraTwo );
        genesZebra.add( geneZebraTwo );

        // ENSDARG00000008473 ENSDART00000019008 568371 ENSDARP00000006838
        Gene geneZebraThree = Gene.Factory.newInstance();
        geneZebraThree.setName( "LOC568371" );
        geneZebraThree.setOfficialName( "LOC568371" );
        geneZebraThree.setOfficialSymbol( "LOC568371" );
        geneZebraThree.setNcbiId( "568371" );
        geneZebraThree.setTaxon( zebraFish );
        List<GeneProduct> geneProductThree = new ArrayList<GeneProduct>();
        geneProductThree.add( super.getTestPersistentGeneProduct( geneZebraThree ) );
        geneZebraThree.setProducts( geneProductThree );
        geneZebraThree = ( Gene ) persisterHelper.persist( geneZebraThree );
        genesZebra.add( geneZebraThree );

        Gene2GeneProteinAssociation existingGene2GeneProteinAssociationOne = Gene2GeneProteinAssociation.Factory
                .newInstance();
        existingGene2GeneProteinAssociationOne.setFirstGene( geneZebraOne );
        existingGene2GeneProteinAssociationOne.setSecondGene( geneZebraThree );

        Gene2GeneProteinAssociation existingGene2GeneProteinAssociationTwo = Gene2GeneProteinAssociation.Factory
                .newInstance();
        existingGene2GeneProteinAssociationTwo.setFirstGene( geneZebraOne );
        existingGene2GeneProteinAssociationTwo.setSecondGene( geneZebraTwo );

        geneAssociationZebra = new ArrayList<Gene2GeneProteinAssociation>();
        geneAssociationZebra.add( existingGene2GeneProteinAssociationTwo );
        geneAssociationZebra.add( existingGene2GeneProteinAssociationOne );

    }

    @Before
    public void setUp() throws Exception {

        stringBiomartGene2GeneProteinAssociationLoader = new StringBiomartGene2GeneProteinAssociationLoader();
        stringBiomartGene2GeneProteinAssociationLoader.setPersisterHelper( super.persisterHelper );

        stringBiomartGene2GeneProteinAssociationLoader.setGeneService( geneService );
        stringBiomartGene2GeneProteinAssociationLoader.setExternalDatabaseService( super.externalDatabaseService );

        taxa = getTaxonToProcess();
        getTestPeristentGenesZebra();
        getTestPeristentGenesRat();

        // make sure all the data is cleared out before starting
        this.gene2GeneProteinAssociationService.deleteAll( gene2GeneProteinAssociationService.loadAll() );
        assertTrue( gene2GeneProteinAssociationService.loadAll().isEmpty() );

    }

    /**
     * tests that given a local biomart and local string file data is processed
     */
    @Test
    public void testDoLoadLocalBiomartLocalStringOneTaxon() throws Exception {

        String fileNameStringZebraFish = "/data/loader/protein/string/protein.links.zebrafish.txt";
        URL fileNameStringZebraFishURL = this.getClass().getResource( fileNameStringZebraFish );

        String fileNameStringZebra = "/data/loader/protein/biomart/biomartzebrafish.txt";
        URL fileNameBiomartZebraURL = this.getClass().getResource( fileNameStringZebra );

        Collection<Taxon> taxaZebraFish = new ArrayList<Taxon>();
        taxaZebraFish.add( zebraFish );

        stringBiomartGene2GeneProteinAssociationLoader.load( new File( fileNameStringZebraFishURL.getFile() ), null,
                new File( fileNameBiomartZebraURL.getFile() ), taxaZebraFish );

        Collection<Gene2GeneProteinAssociation> associations = gene2GeneProteinAssociationService.loadAll();
        assertEquals( 3, associations.size() );

        for ( Gene gene : genesZebra ) {
            Collection<Gene2GeneProteinAssociation> interactionsForGene = this.gene2GeneProteinAssociationService
                    .findProteinInteractionsForGene( gene );

            if ( gene.getName().equals( "zgc:153184" ) ) {
                assertEquals( 2, interactionsForGene.size() );
            }
            if ( gene.getName().equals( "appl1" ) ) {
                assertEquals( 2, interactionsForGene.size() );
            }
            if ( gene.getName().equals( "LOC568371" ) ) {
                assertEquals( 2, interactionsForGene.size() );
            }
        }

        this.gene2GeneProteinAssociationService.deleteAll( associations );
        associations = gene2GeneProteinAssociationService.loadAll();
        assertTrue( associations.isEmpty() );

    }

    /**
     * Tests that two taxons can be processed at same time. NOTE it does get the files from the biomart site. It also
     * checks that the byte representing the evidence code is stored correctly.
     */
    @Test
    public void testDoLoadRemoteBiomartFileLocalStringFileMultipleTaxon() throws Exception {
        String fileNameStringmouse = "/data/loader/protein/string/protein.links.multitaxon.txt";
        URL fileNameStringmouseURL = this.getClass().getResource( fileNameStringmouse );
        int counterAssociationsSavedZebra = 0;
        int counterAssociationsSavedRat = 0;

        try {
            stringBiomartGene2GeneProteinAssociationLoader.load( new File( fileNameStringmouseURL.getFile() ), null,
                    null, getTaxonToProcess() );
        } catch ( IOException e ) {
            if ( e.getMessage().startsWith( "Error from BioMart" ) ) {
                log.warn( e.getMessage() );
                return;
            }
        }

        Collection<Gene2GeneProteinAssociation> associations = gene2GeneProteinAssociationService.loadAll();

        assertEquals( 4, associations.size() );

        for ( Gene2GeneProteinAssociation association : associations ) {
            gene2GeneProteinAssociationService.thaw( association );
            Gene secondGene = association.getSecondGene();
            secondGene = this.geneService.thaw( secondGene );
            String taxonScientificName = secondGene.getTaxon().getScientificName();
            ProteinLinkOutFormatter formatter = new ProteinLinkOutFormatter();
            if ( taxonScientificName.equals( zebraFish.getScientificName() ) ) {

                if ( association.getFirstGene().getNcbiId().equals( "751652" ) ) {
                    assertEquals( "571540", secondGene.getNcbiId() );
                    byte[] array = new byte[] { 0, 0, 1, 0, 1, 1, 1 };
                    String accession = ( databaseService.find( association.getDatabaseEntry() ) ).getAccession();
                    assertTrue( "Accession was: " + accession, accession.contains( "%0D7955." ) );
                    log.info( "Accession for zebra fish " + accession );
                    Double associationScore = association.getConfidenceScore();
                    assertEquals( new Double( 180 ), associationScore );
                    assertArrayEquals( array, association.getEvidenceVector() );
                    counterAssociationsSavedZebra++;
                    byte[] arrayBytes = association.getEvidenceVector();

                    String formatedEvidence = formatter.getEvidenceDisplayText( arrayBytes );
                    assertEquals( "Coocurrence:Experimental:Database:TextMining", formatedEvidence );

                } else if ( association.getFirstGene().getNcbiId().equals( "568371" ) ) {
                    assertEquals( "751652", secondGene.getNcbiId() );
                    byte[] array = new byte[] { 1, 1, 1, 1, 1, 1, 1 };
                    String accession = ( databaseService.find( association.getDatabaseEntry() ) ).getAccession();
                    assertTrue( accession.contains( "%0D7955." ) );
                    log.info( "Accession for zebra fish " + accession );
                    Double associationScore = association.getConfidenceScore();
                    assertEquals( new Double( 200 ), associationScore );
                    assertArrayEquals( array, association.getEvidenceVector() );

                    byte[] arrayBytes = association.getEvidenceVector();
                    String formatedEvidence = formatter.getEvidenceDisplayText( arrayBytes );
                    assertEquals( "Neighbourhood:GeneFusion:Coocurrence:Coexpression:Experimental:Database:TextMining",
                            formatedEvidence );
                    counterAssociationsSavedZebra++;
                } else {
                    fail( "Did not find correct association" );
                }

            }// rat
            else if ( taxonScientificName.equals( rat.getScientificName() ) ) {
                // should be same accession number both entries as these two map to one ensembl
                String accession = association.getDatabaseEntry().getAccession();
                assertTrue( accession.contains( "%0D10116.ENSRN" ) );
                log.info( "Accession for rat  " + accession );
                counterAssociationsSavedRat++;
            } else {
                fail();
            }

        }
        assertEquals( 2, counterAssociationsSavedRat );
        assertEquals( 2, counterAssociationsSavedZebra );
        // delete the newly entered records

    }

    /**
     * Test given a local string file and remote biomart file for one taxon can be processed
     */
    @Test
    public void testDoLoadRemoteBiomartFileLocalStringFileOneTaxon() throws Exception {

        // test pass in files so do not trigger fetcher
        // but make sure they get
        String fileNameStringZebraFish = "/data/loader/protein/string/protein.links.zebrafish.txt";
        URL fileNameStringZebraFishURL = this.getClass().getResource( fileNameStringZebraFish );
        Collection<Taxon> taxaZebraFish = new ArrayList<Taxon>();
        taxaZebraFish.add( zebraFish );

        Collection<Gene2GeneProteinAssociation> associationsBefore = gene2GeneProteinAssociationService.loadAll();
        assertEquals( 0, associationsBefore.size() );
        try {
            stringBiomartGene2GeneProteinAssociationLoader.load( new File( fileNameStringZebraFishURL.getFile() ),
                    null, null, taxaZebraFish );
        } catch ( IOException e ) {
            if ( e.getMessage().startsWith( "Error from BioMart" ) ) {
                log.warn( e.getMessage() );
                return;
            }
        }
        Collection<Gene2GeneProteinAssociation> associations = gene2GeneProteinAssociationService.loadAll();
        assertEquals( 1, associations.size() );

        this.gene2GeneProteinAssociationService.deleteAll( associations );
        associations = gene2GeneProteinAssociationService.loadAll();
        assertTrue( associations.isEmpty() );

    }

}
