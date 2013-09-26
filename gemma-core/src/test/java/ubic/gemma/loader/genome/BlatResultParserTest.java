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

import java.io.InputStream;
import java.util.Collection;

import junit.framework.TestCase;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;

/**
 * @author pavlidis
 * @version $Id$
 */
public class BlatResultParserTest extends TestCase {

    public void testParseInputStreamNoheader() throws Exception {
        try (InputStream is = this.getClass().getResourceAsStream( "/data/loader/genome/blatResult.noheader.txt" );) {
            BlatResultParser bp = new BlatResultParser();
            bp.parse( is );
            Collection<BlatResult> res = bp.getResults();
            assertEquals( 18, res.size() );
        }
    }

    /*
     * Test method for 'ubic.gemma.loader.loaderutils.BasicLineParser.parse(InputStream)'
     */
    public void testParseInputStreamWheader() throws Exception {
        try (InputStream is = this.getClass().getResourceAsStream( "/data/loader/genome/blatResult.wheader.txt" );) {
            BlatResultParser bp = new BlatResultParser();
            bp.parse( is );
            Collection<BlatResult> res = bp.getResults();
            assertEquals( 15, res.size() );
        }

    }

}
