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
package ubic.gemma.core.loader.genome.taxon;

import junit.framework.TestCase;
import ubic.gemma.model.genome.Taxon;

import java.io.InputStream;
import java.util.Collection;

/**
 * @author pavlidis
 */
public class TaxonParserTest extends TestCase {

    private InputStream is;

    public void testParseInputStream() throws Exception {
        TaxonParser tp = new TaxonParser();
        tp.parse( is );
        Collection<Taxon> results = tp.getResults();
        TestCase.assertEquals( 75, results.size() );
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        is = this.getClass().getResourceAsStream( "/data/loader/genome/taxon.names.dmp.sample.txt" );
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        is.close();
    }

}
