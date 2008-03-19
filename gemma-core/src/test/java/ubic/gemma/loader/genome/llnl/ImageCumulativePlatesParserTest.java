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
package ubic.gemma.loader.genome.llnl;

import java.io.InputStream;
import java.util.Collection;

import junit.framework.TestCase;
import ubic.gemma.model.genome.biosequence.BioSequence;

/**
 * @author pavlidis
 * @version $Id$
 */
public class ImageCumulativePlatesParserTest extends TestCase {

    InputStream is;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        is = this.getClass().getResourceAsStream( "/data/loader/genome/cumulative.plates.test.txt" );
    }

    public void testParseInputStream() throws Exception {
        ImageCumulativePlatesParser parser = new ImageCumulativePlatesParser();
        parser.parse( is );
        Collection<BioSequence> bs = parser.getResults();
        assertEquals( 418, bs.size() );
    }

    @Override
    protected void tearDown() throws Exception {
        is.close();
    }

}
