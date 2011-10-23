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

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.analysis.expression.coexpression.CoexpressionCollectionValueObject;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabaseService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.GeneDao;
import ubic.gemma.model.genome.PredictedGene;
import ubic.gemma.model.genome.ProbeAlignedRegion;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author jsantos
 * @version $Id$
 */
public class GeneDaoTest extends BaseSpringContextTest {

    @Autowired
    private GeneDao geneDao = null;

    @Test
    public void testFindByAccessionNcbi() {

        Gene gene = Gene.Factory.newInstance();
        gene.setId( ( long ) 1 );
        gene.setNcbiId( "12345" );
        gene.setName( "test_genedao" );

        Taxon human = taxonService.findByCommonName( "human" );
        gene.setTaxon( human );

        geneDao.create( gene );

        Gene g = geneDao.findByAccession( "12345", null );
        assertNotNull( g );
        assertEquals( g, gene );
        geneDao.remove( gene );
    }

    @Test
    public void testFindByAccessionNcbiWithSource() {
        ExternalDatabaseService edbs = ( ExternalDatabaseService ) this.getBean( "externalDatabaseService" );

        Gene gene = Gene.Factory.newInstance();
        gene.setId( ( long ) 1 );
        gene.setNcbiId( "12345" );
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
        gene.setId( ( long ) 1 );
        gene.setNcbiId( "12345" );
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
        gene.setId( ( long ) 1 );
        gene.setNcbiId( "12345" );
        gene.setName( "test_genedao" );

        Taxon human = taxonService.findByCommonName( "human" );
        gene.setTaxon( human );

        geneDao.create( gene );

        Collection<Gene> genes = geneDao.findByNcbiId( "12345" );
        assertNotNull( genes );
        assertTrue( genes.contains( gene ) );
        geneDao.remove( gene );
    }

    @Test
    public void testGetByGeneAlias() {
        Gene gene = Gene.Factory.newInstance();
        gene.setId( ( long ) 1 );
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
    public void testGetCoexpressedGenes() {
        Gene gene = Gene.Factory.newInstance();
        gene.setId( ( long ) 1 );
        gene.setName( "test_genedao" );
        Taxon taxon = Taxon.Factory.newInstance();
        taxon.setCommonName( "human" );
        taxon.setIsSpecies( true );
        taxon.setIsGenesUsable( true );
        gene.setTaxon( taxon );
        Collection<ExpressionExperiment> ees = new ArrayList<ExpressionExperiment>();
        CoexpressionCollectionValueObject genes = geneDao.getCoexpressedGenes( gene, ees, 1 );
        assertNotNull( genes );
    }

    @Test
    public void testGetCompositeSequenceCountById() {
        Gene gene = Gene.Factory.newInstance();
        gene.setId( ( long ) 1 );
        gene.setName( "test_genedao" );
        long num = geneDao.getCompositeSequenceCountById( 1 );
        assertNotNull( num );
    }

    @Test
    public void testGetCompositeSequencesById() {
        Gene gene = Gene.Factory.newInstance();
        gene.setId( ( long ) 1 );
        gene.setName( "test_genedao" );
        Collection<CompositeSequence> cs = geneDao.getCompositeSequencesById( 1 );
        assertNotNull( cs );
    }

    @Test
    public void testGetMicroRnaByTaxon() {

        Gene gene = Gene.Factory.newInstance();
        gene.setId( ( long ) 1 );
        gene.setName( "test_genedao" );

        // either one of these should work now.
        // gene.setDescription( "Imported from Golden Path: micro RNA or sno RNA" );
        gene.setDescription( "miRNA" );
        Taxon human = taxonService.findByCommonName( "human" );
        gene.setTaxon( human );

        geneDao.create( gene );

        Collection<Gene> genes = geneDao.getMicroRnaByTaxon( human );
        assertNotNull( genes );
        assertTrue( genes.contains( gene ) );
        geneDao.remove( gene );

    }

    @Test
    public void testLoadGenes() {

        Taxon human = taxonService.findByCommonName( "human" );

        Gene gene = Gene.Factory.newInstance();
        gene.setId( ( long ) 1 );
        gene.setName( "Ma_Gene" );
        gene.setDescription( "Lost in space" );
        gene.setTaxon( human );
        geneDao.create( gene );

        Collection<Gene> genes = geneDao.loadKnownGenes( human );
        assertNotNull( genes );
        assertTrue( genes.contains( gene ) );
        geneDao.remove( gene );

    }

    @Test
    public void testLoadPredictedGenes() {

        Taxon human = taxonService.findByCommonName( "human" );

        Gene gene = PredictedGene.Factory.newInstance();
        gene.setId( ( long ) 1 );
        gene.setName( "Ma_predictedGene" );
        gene.setDescription( "Lost in space" );
        gene.setTaxon( human );
        geneDao.create( gene );

        Collection<PredictedGene> genes = geneDao.loadPredictedGenes( human );
        assertNotNull( genes );
        assertTrue( genes.contains( gene ) );
        geneDao.remove( gene );

    }

    @Test
    public void testLoadProbeAlignedRegions() {

        Taxon human = taxonService.findByCommonName( "human" );

        Gene gene = ProbeAlignedRegion.Factory.newInstance();
        gene.setId( ( long ) 1 );
        gene.setName( "Ma_pal" );
        gene.setDescription( "Lost in space" );
        gene.setTaxon( human );
        geneDao.create( gene );

        Collection<ProbeAlignedRegion> genes = geneDao.loadProbeAlignedRegions( human );
        assertNotNull( genes );
        assertTrue( genes.contains( gene ) );
        geneDao.remove( gene );

    }

}
