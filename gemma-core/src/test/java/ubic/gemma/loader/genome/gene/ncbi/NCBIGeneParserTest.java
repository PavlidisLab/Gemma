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

import java.io.InputStream;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.zip.GZIPInputStream;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.loader.genome.gene.ncbi.model.NCBIGeneInfo;

/**
 * @author keshav
 * @version $Id$
 */
public class NCBIGeneParserTest extends TestCase {

    protected static final Log log = LogFactory.getLog( NCBIGeneParserTest.class );

    public void testParseGene2Accession() throws Exception {
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/genome/gene/gene2accession.sample.gz" ) );
        NcbiGene2AccessionParser ngip = new NcbiGene2AccessionParser();
        ngip.geneInfo = new HashMap<String, NCBIGeneInfo>();
        ngip.parse( is, new ArrayBlockingQueue<NcbiGeneData>( 10 ) );
        assertEquals( 100, ngip.getCount() );
    }

    public void testParseGeneInfo() throws Exception {
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/genome/gene/gene_info.sample.gz" ) );
        NcbiGeneInfoParser ngip = new NcbiGeneInfoParser();
        ngip.setFilter( false );
        ngip.parse( is );
        assertEquals( 99, ngip.getResults().size() );
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

}
