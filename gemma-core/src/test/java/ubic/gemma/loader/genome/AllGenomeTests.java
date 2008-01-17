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
package ubic.gemma.loader.genome;

import ubic.gemma.loader.genome.gene.GeneServiceIntegrationTest;
import ubic.gemma.loader.genome.gene.SwissProtParserTest;
import ubic.gemma.loader.genome.gene.ncbi.NCBIGeneIntegrationTest;
import ubic.gemma.loader.genome.gene.ncbi.NCBIGeneParserTest;
import ubic.gemma.loader.genome.goldenpath.GoldenPathBioSequenceLoaderTest;
import ubic.gemma.loader.genome.llnl.ImageCumulativePlatesFetcherTest;
import ubic.gemma.loader.genome.llnl.ImageCumulativePlatesLoaderTest;
import ubic.gemma.loader.genome.llnl.ImageCumulativePlatesParserTest;
import ubic.gemma.loader.genome.taxon.SupportedTaxaTest;
import ubic.gemma.loader.genome.taxon.TaxonFetcherTest;
import ubic.gemma.loader.genome.taxon.TaxonLoaderTest;
import ubic.gemma.loader.genome.taxon.TaxonParserTest;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests for the genome package.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class AllGenomeTests {

    public static Test suite() {
        TestSuite suite = new TestSuite( "Tests for ubic.gemma.loader.genome" );
        // $JUnit-BEGIN$
        suite.addTestSuite( BlatResultParserTest.class );
        suite.addTestSuite( FastaParserTest.class );
        suite.addTestSuite( NCBIGeneIntegrationTest.class );
        suite.addTestSuite( NCBIGeneParserTest.class );
        suite.addTestSuite( GoldenPathBioSequenceLoaderTest.class );
        suite.addTestSuite( ImageCumulativePlatesFetcherTest.class );
        suite.addTestSuite( ImageCumulativePlatesLoaderTest.class );
        suite.addTestSuite( ImageCumulativePlatesParserTest.class );
        suite.addTestSuite( TaxonFetcherTest.class );
        suite.addTestSuite( TaxonLoaderTest.class );
        suite.addTestSuite( TaxonParserTest.class );
        suite.addTestSuite( SupportedTaxaTest.class );
        suite.addTestSuite( SimpleFastaCmdTest.class );
        suite.addTestSuite( GffParserTest.class );
        suite.addTestSuite( GeneServiceIntegrationTest.class );
        suite.addTestSuite( SwissProtParserTest.class );
        // $JUnit-END$
        return suite;
    }

}
