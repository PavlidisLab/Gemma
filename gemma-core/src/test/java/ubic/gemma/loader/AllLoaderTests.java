/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.loader;

import junit.framework.Test;
import junit.framework.TestSuite;
import ubic.gemma.loader.association.NCBIGene2GOAssociationParserTest;
import ubic.gemma.loader.entrez.EutilFetchTest;
import ubic.gemma.loader.entrez.pubmed.AllPubMedTests;
import ubic.gemma.loader.expression.AllExpressionLoaderTests;
import ubic.gemma.loader.genome.AllGenomeTests;
import ubic.gemma.loader.util.HttpFetcherTest;
import ubic.gemma.loader.util.fetcher.AbstractFetcherTest;

/**
 * @author paul
 * @version $Id$
 */
public class AllLoaderTests {
    public static Test suite() {
        TestSuite suite = new TestSuite( "Tests for gemma-core loaders" );
        suite.addTest( AllGenomeTests.suite() );
        suite.addTest( AllPubMedTests.suite() );
        suite.addTestSuite( EutilFetchTest.class );
        suite.addTestSuite( NCBIGene2GOAssociationParserTest.class );
        suite.addTest( AllExpressionLoaderTests.suite() );
        suite.addTestSuite( HttpFetcherTest.class );
        suite.addTestSuite( AbstractFetcherTest.class );
        return suite;
    }
}
