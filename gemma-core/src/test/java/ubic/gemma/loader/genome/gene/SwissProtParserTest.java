/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.loader.genome.gene;

import java.io.InputStream;
import java.util.Collection;

import junit.framework.TestCase;

/**
 * @author pavlidis
 * @version $Id$
 */
public class SwissProtParserTest extends TestCase {

    public void testParse() throws Exception {

        try (InputStream is = this.getClass().getResourceAsStream(
                "/data/loader/genome/gene/uniprot_sprot_human.sample.dat" );) {
            assertNotNull( is );
            SwissProtParser p = new SwissProtParser();
            p.parse( is );
            is.close();
            Collection<?> results = p.getResults();

            /*
             * Parser not fully implemented, doesn't return anything.
             */
            assertEquals( 0, results.size() );
        }

    }

}
