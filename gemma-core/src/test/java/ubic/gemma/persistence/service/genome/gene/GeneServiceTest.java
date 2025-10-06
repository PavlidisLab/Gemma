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
package ubic.gemma.persistence.service.genome.gene;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.model.genome.gene.GeneAlias;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author jsantos
 */
public class GeneServiceTest extends BaseSpringContextTest {

    private static final String TEST_GENE_NAME = "test_genedao" + RandomStringUtils.insecure().next( 3 );

    @Autowired
    private ExternalDatabaseService edbs;

    @Autowired
    private GeneService geneDao = null;

    @After
    public void tearDown() {
        Collection<Gene> testGene = geneDao.findByOfficialSymbol( GeneServiceTest.TEST_GENE_NAME );
        for ( Gene gene : testGene ) {
            geneDao.remove( gene );
        }
    }

    @Test
    public void testFindByAccessionNcbi() {

        Gene gene = Gene.Factory.newInstance();
        Integer id = Integer.parseInt( RandomStringUtils.insecure().nextNumeric( 5 ) );
        gene.setNcbiGeneId( id );
        gene.setName( GeneServiceTest.TEST_GENE_NAME );

        Taxon human = taxonService.findByCommonName( "human" );
        gene.setTaxon( human );

        geneDao.create( gene );

        Gene g = geneDao.findByAccession( id.toString(), null );
        assertNotNull( g );
        assertEquals( g, gene );
        geneDao.remove( gene );
    }

    @Test
    public void testFindByAccessionNcbiWithSource() {

        Gene gene = Gene.Factory.newInstance();

        Integer id = Integer.parseInt( RandomStringUtils.insecure().nextNumeric( 5 ) );
        gene.setNcbiGeneId( id );
        gene.setName( GeneServiceTest.TEST_GENE_NAME );
        ExternalDatabase ncbi = edbs.findByName( "Entrez Gene" );
        DatabaseEntry dbe = DatabaseEntry.Factory.newInstance();
        dbe.setAccession( "12345" ); // this gets ignored, because the ncbi id is part of the object.
        dbe.setExternalDatabase( ncbi );
        gene.getAccessions().add( dbe );

        Taxon human = taxonService.findByCommonName( "human" );
        gene.setTaxon( human );

        geneDao.create( gene );

        Gene g = geneDao.findByAccession( "12345", ncbi );
        assertNotNull( g );
        assertEquals( g, gene );
        geneDao.remove( gene );
    }

    @Test
    public void testFindByAccessionOther() {

        Gene gene = Gene.Factory.newInstance();
        Integer id = Integer.parseInt( RandomStringUtils.insecure().nextNumeric( 5 ) );
        gene.setNcbiGeneId( id );
        gene.setName( GeneServiceTest.TEST_GENE_NAME );
        ExternalDatabase ensembl = edbs.findByName( "Ensembl" );
        DatabaseEntry dbe = DatabaseEntry.Factory.newInstance();
        dbe.setAccession( "E129458" );
        dbe.setExternalDatabase( ensembl );
        gene.getAccessions().add( dbe );

        Taxon human = taxonService.findByCommonName( "human" );
        gene.setTaxon( human );

        geneDao.create( gene );

        Gene g = geneDao.findByAccession( "E129458", ensembl );
        assertNotNull( g );
        assertEquals( g, gene );
        geneDao.remove( gene );
    }

    @Test
    public void testFindByNcbiId() {

        Gene gene = Gene.Factory.newInstance();
        Integer id = Integer.parseInt( RandomStringUtils.insecure().nextNumeric( 5 ) );

        gene.setNcbiGeneId( id );
        gene.setName( GeneServiceTest.TEST_GENE_NAME );

        Taxon human = taxonService.findByCommonName( "human" );
        gene.setTaxon( human );

        geneDao.create( gene );

        Gene ga = geneDao.findByNCBIId( id );
        assertEquals( gene, ga );
        geneDao.remove( gene );
    }

    @Test
    public void testFindEvenThoughHaveSameSymbol() {
        Gene gene = Gene.Factory.newInstance();

        Integer id = Integer.parseInt( RandomStringUtils.insecure().nextNumeric( 5 ) );
        gene.setNcbiGeneId( id );
        gene.setName( GeneServiceTest.TEST_GENE_NAME );
        gene.setOfficialName( GeneServiceTest.TEST_GENE_NAME );
        gene.setOfficialSymbol( GeneServiceTest.TEST_GENE_NAME );

        Taxon human = taxonService.findByCommonName( "human" );
        gene.setTaxon( human );
        PhysicalLocation pl1 = PhysicalLocation.Factory.newInstance();
        Chromosome chromosome = new Chromosome( "X", null, this.getTestPersistentBioSequence(), human );
        chromosome = ( Chromosome ) persisterHelper.persist( chromosome );
        pl1.setChromosome( chromosome );
        pl1.setNucleotide( 10000010L );
        pl1.setNucleotideLength( 1001 );
        pl1.setStrand( "-" );
        gene.setPhysicalLocation( pl1 );

        gene = geneDao.create( gene );
        Long idWeWant = gene.getId();

        Gene gene2 = Gene.Factory.newInstance();

        gene2.setNcbiGeneId( null );
        gene2.setName( GeneServiceTest.TEST_GENE_NAME );
        gene2.setOfficialName( GeneServiceTest.TEST_GENE_NAME );
        gene2.setOfficialSymbol( GeneServiceTest.TEST_GENE_NAME );

        gene2.setTaxon( human );
        PhysicalLocation pl2 = PhysicalLocation.Factory.newInstance();
        Chromosome chromosome2 = new Chromosome( "Y", null, this.getTestPersistentBioSequence(), human );
        chromosome2 = ( Chromosome ) persisterHelper.persist( chromosome2 );
        pl2.setChromosome( chromosome2 );
        pl2.setChromosome( chromosome );
        pl2.setNucleotide( 10000010L );
        pl2.setNucleotideLength( 1001 );
        pl2.setStrand( "-" );
        gene2.setPhysicalLocation( pl2 );

        gene2 = geneDao.create( gene2 );

        gene.setId( null );
        Gene g = geneDao.find( gene );
        assertNotNull( g );
        assertEquals( idWeWant, g.getId() );

        geneDao.remove( g );
        geneDao.remove( gene2 );
    }

    @Test
    public void testGetByGeneAlias() {
        Gene gene = Gene.Factory.newInstance();
        Integer id = Integer.parseInt( RandomStringUtils.insecure().nextNumeric( 5 ) );

        gene.setNcbiGeneId( id );
        gene.setName( GeneServiceTest.TEST_GENE_NAME );

        Set<GeneAlias> aliases = new HashSet<>();
        GeneAlias alias = GeneAlias.Factory.newInstance();
        alias.setId( ( long ) 1 );
        alias.setAlias( "GRIN1" );
        aliases.add( alias );

        gene.setAliases( aliases );
        Collection<Gene> genes = geneDao.findByAlias( "GRIN1" );
        assertNotNull( genes );
    }

    @Test
    public void testGetMicroRnaByTaxon() {

        Gene gene = Gene.Factory.newInstance();
        Integer id = Integer.parseInt( RandomStringUtils.insecure().nextNumeric( 5 ) );

        gene.setNcbiGeneId( id );
        gene.setName( GeneServiceTest.TEST_GENE_NAME );

        // either one of these should work now.
        // gene.setDescription( "Imported from Golden Path: micro RNA or sno RNA" );
        gene.setDescription( "miRNA" );
        Taxon human = taxonService.findByCommonName( "human" );
        gene.setTaxon( human );

        geneDao.create( gene );

        Collection<Gene> genes = geneDao.loadMicroRNAs( human );
        assertNotNull( genes );
        assertTrue( genes.contains( gene ) );
        geneDao.remove( gene );

    }

    @Test
    public void testLoadGenes() {

        Taxon human = taxonService.findByCommonName( "human" );

        Gene gene = Gene.Factory.newInstance();
        Integer id = Integer.parseInt( RandomStringUtils.insecure().nextNumeric( 5 ) );

        gene.setNcbiGeneId( id );
        gene.setName( "Ma_Gene" );
        gene.setDescription( "Lost in space" );
        gene.setTaxon( human );
        geneDao.create( gene );

        Collection<Gene> genes = geneDao.loadAll( human );
        assertNotNull( genes );
        assertTrue( genes.contains( gene ) );
        geneDao.remove( gene );

    }

}
