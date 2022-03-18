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
package ubic.gemma.model.genome.gene;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ubic.gemma.core.genome.gene.GeneSetValueObjectHelper;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.core.genome.gene.service.GeneServiceImpl;
import ubic.gemma.core.loader.genome.gene.ncbi.homology.HomologeneService;
import ubic.gemma.core.ontology.providers.GeneOntologyService;
import ubic.gemma.core.search.GeneSetSearch;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.association.Gene2GOAssociationService;
import ubic.gemma.persistence.service.association.coexpression.CoexpressionService;
import ubic.gemma.persistence.service.genome.GeneDao;
import ubic.gemma.persistence.service.genome.sequenceAnalysis.AnnotationAssociationService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.*;

/**
 * @author daq2101
 */
@SuppressWarnings({ "MismatchedQueryAndUpdateOfCollection", "FieldCanBeLocal" }) // In a test it makes sense
@ContextConfiguration
public class GeneServiceImplTest extends AbstractJUnit4SpringContextTests {

    private static final String STRAND = "+";

    @Configuration
    public static class GeneServiceImplTestContextConfiguration {

        @Bean
        public GeneDao geneDao() {
            return mock( GeneDao.class );
        }

        @Bean
        public AnnotationAssociationService annotationAssociationService() {
            return mock( AnnotationAssociationService.class );
        }

        @Bean
        public CoexpressionService coexpressionService() {
            return mock( CoexpressionService.class );
        }

        @Bean
        public Gene2GOAssociationService gene2GOAssociationService() {
            return mock( Gene2GOAssociationService.class );
        }

        @Bean
        public GeneOntologyService geneOntologyService() {
            return mock( GeneOntologyService.class );
        }

        @Bean
        public GeneSetSearch geneSetSearch() {
            return mock( GeneSetSearch.class );
        }

        @Bean
        public GeneSetValueObjectHelper geneSetValueObjectHelper() {
            return mock( GeneSetValueObjectHelper.class );
        }

        @Bean
        public HomologeneService homologeneService() {
            return mock( HomologeneService.class );
        }

        @Bean
        public SearchService searchService() {
            return mock( SearchService.class );
        }

        @Bean
        public TaxonService taxonService() {
            return mock( TaxonService.class );
        }

        @Bean
        public GeneService geneService() {
            return new GeneServiceImpl( geneDao() );
        }
    }

    @Autowired
    private GeneService geneService;

    @Autowired
    private GeneDao geneDao;

    @Before
    public void setUp() throws Exception {
        geneService = new GeneServiceImpl( geneDao );

        List<Gene> allThree = new ArrayList<>();
        Set<Gene> justRab = new HashSet<>();
        Set<Gene> justRabble = new HashSet<>();

        Taxon t = Taxon.Factory.newInstance();
        t.setCommonName( "moose" );
        t.setScientificName( "moose" );
        t.setIsGenesUsable( true );

        Gene g = Gene.Factory.newInstance();
        g.setOfficialName( "rabble" );
        g.setOfficialSymbol( "rab" );
        allThree.add( g );
        justRab.add( g );

        Gene g2 = Gene.Factory.newInstance();
        g2.setOfficialName( "rabblebong" );
        g2.setTaxon( t );
        allThree.add( g2 );

        Gene g3 = Gene.Factory.newInstance();
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

        when( geneDao.loadAll() ).thenReturn( allThree );
        when( geneDao.findByAccession( "12345", null ) ).thenReturn( g3 );
        when( geneDao.findByNcbiId( 12345 ) ).thenReturn( g3 );
        when( geneDao.findByOfficialName( "rabble" ) ).thenReturn( justRab );
        when( geneDao.findByOfficialSymbol( "rabble" ) ).thenReturn( justRab );
        when( geneDao.findByOfficialSymbolInexact( "ra%" ) ).thenReturn( allThree );
    }

    @After
    public void tearDown() {
        reset( geneDao );
    }

    @SuppressWarnings("Duplicates") // Not effective to extract
    @Test
    public void testFindAll() {
        reset( geneDao );
        geneService.loadAll();
        verify( geneDao ).loadAll();
    }

    @Test
    public void testFindByAccessionNoSource() {
        geneService.findByAccession( "12345", null );
        verify( geneDao ).findByAccession( "12345", null );
    }

    @Test
    public void testFindByNcbiId() {
        geneService.findByNCBIId( 12345 );
        verify( geneDao ).findByNcbiId( 12345 );
    }

    @Test
    public void testFindByOfficialName() {
        geneService.findByOfficialName( "rabble" );
        verify( geneDao ).findByOfficialName( "rabble" );
    }

    @Test
    public void testFindByOfficialSymbol() {
        geneService.findByOfficialSymbol( "rabble" );
        verify( geneDao ).findByOfficialSymbol( "rabble" );
    }

    @Test
    public void testFindByOfficialSymbolInexact() {
        geneService.findByOfficialSymbolInexact( "ra%" );
        verify( geneDao ).findByOfficialSymbolInexact( "ra%" );
    }

}
