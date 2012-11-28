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
package ubic.gemma.loader.genome.gene.ncbi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.basecode.util.FileTools;
import ubic.gemma.genome.gene.service.GeneService;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author pavlidis
 * @version $Id$
 */
public class NCBIGeneLoadingTest extends BaseSpringContextTest {

    @Autowired
    GeneService geneService;

    Gene g = null;

    @Before
    public void setup() {
        clean();
    }

    @After
    public void teardown() {
        if ( g != null ) {
            try {
                g = geneService.load( g.getId() );
                geneService.remove( g );
            } catch ( Exception e ) {

            }
        }
        try {
            Collection<Gene> allGenes = geneService.loadAll();
            for ( Gene gene : allGenes ) {
                geneService.remove( gene );
            }
        } catch ( Exception e ) {

        }

    }

    /**
     * @throws Exception
     */
    @Test
    public void testGeneLoader() throws Exception {
        NcbiGeneLoader loader = new NcbiGeneLoader( persisterHelper );
        loader.setTaxonService( taxonService );

        String geneInfoTestFile = "/data/loader/genome/gene/gene_info.human.sample";
        String gene2AccTestFile = "/data/loader/genome/gene/gene2accession.human.sample";
        String geneHistoryFile = "/data/loader/genome/gene/gene_history.human.sample";

        // threaded load

        Taxon ta = taxonService.findByCommonName( "human" );

        assertNotNull( ta );

        loader.load( FileTools.resourceToPath( geneInfoTestFile ), FileTools.resourceToPath( gene2AccTestFile ),
                FileTools.resourceToPath( geneHistoryFile ), null, ta );

        // wait until the loader is done.
        while ( !loader.isLoaderDone() ) {
            Thread.sleep( 100 );
        }

        // loader is done.
        // check if it loaded elements to the database
        log.debug( "Loader done with number of elements: " + loader.getLoadedGeneCount() );
        assertEquals( 51, loader.getLoadedGeneCount() );

        // grab one gene and check its information
        // (depends on information in gene_info and gene2accession file
        // gene_info
        Collection<Gene> geneCollection = geneService.findByOfficialSymbol( "A2M" );

        assertEquals( 1, geneCollection.size() );

        g = geneCollection.iterator().next();
        g = geneService.thaw( g );

        Collection<GeneProduct> products = g.getProducts();
        Collection<String> expectedAccessions = new ArrayList<String>();
        Collection<String> hasAccessions = new ArrayList<String>();
        expectedAccessions.add( "AB209614.2" );
        expectedAccessions.add( "AK307832.1" );

        for ( GeneProduct product : products ) {
            Collection<DatabaseEntry> accessions = product.getAccessions();
            for ( DatabaseEntry de : accessions ) {
                String accession = de.getAccession();
                String accVersion = de.getAccessionVersion();
                hasAccessions.add( accession + "." + accVersion );
                log.debug( accession + "." + accVersion );
            }
        }
        assertEquals( 12, hasAccessions.size() );
        assertTrue( hasAccessions.containsAll( expectedAccessions ) );
        Taxon t = g.getTaxon();
        assertEquals( 9606, t.getNcbiId().intValue() );
        assertEquals( new Integer( 2 ), g.getNcbiGeneId() );

        /*
         * Test history change. One gene has been updated, from 7003 to 44444 (fake), and mimic adding ensembl
         */

        geneInfoTestFile = "/data/loader/genome/gene/gene_info.human.changed.sample";
        gene2AccTestFile = "/data/loader/genome/gene/gene2accession.human.changed.sample";
        String updatedHistory = "/data/loader/genome/gene/gene_history.human.changed.sample";
        String geneEnsemblFile = "/data/loader/genome/gene/gene2ensembl.human.sample";

        loader.load( FileTools.resourceToPath( geneInfoTestFile ), FileTools.resourceToPath( gene2AccTestFile ),
                FileTools.resourceToPath( updatedHistory ), FileTools.resourceToPath( geneEnsemblFile ), ta );
        // wait until the loader is done.
        while ( !loader.isLoaderDone() ) {
            Thread.sleep( 100 );
        }
        Collection<Gene> updatedTestGene = geneService.findByOfficialSymbol( "TEAD1" );
        assertEquals( 1, updatedTestGene.size() );
        g = updatedTestGene.iterator().next();
        assertEquals( "7003", g.getPreviousNcbiId() );
        assertEquals( new Integer( 44444 ), g.getNcbiGeneId() );

        g = geneService.findByNCBIId( 1 );
        assertEquals( "ENSG00000121410", g.getEnsemblId() );

    }

    private void clean() {
        if ( g != null ) {
            try {
                g = geneService.load( g.getId() );
                geneService.remove( g );
            } catch ( Exception e ) {
                // ignore
            }
        }
        try {
            Collection<Gene> allGenes = geneService.loadAll();
            for ( Gene gene : allGenes ) {
                geneService.remove( gene );
            }
        } catch ( Exception e ) {
            // ignore
        }
    }
}
