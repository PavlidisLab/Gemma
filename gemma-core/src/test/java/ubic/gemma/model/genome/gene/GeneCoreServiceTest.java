/*
 * The Gemma project
 * 
 * Copyright (c) 2013 University of British Columbia
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

import java.util.Collection;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.genome.gene.service.GeneCoreService;
import ubic.gemma.genome.gene.service.GeneService;
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author cmcdonald
 * @version $Id$
 */
public class GeneCoreServiceTest extends BaseSpringContextTest {

    @Autowired
    private GeneService geneDao = null;

    @Autowired
    private GeneCoreService geneCoreService = null;

    @Test
    public void testLoadGeneDetails() throws Exception {
        Gene gene = Gene.Factory.newInstance();

        Integer id = Integer.parseInt( RandomStringUtils.randomNumeric( 5 ) );
        gene.setNcbiGeneId( id );
        gene.setName( "test_genedao" );
        gene.setOfficialName( "test_genedao" );
        gene.setOfficialSymbol( "test_genedao" );

        Taxon human = taxonService.findByCommonName( "human" );
        gene.setTaxon( human );
        PhysicalLocation pl1 = PhysicalLocation.Factory.newInstance();
        Chromosome chromosome = Chromosome.Factory.newInstance( "X", null, getTestPersistentBioSequence(), human );
        chromosome = ( Chromosome ) persisterHelper.persist( chromosome );
        pl1.setChromosome( chromosome );
        pl1.setNucleotide( 10000010L );
        pl1.setNucleotideLength( 1001 );
        pl1.setStrand( "-" );
        gene.setPhysicalLocation( pl1 );

        gene = geneDao.create( gene );
        Long idWeWant = gene.getId();

        gene.setId( null );
        Gene g = geneDao.find( gene );
        assertNotNull( g );
        assertEquals( idWeWant, g.getId() );

        GeneValueObject gvo = geneCoreService.loadGeneDetails( idWeWant );

        assertEquals( gvo.getName(), g.getName() );

        geneDao.remove( g );

    }

    @Test
    public void testSearchGenes() throws Exception {
        Gene gene = Gene.Factory.newInstance();

        Integer id = Integer.parseInt( RandomStringUtils.randomNumeric( 5 ) );
        gene.setNcbiGeneId( id );
        gene.setName( "test_search" );
        gene.setOfficialName( "test_search" );
        gene.setOfficialSymbol( "test_search" );

        Taxon human = taxonService.findByCommonName( "human" );
        gene.setTaxon( human );
        PhysicalLocation pl1 = PhysicalLocation.Factory.newInstance();
        Chromosome chromosome = Chromosome.Factory.newInstance( "X", null, getTestPersistentBioSequence(), human );

        chromosome = ( Chromosome ) persisterHelper.persist( chromosome );
        pl1.setChromosome( chromosome );
        pl1.setNucleotide( 10000010L );
        pl1.setNucleotideLength( 1001 );
        pl1.setStrand( "-" );
        gene.setPhysicalLocation( pl1 );

        gene = geneDao.create( gene );

        Collection<GeneValueObject> searchResults = geneCoreService.searchGenes( "test_search", 1l );

        assertNotNull( searchResults );

        GeneValueObject gvo = searchResults.iterator().next();

        assertNotNull( gvo );

        geneDao.remove( gene );

    }

}
