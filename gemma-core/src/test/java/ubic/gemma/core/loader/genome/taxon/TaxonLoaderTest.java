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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ubic.gemma.core.util.test.BaseSpringContextTest;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;

/**
 * @author pavlidis
 *
 */
public class TaxonLoaderTest extends BaseSpringContextTest {
    private InputStream is;

    @After
    public void onTearDownInTransaction() throws Exception {
        is.close();
    }

    @Before
    public void setUp() throws Exception {
        is = this.getClass().getResourceAsStream( "/data/loader/genome/taxon.names.dmp.sample.txt" );
    }

    @Test
    public void testLoadInputStream() throws Exception {
        TaxonLoader tl = new TaxonLoader();
        tl.setPersisterHelper( persisterHelper );
        int actualValue = tl.load( is );
        assertEquals( 75, actualValue );
    }
}
