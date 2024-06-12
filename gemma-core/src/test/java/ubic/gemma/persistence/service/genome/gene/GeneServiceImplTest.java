/*
 * The Gemma project.
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

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.genome.GeneDao;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.*;

/**
 * @author daq2101
 */
@SuppressWarnings({ "MismatchedQueryAndUpdateOfCollection", "FieldCanBeLocal" }) // In a test it makes sense
public class GeneServiceImplTest {

    private static final String STRAND = "+";
    private final Collection<Gene> allThree = new HashSet<>();
    private final Collection<Gene> justRab = new HashSet<>();
    private final Collection<Gene> justRabble = new HashSet<>();
    private GeneService svc;
    private Gene g = null;
    private Gene g2 = null;
    private Gene g3 = null;
    private GeneDao geneDaoMock;

    @Before
    public void setUp() throws Exception {

        geneDaoMock = mock( GeneDao.class );
        svc = new GeneServiceImpl( geneDaoMock );

        Taxon t = Taxon.Factory.newInstance();
        t.setCommonName( "moose" );
        t.setScientificName( "moose" );
        t.setIsGenesUsable( true );
        // tDAO.create( t );

        g = Gene.Factory.newInstance();
        g.setOfficialName( "rabble" );
        g.setOfficialSymbol( "rab" );
        allThree.add( g );
        justRab.add( g );

        g2 = Gene.Factory.newInstance();
        g2.setOfficialName( "rabblebong" );
        g2.setTaxon( t );
        allThree.add( g2 );

        g3 = Gene.Factory.newInstance();
        g3.setOfficialName( "rabble" );
        g3.setNcbiGeneId( 12345 );
        g3.setOfficialSymbol( "rab3" );
        g3.setId( ( long ) 1234 );

        // For testing need to add physical locations to the gene products of a given gene.
        Chromosome chromosome = new Chromosome( "fakeChromosome", t );
        FieldUtils.writeField( chromosome, "id", 54321L, true );

        // Gene product 1 (Min=100 max=200)
        PhysicalLocation ploc1 = PhysicalLocation.Factory.newInstance();
        ploc1.setChromosome( chromosome );
        ploc1.setStrand( GeneServiceImplTest.STRAND );
        ploc1.setNucleotide( ( long ) 100 );
        ploc1.setNucleotideLength( 100 );

        GeneProduct gp1 = GeneProduct.Factory.newInstance();
        gp1.setPhysicalLocation( ploc1 );
        gp1.setGene( g3 );
        gp1.setName( "gp1" );

        // gene product 2 (min=110 max = 210)
        PhysicalLocation ploc2 = PhysicalLocation.Factory.newInstance();
        ploc2.setChromosome( chromosome );
        ploc2.setStrand( GeneServiceImplTest.STRAND );
        ploc2.setNucleotide( ( long ) 110 );
        ploc2.setNucleotideLength( 100 );

        GeneProduct gp2 = GeneProduct.Factory.newInstance();
        gp2.setPhysicalLocation( ploc2 );
        gp2.setGene( g3 );
        gp2.setName( "gp2" );

        // Gene Product 3 (min=90 max=140)
        PhysicalLocation ploc3 = PhysicalLocation.Factory.newInstance();
        ploc3.setChromosome( chromosome );
        ploc3.setStrand( GeneServiceImplTest.STRAND );
        ploc3.setNucleotide( ( long ) 90 );
        ploc3.setNucleotideLength( 50 );

        GeneProduct gp3 = GeneProduct.Factory.newInstance();
        gp3.setPhysicalLocation( ploc3 );
        gp3.setGene( g3 );
        gp3.setName( "gp3" );

        // Gene Product 4 (wrong strand should get regected, min 10 max 210)
        PhysicalLocation ploc4 = PhysicalLocation.Factory.newInstance();
        ploc4.setChromosome( chromosome );
        ploc4.setStrand( "-" );
        ploc4.setNucleotide( ( long ) 10 );
        ploc4.setNucleotideLength( 200 );

        GeneProduct gp4 = GeneProduct.Factory.newInstance();
        gp4.setPhysicalLocation( ploc4 );
        gp4.setGene( g3 );
        gp4.setName( "wrong strand gp4" );
        gp4.setId( ( long ) 3456 );

        // Gene Product 5 (right strand wrong chromosome should get regected, min 20 max 220)

        Chromosome wrongChromosome = new Chromosome( "wrongFakeChromosome", t );
        FieldUtils.writeField( chromosome, "id", 43215L, true );

        PhysicalLocation ploc5 = PhysicalLocation.Factory.newInstance();
        ploc5.setChromosome( wrongChromosome );
        ploc5.setStrand( GeneServiceImplTest.STRAND );
        ploc5.setNucleotide( ( long ) 20 );
        ploc5.setNucleotideLength( 200 );

        GeneProduct gp5 = GeneProduct.Factory.newInstance();
        gp5.setPhysicalLocation( ploc5 );
        gp5.setGene( g3 );
        gp5.setName( "wrong chromosome gp5" );
        gp5.setId( ( long ) 4567 );

        Set<GeneProduct> gps = new HashSet<>();
        gps.add( gp1 );
        gps.add( gp2 );
        gps.add( gp4 );
        gps.add( gp5 );
        gps.add( gp3 );
        g3.setProducts( gps );

        allThree.add( g3 );
        justRabble.add( g3 );

    }

    @After
    public void tearDown() {
        reset( geneDaoMock );
        justRab.clear();
        justRabble.clear();
        allThree.clear();
    }

    @SuppressWarnings("Duplicates") // Not effective to extract
    @Test
    public void testFindAll() {
        when( geneDaoMock.loadAll() ).thenReturn( allThree );
        svc.loadAll();
        verify( geneDaoMock ).loadAll();
    }

    @Test
    public void testFindByAccessionNoSource() {
        when( geneDaoMock.findByAccession( "12345", null ) ).thenReturn( g3 );
        svc.findByAccession( "12345", null );
        verify( geneDaoMock ).findByAccession( "12345", null );
    }

    @Test
    public void testFindByNcbiId() {
        when( geneDaoMock.findByNcbiId( 12345 ) ).thenReturn( g3 );
        svc.findByNCBIId( 12345 );
        verify( geneDaoMock ).findByNcbiId( 12345 );
    }

    @Test
    public void testFindByOfficialName() {
        when( geneDaoMock.findByOfficialName( "rabble" ) ).thenReturn( justRab );
        svc.findByOfficialName( "rabble" );
        verify( geneDaoMock ).findByOfficialName( "rabble" );
    }

    @Test
    public void testFindByOfficialSymbol() {
        when( geneDaoMock.findByOfficialSymbol( "rabble" ) ).thenReturn( justRab );
        svc.findByOfficialSymbol( "rabble" );
        verify( geneDaoMock ).findByOfficialSymbol( "rabble" );
    }

    @Test
    public void testFindByOfficialSymbolInexact() {
        when( geneDaoMock.findByOfficialSymbolInexact( "ra%" ) ).thenReturn( allThree );
        svc.findByOfficialSymbolInexact( "ra%" );
        verify( geneDaoMock ).findByOfficialSymbolInexact( "ra%" );
    }

}
