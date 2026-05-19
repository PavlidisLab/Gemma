/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
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
 */
package ubic.gemma.core.util.test.fixture;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.test.BaseIntegrationTest;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.persistence.service.genome.gene.GeneService;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the Phase 3 typed {@link GeneFactory}.
 *
 * @see GeneFactory
 */
public class GeneFactoryTest extends BaseIntegrationTest {

    @Autowired
    private GeneFactory geneFactory;

    @Autowired
    private TaxonFactory taxonFactory;

    @Autowired
    private GeneService geneService;

    private final List<Gene> createdGenes = new ArrayList<>();

    @After
    public void cleanUp() {
        for ( Gene g : createdGenes ) {
            try {
                Gene fresh = geneService.load( g.getId() );
                if ( fresh != null ) {
                    geneService.remove( fresh );
                }
            } catch ( Exception ignored ) {
                // best-effort
            }
        }
    }

    @Test
    public void defaults_returnPersistentMouseGene_withSyntheticNcbiId() {
        Gene g = geneFactory.builder().build();
        createdGenes.add( g );

        assertNotNull( "gene must be persisted (id assigned)", g.getId() );
        assertNotNull( "default gene should have a name", g.getName() );
        assertNotNull( "default gene should have an official symbol", g.getOfficialSymbol() );
        assertNotNull( "default gene should have an NCBI id", g.getNcbiGeneId() );
        assertTrue( "synthetic NCBI ids must be >= 500_000",
                g.getNcbiGeneId() >= 500_000 );
        assertNotNull( "default gene should be on a taxon", g.getTaxon() );
        assertEquals( "default taxon is mouse", "mouse", g.getTaxon().getCommonName() );
        assertTrue( "default gene has no products", g.getProducts().isEmpty() );
    }

    @Test
    public void withTaxon_appliesOverride() {
        Taxon human = taxonFactory.human();
        Gene g = geneFactory.builder().withTaxon( human ).build();
        createdGenes.add( g );

        assertEquals( "gene taxon should be the supplied human",
                human.getId(), g.getTaxon().getId() );
    }

    @Test
    public void withOfficialSymbolAndName_appliesOverride() {
        Gene g = geneFactory.builder()
                .withOfficialSymbol( "TESTSYM" )
                .withOfficialName( "test official name" )
                .withName( "test_gene_explicit" )
                .build();
        createdGenes.add( g );

        assertEquals( "TESTSYM", g.getOfficialSymbol() );
        assertEquals( "test official name", g.getOfficialName() );
        assertEquals( "test_gene_explicit", g.getName() );
    }

    @Test
    public void withNcbiId_appliesOverride() {
        int ncbi = 987_654;
        Gene g = geneFactory.builder().withNcbiId( ncbi ).build();
        createdGenes.add( g );

        assertEquals( Integer.valueOf( ncbi ), g.getNcbiGeneId() );
    }

    @Test
    public void withGeneProducts_attachesAndPersistsThem() {
        Gene g = geneFactory.builder().withGeneProducts( 3 ).build();
        createdGenes.add( g );

        assertEquals( "should have exactly 3 gene products", 3, g.getProducts().size() );
        for ( GeneProduct gp : g.getProducts() ) {
            assertNotNull( "each gene product should be persisted (cascade from Gene)",
                    gp.getId() );
            assertNotNull( gp.getName() );
            assertNotNull( gp.getNcbiGi() );
            assertEquals( "each product should point back at the gene",
                    g.getId(), gp.getGene().getId() );
        }
    }

    @Test
    public void withGeneProductsZero_isLegalAndProducesEmptyProducts() {
        Gene g = geneFactory.builder().withGeneProducts( 0 ).build();
        createdGenes.add( g );

        assertNotNull( g.getId() );
        assertTrue( "withGeneProducts(0) should produce empty products",
                g.getProducts().isEmpty() );
    }

    @Test
    public void reloadByNcbiId_findsTheSameGene() {
        int ncbi = 750_001;
        Gene g = geneFactory.builder().withNcbiId( ncbi ).build();
        createdGenes.add( g );

        Gene found = geneService.findByNCBIId( ncbi );
        assertNotNull( "freshly-created gene should be findable by NCBI id", found );
        assertEquals( g.getId(), found.getId() );
    }
}
