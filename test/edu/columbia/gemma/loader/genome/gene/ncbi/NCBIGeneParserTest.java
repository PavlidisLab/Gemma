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
package edu.columbia.gemma.loader.genome.gene.ncbi;

import java.io.InputStream;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This test is more representative of integration testing than unit testing as it tests multiple both parsing and
 * loading.
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2006 University of British Columbia
 * 
 * @author keshav
 * @version $Id$
 */
public class NCBIGeneParserTest extends TestCase {

    protected static final Log log = LogFactory.getLog( NCBIGeneParserTest.class );

    public void testParseGeneInfo() throws Exception {
        InputStream is = this.getClass().getResourceAsStream( "/data/loader/genome/gene/geneinfo.txt" );
        NcbiGeneInfoParser ngip = new NcbiGeneInfoParser();
        ngip.parse( is );
        assertTrue( ngip.getResults().size() == 20 );
    }

    public void testParseGene2Accession() throws Exception {
        InputStream is = this.getClass().getResourceAsStream( "/data/loader/genome/gene/gene2accession.txt" );
        NcbiGene2AccessionParser ngip = new NcbiGene2AccessionParser();
        ngip.parse( is );
        assertTrue( ngip.getResults().size() == 20 );
    }

    protected void setUp() throws Exception {
        super.setUp();

    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

}
