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

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.testing.BaseSpringContextTest;
import ubic.gemma.util.ConfigUtils;

/**
 * @author pavlidis
 * @version $Id$
 */
public class NCBIGeneIntegrationTest extends BaseSpringContextTest {

    /**
     * @throws Exception
     */
    public void testGeneDomainObjectLoad() throws Exception {
        NcbiGeneDomainObjectGenerator sdog = new NcbiGeneDomainObjectGenerator();

        String geneInfoTestFile = "/gemma-core/src/test/resources/data/loader/genome/gene/gene_info.sample.gz";
        String gene2AccTestFile = "/gemma-core/src/test/resources/data/loader/genome/gene/gene2accession.sample.gz";
        String geneHistoryFile = "/gemma-core/src/test/resources/data/loader/genome/gene/geneHistory.sample.gz";

        String basePath = ConfigUtils.getString( "gemma.home" );
        final BlockingQueue<NcbiGeneData> queue = new ArrayBlockingQueue<NcbiGeneData>( 100 );
        sdog.generateLocal( basePath + geneInfoTestFile, basePath + gene2AccTestFile, basePath + geneHistoryFile,
                queue, false );

        // wait until the producer is done.
        while ( !sdog.isProducerDone() ) {
            Thread.sleep( 100 );
        }

        // producer is done.
        log.debug( "Producer done with number of elements: " + queue.size() );
        assertTrue( queue.size() == 99 );
    }

    /**
     * @throws Exception
     */
    public void testGeneConverter() throws Exception {
        NcbiGeneDomainObjectGenerator sdog = new NcbiGeneDomainObjectGenerator();
        NcbiGeneConverter converter = new NcbiGeneConverter();
        // set flags
        AtomicBoolean generatorDone = new AtomicBoolean( false );
        AtomicBoolean converterDone = new AtomicBoolean( false );

        sdog.setProducerDoneFlag( generatorDone );
        converter.setSourceDoneFlag( generatorDone );
        converter.setProducerDoneFlag( converterDone );

        String geneInfoTestFile = "/gemma-core/src/test/resources/data/loader/genome/gene/gene_info.sample.gz";
        String gene2AccTestFile = "/gemma-core/src/test/resources/data/loader/genome/gene/gene2accession.sample.gz";
        String geneHistoryFile = "/gemma-core/src/test/resources/data/loader/genome/gene/geneHistory.sample.gz";

        String basePath = ConfigUtils.getString( "gemma.home" );
        final BlockingQueue<NcbiGeneData> queue = new ArrayBlockingQueue<NcbiGeneData>( 100 );
        final BlockingQueue<Gene> geneQueue = new ArrayBlockingQueue<Gene>( 100 );

        sdog.generateLocal( basePath + geneInfoTestFile, basePath + gene2AccTestFile, basePath + geneHistoryFile,
                queue, false );

        converter.convert( queue, geneQueue );

        // wait until the producer is done.
        while ( !converter.isProducerDone() ) {
            Thread.sleep( 100 );
        }

        // producer is done.
        log.debug( "Converter done with number of elements: " + geneQueue.size() );
        assertTrue( geneQueue.size() == 99 );
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.test.AbstractTransactionalSpringContextTests#onSetUp()
     */
    @Override
    protected void onSetUp() throws Exception {
        super.onSetUp();
        this.endTransaction();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.test.AbstractTransactionalSpringContextTests#onTearDown()
     */
    @Override
    protected void onTearDown() throws Exception {
        super.onTearDown();
        GeneService geneService = ( GeneService ) getBean( "geneService" );
        geneService.remove( geneService.loadAll() );
    }

    /**
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public void testGeneLoader() throws Exception {
        GeneService geneService = ( GeneService ) getBean( "geneService" );
        NcbiGeneLoader loader = new NcbiGeneLoader( persisterHelper );

        String geneInfoTestFile = "/gemma-core/src/test/resources/data/loader/genome/gene/gene_info.sample.gz";
        String gene2AccTestFile = "/gemma-core/src/test/resources/data/loader/genome/gene/gene2accession.sample.gz";
        String geneHistoryFile = "/gemma-core/src/test/resources/data/loader/genome/gene/geneHistory.sample.gz";

        // threaded load
        String basePath = ConfigUtils.getString( "gemma.home" );
        loader.load( basePath + geneInfoTestFile, basePath + gene2AccTestFile, basePath + geneHistoryFile, false );

        // wait until the loader is done.
        while ( !loader.isLoaderDone() ) {
            Thread.sleep( 100 );
        }

        // loader is done.
        // check if it loaded 100 elements to the database
        log.debug( "Loader done with number of elements: " + loader.getLoadedGeneCount() );
        assertEquals( 99, loader.getLoadedGeneCount() );

        // grab one gene and check its information
        // (depends on information in gene_info and gene2accession file
        // gene_info
        Collection<Gene> geneCollection = geneService.findByOfficialName( "orf31" );
        Gene g = geneCollection.iterator().next();
        Collection<GeneProduct> products = g.getProducts();
        Collection<String> expectedAccessions = new ArrayList<String>();
        Collection<String> hasAccessions = new ArrayList<String>();
        expectedAccessions.add( "AAF29803.1" );
        expectedAccessions.add( "NP_862654.1" );
        geneService.thaw( g );
        for ( GeneProduct product : products ) {
            Collection<DatabaseEntry> accessions = product.getAccessions();
            for ( DatabaseEntry de : accessions ) {
                String accession = de.getAccession();
                String accVersion = de.getAccessionVersion();
                hasAccessions.add( accession + "." + accVersion );
                log.debug( accession + "." + accVersion );
            }
        }
        assertEquals( 2, hasAccessions.size() );
        assertTrue( hasAccessions.containsAll( expectedAccessions ) );
        Taxon t = g.getTaxon();
        assertEquals( 139, t.getNcbiId().intValue() );
        assertEquals( "1343074", g.getNcbiId() );

        /*
         * Test history change. One gene has been updated.
         */
        geneInfoTestFile = "/gemma-core/src/test/resources/data/loader/genome/gene/gene_info.sample.changeTest.gz";
        gene2AccTestFile = "/gemma-core/src/test/resources/data/loader/genome/gene/gene2accession.sample.changeTest.gz";
        String updatedHistory = "/gemma-core/src/test/resources/data/loader/genome/gene/geneHistory.changeTest.txt.gz";
        loader.load( basePath + geneInfoTestFile, basePath + gene2AccTestFile, basePath + updatedHistory, false );
        // wait until the loader is done.
        while ( !loader.isLoaderDone() ) {
            Thread.sleep( 100 );
        }
        Collection<Gene> updatedTestGene = geneService.findByOfficialName( "orf31" );
        assertEquals( 1, updatedTestGene.size() );
        g = updatedTestGene.iterator().next();
        assertEquals( "1343074", g.getPreviousNcbiId() );
        assertEquals( "9343074", g.getNcbiId() );
    }
}
