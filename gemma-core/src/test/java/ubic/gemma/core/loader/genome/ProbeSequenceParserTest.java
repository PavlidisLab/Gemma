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
package ubic.gemma.core.loader.genome;

import junit.framework.TestCase;

import java.io.InputStream;

/**
 * @author paul
 */
public class ProbeSequenceParserTest extends TestCase {

    public void testParseInputStream() throws Exception {
        ProbeSequenceParser p = new ProbeSequenceParser();
        try (InputStream i = this.getClass().getResourceAsStream( "/data/loader/genome/probesequence.test.txt" )) {
            p.parse( i );
        }
        TestCase.assertNotNull( p.get( "117" ) );
        TestCase.assertEquals( "GE59978", p.get( "117" ).getName() );
        TestCase.assertEquals( "ATGGGTGCTTATTGGTATTGTCTCCTGGGG", p.get( "117" ).getSequence() );
        TestCase.assertEquals( 9, p.getResults().size() );

        TestCase.assertEquals( "GE59979", p.get( "118" ).getName() );
        TestCase.assertEquals( "ATGGGTGCTTATTGGTATTGTCTCCTGGGG", p.get( "118" ).getSequence() );
        TestCase.assertEquals( 9, p.getResults().size() );
    }

}
