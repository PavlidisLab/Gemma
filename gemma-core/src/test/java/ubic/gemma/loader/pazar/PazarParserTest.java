/*
 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.loader.pazar;

import java.io.InputStream;
import java.util.Collection;

import junit.framework.TestCase;

import org.junit.Test;

import ubic.gemma.loader.pazar.model.PazarRecord;

/**
 * @author paul
 * @version $Id$
 */
public class PazarParserTest extends TestCase {

    @Test
    public void testParse() throws Exception {

        PazarParser p = new PazarParser();
        InputStream is = this.getClass().getResourceAsStream( "/data/loader/pazar-test.txt" );
        assertNotNull( is );
        p.parse( is );
        Collection<PazarRecord> recs = p.getResults();
        assertEquals( 49, recs.size() );

    }

}
