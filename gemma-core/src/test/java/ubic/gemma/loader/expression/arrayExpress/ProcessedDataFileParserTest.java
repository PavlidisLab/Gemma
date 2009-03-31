/*
 * The Gemma project Copyright (c) 2009 University of British Columbia Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package ubic.gemma.loader.expression.arrayExpress;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

/**
 * @author paul
 * @version $Id$
 */
public class ProcessedDataFileParserTest extends TestCase {

    /**
     * @throws Exception
     */
    public final void testParseA() throws Exception {
        InputStream is = getClass().getResourceAsStream(
                "/data/loader/expression/mage/E-TABM-631.processedE-TABM-631-processed-data-1721516524.test.txt" );

        ProcessedDataFileParser p = new ProcessedDataFileParser();
        p.parse( is );

        assertTrue( p.isUsingReporters() );

        Object[] samples = p.getSamples();

        assertEquals( 27, samples.length );

        Map<String, Map<String, List<String>>> map = p.getMap();
        assertEquals( 234, map.size() ); // DesignElements
        for ( String s : map.keySet() ) {
            Map<String, List<String>> map2 = map.get( s );
            assertEquals( 7, map2.keySet().size() ); // QTs

            for ( String ss : map2.keySet() ) {
                assertEquals( 27, map2.get( ss ).size() ); // data points
            }
        }

    }

}
