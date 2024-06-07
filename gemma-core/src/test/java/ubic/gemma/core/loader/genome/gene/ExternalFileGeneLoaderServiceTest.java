/*
 * The Gemma project
 * 
 * Copyright (c) 2012 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package ubic.gemma.core.loader.genome.gene;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.basecode.util.FileTools;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.persistence.service.genome.gene.GeneProductService;

import java.io.IOException;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Test that Gemma can load genes from an external gene file with format : #GeneSymbol GeneName Uniprot ZYX ZYXIN Q15942
 * ZXDC ZXD FAMILY ZINC FINGER C Q8C8V1
 *
 * @author ldonnison
 */
public class ExternalFileGeneLoaderServiceTest extends BaseSpringContextTest {

    private static final String TAXON_NAME = "human";

    @Autowired
    private GeneService geneService;

    @Autowired
    private ExternalFileGeneLoaderService externalFileGeneLoaderService = null;

    private String geneFile = null;

    @Autowired
    private GeneProductService geneProductService;

    @Before
    public void setUp() throws Exception {
        geneFile = FileTools.resourceToPath( "/data/loader/genome/gene/externalGeneFileLoadTest.txt" );
        try {
            Collection<Gene> zyx = geneService.findByOfficialSymbol( "ZYXMMMM" );
            if ( !zyx.isEmpty() )
                geneService.remove( zyx );
            zyx = geneService.findByOfficialSymbol( "ZXDCMMMM" );
            if ( !zyx.isEmpty() )
                geneService.remove( zyx );
            zyx = geneService.findByOfficialSymbol( "ZYXIN" );
            if ( !zyx.isEmpty() )
                geneService.remove( zyx );
        } catch ( Exception e ) {
            log.warn( e );
            // this test may fail if we don't start with an empty database.
        }
    }

    /*
     * Tests that if the file is not in the correct format of 3 tab delimited fields exception thrown.
     */
    @Test
    public void testFileIncorrectFormatIllegalArgumentExceptionException() {
        try {
            String ncbiFile = FileTools.resourceToPath( "/data/loader/genome/gene/geneloadtest.txt" );
            externalFileGeneLoaderService.load( ncbiFile, TAXON_NAME );
        } catch ( IOException e ) {
            assertEquals( "Illegal format, expected three columns, got 13", e.getMessage() );
        } catch ( Exception e ) {
            fail();
        }
    }

    /*
     * Test method for
     * {@link ubic.gemma.core.loader.genome.gene.ExternalFileGeneLoaderServiceImpl#load(java.lang.String, java.lang.String)}.
     * Tests that 2 genes are loaded sucessfully into Gemma.
     */
    @Test
    public void testLoad() throws Exception {

        int numbersGeneLoaded = externalFileGeneLoaderService.load( geneFile, TAXON_NAME );
        assertEquals( 2, numbersGeneLoaded );
        Collection<Gene> geneCollection = geneService.findByOfficialSymbol( "ZYXMMMM" );
        Gene gene = geneCollection.iterator().next();

        gene = geneService.thaw( gene );

        Collection<GeneProduct> geneProducts = gene.getProducts();

        assertEquals( TAXON_NAME, gene.getTaxon().getCommonName() );
        assertEquals( "ZYXMMMM", gene.getName() ); // same as the symbol
        assertEquals( "ZYXMMMM", gene.getOfficialSymbol() );
        assertEquals( "zyxin", gene.getOfficialName() );

        assertEquals( 1, geneProducts.size() );
        GeneProduct prod = geneProducts.iterator().next();
        assertEquals( "Gene product placeholder", prod.getDescription() );

        // should not involve any updates if we try loading it again.
        numbersGeneLoaded = externalFileGeneLoaderService.load( geneFile, TAXON_NAME );
        assertEquals( 0, numbersGeneLoaded );

        // show that we add a product if the gene exists, but is missing one.
        GeneProduct gp = gene.getProducts().iterator().next();
        gene.getProducts().clear();
        geneProductService.remove( gp );
        geneService.update( gene );
        assertEquals( 0, geneService.getProducts( gene.getId() ).size() );
        numbersGeneLoaded = externalFileGeneLoaderService.load( geneFile, TAXON_NAME );
        assertEquals( 1, numbersGeneLoaded );

        assertEquals( 1, geneService.getProducts( gene.getId() ).size() );

        // should not involve any updates if we try loading it again as it is now complete.
        numbersGeneLoaded = externalFileGeneLoaderService.load( geneFile, TAXON_NAME );
        assertEquals( 0, numbersGeneLoaded );

    }

    /*
     * Tests that if file can not be found file not found exception thrown.
     */
    @Test
    public void testLoadGeneFileNotFoundIOException() throws Exception {
        try {
            externalFileGeneLoaderService.load( "blank", TAXON_NAME );
        } catch ( IOException e ) {
            assertEquals( "Cannot read from blank", e.getMessage() );
        }
    }

    /*
     * Tests that if taxon not stored in system IllegalArgumentExceptionThrown
     */
    @Test
    public void testTaxonNotFoundIllegalArgumentExceptionException() {
        try {
            externalFileGeneLoaderService.load( geneFile, "fishy" );
        } catch ( IllegalArgumentException e ) {
            assertEquals( "No taxon with common name fishy found", e.getMessage() );
        } catch ( Exception e ) {
            fail();
        }
    }

}
