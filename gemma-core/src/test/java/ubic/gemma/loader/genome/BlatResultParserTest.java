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

import junit.framework.TestCase;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2006 University of British Columbia
 * 
 * @author pavlidis
 * @version $Id$
 */
public class BlatResultParserTest extends TestCase {

    BlatResultParser bp;

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        bp = new BlatResultParser();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Test method for 'ubic.gemma.loader.loaderutils.BasicLineParser.parse(InputStream)'
     */
    public void testParseInputStreamWheader() throws Exception {
        InputStream is = this.getClass().getResourceAsStream( "/data/loader/genome/blatResult.wheader.txt" );
        bp.parse( is );
        bp.getResults();

    }

    public void testParseInputStreamNoheader() throws Exception {
        InputStream is = this.getClass().getResourceAsStream( "/data/loader/genome/blatResult.noheader.txt" );
        bp.parse( is );
        bp.getResults();
    }

}
