/*
 * The gemma-core project
 * 
 * Copyright (c) 2018 University of British Columbia
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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * TODO Document Me
 * 
 * @author paul
 */
public class NCBIGeneHistoryParserTest {

    @Test
    public void testParseA() throws Exception {

        NcbiGeneHistoryParser parser = new NcbiGeneHistoryParser();
        parser.parse( NCBIGeneHistoryParserTest.class.getResourceAsStream( "/data/loader/genome/gene/gene_history.human.sample" ) );

        String r = parser.discontinuedIdForSymbol( "ACTBP12", 9606 );
        assertEquals( "77", r );
    }
    
    
    @Test
    public void testParseB() throws Exception {

        NcbiGeneHistoryParser parser = new NcbiGeneHistoryParser();
        parser.parse( NCBIGeneHistoryParserTest.class.getResourceAsStream( "/data/loader/genome/gene/gene_history.rat.sample" ) );

        String r = parser.discontinuedIdForSymbol( "Olr617-ps", 10116 );
        assertEquals( "405484", r );
    }

}
