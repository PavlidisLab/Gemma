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
package ubic.gemma.model.genome.gene;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.analysis.expression.coexpression.CoexpressionCollectionValueObject;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabaseService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.GeneDao;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author jsantos
 * @version $Id$
 */
public class GeneServiceTest extends BaseSpringContextTest {

    @Autowired
    private GeneService geneDao = null;

    @Test
    public void testFindEvenThoughHaveSameSymbol() throws Exception {
        Gene gene = Gene.Factory.newInstance();

        Integer id = Integer.parseInt( RandomStringUtils.randomNumeric( 5 ) );
        gene.setNcbiGeneId( id );
        gene.setName( "test_genedao" );
        gene.setOfficialName( "test_genedao" );
        gene.setOfficialSymbol( "test_genedao" );

        Taxon human = taxonService.findByCommonName( "human" );
        gene.setTaxon( human );
        PhysicalLocation pl1 = PhysicalLocation.Factory.newInstance();
        Chromosome chromosome = Chromosome.Factory.newInstance( "X", human );
        chromosome.setSequence( getTestPersistentBioSequence() );
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
        gene2.setName( "test_genedao" );
        gene2.setOfficialName( "test_genedao" );
        gene2.setOfficialSymbol( "test_genedao" );

        gene2.setTaxon( human );
        PhysicalLocation pl2 = PhysicalLocation.Factory.newInstance();
        Chromosome chromosome2 = Chromosome.Factory.newInstance( "Y", human );
        chromosome2.setSequence( getTestPersistentBioSequence() );
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
    public void testFindByAccessionNcbi() {

        Gene gene = Gene.Factory.newInstance();
        gene.setId( ( long ) 1 );
        Integer id = Integer.parseInt( RandomStringUtils.randomNumeric( 5 ) );
        gene.setNcbiGeneId( id );
        gene.setName( "test_genedao" );

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
        ExternalDatabaseService edbs = ( ExternalDatabaseService ) this.getBean( "externalDatabaseService" );

        Gene gene = Gene.Factory.newInstance();

        Integer id = Integer.parseInt( RandomStringUtils.randomNumeric( 5 ) );
        gene.setNcbiGeneId( id );
        gene.setName( "test_genedao" );
        ExternalDatabase ncbi = edbs.find( "Entrez Gene" );
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
        ExternalDatabaseService edbs = ( ExternalDatabaseService ) this.getBean( "externalDatabaseService" );

        Gene gene = Gene.Factory.newInstance();
        Integer id = Integer.parseInt( RandomStringUtils.randomNumeric( 5 ) );
        gene.setNcbiGeneId( id );
        gene.setName( "test_genedao" );
        ExternalDatabase ensembl = edbs.find( "Ensembl" );
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
        Integer id = Integer.parseInt( RandomStringUtils.randomNumeric( 5 ) );

        gene.setNcbiGeneId( id );
        gene.setName( "test_genedao" );

        Taxon human = taxonService.findByCommonName( "human" );
        gene.setTaxon( human );

        geneDao.create( gene );

        Gene ga = geneDao.findByNCBIId( id );
        assertEquals( gene, ga );
        geneDao.remove( gene );
    }

    @Test
    public void testGetByGeneAlias() {
        Gene gene = Gene.Factory.newInstance();
        Integer id = Integer.parseInt( RandomStringUtils.randomNumeric( 5 ) );

        gene.setNcbiGeneId( id );
        gene.setName( "test_genedao" );

        Collection<GeneAlias> aliases = new ArrayList<GeneAlias>();
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
        Integer id = Integer.parseInt( RandomStringUtils.randomNumeric( 5 ) );

        gene.setNcbiGeneId( id );
        gene.setName( "test_genedao" );

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
        Integer id = Integer.parseInt( RandomStringUtils.randomNumeric( 5 ) );

        gene.setNcbiGeneId( id );
        gene.setName( "Ma_Gene" );
        gene.setDescription( "Lost in space" );
        gene.setTaxon( human );
        geneDao.create( gene );

        Collection<Gene> genes = geneDao.loadKnownGenes( human );
        assertNotNull( genes );
        assertTrue( genes.contains( gene ) );
        geneDao.remove( gene );

    }

}
