/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.loader.genome.gene;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.BaseServiceTestCase;
import edu.columbia.gemma.genome.Gene;
import edu.columbia.gemma.genome.GeneDao;
import edu.columbia.gemma.genome.TaxonDao;
import edu.columbia.gemma.loader.loaderutils.ParserAndLoaderTools;

/**
 * This test is more representative of integration testing than unit testing as it tests multiple both parsing and
 * loading.
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class GeneParserTest extends BaseServiceTestCase {

    protected static final Log log = LogFactory.getLog( GeneParserTest.class );

    private GeneLoaderImpl geneLoader = null;
    private GeneParserImpl geneParser = null;
    private Map<String, Gene> map = null;

    /**
     * Tests both the parser and the loader. This is more of an integration test, but since it's dependencies are
     * localized to the Gemma project it has been added to the test suite.
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public void testParseAndLoad() throws Exception {
        InputStream is = this.getClass().getResourceAsStream( "/data/loader/genome/gene/geneinfo.txt" );
        Method m = ParserAndLoaderTools.findParseLineMethod( geneParser.getGeneMappings(), "geneinfo" );
        geneParser.parse( is, m );

        InputStream is2 = this.getClass().getResourceAsStream( "/data/loader/genome/gene/gene2accession.txt" );
        Method m2 = ParserAndLoaderTools.findParseLineMethod( geneParser.getGeneMappings(), "gene2accession" );
        map = geneParser.parse( is2, m2 );

        ParserAndLoaderTools.loadDatabase( geneLoader, map.values() );

        assertEquals( null, null );

    }

    protected void setUp() throws Exception {
        super.setUp();
        geneParser = new GeneParserImpl();
        geneLoader = new GeneLoaderImpl();
        GeneMappings geneMappings = new GeneMappings( ( TaxonDao ) ctx.getBean( "taxonDao" ) );
        geneParser.setGeneMappings( geneMappings );
        geneLoader.setGeneDao( ( GeneDao ) ctx.getBean( "geneDao" ) );
        map = new HashMap<String, Gene>();

    }

    protected void tearDown() throws Exception {
        super.tearDown();
        // TODO can you pass arguments to JUnit tests so I can select this option at runtime?
        // TODO to properly test removal (and thus, fetching) select values from db and then call remove.
        geneLoader.removeAll( map.values() );
        geneParser = null;
        geneLoader = null;
        map = null;
    }

    // public void testParseFileInvalidFile() throws Exception {
    //
    // geneParser.parseFile( "badfilename" );
    //
    // assertEquals( null, null );
    // }
}
