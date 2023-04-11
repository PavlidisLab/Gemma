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
package ubic.gemma.core.loader.genome.gene.ncbi;

import junit.framework.TestCase;
import org.springframework.core.io.ClassPathResource;
import ubic.gemma.core.loader.genome.gene.ncbi.model.NCBIGeneInfo;

import java.io.InputStream;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.zip.GZIPInputStream;

/**
 * @author keshav
 */
public class NCBIGeneParserTest extends TestCase {

    public void testParseGene2Accession() throws Exception {
        InputStream is = new GZIPInputStream( new ClassPathResource( "/data/loader/genome/gene/gene2accession.sample.gz" ).getInputStream() );
        NcbiGene2AccessionParser ncbiGene2AccessionParser = new NcbiGene2AccessionParser();
        ncbiGene2AccessionParser.geneInfo = new HashMap<>();
        ncbiGene2AccessionParser.parse( is, new ArrayBlockingQueue<NcbiGeneData>( 10 ) );
        TestCase.assertEquals( 100, ncbiGene2AccessionParser.getCount() );
    }

    public void testParseGeneInfo() throws Exception {
        InputStream is = new GZIPInputStream( new ClassPathResource( "/data/loader/genome/gene/gene_info.sample.gz" ).getInputStream() );
        NcbiGeneInfoParser ncbiGeneInfoParser = new NcbiGeneInfoParser();
        ncbiGeneInfoParser.setFilter( false );
        ncbiGeneInfoParser.parse( is );
        TestCase.assertEquals( 99, ncbiGeneInfoParser.getResults().size() );
        NCBIGeneInfo n = ncbiGeneInfoParser.getResults().iterator().next();
        assertNotNull( n.getGeneType() );
    }

}
